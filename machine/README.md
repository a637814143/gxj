# 机器学习预测服务

`machine/` 目录提供一个基于 Flask 的机器学习微服务，用于农作物产量预测。服务启动时会加载 `data/crop_yield_samples.csv` 中的示例数据，并训练随机森林回归模型。

## 运行步骤

```bash
cd machine
python -m venv .venv
source .venv/bin/activate  # Windows 使用 .venv\\Scripts\\activate
pip install -r requirements.txt
python app.py
```

服务默认监听 `http://localhost:5001`，运行后可通过以下接口进行访问：

- `GET /health`：查看服务状态及训练数据量。
- `GET /model/preview?limit=5`：预览训练数据。
- `POST /predict`：根据输入特征返回预测产量。

### 示例请求

```bash
curl -X POST http://localhost:5001/predict \
  -H "Content-Type: application/json" \
  -d '{
    "crop": "粮食",
    "region": "云南省",
    "year": 2024,
    "sown_area_kha": 243.0,
    "avg_price_yuan_per_ton": 2325
  }'
```

> 💡 **Windows 命令行（cmd.exe）注意**：`\` 续行符与单引号在 Windows 默认终端中不起作用，请将上述命令写成单行，并使用双引号包裹 JSON：
>
> ```bat
> curl -X POST http://localhost:5001/predict -H "Content-Type: application/json" -d "{\"crop\":\"粮食\",\"region\":\"云南省\",\"year\":2024,\"sown_area_kha\":243.0,\"avg_price_yuan_per_ton\":2325}"
> ```
>
> 在 PowerShell 中可以使用反引号 `` ` `` 作为续行符，或同样直接粘贴单行命令。

响应示例：

```json
{
  "success": true,
  "data": {
    "predicted_yield_10kt": 1033.4,
    "predicted_yield_tonnes": 10334000.0,
    "inputs": {
      "crop": "粮食",
      "region": "云南省",
      "year": 2024,
      "sown_area_kha": 243.0,
      "avg_price_yuan_per_ton": 2325.0
    }
  }
}
```

可以通过设置环境变量 `CROP_DATASET_PATH` 指定外部数据集文件，服务会在启动时自动加载并重新训练模型。
