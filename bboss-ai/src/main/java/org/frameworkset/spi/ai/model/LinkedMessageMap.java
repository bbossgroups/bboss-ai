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

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.frameworkset.spi.ai.store.SessionMessage;

import java.time.LocalDateTime;
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
	/**
	 * 裁剪时在副本上打标记并跳过已标记消息
	 */
	@JsonIgnore
	private boolean inputTrimmed;
 
	
	/**
	 * 裁剪时在副本上打标记并跳过已标记消息
	 */
	@JsonIgnore
	private boolean resultTrimmed;

	/**
	 * 消息类型
	 * @SessionMessage.MESSAGE_TYPE_USER_MESSAGE
	 */
	@JsonIgnore
	private String messageType;
	@JsonIgnore
	private String id;
	/**
	 * 父消息id
	 */
	@JsonIgnore
	private String parentMsgId;
	/**
	 * 后序消息id，摘要消息时需指定，以便恢复状态时，能够将摘要消息放置到正确的位置（后序消息的前面）
	 */
	@JsonIgnore
	private String nextMsgId;
	
	@JsonIgnore
	private int seqNo = -1;
	/**
	 * 消息名称：工具调用输入消息和工具调用结果消息时，代表工具名称
	 */
	@JsonIgnore
	private String name;
	
	/**
	 * 消息时间戳，如果不存在则使用当前时间戳
	 */
	@JsonIgnore
	private String timestamp;
	@JsonIgnore
	private LocalDateTime localDateTime;
	
	
	@JsonIgnore
	private Map<String,Object> meta;
	
	@JsonIgnore
	private String agentId;
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
	
	public boolean isAgentResultMessage(){
		if(this.messageType == null)
			return false;
		return SessionMessage.MESSAGE_TYPE_AGENT_RESULTMESSAGE.equals(this.messageType);
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
	public void withTimestamp(LocalDateTime timestamp){
		this.timestamp = TIMESTAMP_FORMATTER.format(timestamp);
		this.localDateTime = timestamp;
	}
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getMessageType() {
		return messageType;
	}
	
	public void setMessageType(String messageType) {
		this.messageType = messageType;
	}
	
	public int getSeqNo() {
		return seqNo;
	}
	
	public void setSeqNo(int seqNo) {
		this.seqNo = seqNo;
	}
	
	public LocalDateTime getLocalDateTime() {
		return localDateTime;
	}
	
	public void setLocalDateTime(LocalDateTime localDateTime) {
		this.localDateTime = localDateTime;
	}
	public Map<String, Object> getMeta() {
		return meta;
	}
	
	public void setMeta(Map<String, Object> meta) {
		this.meta = meta;
	}
	
	public String getParentMsgId() {
		return parentMsgId;
	}
	
	public void setParentMsgId(String parentMsgId) {
		this.parentMsgId = parentMsgId;
	}
	
	public String getNextMsgId() {
		return nextMsgId;
	}
	
	public void setNextMsgId(String nextMsgId) {
		this.nextMsgId = nextMsgId;
	}
	public Object getMetaValue(String key){
		if(meta != null){
			return meta.get(key);
		}
		return null;
	}
	public String getAgentId() {
		return agentId;
	}
	
	public void setAgentId(String agentId) {
		this.agentId = agentId;
	}
	
	public LinkedMessageMap<String, Object> copyMessage() {
		LinkedMessageMap<String, Object> copy = new LinkedMessageMap<>(this);
		copy.setMessageType(getMessageType());
		copy.setId(getId());
		copy.setParentMsgId(getParentMsgId());
		copy.setNextMsgId(getNextMsgId());
		copy.setSeqNo(getSeqNo());
		copy.setName(getName());
		copy.setTimestamp(getTimestamp());
		copy.setLocalDateTime(getLocalDateTime());
		copy.setMeta(getMeta());
		copy.setAgentId(getAgentId());
		copy.setInputTrimmed(isInputTrimmed());
		copy.setResultTrimmed(isResultTrimmed());
		return copy;
	}
	
	public boolean isInputTrimmed() {
		return inputTrimmed;
	}
	
	public void setInputTrimmed(boolean inputTrimmed) {
		this.inputTrimmed = inputTrimmed;
	}
	
	public boolean isResultTrimmed() {
		return resultTrimmed;
	}
	
	public void setResultTrimmed(boolean resultTrimmed) {
		this.resultTrimmed = resultTrimmed;
	}
	
 
}
