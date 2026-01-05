# 审计日志字段中文映射

## 前端显示字段对照表

在前端显示审计日志时，请使用以下中文名称：

| 后端字段 | 前端显示 |
|---------|---------|
| username | 用户名 |
| operation | 操作类型 |
| module | 模块 |
| result | 结果 |
| ipAddress | IP地址 |
| userAgent | 用户代理 |
| createdAt | 记录时间 |
| executionTime | 执行时长 |

## 说明

- 后端API返回的字段名保持英文（如 `userAgent`）
- 前端在表格列标题等位置显示中文（如"用户代理"）
- 这样既保持了API的规范性，又提供了友好的中文界面
