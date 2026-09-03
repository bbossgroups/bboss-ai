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
package org.frameworkset.spi.ai.filesystem.model;

import java.time.Instant;

/**
 * Data structure for storing file contents with metadata.
 *
 
 */
public class FileData  {
	/**
	 * file content as a plain string (utf-8 text or base64-encoded binary)
	 */
	private String content;
	/**
	 * content encoding: {@code "utf-8"} for text, {@code "base64"} for binary
	 */
	private String encoding;
	/**
	 * ISO 8601 timestamp of file creation (nullable)
	 */
	private String createdAt;
	/**
	 * ISO 8601 timestamp of last modification (nullable)
	 */
	private String modifiedAt;
	public FileData(String content, String encoding, String createdAt, String modifiedAt){
		this.content = content;
		this.encoding = encoding;
		this.createdAt = createdAt;
		this.modifiedAt = modifiedAt;
	}
	public FileData(){
		
	}
	
	public FileData(String content, String encoding) {
        this(content, encoding, null, null);
    }

    /** Creates a new UTF-8 text FileData with timestamps set to now. */
    public static FileData create(String content) {
        return create(content, "utf-8");
    }

    /** Creates a new FileData with specified encoding and timestamps set to now. */
    public static FileData create(String content, String encoding) {
        String now = Instant.now().toString();
        return new FileData(content, encoding, now, now);
    }

    /** Returns a copy with updated content and modified timestamp. */
    public FileData withContent(String newContent) {
        return new FileData(newContent, encoding, createdAt, Instant.now().toString());
    }
	
	public String getContent() {
		return content;
	}
	
	public void setContent(String content) {
		this.content = content;
	}
	
	public String getEncoding() {
		return encoding;
	}
	
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}
	
	public String getCreatedAt() {
		return createdAt;
	}
	
	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}
	
	public String getModifiedAt() {
		return modifiedAt;
	}
	
	public void setModifiedAt(String modifiedAt) {
		this.modifiedAt = modifiedAt;
	}
}
