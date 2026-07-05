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

import org.frameworkset.spi.ai.model.annotation.Tool;
import org.slf4j.Logger;

import java.util.Map;

/**
 * 操作系统信息查询工具类。
 * <p>
 * 提供获取当前运行环境操作系统名称及版本信息的能力，
 * 适用于智能体通过 {@link Tool} 注解暴露为可调用工具的场景。
 * </p>
 *
 * @author biaoping.yin
 * @Date 2026/6/23
 */
public class GetOSFunctionToolTest {
    public static void main(String[] args){
        GetOSFunctionTool getOSFunctionTool = new GetOSFunctionTool();
        System.out.println(getOSFunctionTool.getOS2ndCpu());
    }
     
}
