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

import java.util.Arrays;
import java.util.List;

/**
 *
 * @author biaoping.yin
 * @Date 2026/8/24
 */
public class ToolDangerousPathConstants {
	
	private ToolDangerousPathConstants() {}
	
	/**
	 * Sensitive files that should never be auto-edited.
	 *
	 * <p>Includes shell rc files, SSH config, credential stores, and {@code .env}
	 * variants that commonly hold secrets. Aligned with Python {@code tool/_constants.py}.
	 */
	public static final List<String> DEFAULT_DANGEROUS_FILES =
			Arrays.asList(
					".gitconfig",
					".gitmodules",
					".bashrc",
					".bash_profile",
					".zshrc",
					".zprofile",
					".profile",
					".ssh/config",
					".ssh/authorized_keys",
					".netrc",
					".npmrc",
					".pypirc",
					".env",
					".envrc",
					".env.local",
					".env.development",
					".env.development.local",
					".env.test",
					".env.test.local",
					".env.staging",
					".env.production",
					".env.production.local");
	
	/** Directory names whose presence anywhere in a path marks it sensitive. */
	public static final List<String> DEFAULT_DANGEROUS_DIRECTORIES =
			Arrays.asList(".git", ".vscode", ".idea", ".ssh");
	
	/** Shell command fragments that always require explicit user approval. */
	public static final List<String> DANGEROUS_COMMANDS =
			Arrays.asList( 
					"rm -rf",
					"sudo rm",
					"dd",
					"mkfs",
					"fdisk",
					"format",
					"chmod 777",
					"chmod -R 777",
					"chown -R",
					"kill -9",
					"> /dev/");
	
}
