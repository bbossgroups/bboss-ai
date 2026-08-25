package org.frameworkset.spi.ai.skill;
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
import org.frameworkset.util.io.ClassPathResource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author biaoping.yin
 * @Date 2026/7/14
 */
public class SkillsToolRegist  implements ToolsRegist {
	private List<Skill> skills = new ArrayList<>();
	private Map<String,Skill> skillMap = new LinkedHashMap<>();
	private SkillFilter skillFilter;
	
	private String toolDescriptionTemplate = SkillUtils.getToolDescriptionTemplate();
	private List<FunctionToolDefine> functionToolDefines = new ArrayList<>();
	
	public SkillsToolRegist setToolDescriptionTemplate(String toolDescriptionTemplate) {
		this.toolDescriptionTemplate = toolDescriptionTemplate;
		return this;
	}
	private Object lock = new Object();
	private boolean initialized = false;
	@Override
	public void init(){
		if(initialized){
			return;
		}
		synchronized (lock) {
			if (initialized) {
				return;
			}
			FunctionToolDefine functionToolDefine = new FunctionToolDefine();
			StringBuilder skillsXml = new StringBuilder();
			
			for (Skill skill : skills) {
				if(skillFilter != null && !skillFilter.isAllowed(skill.getName())){
					continue;
				}
				skillsXml.append(skill.toXml()).append("\n");
				skillMap.put(skill.getName(), skill);
			}
			if(skillsXml.length() > 0) {
				functionToolDefine.funtionName2ndDescription("Skill", this.toolDescriptionTemplate.replace("#{skills}", skillsXml.toString()))
						.requiredParameters("skillName")
						.addParameter("skillName", "string", "The skill name (no arguments). E.g., \"pdf\" or \"xlsx\"")
						.setFunctionCall(new SkillFunctionCall(skillMap));
				functionToolDefine.setToolsRegist(this);
				functionToolDefines.add(functionToolDefine);
			}
			initialized = true;
		}
		
	}
	public SkillsToolRegist addClasspathSkills(String resourceSkillsDir){
		this.skills.addAll(SkillUtils.loadResource(new ClassPathResource(resourceSkillsDir)));	
		return this;
	}
	
	public SkillsToolRegist addDirectorySkills(String dir){
		this.skills.addAll(SkillUtils.loadDirectory(dir));
		return this;
	}
	
	@Override
	public List<FunctionToolDefine> registTools() {
		return functionToolDefines;
	}
	
	@Override
	public FunctionCall getFunctionCall(String functionName) {
		return null;
	}
}
