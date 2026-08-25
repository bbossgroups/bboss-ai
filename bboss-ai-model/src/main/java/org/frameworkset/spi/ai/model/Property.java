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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *  "location": {
 *  *                                                 "type": "string",
 *  *                                                 "description": "城市或者地州, 例如：上海市"
 *  *                                             }
 * @author biaoping.yin
 * @Date 2026/2/10
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Property {
    /**
     * object
     * string
     * number
     * integer
     * boolean
     * array
     * enum
     * anyOf
     */
    private String type = "object";
    private String description;
    private String format;
    private String pattern;


    @JsonProperty("enum")
    private String[] enumValue;


    @JsonProperty("default")
    private Object defaultValue;
    private Map<String,Property> properties;

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
    private Integer minimum;
    private Integer maximum;
    private Integer exclusiveMinimum;
    private Integer exclusiveMaximum;
    private Integer multipleOf;
    @JsonProperty("const")
    private Integer constValue;

    /**
     * type为array 类型，指定数组元素的类型
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
     */
    private Property items;
	

	
	private List<String> required;
 

    public Property() {
    }
    
    public Property(String type, String desc) {
        this.type = type;
        this.description = desc;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Property addParameter(String name, String type, String desc) {
        if (properties == null)
            properties = new LinkedHashMap<>();
        properties.put(name, new Property(type, desc));
        return this;
    }

    public Property addParameter(String name, Property property) {
        if (properties == null)
            properties = new LinkedHashMap<>();
        properties.put(name, property);
        return this;
    }

    public Map<String, Property> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Property> properties) {
        this.properties = properties;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String[] getEnumValue() {
        return enumValue;
    }

    public void setEnumValue(String[] enumValue) {
        this.enumValue = enumValue;
    }
    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Integer getMinimum() {
        return minimum;
    }

    public void setMinimum(Integer minimum) {
        this.minimum = minimum;
    }

    public Integer getMaximum() {
        return maximum;
    }

    public void setMaximum(Integer maximum) {
        this.maximum = maximum;
    }

    public Integer getExclusiveMinimum() {
        return exclusiveMinimum;
    }

    public void setExclusiveMinimum(Integer exclusiveMinimum) {
        this.exclusiveMinimum = exclusiveMinimum;
    }

    public Integer getExclusiveMaximum() {
        return exclusiveMaximum;
    }

    public void setExclusiveMaximum(Integer exclusiveMaximum) {
        this.exclusiveMaximum = exclusiveMaximum;
    }

    public Integer getMultipleOf() {
        return multipleOf;
    }

    public void setMultipleOf(Integer multipleOf) {
        this.multipleOf = multipleOf;
    }

    public Integer getConstValue() {
        return constValue;
    }

    public void setConstValue(Integer constValue) {
        this.constValue = constValue;
    }

    public Property getItems() {
        return items;
    }

    public void setItems(Property items) {
        this.items = items;
    }
	public List<String> getRequired() {
		return required;
	}
	
	public void setRequired(List<String> required) {
		this.required = required;
	}
	
	public Property addRequired(String name) {
		if (required == null)
			required = new java.util.ArrayList<String>();
		required.add(name);
		return this;
	}
}
