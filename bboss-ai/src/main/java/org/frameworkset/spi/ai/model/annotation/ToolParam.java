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
 * https://api-docs.deepseek.com/zh-cn/guides/tool_calls
 * @author biaoping.yin
 * @Date 2026/5/20
 */
@Target({ElementType.PARAMETER,ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ToolParam {
    String type() default "object";
    String name();
	
	/**
	 * 数组元素类型
	 * @return
	 */
    String elementType() default "";
	/**
	 * 数组元素描述
	 * @return
	 */
	String elementDescription() default "";
    String description();
 

    /**
     * {
     *     "type": "object",
     *     "properties": {
     *         "user_email": {
     *             "type": "string",
     *             "description": "The user's email address",
     *             "format": "email" 
     *         },
     *         "zip_code": {
     *             "type": "string",
     *             "description": "Six digit postal code",
     *             "pattern": "^\\d{6}$"
     *         }
     *     }
     * }
     * 
     * 支持的参数：
     * 
     * format：使用预定义的常见格式进行校验，目前支持：
     * email：电子邮件地址
     * hostname：主机名
     * ipv4：IPv4 地址
     * ipv6：IPv6 地址
     * uuid：uuid
     * 不支持的参数
     * minLength
     * maxLength
     * @return
     */
    String format() default "";

    /**
     * pattern：使用正则表达式来约束字符串的格式
     * {
     *     "type": "object",
     *     "properties": {
     *         "user_email": {
     *             "type": "string",
     *             "description": "The user's email address",
     *             "format": "email" 
     *         },
     *         "zip_code": {
     *             "type": "string",
     *             "description": "Six digit postal code",
     *             "pattern": "^\\d{6}$"
     *         }
     *     }
     * }
     * @return
     */
    String pattern() default "";

    /**
     * {
     *     "type": "object",
     *     "properties": {
     *         "keywords": {
     *             "type": "array",
     *             "description": "Five keywords of the article, sorted by importance",
     *             "items": {
     *                 "type": "string",
     *                 "description": "A concise and accurate keyword or phrase."
     *             }
     *         }
     *     },
     *     "required": ["keywords"],
     *     "additionalProperties": false
     * }
     * @return
     */
    String arrayItemType() default "";

    String arrayItemDescription() default "";

    /**
     * enum
     * enum 可以确保输出是预期的几个选项之一，例如在订单状态的场景下，只能是有限几个状态之一。
     *
     * 样例：
     *
     * {
     *     "type": "object",
     *     "properties": {
     *         "order_status": {
     *             "type": "string",
     *             "description": "Ordering status",
     *             "enum": ["pending", "processing", "shipped", "cancelled"]
     *         }
     *     }
     * }
     * @return
     */
    String[] enumValues() default {};


    /**
     * number/integer 类型
     * 支持的参数
     * const：固定数字为常数
     * default：数字的默认值
     * minimum：最小值
     * maximum：最大值
     * exclusiveMinimum：不小于
     * exclusiveMaximum：不大于
     * multipleOf：数字输出为这个值的倍数
     */
    
    String constValue() default "";
    String defaultValue() default "";
    String minimum() default "";
    String maximum() default "";
    String exclusiveMinimum() default "";
    String exclusiveMaximum() default "";
    String multipleOf() default "";
    boolean required() default false;
}
