package org.frameworkset.spi.ai.compaction;
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
import org.frameworkset.spi.ai.model.LinkedMessageMap;

import java.util.List;

/**
 *
 * @author biaoping.yin
 * @Date 2026/9/8
 */
public interface CompactionManagerInf {
	/**
	 * 消息压缩：如果第一条消息是System消息需要保留，不能压缩；工具入参和工具结果是成对出现，不能割裂截断和压缩
	 * 压缩后 LLM 实际看到的列表长度 ≈ 1(SYSTEM) + 1(新摘要) + tail条数,而 tail条数 → keepMessages 只是个软目标——这也意味着单看“压缩后上下文还有几条消息”，不能直接反推出 keepMessages 的配置值。
	 * @param agent
	 * @param messages
	 * @return
	 */
	List<LinkedMessageMap<String,Object>> compact(ChatContext chatContext,
												  AIAgent agent, List<LinkedMessageMap<String,Object>> messages
			 
	);
}
