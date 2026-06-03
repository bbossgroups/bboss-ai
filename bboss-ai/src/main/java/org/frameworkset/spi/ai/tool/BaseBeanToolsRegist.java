package org.frameworkset.spi.ai.tool;
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

import org.frameworkset.spi.ai.model.FunctionCall;
import org.frameworkset.spi.ai.model.FunctionToolDefine;
import org.frameworkset.spi.ai.tools.ToolsRegist;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author biaoping.yin
 * @Date 2026/5/21
 */
public abstract class BaseBeanToolsRegist implements ToolsRegist {
    protected Object toolBean;
    protected List<FunctionToolDefine> functionToolDefines;


    protected Map<String,FunctionToolDefine> functionToolDefinesIndexByName;
    public BaseBeanToolsRegist(Object toolBean){
        this.toolBean = toolBean;
    }
    protected abstract BaseBeanToolFunctionCall buildBeanToolFunctionCall(Method toolMethod, Object toolBean, FunctionToolDefine functionToolDefine, Parameter[] parameters);
    @Override
    public void init(){
//        functionToolDefines = BeanToolHandle.parserTools(toolBean, new BeanToolFunctionCallBuilder() {
//            @Override
//            public BaseBeanToolFunctionCall buildBeanToolFunctionCall(Method toolMethod, Object toolBean, FunctionToolDefine functionToolDefine, Parameter[] parameters) {
//                return BaseBeanToolsRegist.this.buildBeanToolFunctionCall(  toolMethod,   toolBean,   functionToolDefine,   parameters);
//            }
//        });
//        if(functionToolDefines != null){
//            functionToolDefinesIndexByName = new LinkedHashMap<>();
//            for(FunctionToolDefine functionToolDefine : functionToolDefines) {
//                functionToolDefinesIndexByName.put(functionToolDefine.getFunction().getName(), functionToolDefine);
//            }
//        }
        registBeanTools(toolBean);
    }
    protected Object registLock = new Object();
    public void registBeanTools(Object toolBean) {
        synchronized (registLock){
            if(this.toolBean == null){
                this.toolBean = toolBean;
            }

            List<FunctionToolDefine> functionToolDefines = BeanToolHandle.parserTools(toolBean, new BeanToolFunctionCallBuilder() {
                @Override
                public BaseBeanToolFunctionCall buildBeanToolFunctionCall(Method toolMethod, Object toolBean, FunctionToolDefine functionToolDefine, Parameter[] parameters) {
                    return BaseBeanToolsRegist.this.buildBeanToolFunctionCall(  toolMethod,   toolBean,   functionToolDefine,   parameters);
                }
            });
            if(functionToolDefines != null){
                if(functionToolDefinesIndexByName == null) {
                    functionToolDefinesIndexByName = new LinkedHashMap<>();
                }
                List<FunctionToolDefine> newFunctionToolDefines = new ArrayList<>(); 
                for(FunctionToolDefine functionToolDefine : functionToolDefines) {
                    String name = functionToolDefine.getFunction().getName();
                    if(functionToolDefinesIndexByName.containsKey(name)){
                        continue;
                    }
                    newFunctionToolDefines.add(functionToolDefine);
                    functionToolDefinesIndexByName.put(name, functionToolDefine);
                }
                if(newFunctionToolDefines.size() > 0){
                    if(this.functionToolDefines == null){
                        this.functionToolDefines = new ArrayList<>();
                    }
                    this.functionToolDefines.addAll(newFunctionToolDefines);
                }
            }
        }
         
    }

    @Override
    public List<FunctionToolDefine> registTools() {
        return functionToolDefines;
    }

    /**
     * beanTools无需实现本方法，
     * @param functionName
     * @return
     */
    @Override
    public FunctionCall getFunctionCall(String functionName) {
        if(functionToolDefinesIndexByName != null)
            return functionToolDefinesIndexByName.get(functionName).getFunctionCall();
        return null;
    }

    public FunctionToolDefine getFunctionToolDefine(String functionName) {
        if(functionToolDefinesIndexByName != null)
            return functionToolDefinesIndexByName.get(functionName);
        return null;
    }
}
