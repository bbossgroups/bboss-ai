package org.frameworkset.spi.ai.tools;
/**
 * Copyright 2026 bboss
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.apache.commons.lang3.StringUtils;
import org.frameworkset.spi.ai.state.Task;
import org.frameworkset.spi.ai.model.ChatObject;
import org.frameworkset.spi.ai.model.annotation.Tool;
import org.frameworkset.spi.ai.model.annotation.ToolParam;
import org.frameworkset.spi.ai.state.TaskContextState;
import org.frameworkset.spi.ai.tool.AgentTraceHolder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Built-in task-list tool that lets the agent maintain a structured todo list for the current
 * session, mirroring opencode's {@code todowrite}.
 *
 * <p>The single {@code todo_write} tool uses <b>full-list-replace</b> semantics: the model passes
 * the <i>entire</i> updated list every call and the tool rebuilds
 * {@link } from it. This is intentionally simpler than a granular
 * create/update/delete surface — the model never has to reason about ids or partial diffs, and the
 * latest complete list is always the source of truth (and is re-surfaced every turn by
 * {@code TaskReminderMiddleware}).
 *
 * <p>The list is persisted automatically: {@code AgentState.tasksContext} is part of the agent
 * state that {@code ReActAgent} flushes to its {@code AgentStateStore} after every call.
 *
 * <p>This tool is independent from the harness {@code TaskTool} ({@code task_output} /
 * {@code task_cancel} / {@code task_list}), which manages background <i>subagent</i> runs — a
 * different layer that this tool neither reads nor writes.
 */
public class TodoTools {
//	private static final String DESCRIPTION =
//			"""
//			Create and maintain a structured task list for the current session. Tracks progress,
//			organizes multi-step work, and surfaces status to the user. Pass the COMPLETE updated
//			list every call — this tool replaces the whole list (it does not merge).
//
//			## When to use
//			Use proactively when:
//			- The task needs 3+ distinct steps or actions
//			- The work is non-trivial and benefits from planning
//			- The user gives multiple tasks (numbered or comma-separated) or asks for a todo list
//			- New instructions arrive — capture them as todos
//			- You start a task — mark it in_progress (only one at a time) before working
//			- You finish a task — mark it completed and add any follow-ups you discovered
//
//			## When NOT to use
//			Skip when the work is a single straightforward step (<3 trivial steps), or the request
//			is purely informational/conversational, or tracking adds no value.
//
//			## States
//			- pending     — not started
//			- in_progress — actively working (exactly ONE at a time)
//			- completed   — finished successfully
//
//			## Rules
//			- Update status in real time; do not batch completions.
//			- Mark completed only after the work is actually done, including any required
//			  verification — never based on intent.
//			- Keep exactly one task in_progress while work remains.
//			- If blocked or partial, keep the task in_progress and add a follow-up todo describing
//			  the blocker.
//			- Items must be specific and actionable; break large work into smaller steps.
//			- To drop a task that is no longer needed, simply omit it from the list.
//			""";
private static final String DESCRIPTION =
		"Create and maintain a structured task list for the current session. Tracks progress,\n" +
		"organizes multi-step work, and surfaces status to the user. Pass the COMPLETE updated\n" +
		"list every call — this tool replaces the whole list (it does not merge).\n" +
		"\n" +
		"## When to use\n" +
		"Use proactively when:\n" +
		"- The task needs 3+ distinct steps or actions\n" +
		"- The work is non-trivial and benefits from planning\n" +
		"- The user gives multiple tasks (numbered or comma-separated) or asks for a todo list\n" +
		"- New instructions arrive — capture them as todos\n" +
		"- You start a task — mark it in_progress (only one at a time) before working\n" +
		"- You finish a task — mark it completed and add any follow-ups you discovered\n" +
		"\n" +
		"## When NOT to use\n" +
		"Skip when the work is a single straightforward step (<3 trivial steps), or the request\n" +
		"is purely informational/conversational, or tracking adds no value.\n" +
		"\n" +
		"## States\n" +
		"- pending     — not started\n" +
		"- in_progress — actively working (exactly ONE at a time)\n" +
		"- completed   — finished successfully\n" +
		"\n" +
		"## Rules\n" +
		"- Update status in real time; do not batch completions.\n" +
		"- Mark completed only after the work is actually done, including any required\n" +
		"  verification — never based on intent.\n" +
		"- Keep exactly one task in_progress while work remains.\n" +
		"- If blocked or partial, keep the task in_progress and add a follow-up todo describing\n" +
		"  the blocker.\n" +
		"- Items must be specific and actionable; break large work into smaller steps.\n" +
		"- To drop a task that is no longer needed, simply omit it from the list.";
	
	/**
	 * Replaces the session task list with {@code todos}.
	 *
	 * <p>{@code state} is auto-injected by the framework ({@code stateInjected = true}) and is not
	 * part of the model-visible schema.
	 */
	@Tool(
			name = "todo_write",
			description = DESCRIPTION)
	public String todoWrite(
			@ToolParam(
					name = "todos",
					description =
							"The COMPLETE updated todo list. Replaces the existing list"
									+ " entirely.",
					required = true)
			List<TodoItem> todos) {
//		if (state == null) {
//			return "Error: agent state unavailable; cannot persist todo list.";
//		}
		List<TodoItem> items = todos == null ? new ArrayList<>() : todos;
		
		// Validate the single-in_progress invariant before mutating anything (reject, don't
		// downgrade): the model is responsible for keeping exactly one active task.
		int inProgress = 0;
		for (TodoItem item : items) {
			Task.State parsed = parseState(item.getStatus());
			if (parsed == null) {
				return "Error: invalid status '"
						+ item.getStatus()
						+ "' for todo '"
						+ safe(item.getContent())
						+ "'. Allowed: pending, in_progress, completed.";
			}
			if (parsed == Task.State.IN_PROGRESS) {
				inProgress++;
			}
			if (item.getContent() == null || item.getContent().isEmpty()) {
				return "Error: every todo must have non-blank content.";
			}
		}
		if (inProgress > 1) {
			return "Error: at most one task may be in_progress at a time, but "
					+ inProgress
					+ " were provided. Keep exactly one in_progress.";
		}
		
		ChatObject chatObject = AgentTraceHolder.getChatObject();
		
		TaskContextState ctx = chatObject.getChatContext().getTaskContextState();
		// Preserve ids/created_at for tasks whose content matches an existing one (best-effort),
		// so stable identifiers survive across full-list rewrites.
		Map<String, Task> byContent = new LinkedHashMap<>();
		for (Task existing : ctx.getTasks()) {
			byContent.putIfAbsent(existing.getSubject(), existing);
		}
		
		List<Task> rebuilt = new ArrayList<>(items.size());
		for (TodoItem item : items) {
			Task.State parsed = parseState(item.getStatus());
			Task prior = byContent.get(item.getContent());
			Map<String, Object> metadata = new LinkedHashMap<>();
			if (prior != null && prior.getMetadata() != null) {
				metadata.putAll(prior.getMetadata());
			}
			String priority = normalizePriority(item.getPriority());
			if (priority != null) {
				metadata.put("priority", priority);
			}
			Task task = new Task();
			task.setSubject(item.getContent());
			task.setDescription(item.getContent());
			task.setState(parsed);
			task.setMetadata(metadata.isEmpty() ? null : metadata);
			if (prior != null) {
				task.setId(prior.getId());
				task.setCreatedAt(prior.getCreatedAt());
			}
			rebuilt.add(task);
		}
		
		List<Task> live = ctx.getTasks();
		live.clear();
		live.addAll(rebuilt);
		
		return render(rebuilt);
	}
	
	private static String render(List<Task> tasks) {
		if (tasks.isEmpty()) {
			return "Todo list cleared (0 items).";
		}
		long open = tasks.stream().filter(t -> t.getState() != Task.State.COMPLETED).count();
		StringBuilder sb = new StringBuilder();
		sb.append(open).append(" open todo(s):\n");
		for (Task t : tasks) {
			sb.append(marker(t.getState())).append(' ').append(t.getSubject());
			Object priority = t.getMetadata() == null ? null : t.getMetadata().get("priority");
			if (priority != null) {
				sb.append(" (priority: ").append(priority).append(')');
			}
			sb.append('\n');
		}
		return StringUtils.stripEnd(sb.toString(), null);
//		return sb.toString().stripTrailing();
	}
	
	static String marker(Task.State state) {
		switch (state) {
			case COMPLETED : return "- [x]";
			case IN_PROGRESS : return "- [~]";
			case PENDING : return "- [ ]";
		};
		return "?";
	}
	
	private static Task.State parseState(String wire) {
		if (wire == null || wire.isEmpty()) {
			return Task.State.PENDING;
		}
		try {
			return Task.State.fromWire(wire.trim());
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
	
	private static String normalizePriority(String priority) {
		if (priority == null || priority.isEmpty()) {
			return null;
		}
		String p = priority.trim().toLowerCase();
		switch (p) {
			case "high":
			case "medium":
			case "low" : return p;
			default : return null;
		}
	}
	
	private static String safe(String s) {
		return s == null ? "" : s;
	}
	
	/** A single todo item as supplied by the model. */
	public static final class TodoItem {
		@ToolParam(name = "content",description = "Brief, specific, actionable description of the task.",required = true)
		private String content;
		@ToolParam(name = "status",description = "One of: pending, in_progress, completed.",enumValues = {"pending", "in_progress", "completed"},required = true)
		private String status;
		@ToolParam(name = "priority",description = "Optional priority: high, medium, or low.",enumValues = {"high", "medium", "low"})
		private String priority;
		
		public TodoItem() {
		}		
		
		public String getContent() {
			return content;
		}
		public String getStatus() {
			return status;
		}
		public String getPriority() {
			return priority;
		}
	}
}
