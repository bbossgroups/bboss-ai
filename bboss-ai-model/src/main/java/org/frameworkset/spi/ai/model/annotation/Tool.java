package org.frameworkset.spi.ai.model.annotation;
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

import java.lang.annotation.*;

/**
 * @author biaoping.yin
 * @Date 2026/5/20
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Tool {
    String name() default "";
    String description();
    String type() default "function";
    boolean strict() default true;
    boolean additionalProperties() default false;
	/**
	 * Whether the tool only reads data without observable side effects.
	 *
	 * <p>Read-only tools are auto-allowed under {@code PermissionMode.EXPLORE} and
	 * {@code ACCEPT_EDITS}, mirroring the {@code ToolBase.isReadOnly()} contract.
	 *
	 * @return true if this tool performs no mutation
	 */
	boolean readOnly() default false;
	
	/**
	 * Whether the tool is safe to invoke concurrently with itself.
	 *
	 * <p>When false, the framework serialises invocations of this tool inside a parallel batch.
	 * Defaults to {@code true} to match the typical pure-function shape of {@code @Tool} methods.
	 *
	 * @return true if multiple invocations may run in parallel without coordination
	 */
	boolean concurrencySafe() default true;
}
