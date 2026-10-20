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
package org.frameworkset.spi.ai.harness.subagents;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Durable, recovery-oriented descriptor of an exposed subagent. Unlike the in-memory
 * {@code ExposedSession} (which holds a live {@code Agent} reference), a {@code SubagentRecord}
 * stores only the <em>recipe</em> needed to re-materialize and route to the subagent on any node:
 * its type ({@code agentId}), its own conversational session ({@code sessionId}), and lifecycle
 * metadata.
 *
 * <p>The {@code agentId} feeds the agent factory to rebuild the instance; the {@code sessionId} is
 * passed at invocation time so the rebuilt agent loads the correct conversation history from a
 * (distributed) {@code AgentStateStore}.
 */
public final class SubagentRecord {
	
	private final String subagentId;
	private final String agentId;
	private final String sessionId;
	private final String userId;
	private final String parentSessionId;
	private final Instant createdAt;
	private final Instant expiresAt;
	
	public SubagentRecord(
			String subagentId,
			String agentId,
			String sessionId,
			String userId,
			String parentSessionId,
			Instant createdAt,
			Instant expiresAt) {
		this.subagentId = subagentId;
		this.agentId = agentId;
		this.sessionId = sessionId;
		this.userId = userId;
		this.parentSessionId = parentSessionId;
		this.createdAt = createdAt;
		this.expiresAt = expiresAt;
	}
	
	public String getSubagentId() {
		return subagentId;
	}
	
	public String getAgentId() {
		return agentId;
	}
	
	public String getSessionId() {
		return sessionId;
	}
	
	public String getUserId() {
		return userId;
	}
	
	public String getParentSessionId() {
		return parentSessionId;
	}
	
	public Instant getCreatedAt() {
		return createdAt;
	}
	
	public Instant getExpiresAt() {
		return expiresAt;
	}
	
	/** Whether this record has a TTL that has already elapsed relative to {@code now}. */
	public boolean isExpired(Instant now) {
		return expiresAt != null && now != null && now.isAfter(expiresAt);
	}
	
	/** Serializes this record to a flat string/number map for {@code BaseStore} persistence. */
	public Map<String, Object> toMap() {
		Map<String, Object> m = new HashMap<>();
		m.put("subagentId", subagentId);
		m.put("agentId", agentId);
		m.put("sessionId", sessionId);
		if (userId != null) {
			m.put("userId", userId);
		}
		if (parentSessionId != null) {
			m.put("parentSessionId", parentSessionId);
		}
		if (createdAt != null) {
			m.put("createdAt", createdAt.toEpochMilli());
		}
		if (expiresAt != null) {
			m.put("expiresAt", expiresAt.toEpochMilli());
		}
		return m;
	}
	
	/** Reconstructs a record from a persisted map, tolerating missing optional fields. */
	public static SubagentRecord fromMap(Map<String, Object> m) {
		if (m == null) {
			return null;
		}
		return new SubagentRecord(
				asString(m.get("subagentId")),
				asString(m.get("agentId")),
				asString(m.get("sessionId")),
				asString(m.get("userId")),
				asString(m.get("parentSessionId")),
				asInstant(m.get("createdAt")),
				asInstant(m.get("expiresAt")));
	}
	
	private static String asString(Object v) {
		return v != null ? v.toString() : null;
	}
	
	private static Instant asInstant(Object v) {
		if (v instanceof Number) {
			Number n = (Number) v;
			return Instant.ofEpochMilli(n.longValue());
		}
		if (v instanceof String) {
			String s = (String) v;
			if (!s.trim().isEmpty()) {
				try {
					return Instant.ofEpochMilli(Long.parseLong(s.trim()));
				} catch (NumberFormatException ignored) {
					return null;
				}
			}
		}
		return null;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof SubagentRecord)) {
			return false;
		}
		SubagentRecord that = (SubagentRecord) o;
		return Objects.equals(subagentId, that.subagentId)
				&& Objects.equals(agentId, that.agentId)
				&& Objects.equals(sessionId, that.sessionId)
				&& Objects.equals(userId, that.userId)
				&& Objects.equals(parentSessionId, that.parentSessionId)
				&& Objects.equals(createdAt, that.createdAt)
				&& Objects.equals(expiresAt, that.expiresAt);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(
				subagentId, agentId, sessionId, userId, parentSessionId, createdAt, expiresAt);
	}
	
	@Override
	public String toString() {
		return "SubagentRecord{"
				+ "subagentId='" + subagentId + '\''
				+ ", agentId='" + agentId + '\''
				+ ", sessionId='" + sessionId + '\''
				+ ", userId='" + userId + '\''
				+ ", parentSessionId='" + parentSessionId + '\''
				+ ", createdAt=" + createdAt
				+ ", expiresAt=" + expiresAt
				+ '}';
	}
}