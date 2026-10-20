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
package org.frameworkset.spi.ai.store;

import java.util.Map;
import java.util.Objects;

/**
 * A single item retrieved from a {@link BaseStore}.
 */
public final class StoreItem {
	
	private final String key;
	private final Map<String, Object> value;
	private final long version;
	
	/**
	 * @param key the item's key within its namespace
	 * @param value the item's data as a string-keyed map
	 * @param version monotonically increasing version counter; starts at 1 and increments on every
	 *     successful {link BaseStore#put} or {link BaseStore#putIfVersion}. A value of
	 *     {@code 0} means the version is unknown (e.g. items returned by legacy implementations
	 *     that predate versioning).
	 */
	public StoreItem(String key, Map<String, Object> value, long version) {
		this.key = key;
		this.value = value;
		this.version = version;
	}
	
	/** Back-compat constructor for code that does not yet supply a version. */
	public StoreItem(String key, Map<String, Object> value) {
		this(key, value, 0L);
	}
	
	public String getKey() {
		return key;
	}
	
	public Map<String, Object> getValue() {
		return value;
	}
	
	public long getVersion() {
		return version;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof StoreItem)) {
			return false;
		}
		StoreItem that = (StoreItem) o;
		return version == that.version
				&& Objects.equals(key, that.key)
				&& Objects.equals(value, that.value);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(key, value, version);
	}
	
	@Override
	public String toString() {
		return "StoreItem{"
				+ "key='" + key + '\''
				+ ", value=" + value
				+ ", version=" + version
				+ '}';
	}
}