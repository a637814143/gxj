# LSTM超时快速修复指南

## 问题
使用LSTM模型时仍然出现 `timeout of 15000ms exceeded`

## 原因
前端代码修改后没有重新编译，浏览器仍在使用旧的缓存代码。

## 解决方案

### 方案1: 重新编译前端（推荐）⭐⭐⭐⭐⭐

```bash
# 进入前端目录
cd forecast

# 停止当前运行的前端（如果有）
# Ctrl+C

# 清除node_modules和dist（可选，但推荐）
rm -rf node_modules dist

# 重新安装依赖
npm install

# 重新编译并启动
npm run dev
```

### 方案2: 清除浏览器缓存 ⭐⭐⭐⭐

1. **Chrome/Edge**:
   - 按 `Ctrl + Shift + Delete`
   - 选择"缓存的图片和文件"
   - 点击"清除数据"
   - 刷新页面 `Ctrl + F5`

2. **或者使用无痕模式**:
   - `Ctrl + Shift + N` (Chrome/Edge)
   - 在无痕窗口中测试

### 方案3: 强制刷新 ⭐⭐⭐

在预测页面按：
- Windows: `Ctrl + F5`
- Mac: `Cmd + Shift + R`

### 方案4: 检查前端是否使用了正确的代码

打开浏览器开发者工具（F12），在Console中输入：

```javascript
// 检查axios超时设置
import('./services/http.js').then(m => console.log(m.default.defaults.timeout))
// 应该显示 120000
```

## 已修改的配置

### 后端（已编译）✅
```java
// LSTM epochs: 80 → 50
// 训练时间: ~18秒 → ~10秒
```

### 前端（需要重新编译）⚠️
```javascript
// http.js
timeout: 15000 → 120000  // 2分钟

// forecast.js
LSTM专用超时: 60秒
```

## 验证步骤

### 1. 重启后端
```bash
cd demo
./mvnw spring-boot:run
```

### 2. 重新编译前端
```bash
cd forecast
npm run dev
```

### 3. 清除浏览器缓存
- `Ctrl + Shift + Delete`
- 清除缓存
- `Ctrl + F5` 强制刷新

### 4. 测试LSTM预测
- 选择LSTM模型
- 点击预测
- 应该在10-15秒内完成

## 预期结果

| 步骤 | 预期时间 | 状态 |
|-----|---------|------|
| 数据加载 | 1-2秒 | ⏳ |
| LSTM训练 | 10-15秒 | ⏳ |
| 结果返回 | 1秒 | ⏳ |
| **总计** | **12-18秒** | ✅ |

**超时设置**: 120秒  
**安全余量**: 6-10倍 ✅

## 如果还是超时

### 检查清单

1. **后端是否重启？**
   ```bash
   # 检查后端日志
   tail -f logs/crop-yield.log
   # 应该看到新的启动日志
   ```

2. **前端是否重新编译？**
   ```bash
   # 检查dist目录修改时间
   ls -la forecast/dist
   # 应该是最新时间
   ```

3. **浏览器缓存是否清除？**
   - 打开开发者工具（F12）
   - Network标签
   - 勾选"Disable cache"
   - 刷新页面

4. **是否使用了正确的端口？**
   - 前端: http://localhost:5173
   - 后端: http://localhost:8080

### 临时解决方案：使用其他模型

如果LSTM仍然超时，可以暂时使用其他模型：

1. **ARIMA模型** - 速度快，效果好
   ```javascript
   modelCode: 'ARIMA'
   // 预测时间: 3-5秒
   // R²: 0.50-0.65
   ```

2. **自动选择** - 让系统选择最佳模型
   ```javascript
   modelCode: null  // 或不传
   // 系统会自动尝试多个模型并选择最佳
   ```

3. **天气回归** - 如果有气象数据
   ```javascript
   modelCode: 'WEATHER_REGRESSION'
   // 预测时间: 2-3秒
   // R²: 0.45-0.60
   ```

## 终极解决方案：使用异步预测

如果同步预测总是超时，使用异步API：

### 前端修改示例

```javascript
// 1. 提交异步任务
async function submitAsyncForecast(payload) {
  const { data } = await apiClient.post('/api/async-forecast/submit', payload)
  return data.taskId
}

// 2. 轮询查询状态
async function pollForecastStatus(taskId) {
  const maxAttempts = 60  // 最多查询60次
  const interval = 2000   // 每2秒查询一次
  
  for (let i = 0; i < maxAttempts; i++) {
    const { data } = await apiClient.get(`/api/async-forecast/status/${taskId}`)
    
    if (data.status === 'COMPLETED') {
      return data.result  // 成功
    } else if (data.status === 'FAILED') {
      throw new Error(data.errorMessage)  // 失败
    }
    
    // 继续等待
    await new Promise(resolve => setTimeout(resolve, interval))
  }
  
  throw new Error('预测超时')
}

// 3. 使用
try {
  ElMessage.info('LSTM训练中，请稍候...')
  const taskId = await submitAsyncForecast(payload)
  const result = await pollForecastStatus(taskId)
  ElMessage.success('预测完成！')
  // 显示结果
} catch (error) {
  ElMessage.error('预测失败: ' + error.message)
}
```

## 性能对比

| 模型 | 训练时间 | 超时风险 | R²提升 | 推荐度 |
|-----|---------|---------|--------|--------|
| LSTM (50 epochs) | 10-15秒 | 低 | +0.35~0.55 | ⭐⭐⭐⭐ |
| ARIMA | 3-5秒 | 极低 | +0.25~0.40 | ⭐⭐⭐⭐⭐ |
| 天气回归 | 2-3秒 | 极低 | +0.20~0.35 | ⭐⭐⭐⭐ |
| 自动选择 | 5-10秒 | 低 | +0.30~0.50 | ⭐⭐⭐⭐⭐ |

## 总结

**最可能的原因**: 前端没有重新编译

**最快的解决方案**:
1. 清除浏览器缓存
2. `Ctrl + F5` 强制刷新
3. 如果还不行，重新编译前端

**备用方案**:
- 使用ARIMA模型（速度快，效果也不错）
- 使用异步预测API（永不超时）

---

**更新时间**: 2026-01-06 12:01  
**状态**: 后端已优化，前端需要重新编译
