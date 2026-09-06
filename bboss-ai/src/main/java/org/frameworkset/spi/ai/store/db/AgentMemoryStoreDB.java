package org.frameworkset.spi.ai.store.db;
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

import org.frameworkset.spi.ai.AIAgent;
import org.frameworkset.spi.ai.context.ChatContext;
import org.frameworkset.spi.ai.model.Memory;
import org.frameworkset.spi.ai.store.AgentMemoryStore;
import org.frameworkset.spi.ai.store.StoreContext;

/**
 *
 * @author biaoping.yin
 * @Date 2026/8/31
 */
public class AgentMemoryStoreDB implements AgentMemoryStore {
	private StoreContext storeContext;
	public AgentMemoryStoreDB(StoreContext storeContext){
		this.storeContext = storeContext;
		
	}
	
	@Override
	public Memory readExistingLongTermMemoryContent(ChatContext rc, AIAgent agent) {
		return null;
	}
	
	@Override
	public Memory readExistingDayMemoryContent(ChatContext rc, AIAgent agent, String day) {
		return null;
	}
	
	@Override
	public void writeDailyMemory(ChatContext rc, Memory content) {
		
	}
	
	public StoreContext getStoreContext() {
		return storeContext;
	}
	
	public void setStoreContext(StoreContext storeContext) {
		this.storeContext = storeContext;
	}
}
