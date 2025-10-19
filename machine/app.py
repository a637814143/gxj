"""Flask service that exposes machine learning crop yield predictions."""
from __future__ import annotations

import json
import logging
import os
from pathlib import Path
from typing import Any, Dict

from flask import Flask, jsonify, request
from flask_cors import CORS

from model import (
    DEFAULT_DATASET,
    PredictionInput,
    load_training_data,
    predict_yield,
    train_model,
)

LOGGER = logging.getLogger(__name__)


def create_app() -> Flask:
    app = Flask(__name__)
    CORS(app)

    dataset_path = Path(os.environ.get("CROP_DATASET_PATH", DEFAULT_DATASET))
    app.config["DATASET_PATH"] = dataset_path

    dataset = load_training_data(dataset_path)
    model = train_model(dataset)
    app.config["MODEL"] = model
    app.config["TRAINING_ROWS"] = len(dataset)

    @app.route("/health", methods=["GET"])
    def health() -> Any:
        return jsonify(
            {
                "status": "ok",
                "records": app.config["TRAINING_ROWS"],
                "dataset": str(app.config["DATASET_PATH"].name),
            }
        )

    @app.route("/predict", methods=["POST"])
    def predict() -> Any:
        try:
            payload: Dict[str, Any] = request.get_json(force=True, silent=False)  # type: ignore[assignment]
        except Exception:
            return jsonify({"success": False, "error": "请求体需要是合法的 JSON"}), 400

        if not isinstance(payload, dict):
            return jsonify({"success": False, "error": "JSON 请求体必须是对象"}), 400

        try:
            prediction_input = PredictionInput.from_payload(payload)
            result = predict_yield(app.config["MODEL"], prediction_input)
            return jsonify({"success": True, "data": result})
        except ValueError as exc:
            return jsonify({"success": False, "error": str(exc)}), 400
        except Exception as exc:  # pragma: no cover - defensive logging
            LOGGER.exception("Prediction failed: %s", exc)
            return jsonify({"success": False, "error": "服务器内部错误"}), 500

    @app.route("/model/preview", methods=["GET"])
    def preview() -> Any:
        try:
            preview_rows = max(int(request.args.get("limit", 5)), 1)
        except (TypeError, ValueError):
            preview_rows = 5
        subset = dataset.head(preview_rows)
        return jsonify(
            {
                "columns": list(subset.columns),
                "rows": json.loads(subset.to_json(orient="records", force_ascii=False)),
            }
        )

    return app


app = create_app()


if __name__ == "__main__":
    port = int(os.environ.get("PORT", 5001))
    debug = os.environ.get("FLASK_DEBUG", "false").lower() == "true"
    app.run(host="0.0.0.0", port=port, debug=debug)
