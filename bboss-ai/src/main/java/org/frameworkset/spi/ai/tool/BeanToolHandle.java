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

import com.frameworkset.util.SimpleStringUtil;
import org.frameworkset.spi.ai.model.FunctionToolDefine;
import org.frameworkset.spi.ai.model.Property;
import org.frameworkset.spi.ai.model.annotation.Tool;
import org.frameworkset.spi.ai.model.annotation.ToolParam;
import org.frameworkset.util.ClassUtil;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 
 * @author biaoping.yin
 * @Date 2026/5/20
 */
public class BeanToolHandle {
    private static String getParamType(Class type){
        String typeStr = type.getName();
        //根据参数类型type名称，将其转换为以下值：
        //object
        //string
        //number
        //integer
        //boolean
        //array
        //enum
        //anyOf
        if(typeStr.equals("java.lang.Object")){
            return "object";
        }
        else if(typeStr.equals("java.lang.String")){
            return "string";
        }
		
		else if(long.class.isAssignableFrom(type) || Long.class.isAssignableFrom(type)){
			return "number";
		}
		
		else if(double.class.isAssignableFrom(type) || Double.class.isAssignableFrom(type)){
			return "number";
		}
		
		
		else if(float.class.isAssignableFrom(type) || Float.class.isAssignableFrom(type)){
			return "number";
		}
		
		
		else if(int.class.isAssignableFrom(type) || Integer.class.isAssignableFrom(type)){
			return "integer";
		}
		else if(boolean.class.isAssignableFrom(type) || Boolean.class.isAssignableFrom(type)){
			return "boolean";
		}
        else if(Number.class.isAssignableFrom(type)){
            return "number";
        }
        else if(List.class.isAssignableFrom(type)){
            return "array";
        }
        
        else if(Set.class.isAssignableFrom(type)){
            return "array";
        }
		else if(type.isArray()){
            return "array";
        }
        else if(Map.class.isAssignableFrom(type)){
            return "map";
        }
        else{
            return "object";
        }
        
        
    }
    public static List<FunctionToolDefine> parserTools(Object toolObject,BeanToolFunctionCallBuilder beanToolFunctionCallBuilder) {
        ClassUtil.ClassInfo classInfo = ClassUtil.getClassInfo(toolObject.getClass());
        Method[] methods = classInfo.getDeclaredMethods();
       
        List<FunctionToolDefine> functionToolDefines = null;

        //从methods中筛选出包含注解Tool的方法
        for (Method method : methods) {
            if (method.isAnnotationPresent(Tool.class)) {
                Tool tool= method.getAnnotation(Tool.class);
                String name = tool.name();
                if(SimpleStringUtil.isEmpty(name)){
                    name = method.getName();
                }
                //创建FunctionToolDefine对象
                FunctionToolDefine functionToolDefine = new FunctionToolDefine();
                //设置方法名和方法描述
                functionToolDefine.funtionName2ndDescription(name,tool.description());
                functionToolDefine.setType(tool.type());
                functionToolDefine.additionalProperties(tool.additionalProperties());
                functionToolDefine.strict(tool.strict());                
                
                List<String> requirements = new ArrayList<>();

                Parameter[] parameters = method.getParameters();
                if (parameters != null && parameters.length > 0) {
                    Parameter parameter = null;
                    for (int i = 0; i < parameters.length; i++) {
                        parameter = parameters[i];
                        ToolParam toolParam = parameter.getAnnotation(ToolParam.class);
                        if(toolParam != null){
                            String paramName =  toolParam.name();
                            if(SimpleStringUtil.isEmpty(paramName)){
                                paramName = parameter.getName();
                            }
                            if(toolParam.required()){
                                requirements.add(paramName);
                            }
							Class paramTypeClass = parameter.getType();
                            String paramType = getParamType( paramTypeClass);
                            String paramDesc = toolParam.description();
                            Property property = new Property(paramType, paramDesc);       
                            String arrayItemDescription = toolParam.arrayItemDescription();
                            String arrayItemType = toolParam.arrayItemType();
                            if(SimpleStringUtil.isNotEmpty(arrayItemType)){
                                Property items = new Property(arrayItemType, arrayItemDescription);
                                property.setItems(items);
                            }
                            String[] enumValues = toolParam.enumValues();
                            if(enumValues != null && enumValues.length > 0)
                                property.setEnumValue(enumValues);
                            String constValue = toolParam.constValue();
                            if(SimpleStringUtil.isNotEmpty(constValue))
                                property.setConstValue(Integer.parseInt(constValue));
                            String defaultValue = toolParam.defaultValue();
                            if(SimpleStringUtil.isNotEmpty(defaultValue))
                                property.setDefaultValue(Integer.parseInt(defaultValue));
                            String minimum = toolParam.minimum();
                            if(SimpleStringUtil.isNotEmpty(minimum))
                                property.setMinimum(Integer.parseInt(minimum));
                            String maximum = toolParam.maximum();
                            if(SimpleStringUtil.isNotEmpty(maximum))
                                property.setMaximum(Integer.parseInt(maximum));
                            String exclusiveMinimum = toolParam.exclusiveMinimum();
                            if(SimpleStringUtil.isNotEmpty(exclusiveMinimum))
                                property.setExclusiveMinimum(Integer.parseInt(exclusiveMinimum));
                            String exclusiveMaximum = toolParam.exclusiveMaximum();
                            if(SimpleStringUtil.isNotEmpty(exclusiveMaximum))
                                property.setExclusiveMaximum(Integer.parseInt(exclusiveMaximum));
                            String multipleOf = toolParam.multipleOf();
                            if(SimpleStringUtil.isNotEmpty(multipleOf))
                                property.setMultipleOf(Integer.parseInt(multipleOf));
                            
                            String pattern = toolParam.pattern();
                            if(SimpleStringUtil.isNotEmpty(pattern))
                                property.setPattern(pattern);
                            String format = toolParam.format();
                            if(SimpleStringUtil.isNotEmpty(format))
                                property.setFormat(format);
							if("array".equals(paramType)){
								if(paramTypeClass.isArray()){
									Property arrayPropertyItems = new Property();
									arrayPropertyItems.setType(toolParam.elementType());
									arrayPropertyItems.setDescription(toolParam.elementDescription());
									property.setItems(arrayPropertyItems);
								}
								else{
									 
									// 获取方法参数的泛型
									Type[] parameterTypes = method.getGenericParameterTypes();
									if (parameterTypes[0] instanceof ParameterizedType) {
										ParameterizedType pt = (ParameterizedType) parameterTypes[i];
										Type[] actualTypeArguments = pt.getActualTypeArguments();
										Class<?> elementType = (Class<?>) actualTypeArguments[0];
										Property arrayPropertyItems_ = parserToolArrayParams(elementType);
										property.setItems(arrayPropertyItems_);
									}
									
									
								}
							}
							else if("object".equals(paramType)){
								parserToolObjectParams(property,paramTypeClass);
							}
                            functionToolDefine.addParameter(paramName, property);
                            
                        }
                    }
                }
                
               if(requirements != null && requirements.size() > 0){
                   functionToolDefine.requiredParameters(requirements.toArray(new String[requirements.size()]));
               }
               functionToolDefine.setFunctionCall(beanToolFunctionCallBuilder.buildBeanToolFunctionCall( method,toolObject,functionToolDefine,parameters));
                //将FunctionToolDefine对象添加到列表中
                if (functionToolDefines == null) {
                    functionToolDefines = new ArrayList<>();
                }
                
                functionToolDefines.add(functionToolDefine);
            }
        }
        return functionToolDefines;
    }
	
	private static Property parserToolArrayParams(Class<?> elementType) {
		Property arrayPropertyItems = new Property();
		arrayPropertyItems.setType("object");
		List<String> requirements = new ArrayList<>();
		ClassUtil.ClassInfo classInfo = ClassUtil.getClassInfo(elementType);
		List<ClassUtil.PropertieDescription> propertyDescriptors = classInfo.getPropertyDescriptors();
		for(ClassUtil.PropertieDescription propertyDescriptor : propertyDescriptors) {
			Field field = propertyDescriptor.getField();
			ToolParam toolParam = propertyDescriptor.getField().getAnnotation(ToolParam.class);
			if(toolParam != null){
				String paramName =  toolParam.name();
				if(SimpleStringUtil.isEmpty(paramName)){
					paramName = propertyDescriptor.getName();
				}
				if(toolParam.required()){
					requirements.add(paramName);
				}
				Class paramTypeClass = propertyDescriptor.getPropertyType();
				String paramType = getParamType( paramTypeClass);
				String paramDesc = toolParam.description();
				Property property = new Property(paramType, paramDesc);
				String arrayItemDescription = toolParam.arrayItemDescription();
				String arrayItemType = toolParam.arrayItemType();
				if(SimpleStringUtil.isNotEmpty(arrayItemType)){
					Property items = new Property(arrayItemType, arrayItemDescription);
					property.setItems(items);
				}
				String[] enumValues = toolParam.enumValues();
				if(enumValues != null && enumValues.length > 0)
					property.setEnumValue(enumValues);
				String constValue = toolParam.constValue();
				if(SimpleStringUtil.isNotEmpty(constValue))
					property.setConstValue(Integer.parseInt(constValue));
				String defaultValue = toolParam.defaultValue();
				if(SimpleStringUtil.isNotEmpty(defaultValue))
					property.setDefaultValue(Integer.parseInt(defaultValue));
				String minimum = toolParam.minimum();
				if(SimpleStringUtil.isNotEmpty(minimum))
					property.setMinimum(Integer.parseInt(minimum));
				String maximum = toolParam.maximum();
				if(SimpleStringUtil.isNotEmpty(maximum))
					property.setMaximum(Integer.parseInt(maximum));
				String exclusiveMinimum = toolParam.exclusiveMinimum();
				if(SimpleStringUtil.isNotEmpty(exclusiveMinimum))
					property.setExclusiveMinimum(Integer.parseInt(exclusiveMinimum));
				String exclusiveMaximum = toolParam.exclusiveMaximum();
				if(SimpleStringUtil.isNotEmpty(exclusiveMaximum))
					property.setExclusiveMaximum(Integer.parseInt(exclusiveMaximum));
				String multipleOf = toolParam.multipleOf();
				if(SimpleStringUtil.isNotEmpty(multipleOf))
					property.setMultipleOf(Integer.parseInt(multipleOf));
				
				String pattern = toolParam.pattern();
				if(SimpleStringUtil.isNotEmpty(pattern))
					property.setPattern(pattern);
				String format = toolParam.format();
				if(SimpleStringUtil.isNotEmpty(format))
					property.setFormat(format);
				if("array".equals(paramType)){
					if(paramTypeClass.isArray()){
						Property arrayPropertyItems_ = new Property();
						arrayPropertyItems_.setType(toolParam.elementType());
						arrayPropertyItems_.setDescription(toolParam.elementDescription());
						property.setItems(arrayPropertyItems_);
					}
					else{
						// 获取方法参数的泛型
						Type genericType = propertyDescriptor.getField().getGenericType();
						if (genericType instanceof ParameterizedType) {
							ParameterizedType pt = (ParameterizedType) genericType;
							Type[] actualTypeArguments = pt.getActualTypeArguments();
							Class<?> fieldElementType = (Class<?>) actualTypeArguments[0];
							Property arrayPropertyItems_ = parserToolArrayParams(fieldElementType);
							property.setItems(arrayPropertyItems_);
						}
						 
					}
				}
				else if("object".equals(paramType)){
					parserToolObjectParams(property,paramTypeClass);
				}
				arrayPropertyItems.addParameter(paramName, property);
				
			}
			
		}
		if(requirements != null && requirements.size() > 0)
			arrayPropertyItems.setRequired(requirements);
		return arrayPropertyItems;
    }
	
	
	private static void parserToolObjectParams(Property parent,Class<?> elementType) {
	  
		List<String> requirements = new ArrayList<>();
		ClassUtil.ClassInfo classInfo = ClassUtil.getClassInfo(elementType);
		List<ClassUtil.PropertieDescription> propertyDescriptors = classInfo.getPropertyDescriptors();
		for(ClassUtil.PropertieDescription propertyDescriptor : propertyDescriptors) {
			Field field = propertyDescriptor.getField();
			ToolParam toolParam = propertyDescriptor.getField().getAnnotation(ToolParam.class);
			if(toolParam != null){
				String paramName =  toolParam.name();
				if(SimpleStringUtil.isEmpty(paramName)){
					paramName = propertyDescriptor.getName();
				}
				if(toolParam.required()){
					requirements.add(paramName);
				}
				Class paramTypeClass = propertyDescriptor.getPropertyType();
				String paramType = getParamType( paramTypeClass);
				String paramDesc = toolParam.description();
				Property property = new Property(paramType, paramDesc);
				String arrayItemDescription = toolParam.arrayItemDescription();
				String arrayItemType = toolParam.arrayItemType();
				if(SimpleStringUtil.isNotEmpty(arrayItemType)){
					Property items = new Property(arrayItemType, arrayItemDescription);
					property.setItems(items);
				}
				String[] enumValues = toolParam.enumValues();
				if(enumValues != null && enumValues.length > 0)
					property.setEnumValue(enumValues);
				String constValue = toolParam.constValue();
				if(SimpleStringUtil.isNotEmpty(constValue))
					property.setConstValue(Integer.parseInt(constValue));
				String defaultValue = toolParam.defaultValue();
				if(SimpleStringUtil.isNotEmpty(defaultValue))
					property.setDefaultValue(Integer.parseInt(defaultValue));
				String minimum = toolParam.minimum();
				if(SimpleStringUtil.isNotEmpty(minimum))
					property.setMinimum(Integer.parseInt(minimum));
				String maximum = toolParam.maximum();
				if(SimpleStringUtil.isNotEmpty(maximum))
					property.setMaximum(Integer.parseInt(maximum));
				String exclusiveMinimum = toolParam.exclusiveMinimum();
				if(SimpleStringUtil.isNotEmpty(exclusiveMinimum))
					property.setExclusiveMinimum(Integer.parseInt(exclusiveMinimum));
				String exclusiveMaximum = toolParam.exclusiveMaximum();
				if(SimpleStringUtil.isNotEmpty(exclusiveMaximum))
					property.setExclusiveMaximum(Integer.parseInt(exclusiveMaximum));
				String multipleOf = toolParam.multipleOf();
				if(SimpleStringUtil.isNotEmpty(multipleOf))
					property.setMultipleOf(Integer.parseInt(multipleOf));
				
				String pattern = toolParam.pattern();
				if(SimpleStringUtil.isNotEmpty(pattern))
					property.setPattern(pattern);
				String format = toolParam.format();
				if(SimpleStringUtil.isNotEmpty(format))
					property.setFormat(format);
				if("array".equals(paramType)){
					if(paramTypeClass.isArray()){
						Property arrayPropertyItems_ = new Property();
						arrayPropertyItems_.setType(toolParam.elementType());
						arrayPropertyItems_.setDescription(toolParam.elementDescription());
						property.setItems(arrayPropertyItems_);
					}
					else{
						// 获取方法参数的泛型
						Type genericType = propertyDescriptor.getField().getGenericType();
						if (genericType instanceof ParameterizedType) {
							ParameterizedType pt = (ParameterizedType) genericType;
							Type[] actualTypeArguments = pt.getActualTypeArguments();
							Class<?> fieldElementType = (Class<?>) actualTypeArguments[0];
							Property arrayPropertyItems_ = parserToolArrayParams(fieldElementType);
							property.setItems(arrayPropertyItems_);
						}
						
					}
				}
				else if("object".equals(paramType)){
					parserToolObjectParams(property,paramTypeClass);
				}
				parent.addParameter(paramName, property);
				
			}
			
		}
	}

}
