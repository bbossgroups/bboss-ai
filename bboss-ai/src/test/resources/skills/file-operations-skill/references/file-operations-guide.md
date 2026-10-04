# 文件操作详细指南

本文档是 `file-operations-skill` 的补充参考资料，提供详细的操作示例和常见问题处理方案。

## 一、常见操作场景示例

### 场景 1：查看项目配置文件

用户请求："帮我看看项目的配置文件内容"

```
1. glob_files("**/*.properties", "/project") → 找到所有 .properties 文件
2. glob_files("**/*.yml", "/project") → 找到所有 .yml 文件
3. glob_files("**/*.yaml", "/project") → 找到所有 .yaml 文件
4. readFile(path) → 逐个读取配置文件内容
5. 汇总所有配置项，标注关键配置和可能的风险项
```

### 场景 2：日志错误排查

用户请求："查找日志中的 ERROR 和异常信息"

```
1. glob_files("**/*.log", "/project/logs") → 找到所有日志文件
2. grep_files("ERROR", "/project/logs", "*.log") → 搜索 ERROR 关键字
3. grep_files("Exception", "/project/logs", "*.log") → 搜索异常关键字
4. 对搜索结果按时间或严重程度分类汇总
5. 如需查看上下文，用 readFile 读取相关日志文件的特定段落
```

### 场景 3：项目结构梳理

用户请求："列出项目的目录结构"

```
1. list_files("/project") → 列出根目录
2. 对关键子目录递归调用 list_files
3. getFileAttributes(path) → 获取文件大小和修改时间
4. 输出项目结构树形图，标注关键文件和目录
```

### 场景 4：批量创建项目骨架

用户请求："创建标准的 Maven 项目结构"

```
1. createFile("/project/src/main/java", true) → 创建源码目录
2. createFile("/project/src/main/resources", true) → 创建资源目录
3. createFile("/project/src/test/java", true) → 创建测试目录
4. createFile("/project/src/test/resources", true) → 创建测试资源目录
5. createFile("/project/pom.xml", false) → 创建 pom 文件
6. writeFile("/project/pom.xml", pomContent) → 写入 pom 模板内容
7. 汇总创建的目录和文件列表
```

### 场景 5：配置文件修改

用户请求："把数据库连接地址改为新的地址"

```
1. grep_files("jdbc", "/project", "*.properties") → 找到包含数据库配置的项
2. readFile(path) → 读取目标配置文件完整内容
3. 在内存中修改对应的值
4. writeFile(path, newContent, "UTF-8", false) → 覆盖写入
5. readFile(path) → 验证修改结果
6. 输出修改前后的对比
```

### 场景 6：代码搜索与统计

用户请求："统计项目中有多少个 Java 类，以及代码行数"

```
1. glob_files("**/*.java", "/project/src") → 找到所有 Java 文件
2. 统计文件数量
3. 对关键文件用 readFile 读取并统计行数
4. 用 grep_files 搜索特定模式（如 @Service、@Controller 等注解）
5. 输出统计报告：文件数、总行数、按类型分布
```

### 场景 7：文件备份与迁移

用户请求："把 src 目录下的所有配置文件备份到 backup 目录"

```
1. createFile("/project/backup", true) → 创建备份目录
2. glob_files("**/*.properties", "/project/src") → 找到所有配置文件
3. glob_files("**/*.yml", "/project/src") → 找到所有 yml 文件
4. 逐个 copyFile(source, "/project/backup/" + relativePath, true) → 拷贝到备份目录
5. 汇总备份结果：成功/失败文件数、总大小
```

### 场景 8：编码问题排查

用户请求："这个文件读取出来是乱码"

```
1. detectFileEncoding(path) → 检测文件实际编码
2. 如果编码不是 UTF-8：
   a. readFile(path, detectedEncoding) → 用检测到的编码重新读取
3. 如果需要转换为 UTF-8：
   a. readFile(path, detectedEncoding) → 用原编码读取
   b. writeFile(path, content, "UTF-8", false) → 用 UTF-8 重新写入
4. readFile(path) → 验证转换后的内容
```

## 二、工具返回结果说明

### 成功结果

所有工具返回 `Map` 结构，成功时包含：
```json
{
  "success": true,
  "message": "操作描述",
  "...": "具体结果字段（因工具而异）"
}
```

### 失败结果

失败时包含：
```json
{
  "success": false,
  "message": "失败原因描述"
}
```

### readFile 特殊字段

| 字段 | 说明 |
| ---- | ---- |
| `content` | 文件内容字符串 |
| `charset` | 实际使用的字符编码 |
| `truncated` | 是否被截断（文件超过 2MB） |
| `maxReadSize` | 最大读取字节数 |

### getFileAttributes 返回字段

| 字段 | 说明 |
| ---- | ---- |
| `path` | 绝对路径 |
| `name` | 文件/目录名 |
| `isFile` | 是否为文件 |
| `isDirectory` | 是否为目录 |
| `size` | 大小（字节） |
| `sizeReadable` | 可读大小（如 "1.23 MB"） |
| `lastModified` | 最后修改时间 |
| `creationTime` | 创建时间（部分系统不支持） |
| `canRead` | 是否可读 |
| `canWrite` | 是否可写 |
| `canExecute` | 是否可执行 |
| `isHidden` | 是否隐藏 |
| `childrenCount` | 子项数量（仅目录） |

## 三、常见问题处理

### Q1：读取文件报"文件不存在"
- 先用 `fileExists(path)` 确认路径是否正确
- 检查路径中的分隔符是否正确（Windows 用 `\`，Linux 用 `/`）
- 检查路径是否在 `baseDirectory` 允许范围内

### Q2：读取中文文件出现乱码
- 先用 `detectFileEncoding(path)` 检测编码
- 用检测到的编码作为 `charset` 参数重新读取
- 常见中文编码：UTF-8、GBK、GB2312

### Q3：写入文件后内容丢失
- 确认 `append` 参数：`false`（默认）= 覆盖写入，`true` = 追加写入
- 修改文件前务必先 `readFile` 读取原内容
- 写入后用 `readFile` 验证

### Q4：删除目录失败
- 目录非空时需要设置 `recursive=true`
- 检查目录中是否有只读文件
- 检查是否有其他进程占用目录中的文件

### Q5：拷贝大文件失败
- 检查目标磁盘空间是否充足
- 检查目标路径的父目录是否存在（`copyFile` 会自动创建父目录）
- 如果目标已存在，需设置 `overwrite=true`

### Q6：glob_files 找不到文件
- 检查 pattern 语法：`**` 表示递归匹配，`*` 表示单层匹配
- 确认 path 参数指向的目录存在
- 尝试放宽匹配条件，如 `**/*` 列出所有文件

### Q7：grep_files 搜索结果为空
- 确认搜索路径存在且包含目标文件类型
- 检查 glob 参数是否过于严格
- 尝试搜索更短的关键词
