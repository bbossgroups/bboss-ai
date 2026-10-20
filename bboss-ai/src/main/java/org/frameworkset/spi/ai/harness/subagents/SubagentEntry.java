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

import java.util.Objects;

/**
 * Descriptor for a subagent identified by agent id, with its description,
 * {@link SubagentFactory}, and optional {@link SubagentDeclaration} (for
 * remote URL and headers).
 */
public final class SubagentEntry {
	
	private final String name;
	private final String description;
	private final SubagentFactory factory;
	private final SubagentDeclaration declaration;
	
	public SubagentEntry(
			String name,
			String description,
			SubagentFactory factory,
			SubagentDeclaration declaration) {
		this.name = name;
		this.description = description;
		this.factory = factory;
		this.declaration = declaration;
	}
	
	public SubagentEntry(String name, String description, SubagentFactory factory) {
		this(name, description, factory, null);
	}
	
	public String getName() {
		return name;
	}
	
	public String getDescription() {
		return description;
	}
	
	public SubagentFactory getFactory() {
		return factory;
	}
	
	public SubagentDeclaration getDeclaration() {
		return declaration;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof SubagentEntry)) {
			return false;
		}
		SubagentEntry that = (SubagentEntry) o;
		return Objects.equals(name, that.name)
				&& Objects.equals(description, that.description)
				&& Objects.equals(factory, that.factory)
				&& Objects.equals(declaration, that.declaration);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(name, description, factory, declaration);
	}
	
	@Override
	public String toString() {
		return "SubagentEntry{"
				+ "name='" + name + '\''
				+ ", description='" + description + '\''
				+ ", factory=" + factory
				+ ", declaration=" + declaration
				+ '}';
	}
}