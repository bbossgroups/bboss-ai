package org.frameworkset.spi.ai.tools;
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

import org.frameworkset.spi.ai.tool.ToolCallContext;

import java.util.Map;

/**
 *
 * @author biaoping.yin
 * @Date 2026/8/5
 */
public interface HitlAssistant {
	/**
	 * 人工干预时，智能体通过方法getHumanAssistantDatas给前端提供辅助干预帮助信息
	 * @param toolCallContext
	 * @return
	 */
	Map<String,Object> getHumanAssistantDatas(ToolCallContext toolCallContext);
	/**
	 * 人工干预时，前端提交的干预数据通过handleHumanSubbmitDatas方法返回给智能体，智能体拿到人工提交的数据进行相应处理后，将数据返回给大模型
	 * @param humanSubbmitDatas
	 * @param toolCallContext
	 */
	void handleHumanSubbmitDatas(Map<String, Object> humanSubbmitDatas,ToolCallContext toolCallContext); 
	
	
}
