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

/**
 * @author biaoping.yin
 * @Date 2026/2/10
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Function {
//    private String type;
    private String name;
    private String description;
    private Parameters parameters;


    private Boolean strict;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Parameters getParameters() {
        return parameters;
    }

    public void setParameters(Parameters parameters) {
        this.parameters = parameters;
    }

    public Function parametersType(String type) {
        if(parameters == null){
            parameters = new Parameters();
        }
        parameters.setType(type);
        return this;
    }

    public Function parametersRequired(String[] requireds) {
        if(parameters == null){
            parameters = new Parameters();
        }
        for(String required:requireds) {
            parameters.addRequired(required);
        }
        return this;
    }

    public Function addRequiredParameter(String required) {
        if(parameters == null){
            parameters = new Parameters();
        }
        parameters.addRequired(required);
        return this;
    }

    public Function addParameter(String name, String type, String desc) {
        if(parameters == null){
            parameters = new Parameters();
        }
        parameters.addParameter(name,type,desc);
        return this;
    }

    public Function addSubParameter(String name,String key, String type, String desc) {
        if(parameters == null){
            parameters = new Parameters();
        }
        parameters.addSubParameter(name,key,type,desc);
        return this;
    }

    /**
     * 添加复合参数条件
     * @param name
     */
    public Function addParameter(String name,Property property) {
        if(parameters == null){
            parameters = new Parameters();
        }
        parameters.addParameter(name,property);
        return this;
    }


//    public String getType() {
//        return type;
//    }
//
//    public void setType(String type) {
//        this.type = type;
//    }

    public Boolean getStrict() {
        return strict;
    }

    public void setStrict(Boolean strict) {
        this.strict = strict;
    }


    public Function additionalProperties(boolean additionalProperties) {
        if(parameters == null){
            parameters = new Parameters();
        }
        parameters.setAdditionalProperties(additionalProperties);
        return this;
    }
}
