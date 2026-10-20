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
package org.frameworkset.spi.ai.harness.bus;

import java.util.Map;
import java.util.Objects;

/**
 * A single entry read from a {@link MessageBus} queue or log.
 */
public final class BusEntry {
	
	private final String entryId;
	private final Map<String, Object> payload;
	
	public BusEntry(String entryId, Map<String, Object> payload) {
		this.entryId = entryId;
		this.payload = payload;
	}
	
	/** Transport-level entry identifier (e.g. Redis Stream entry id). */
	public String getEntryId() {
		return entryId;
	}
	
	/** The JSON-serializable payload. */
	public Map<String, Object> getPayload() {
		return payload;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof BusEntry)) {
			return false;
		}
		BusEntry that = (BusEntry) o;
		return Objects.equals(entryId, that.entryId)
				&& Objects.equals(payload, that.payload);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(entryId, payload);
	}
	
	@Override
	public String toString() {
		return "BusEntry{"
				+ "entryId='" + entryId + '\''
				+ ", payload=" + payload
				+ '}';
	}
}