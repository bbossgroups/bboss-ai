package org.frameworkset.spi.ai.util;
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

import com.frameworkset.util.JsonUtil;
import com.frameworkset.util.SimpleStringUtil;
import org.frameworkset.spi.ai.tools.FileToolException;

import java.io.File;
import java.util.List;

/**
 *
 * @author biaoping.yin
 * @Date 2026/8/17
 */
public class FileToolUtil {
	/**
	 * 校验路径是否在允许的基目录范围内，并返回 File 对象
	 */
	public static File validateAndGetFile(List<String> baseDirectories, String path,boolean validateEmpty) {
		if (validateEmpty && SimpleStringUtil.isEmpty(path)) {
			throw new FileToolException("路径不能为空");
		}
		path = path.replace('\\', '/').replaceAll("/+", "/");	 
		File file = null;
		if(path.startsWith("/") || path.contains(":")){ // 绝对路径
			file = new File(path);
		}
		else {
			if (baseDirectories != null && baseDirectories.size() > 0) {
				for(String baseDirectory:baseDirectories) {
					file = new File(baseDirectory, path);
					if (file.exists()) {
						break;
					}
				 
				}
			}
		}
		String targetPath = file.getAbsolutePath();
		
		String normalizedTarget = targetPath.replace('\\', '/').replaceAll("/+", "/");
		if (baseDirectories != null && baseDirectories.size() > 0) {
			boolean isOk = false;
			for(String baseDirectory:baseDirectories) {
				File base = new File(baseDirectory);
				String basePath = base.getAbsolutePath();
				
				// 统一分隔符并规范化路径，防止路径穿越
				String normalizedBase = basePath.replace('\\', '/').replaceAll("/+", "/");
				// 应改为
				if (normalizedTarget.equals(normalizedBase) || normalizedTarget.startsWith(normalizedBase + "/")) {
					isOk = true;
				}
			}
			if (!isOk) {
				throw new FileToolException("路径"+path+"超出允许的操作范围，只允许操作以下目录: " + JsonUtil.object2json(baseDirectories));
			}
		}
		return file;
	}
	
}
