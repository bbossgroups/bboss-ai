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

import java.util.Objects;

/**
 * Identifies the conversation peer: who or what is being messaged (a user DM, a channel, a group,
 * or a thread). Used by {@link ChannelRouter} for binding evaluation and session key construction.
 */
public class Peer {
	
	private PeerKind kind;
	private String id;
	
	/** No-arg constructor required for JavaBean conventions. */
	public Peer() {
	}
	
	/**
	 * Full constructor.
	 *
	 * @param kind the peer classification
	 * @param id provider-assigned peer identifier (user id, channel id, thread id, etc.)
	 */
	public Peer(PeerKind kind, String id) {
		setKind(kind);
		setId(id);
	}
	
	/** Returns the composite key {@code "<kind>:<id>"} used in binding matching. */
	public String key() {
		return kind.value() + ":" + id;
	}
	
	/** Creates a direct / DM peer. */
	public static Peer direct(String id) {
		return new Peer(PeerKind.DIRECT, id);
	}
	
	/** Creates a channel peer. */
	public static Peer channel(String id) {
		return new Peer(PeerKind.CHANNEL, id);
	}
	
	/** Creates a group peer. */
	public static Peer group(String id) {
		return new Peer(PeerKind.GROUP, id);
	}
	
	/** Creates a thread peer. */
	public static Peer thread(String id) {
		return new Peer(PeerKind.THREAD, id);
	}
	
	public PeerKind getKind() {
		return kind;
	}
	
	public void setKind(PeerKind kind) {
		this.kind = Objects.requireNonNull(kind, "kind");
	}
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = Objects.requireNonNull(id, "id");
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Peer)) {
			return false;
		}
		Peer that = (Peer) o;
		return kind == that.kind && Objects.equals(id, that.id);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(kind, id);
	}
	
	@Override
	public String toString() {
		return "Peer{" + "kind=" + kind + ", id='" + id + '\'' + '}';
	}
}