/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.frameworkset.spi.ai.permission;


import com.fasterxml.jackson.annotation.JsonIgnore;
import org.frameworkset.spi.ai.context.AgentRuntimeContext;
import org.frameworkset.spi.ai.context.ChatContext;
import org.frameworkset.spi.ai.model.FunctionTool;
import org.frameworkset.spi.ai.tool.ToolBase;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * Evaluates tool execution requests against configured permission rules.
 *
 * <p>Evaluation order:
 *
 * <ol>
 *   <li>Tool-level deny rules (highest priority).
 *   <li>Tool-level ask rules.
 *   <li>Tool-specific checks (bypass-immune): EXPLORE/ACCEPT_EDITS read-only handling, dangerous
 *       path checks, plus whatever the tool's own {link ToolPermissionManager#checkPermissions} returns.
 *   <li>Tool-level allow rules.
 *   <li>{@link PermissionMode#BYPASS} fallback.
 *   <li>Default ASK (converted to DENY under {link PermissionMode#DONT_ASK}).
 * </ol>
 *
 * <p>The engine snapshots rules from the supplied {link PermissionContextState} into its own mutable
 * tables on construction; the original context is never mutated. Use {@link #addRule} to extend
 * the engine's rule set at runtime.
 */
public final class PermissionEngine {

	@JsonIgnore
    private final AgentRuntimeContext context;
	@JsonIgnore
	private ChatContext chatContext	;
    private final Map<String, List<PermissionRule>> allowRules;
    private final Map<String, List<PermissionRule>> denyRules;
    private final Map<String, List<PermissionRule>> askRules;

    /**
     * Creates an engine seeded from the given context's rules and mode.
     *
     * @param chatContext permission context providing mode, working directories, and initial rules
     */
    public PermissionEngine(ChatContext chatContext) {
		this.context = chatContext.getAgentRuntimeContext();
		this.chatContext = chatContext;
//        this.context = Objects.requireNonNull(context, "context must not be null");
        this.allowRules = copyMutable(context.getAllowRules());
        this.denyRules = copyMutable(context.getDenyRules());
        this.askRules = copyMutable(context.getAskRules());
    }

    private static Map<String, List<PermissionRule>> copyMutable(
            Map<String, List<PermissionRule>> source) {
        Map<String, List<PermissionRule>> out = new HashMap<>();
        if (source != null) {
            for (Map.Entry<String, List<PermissionRule>> e : source.entrySet()) {
                out.put(e.getKey(), new ArrayList<>(e.getValue()));
            }
        }
        return out;
    }

    /**
     * Returns the underlying context (mode and working directories are read from it).
     *
     * @return the immutable context this engine was constructed with
     */
    public AgentRuntimeContext getContext() {
        return context;
    }

    /**
     * Adds a rule to the engine's internal rule set.
     *
     * <p>The rule is routed by its {link PermissionRule#behavior()}: ALLOW/DENY/ASK rules are
     * appended to the engine's allow/deny/ask tables; PASSTHROUGH rules are ignored.
     *
     * @param rule the rule to add; must be non-null
     */
    public void addRule(PermissionRule rule) {
        Objects.requireNonNull(rule, "rule must not be null");
        switch (rule.getBehavior()) {
			case ALLOW:
                    allowRules.computeIfAbsent(rule.getToolName(), k -> new ArrayList<>()).add(rule);
					break;
			case DENY:
                    denyRules.computeIfAbsent(rule.getToolName(), k -> new ArrayList<>()).add(rule);
					break;
			case ASK: askRules.computeIfAbsent(rule.getToolName(), k -> new ArrayList<>()).add(rule);
					break;	
			case PASSTHROUGH : {
                // PASSTHROUGH rules are not stored; they signal "defer to engine".
            }
        }
    }

    /** Read-only view of the engine's current allow-rule table. */
    public Map<String, List<PermissionRule>> getAllowRules() {
        return unmodifiableSnapshot(allowRules);
    }

    /** Read-only view of the engine's current deny-rule table. */
    public Map<String, List<PermissionRule>> getDenyRules() {
        return unmodifiableSnapshot(denyRules);
    }

    /** Read-only view of the engine's current ask-rule table. */
    public Map<String, List<PermissionRule>> getAskRules() {
        return unmodifiableSnapshot(askRules);
    }

    private static Map<String, List<PermissionRule>> unmodifiableSnapshot(
            Map<String, List<PermissionRule>> source) {
        Map<String, List<PermissionRule>> snapshot = new HashMap<>();
        for (Map.Entry<String, List<PermissionRule>> e : source.entrySet()) {
            snapshot.put(e.getKey(),new ArrayList<>(e.getValue()));
        }
        return Collections.unmodifiableMap(snapshot);
    }

    /**
     * Resolves a permission decision for the given tool invocation.
     *
     * @param tool the tool being called
     * @param toolInput the input map the tool will receive
     * @return a Mono emitting the resolved {@link PermissionDecision}
     */
    public PermissionDecision checkPermission(FunctionTool tool, Map<String, Object> toolInput) {
		Objects.requireNonNull(tool, "tool must not be null");
		Map<String, Object> input = toolInput == null ? Collections.emptyMap() : toolInput;
		
		// 1. Deny rules (highest priority)
		PermissionDecision denyDecision = checkDenyRules(tool, input);
		if (denyDecision != null) {
			return denyDecision;
		}
		
		// 2. Ask rules
		PermissionDecision askDecision = checkAskRules(tool, input);
		if (askDecision != null) {
			return askDecision.withSuggestedRules(tool.generateSuggestions(input));
		}
		
		// 3. Tool-specific check (bypass-immune)
		PermissionDecision toolCheckPermissionsDecision = toolCheckPermissions(tool, input);
		if (toolCheckPermissionsDecision != null){
			if (toolCheckPermissionsDecision.getBehavior() == PermissionBehavior.DENY)
				return toolCheckPermissionsDecision;
			if (toolCheckPermissionsDecision.getBehavior() == PermissionBehavior.ALLOW) {
				return toolCheckPermissionsDecision;
			}
			if (toolCheckPermissionsDecision.getBehavior() == PermissionBehavior.ASK
					&& toolCheckPermissionsDecision.getDecisionReason() != null
					&& toolCheckPermissionsDecision
					.getDecisionReason()
					.toLowerCase(Locale.ROOT)
					.contains("safety")) {
				return toolCheckPermissionsDecision.withSuggestedRules(tool.generateSuggestions(input));
			}
			return continueAfterToolCheck(tool, input);
	}
	else	
	{
		return continueAfterToolCheck(tool, input);
	}
//                .flatMap(
//                        toolDecision -> {
//                            if (toolDecision.getBehavior() == PermissionBehavior.DENY) {
//                                return Mono.just(toolDecision);
//                            }
//                            if (toolDecision.getBehavior() == PermissionBehavior.ALLOW) {
//                                return Mono.just(toolDecision);
//                            }
//                            if (toolDecision.getBehavior() == PermissionBehavior.ASK
//                                    && toolDecision.getDecisionReason() != null
//                                    && toolDecision
//                                            .getDecisionReason()
//                                            .toLowerCase(Locale.ROOT)
//                                            .contains("safety")) {
//                                return Mono.just(
//                                        toolDecision.withSuggestedRules(
//                                                tool.generateSuggestions(input)));
//                            }
//                            return continueAfterToolCheck(tool, input);
//                        })
//                .switchIfEmpty(Mono.defer(() -> continueAfterToolCheck(tool, input)));
    }

    private PermissionDecision continueAfterToolCheck(
			FunctionTool tool, Map<String, Object> input) {
        // 4. Allow rules
        PermissionDecision allowDecision = checkAllowRules(tool, input);
        if (allowDecision != null) {
            return allowDecision;
        }

        // 5. BYPASS fallback
        if (context.getMode() == PermissionMode.BYPASS) {
			PermissionDecision permissionDecision = new PermissionDecision();
			permissionDecision.setBehavior(PermissionBehavior.ALLOW);
			permissionDecision.setMessage("Permission granted for " + tool.getFunctionName() + " (bypass mode)");
			permissionDecision.setDecisionReason("Bypass mode allows all operations");	
			return permissionDecision;
        }

        // 6. Default (ASK, or DENY under DONT_ASK)
        return  defaultDecisionAsk(tool.getFunctionName())
                        .withSuggestedRules(tool.generateSuggestions(input));
    }

    /**
     * Runs the tool-specific permission pipeline.
     *
     * <p>EXPLORE / ACCEPT_EDITS read-only handling first, then the tool's own
     * {@link ToolBase#checkPermissions}. Emits empty when the tool returns PASSTHROUGH.
     */
    private PermissionDecision toolCheckPermissions(
            FunctionTool tool, Map<String, Object> input) {
        if (context.getMode() == PermissionMode.EXPLORE
                || context.getMode() == PermissionMode.ACCEPT_EDITS) {
            PermissionDecision modeDecision = checkExploreMode(tool);
            if (modeDecision != null) {
                return modeDecision;
            }
        }
		PermissionDecision modeDecision = tool.checkPermissions(input, chatContext);
		if(modeDecision.getBehavior() == PermissionBehavior.PASSTHROUGH)
			return null;
		else 
			return modeDecision;
//                .flatMap(
//                        decision -> {
//                            if (decision.getBehavior() == PermissionBehavior.PASSTHROUGH) {
//                                return Mono.empty();
//                            }
//                            return Mono.just(decision);
//                        });
    }

    private PermissionDecision checkExploreMode(FunctionTool tool) {
        if (context.getMode() == PermissionMode.EXPLORE) {
            if (tool.isReadOnly()) {
				PermissionDecision permissionDecision = new PermissionDecision();
				permissionDecision.setBehavior(PermissionBehavior.ALLOW);
				permissionDecision.setMessage(
						"Permission granted for "
								+ tool.getFunctionName()
								+ " (explore mode - read-only tool)");
				permissionDecision.setDecisionReason("Explore mode allows read-only operations");	
                return permissionDecision;
            }
            PermissionDecision permissionDecision = new PermissionDecision();
            permissionDecision.setBehavior(PermissionBehavior.DENY);
            permissionDecision.setMessage(
                            "Permission denied for "
                                    + tool.getFunctionName()
                                    + " (explore mode is read-only)");
            permissionDecision.setDecisionReason("Explore mode does not allow modifications");
            return permissionDecision		;
        }
        if (context.getMode() == PermissionMode.ACCEPT_EDITS) {
            if (tool.isReadOnly()) {
				PermissionDecision permissionDecision = new PermissionDecision();	
				permissionDecision.setBehavior(PermissionBehavior.ALLOW);
				permissionDecision.setMessage(
                                "Permission granted for "
                                        + tool.getFunctionName()
                                        + " (accept edits mode - read-only tool)");
				permissionDecision.setDecisionReason("Accept edits mode allows read-only operations");
                return permissionDecision;
            }
        }
        return null;
    }

    private PermissionDecision checkDenyRules(FunctionTool tool, Map<String, Object> input) {
        for (PermissionRule rule : rulesFor(denyRules, tool.getFunctionName())) {
            if (ruleMatches(tool, rule, input)) {
				PermissionDecision permissionDecision = new PermissionDecision();
				permissionDecision.setBehavior(PermissionBehavior.DENY);
				permissionDecision.setMessage("Permission to use " + tool.getFunctionName() + " has been denied");
				permissionDecision.setDecisionReason("Rule: " + rule.getRuleContent());
                     
				return permissionDecision;
            }
        }
        return null;
    }

    private PermissionDecision checkAskRules(FunctionTool tool, Map<String, Object> input) {
        for (PermissionRule rule : rulesFor(askRules, tool.getFunctionName())) {
            if (ruleMatches(tool, rule, input)) {
                PermissionDecision permissionDecision = new PermissionDecision();
				permissionDecision.setBehavior(PermissionBehavior.ASK);
				permissionDecision.setMessage("Permission required for " + tool.getFunctionName());
				permissionDecision.setDecisionReason("Rule: " + rule.getRuleContent());
                     
				return permissionDecision;
            }
        }
        return null;
    }

    private PermissionDecision checkAllowRules(FunctionTool tool, Map<String, Object> input) {
        for (PermissionRule rule : rulesFor(allowRules, tool.getFunctionName())) {
            if (ruleMatches(tool, rule, input)) {
				PermissionDecision permissionDecision = new PermissionDecision();
				permissionDecision.setBehavior(PermissionBehavior.ALLOW);
				permissionDecision.setMessage("Permission granted for " + tool.getFunctionName());
				permissionDecision.setUpdatedInput(input);	
				return permissionDecision	;
            }
        }
        return null;
    }

    private static List<PermissionRule> rulesFor(
            Map<String, List<PermissionRule>> table, String toolName) {
        List<PermissionRule> rules = table.get(toolName);
        return rules == null ? Collections.emptyList() : rules;
    }

    private boolean ruleMatches(FunctionTool tool, PermissionRule rule, Map<String, Object> input) {
        String content = rule.getRuleContent();
        if (content == null || content.isEmpty()) {
            return true;
        }
        return tool.matchRule(content, input);
    }

    private PermissionDecision defaultDecisionAsk(String toolName) {
        if (context.getMode() == PermissionMode.DONT_ASK) {
			PermissionDecision permissionDecision = new PermissionDecision();
			permissionDecision.setBehavior(PermissionBehavior.DENY);	
			permissionDecision.setMessage(
                            "Permission denied for "
                                    + toolName
                                    + " (dont_ask mode - user not available)");
			permissionDecision.setDecisionReason("User is not available to answer permission prompts");
			return permissionDecision;
        }
		PermissionDecision permissionDecision = new PermissionDecision();
		permissionDecision.setBehavior(PermissionBehavior.ASK);
		permissionDecision.setMessage("Permission required for " + toolName);
		permissionDecision.setDecisionReason("Mode: " + context.getMode().name().toLowerCase(Locale.ROOT));
		return permissionDecision	;
    }
}
