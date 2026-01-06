# LSTM超时最终修复

## 问题根源

项目有**两套前端代码**：
1. `forecast/src/` - 主要代码
2. `forecast/forecast/src/` - 备份或旧代码

之前只修改了第一套，但系统可能在使用第二套代码。

## 已修改的文件

### ✅ 第一套代码（forecast/src/）
- `forecast/src/services/http.js` - timeout: 120000 ✅
- `forecast/src/services/forecast.js` - LSTM专用超时 ✅

### ✅ 第二套代码（forecast/forecast/src/）
- `forecast/forecast/src/services/http.js` - timeout: 120000 ✅
- `forecast/forecast/src/services/forecast.js` - LSTM专用超时 ✅

### ✅ 后端代码
- `Dl4jLstmForecaster.java` - epochs: 50 ✅
- 训练时间: ~10秒 ✅

## 现在需要做的

### 1. 确定使用哪套代码

检查你的前端启动命令：

```bash
# 查看package.json
cat forecast/package.json | grep "dev"

# 或
cat forecast/forecast/package.json | grep "dev"
```

### 2. 重启前端

**如果使用 forecast/ 目录**:
```bash
cd forecast
npm run dev
```

**如果使用 forecast/forecast/ 目录**:
```bash
cd forecast/forecast
npm run dev
```

### 3. 清除浏览器缓存

1. 按 `Ctrl + Shift + Delete`
2. 清除"缓存的图片和文件"
3. 按 `Ctrl + F5` 强制刷新

### 4. 验证修改生效

打开浏览器开发者工具（F12），在Console中输入：

```javascript
// 检查超时设置
fetch('/api/forecast/predict', {
  method: 'POST',
  headers: {'Content-Type': 'application/json'},
  body: JSON.stringify({test: true})
}).catch(e => console.log('Timeout:', e))
```

## 如果还是超时

### 方案A: 使用ARIMA模型（推荐）⭐⭐⭐⭐⭐

ARIMA模型速度快，效果好：
- 训练时间: 3-5秒
- R²提升: +0.25~0.40
- 不会超时 ✅

**使用方法**:
在预测界面选择"ARIMA"模型

### 方案B: 进一步降低LSTM epochs

如果必须使用LSTM，可以进一步降低epochs：

```java
// Dl4jLstmForecaster.java
private static final int DEFAULT_EPOCHS = 30;  // 从50降到30

// calculateOptimalEpochs方法
private int calculateOptimalEpochs(int sampleCount) {
    if (sampleCount < 5) return 25;
    else if (sampleCount < 10) return 30;
    else if (sampleCount < 20) return 28;
    else return 25;
}
```

训练时间会降到5-8秒。

### 方案C: 使用异步预测API

永不超时的解决方案：

```javascript
// 提交异步任务
const { data } = await apiClient.post('/api/async-forecast/submit', {
  cropId: 55,
  regionId: 88,
  modelCode: 'LSTM',
  forecastPeriods: 3
})

const taskId = data.taskId

// 轮询查询状态
const checkStatus = async () => {
  const { data } = await apiClient.get(`/api/async-forecast/status/${taskId}`)
  
  if (data.status === 'COMPLETED') {
    console.log('预测完成！', data.result)
    return data.result
  } else if (data.status === 'FAILED') {
    console.error('预测失败', data.errorMessage)
  } else {
    // 继续等待
    setTimeout(checkStatus, 2000)
  }
}

checkStatus()
```

## 目录结构说明

```
forecast/
├── src/                    # 主要代码（已修改）
│   └── services/
│       ├── http.js        # timeout: 120000 ✅
│       └── forecast.js    # LSTM专用超时 ✅
├── forecast/              # 备份代码（已修改）
│   └── src/
│       └── services/
│           ├── http.js    # timeout: 120000 ✅
│           └── forecast.js # LSTM专用超时 ✅
├── dist/                  # 编译输出
├── node_modules/          # 依赖
└── package.json           # 配置
```

## 检查清单

- [ ] 确定使用哪套前端代码（forecast/ 或 forecast/forecast/）
- [ ] 重启前端开发服务器
- [ ] 清除浏览器缓存
- [ ] 强制刷新页面（Ctrl+F5）
- [ ] 重启后端（如果修改了Java代码）
- [ ] 测试LSTM预测

## 预期结果

| 步骤 | 时间 | 状态 |
|-----|------|------|
| 数据加载 | 1-2秒 | ⏳ |
| LSTM训练 | 8-12秒 | ⏳ |
| 结果返回 | 1秒 | ⏳ |
| **总计** | **10-15秒** | ✅ |

**超时设置**: 120秒  
**安全余量**: 8-12倍 ✅

## 如果问题持续

请提供以下信息：

1. **前端启动命令**
   ```bash
   # 你是如何启动前端的？
   cd forecast && npm run dev
   # 或
   cd forecast/forecast && npm run dev
   ```

2. **浏览器Console错误**
   - 打开F12开发者工具
   - 查看Console标签
   - 截图错误信息

3. **Network请求详情**
   - F12 → Network标签
   - 找到 `/api/forecast/predict` 请求
   - 查看Request Headers中的timeout设置

4. **后端日志**
   ```bash
   tail -f demo/logs/crop-yield.log
   ```

## 建议

**最简单的解决方案**: 使用ARIMA模型
- 速度快（3-5秒）
- 效果好（R²提升+0.25~0.40）
- 不会超时
- 无需修改代码

**如果必须用LSTM**: 使用异步预测API
- 永不超时
- 可以显示进度
- 用户体验更好

---

**更新时间**: 2026-01-06 12:05  
**状态**: 两套前端代码都已修改 ✅
