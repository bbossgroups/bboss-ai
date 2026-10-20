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
package org.frameworkset.spi.ai.harness.gateway.channel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Per-channel routing configuration: default agent fallback, DM session scope, and ordered binding
 * rules evaluated by {@link ChannelRouter}.
 *
 * <p>Used by {@link Channel} implementations to parameterize routing without hard-coding agent ids
 * or session shapes in adapter code.
 */
public class ChannelConfig {
	
	private String channelId;
	private String defaultAgentId;
	private DmScope dmScope = DmScope.MAIN;
	private List<ChannelBinding> bindings = Collections.emptyList();
	
	/** No-arg constructor required for JavaBean conventions. */
	public ChannelConfig() {
	}
	
	/**
	 * Full constructor.
	 *
	 * @param channelId the logical channel identifier this config applies to (must match {@link
	 *     Channel#channelId()})
	 * @param defaultAgentId fallback agent id when no binding matches; if null the global default
	 *     agent (registered via {link Gateway#bindMainAgent})
	 *     is used
	 * @param dmScope controls how DM ({@link PeerKind#DIRECT}) sessions are keyed; defaults to
	 *     {@link DmScope#MAIN}
	 * @param bindings ordered list of binding rules; evaluated in list order within each priority
	 *     tier
	 */
	public ChannelConfig(
			String channelId, String defaultAgentId, DmScope dmScope, List<ChannelBinding> bindings) {
		setChannelId(channelId);
		setDefaultAgentId(defaultAgentId);
		setDmScope(dmScope);
		setBindings(bindings);
	}
	
	/** Minimal config: channel id only, using global agent default and {@link DmScope#MAIN}. */
	public static ChannelConfig of(String channelId) {
		return new ChannelConfig(channelId, null, DmScope.MAIN, Collections.<ChannelBinding>emptyList());
	}
	
	/** Config with explicit default agent (no binding rules). */
	public static ChannelConfig of(String channelId, String defaultAgentId) {
		return new ChannelConfig(
				channelId, defaultAgentId, DmScope.MAIN, Collections.<ChannelBinding>emptyList());
	}
	
	/** Returns a builder for constructing channel configs fluently. */
	public static Builder builder(String channelId) {
		return new Builder(channelId);
	}
	
	public String getChannelId() {
		return channelId;
	}
	
	public void setChannelId(String channelId) {
		this.channelId = Objects.requireNonNull(channelId, "channelId");
	}
	
	public String getDefaultAgentId() {
		return defaultAgentId;
	}
	
	public void setDefaultAgentId(String defaultAgentId) {
		this.defaultAgentId = defaultAgentId;
	}
	
	public DmScope getDmScope() {
		return dmScope;
	}
	
	public void setDmScope(DmScope dmScope) {
		this.dmScope = dmScope != null ? dmScope : DmScope.defaultScope();
	}
	
	public List<ChannelBinding> getBindings() {
		return bindings;
	}
	
	public void setBindings(List<ChannelBinding> bindings) {
		if (bindings != null) {
			this.bindings = Collections.unmodifiableList(new ArrayList<ChannelBinding>(bindings));
		} else {
			this.bindings = Collections.emptyList();
		}
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ChannelConfig)) {
			return false;
		}
		ChannelConfig that = (ChannelConfig) o;
		return Objects.equals(channelId, that.channelId)
				&& Objects.equals(defaultAgentId, that.defaultAgentId)
				&& dmScope == that.dmScope
				&& Objects.equals(bindings, that.bindings);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(channelId, defaultAgentId, dmScope, bindings);
	}
	
	@Override
	public String toString() {
		return "ChannelConfig{"
				+ "channelId='"
				+ channelId
				+ '\''
				+ ", defaultAgentId='"
				+ defaultAgentId
				+ '\''
				+ ", dmScope="
				+ dmScope
				+ ", bindings="
				+ bindings
				+ '}';
	}
	
	public static final class Builder {
		private final String channelId;
		private String defaultAgentId;
		private DmScope dmScope = DmScope.MAIN;
		private final List<ChannelBinding> bindings = new ArrayList<ChannelBinding>();
		
		private Builder(String channelId) {
			this.channelId = Objects.requireNonNull(channelId, "channelId");
		}
		
		public Builder defaultAgentId(String defaultAgentId) {
			this.defaultAgentId = defaultAgentId;
			return this;
		}
		
		public Builder dmScope(DmScope dmScope) {
			this.dmScope = dmScope;
			return this;
		}
		
		public Builder binding(ChannelBinding binding) {
			this.bindings.add(Objects.requireNonNull(binding, "binding"));
			return this;
		}
		
		public Builder bindings(List<ChannelBinding> bindings) {
			if (bindings != null) {
				this.bindings.addAll(bindings);
			}
			return this;
		}
		
		public ChannelConfig build() {
			return new ChannelConfig(channelId, defaultAgentId, dmScope, bindings);
		}
	}
}