---
name: file-operations-skill
description: 文件系统操作技能，指导智能体按规范流程完成文件读写、目录管理、文件搜索、内容检索、编码检测、文件拷贝和删除等操作。当用户要求进行文件管理、批量文件处理、项目文件整理、日志检索、配置文件修改等文件操作任务时使用。
version: 1.0.0
author: bboss
category: tool-workflow
tags:
  - file
  - filesystem
  - search
  - grep
---

# 文件系统操作技能

本技能指导智能体按照规范流程完成文件系统操作任务，涵盖文件读写、目录管理、内容检索、文件拷贝和删除等场景。

技能依赖 `FileFunctionTool` 提供的以下工具方法：

| 工具方法 | 功能 | 是否只读 |
| -------- | ---- | -------- |
| `readFile(path, charset)` | 读取文件内容，自动识别编码，单次最大 2MB | 是 |
| `writeFile(path, content, charset, append)` | 写入文件内容，支持追加模式和编码 | 否 |
| `createFile(path, isDirectory)` | 创建文件或目录，父目录自动创建 | 否 |
| `deleteFile(path, recursive)` | 删除文件或目录，支持递归删除 | 否 |
| `copyFile(source, target, overwrite)` | 拷贝文件或目录到目标路径 | 否 |
| `fileExists(path)` | 检查文件或目录是否存在 | 是 |
| `getFileAttributes(path)` | 获取文件属性（大小、时间、权限等） | 是 |
| `detectFileEncoding(path)` | 检测文件字符编码 | 是 |
| `list_files(path)` | 列出目录下的文件和子目录 | 是 |
| `glob_files(pattern, path)` | 按 glob 模式查找文件 | 是 |
| `grep_files(pattern, path, glob)` | 在文件中搜索文本内容 | 是 |

## 工作流程

### Step 1：明确操作意图

根据用户请求，判断属于以下哪种操作类型：

- **读取类**：查看文件内容、检查配置、阅读代码
- **搜索类**：按文件名查找、按内容检索、统计关键词
- **写入类**：创建文件、修改配置、写入数据
- **管理类**：拷贝、移动、删除、创建目录结构
- **混合类**：多步组合操作（如"查找所有 .log 文件并统计 ERROR 出现次数"）

### Step 2：安全检查（所有操作必须执行）

在执行任何操作前：

1. **路径验证**：确认操作路径在 `baseDirectory` 允许范围内，不要尝试路径穿越（`..` 越界）
2. **存在性检查**：对读取和管理操作，先用 `fileExists` 确认路径存在
3. **类型确认**：用 `getFileAttributes` 确认目标是文件还是目录
4. **编码预检**：读取非 UTF-8 文件前，先用 `detectFileEncoding` 检测编码
5. **写操作确认**：写入、删除、覆盖操作前，评估影响范围，必要时提醒用户

### Step 3：按操作类型执行

#### 3.1 读取文件

```
1. fileExists(path) → 确认文件存在
2. detectFileEncoding(path) → 检测编码（可选，readFile 会自动识别）
3. readFile(path, charset) → 读取内容
4. 如果 truncated=true，说明文件超过 2MB，需分段处理或提示用户
```

#### 3.2 搜索文件

按文件名查找：
```
1. glob_files(pattern, path) → 按 glob 模式搜索
   示例：glob_files("**/*.java", "/project/src") 查找所有 Java 文件
   示例：glob_files("**/application*.properties", "/project") 查找所有配置文件
```

按内容检索：
```
1. grep_files(pattern, path, glob) → 搜索文件内容
   示例：grep_files("ERROR", "/project/logs", "*.log") 在日志中搜索 ERROR
   示例：grep_files("TODO", "/project/src", "*.java") 在代码中查找 TODO
```

#### 3.3 创建文件或目录

```
1. fileExists(path) → 确认目标不存在（避免冲突）
2. createFile(path, isDirectory) → 创建文件或目录
3. 如需写入内容，再调用 writeFile
```

#### 3.4 写入/修改文件

```
1. fileExists(path) → 检查文件是否存在
2. 如果文件存在且需要修改：
   a. readFile(path) → 读取原内容
   b. 在内存中修改内容
   c. writeFile(path, newContent, charset, false) → 覆盖写入
3. 如果需要追加内容：
   a. writeFile(path, appendContent, charset, true) → 追加写入
4. readFile(path) → 验证写入结果
```

#### 3.5 拷贝文件/目录

```
1. fileExists(source) → 确认源路径存在
2. fileExists(target) → 检查目标是否已存在
3. copyFile(source, target, overwrite) → 执行拷贝
4. getFileAttributes(target) → 验证拷贝结果
```

#### 3.6 删除文件/目录

```
1. fileExists(path) → 确认路径存在
2. getFileAttributes(path) → 获取信息，评估影响
3. 如果是目录且包含子内容，使用 recursive=true
4. deleteFile(path, recursive) → 执行删除
5. fileExists(path) → 验证已删除
```

### Step 4：结果汇总与验证

每次操作完成后：

1. **验证结果**：对写操作后读取验证、对删除操作后确认不存在
2. **汇总报告**：列出操作了哪些文件、成功/失败数量
3. **异常说明**：如有操作失败，说明失败原因和建议

## 边界与安全规则

- **路径安全**：所有路径必须在 `baseDirectory` 范围内，禁止路径穿越
- **大文件保护**：`readFile` 单次最多读取 2MB，超大文件应使用 `grep_files` 检索特定内容，而非全量读取
- **删除谨慎**：删除目录前必须用 `list_files` 或 `getFileAttributes` 确认影响范围；递归删除需明确告知用户
- **覆盖保护**：`writeFile` 默认覆盖写入，修改现有文件前应先读取原内容；`copyFile` 默认不覆盖
- **编码一致**：写入文件时指定编码（推荐 UTF-8），避免编码混乱
- **错误处理**：工具返回 `success=false` 时，读取 `message` 字段了解原因并告知用户
- **不编造路径**：不要假设文件存在，必须先检查再操作

## 输出要求

- 用中文回答
- 使用 Markdown 格式
- 操作结果使用表格或列表汇总
- 对危险操作（删除、覆盖）明确标注风险提醒

## 参考资料

读取文件 `references/file-operations-guide.md` 补充详细的操作示例和常见问题处理方案。
