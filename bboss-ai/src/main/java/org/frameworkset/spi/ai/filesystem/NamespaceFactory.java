package org.frameworkset.spi.ai.filesystem;
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

/**
 *
 * @author biaoping.yin
 * @Date 2026/8/24
 */

import org.frameworkset.spi.ai.context.ChatContext;

import java.util.List;

/**
 * Factory that produces a namespace tuple for {  BaseStore} operations at call time.
 *
 * <p>Unlike a static namespace, a {@code NamespaceFactory} is invoked on <em>every</em> store
 * operation (read, write, ls, etc.), allowing the namespace to vary based on the per-call {@link
 * ChatContext} (user id, session id) rather than mutable shared state on the agent instance.
 *
 * <p>Example:
 *
 * <pre>{@code
 * NamespaceFactory factory = rc ->
 *         List.of("sessions", rc.getSessionId(), "filesystem");
 * RemoteFilesystem fs = new RemoteFilesystem(store, factory);
 * }</pre>
 */
@FunctionalInterface
public interface NamespaceFactory {
	
	/**
	 * Returns the namespace tuple for the current operation context.
	 *
	 * @param runtimeContext per-call runtime context; never {@code null} (callers without a real RC
	 *     must pass {  ChatContext#empty()})
	 * @return non-null, non-empty list of namespace segments
	 */
	List<String> getNamespace(ChatContext runtimeContext);
}
