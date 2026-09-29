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

/**
 *
 * @author biaoping.yin
 * @Date 2026/9/19
 */

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * Outcome of a permission rule or tool self-check.
 *
 * <ul>
 *   <li>{@link #ALLOW}: proceed without further checks.
 *   <li>{@link #DENY}: reject the tool invocation.
 *   <li>{@link #ASK}: defer to the user for a confirmation.
 *   <li>{@link #PASSTHROUGH}: tool defers decision back to the engine for rule matching.
 * </ul>
 */
public enum PermissionBehavior {
	ALLOW("allow"),
	DENY("deny"),
	ASK("ask"),
	PASSTHROUGH("passthrough");
	
	private final String value;
	
	PermissionBehavior(String value) {
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}

	/**
	 * Resolves a string (case-insensitive) to a {@code PermissionBehavior}.
	 *
	 * @param value JSON / config string representation
	 * @return matching behavior
	 * @throws IllegalArgumentException if no enum value matches
	 */
	@JsonCreator
	public static PermissionBehavior fromString(String value) {
		if (value == null) {
			throw new IllegalArgumentException("PermissionBehavior value must not be null");
		}
		String normalized = value.toLowerCase(Locale.ROOT);
		for (PermissionBehavior behavior : values()) {
			if (behavior.value.equals(normalized)) {
				return behavior;
			}
		}
		throw new IllegalArgumentException("Unknown PermissionBehavior: " + value);
	}
}
