package org.frameworkset.spi.ai.state;
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

import java.util.List;
import java.util.Map;

/**
 *
 * @author biaoping.yin
 * @Date 2026/8/24
 */
public class Task {
	/** Allowed values for {@link #state}. */
	public enum State {
		PENDING("pending"),
		IN_PROGRESS("in_progress"),
		COMPLETED("completed");
		
		private final String wire;
		
		State(String wire) {
			this.wire = wire;
		}
		
		public String getWire() {
			return wire;
		}
		
		public static State fromWire(String wire) {
			if (wire == null) {
				return null;
			}
			for (State s : values()) {
				if (s.wire.equalsIgnoreCase(wire)) {
					return s;
				}
			}
			throw new IllegalArgumentException("Unknown task state: " + wire);
		}
	}
	
	private  String subject;
	private  String description;
	private  Map<String, Object> metadata;
	private  String createdAt;
	private  State state;
	private  String id;
	private  String owner;
	private  List<String> blocks;
	private  List<String> blockedBy;
	
	public String getSubject() {
		return subject;
	}
	
	public void setSubject(String subject) {
		this.subject = subject;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public Map<String, Object> getMetadata() {
		return metadata;
	}
	
	public void setMetadata(Map<String, Object> metadata) {
		this.metadata = metadata;
	}
	
	public String getCreatedAt() {
		return createdAt;
	}
	
	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}
	
	public State getState() {
		return state;
	}
	
	public void setState(State state) {
		this.state = state;
	}
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getOwner() {
		return owner;
	}
	
	public void setOwner(String owner) {
		this.owner = owner;
	}
	
	public List<String> getBlocks() {
		return blocks;
	}
	
	public void setBlocks(List<String> blocks) {
		this.blocks = blocks;
	}
	
	public List<String> getBlockedBy() {
		return blockedBy;
	}
	
	public void setBlockedBy(List<String> blockedBy) {
		this.blockedBy = blockedBy;
	}
}
