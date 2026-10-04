package org.frameworkset.spi.ai.permission;
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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.*;

/**
 * Decision returned by a permission rule match or a tool self-check.
 *
 * <p>A decision carries the {@link PermissionBehavior} together with a human-readable message and
 * optional fields used by the engine (rewritten inputs, suggested follow-up rules).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"behavior", "message", "decision_reason", "updated_input", "suggested_rules"})
public class PermissionDecision {
	private PermissionBehavior behavior;
	private String message;
	private String decisionReason;
	private Map<String, Object> updatedInput;
	private List<PermissionRule> suggestedRules;
	
	public static PermissionDecision passthrough(String functionName) {
		PermissionDecision permissionDecision = new PermissionDecision();
		permissionDecision.setBehavior(PermissionBehavior.PASSTHROUGH);
		permissionDecision.setMessage(functionName);
		return permissionDecision	;
	}
	/**
	 * Returns a copy of this decision with the given suggested rules attached.
	 *
	 * @param suggestedRules the rules to attach; may be null to clear
	 * @return a new {@code PermissionDecision} with all other fields preserved
	 */
	public PermissionDecision withSuggestedRules(List<PermissionRule> suggestedRules) {
		PermissionDecision permissionDecision = new PermissionDecision();
		permissionDecision.setBehavior(behavior);
		permissionDecision.setMessage(message);	
		permissionDecision.setDecisionReason(decisionReason);
		permissionDecision.setUpdatedInput(updatedInput);
		permissionDecision.setSuggestedRules(suggestedRules);
				 
		return permissionDecision;
	}
	public void validate() {
		this.behavior = Objects.requireNonNull(behavior, "behavior must not be null");
		this.message = Objects.requireNonNull(message, "message must not be null");
//		this.updatedInput =
//				updatedInput == null
//						? null
//						: Collections.unmodifiableMap(new LinkedHashMap<>(updatedInput));
//		this.suggestedRules =
//				suggestedRules == null ? null : List.copyOf(builder.suggestedRules);
	}
	public PermissionBehavior getBehavior() {
		return behavior;
	}
	
	public void setBehavior(PermissionBehavior behavior) {
		this.behavior = behavior;
	}
	
	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
	
	public String getDecisionReason() {
		return decisionReason;
	}
	
	public void setDecisionReason(String decisionReason) {
		this.decisionReason = decisionReason;
	}
	
	public Map<String, Object> getUpdatedInput() {
		return updatedInput;
	}
	
	public void setUpdatedInput(Map<String, Object> updatedInput) {
		this.updatedInput = updatedInput;
	}
	
	public List<PermissionRule> getSuggestedRules() {
		return suggestedRules;
	}
	
	public void setSuggestedRules(List<PermissionRule> suggestedRules) {
		this.suggestedRules = suggestedRules;
	}
}
