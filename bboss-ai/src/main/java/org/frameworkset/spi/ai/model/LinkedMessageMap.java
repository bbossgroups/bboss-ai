package org.frameworkset.spi.ai.model;
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

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author biaoping.yin
 * @Date 2026/8/31
 */
public class LinkedMessageMap<K,V> extends LinkedHashMap<K,V> {

	
	private String id;
	/**
	 * 消息名称：工具调用输入消息和工具调用结果消息时，代表工具名称
	 */
	private String name;
	
	/**
	 * 消息时间戳，如果不存在则使用当前时间戳
	 */
	private String timestamp;
	
	private static final DateTimeFormatter TIMESTAMP_FORMATTER =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());
	
	/**
	 * Constructs an empty insertion-ordered {@code LinkedHashMap} instance
	 * with the specified initial capacity and load factor.
	 *
	 * @param initialCapacity the initial capacity
	 * @param loadFactor      the load factor
	 * @throws IllegalArgumentException if the initial capacity is negative
	 *                                  or the load factor is nonpositive
	 */
	public LinkedMessageMap(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}
	
	/**
	 * Constructs an empty insertion-ordered {@code LinkedHashMap} instance
	 * with the specified initial capacity and a default load factor (0.75).
	 *
	 * @param initialCapacity the initial capacity
	 * @throws IllegalArgumentException if the initial capacity is negative
	 */
	public LinkedMessageMap(int initialCapacity) {
		super(initialCapacity);
	}
	
	/**
	 * Constructs an empty insertion-ordered {@code LinkedHashMap} instance
	 * with the default initial capacity (16) and load factor (0.75).
	 */
	public LinkedMessageMap() {
	}
	
	/**
	 * Constructs an insertion-ordered {@code LinkedHashMap} instance with
	 * the same mappings as the specified map.  The {@code LinkedHashMap}
	 * instance is created with a default load factor (0.75) and an initial
	 * capacity sufficient to hold the mappings in the specified map.
	 *
	 * @param m the map whose mappings are to be placed in this map
	 * @throws NullPointerException if the specified map is null
	 */
	public LinkedMessageMap(Map m) {
		super(m);
	}
	
	/**
	 * Constructs an empty {@code LinkedHashMap} instance with the
	 * specified initial capacity, load factor and ordering mode.
	 *
	 * @param initialCapacity the initial capacity
	 * @param loadFactor      the load factor
	 * @param accessOrder     the ordering mode - {@code true} for
	 *                        access-order, {@code false} for insertion-order
	 * @throws IllegalArgumentException if the initial capacity is negative
	 *                                  or the load factor is nonpositive
	 */
	public LinkedMessageMap(int initialCapacity, float loadFactor, boolean accessOrder) {
		super(initialCapacity, loadFactor, accessOrder);
	}
	
	public String getName() {
		return name;
	}
	public LinkedMessageMap setName(String name) {
		this.name = name;
		return this;
	}
	
	public String getTimestamp() {
		return timestamp;
	}
	
	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
}
