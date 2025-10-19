"""Utility functions for training and using the crop yield prediction model."""
from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Any, Dict, Iterable, Optional

import pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.ensemble import RandomForestRegressor
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder, StandardScaler


DATA_DIR = Path(__file__).resolve().parent / "data"
DEFAULT_DATASET = DATA_DIR / "crop_yield_samples.csv"


@dataclass
class PredictionInput:
    """Structured input expected by the model."""

    crop: str
    region: str
    year: int
    sown_area_kha: float
    avg_price_yuan_per_ton: float

    @classmethod
    def from_payload(cls, payload: Dict[str, Any]) -> "PredictionInput":
        missing = [key for key in ("crop", "region", "year", "sown_area_kha", "avg_price_yuan_per_ton") if key not in payload]
        if missing:
            raise ValueError(f"缺少必要字段: {', '.join(missing)}")

        try:
            year = int(payload["year"])
        except (TypeError, ValueError) as exc:
            raise ValueError("年份必须是整数") from exc

        try:
            sown_area = float(payload["sown_area_kha"])
        except (TypeError, ValueError) as exc:
            raise ValueError("播种面积(千公顷)必须是数值") from exc

        try:
            avg_price = float(payload["avg_price_yuan_per_ton"])
        except (TypeError, ValueError) as exc:
            raise ValueError("平均价格(元/吨)必须是数值") from exc

        crop = str(payload["crop"]).strip()
        region = str(payload["region"]).strip()
        if not crop or not region:
            raise ValueError("作物与地区名称不能为空")

        return cls(
            crop=crop,
            region=region,
            year=year,
            sown_area_kha=sown_area,
            avg_price_yuan_per_ton=avg_price,
        )

    def to_frame(self) -> pd.DataFrame:
        return pd.DataFrame([
            {
                "crop": self.crop,
                "region": self.region,
                "year": self.year,
                "sown_area_kha": self.sown_area_kha,
                "avg_price_yuan_per_ton": self.avg_price_yuan_per_ton,
            }
        ])


def load_training_data(csv_path: Optional[Path] = None) -> pd.DataFrame:
    """Load and clean the training dataset."""
    csv_path = csv_path or DEFAULT_DATASET
    if not csv_path.exists():
        raise FileNotFoundError(f"找不到数据集文件: {csv_path}")

    df = pd.read_csv(csv_path)

    rename_map = {
        "作物": "crop",
        "作物类别": "crop_category",
        "地区": "region",
        "行政级别": "administrative_level",
        "上级地区": "parent_region",
        "年份": "year",
        "播种面积(千公顷)": "sown_area_kha",
        "产量(万吨)": "yield_10kt",
        "单产(吨/公顷)": "yield_per_ha",
        "平均价格(元/吨)": "avg_price_yuan_per_ton",
        "数据来源": "data_source",
        "采集日期": "collected_at",
    }
    df = df.rename(columns=rename_map)

    numeric_columns = ["year", "sown_area_kha", "yield_10kt", "yield_per_ha", "avg_price_yuan_per_ton"]
    for column in numeric_columns:
        if column in df.columns:
            df[column] = pd.to_numeric(df[column], errors="coerce")

    df = df.dropna(subset=["crop", "region", "year", "sown_area_kha", "avg_price_yuan_per_ton", "yield_10kt"])
    df = df[df["year"] >= 1900]
    return df


def build_model(random_state: int = 42) -> Pipeline:
    """Create an untrained scikit-learn pipeline for crop yield prediction."""
    categorical_features: Iterable[str] = ["crop", "region"]
    numeric_features: Iterable[str] = ["year", "sown_area_kha", "avg_price_yuan_per_ton"]

    preprocessor = ColumnTransformer(
        transformers=[
            ("categorical", OneHotEncoder(handle_unknown="ignore"), list(categorical_features)),
            ("numeric", Pipeline(steps=[("scaler", StandardScaler())]), list(numeric_features)),
        ]
    )

    regressor = RandomForestRegressor(n_estimators=200, random_state=random_state)

    model = Pipeline(
        steps=[
            ("preprocessor", preprocessor),
            ("regressor", regressor),
        ]
    )
    return model


def train_model(dataframe: pd.DataFrame, *, random_state: int = 42) -> Pipeline:
    """Train the prediction pipeline on the provided dataframe."""
    features = ["crop", "region", "year", "sown_area_kha", "avg_price_yuan_per_ton"]
    target = "yield_10kt"

    missing_columns = [col for col in features + [target] if col not in dataframe.columns]
    if missing_columns:
        raise ValueError(f"数据集中缺少列: {', '.join(missing_columns)}")

    model = build_model(random_state=random_state)
    model.fit(dataframe[features], dataframe[target])
    return model


def predict_yield(model: Pipeline, input_data: PredictionInput) -> Dict[str, Any]:
    """Generate a prediction using the trained model and return structured results."""
    prediction = model.predict(input_data.to_frame())
    predicted_yield_10kt = float(prediction[0])

    return {
        "predicted_yield_10kt": predicted_yield_10kt,
        "predicted_yield_tonnes": predicted_yield_10kt * 10000,
        "inputs": input_data.__dict__,
    }


__all__ = [
    "PredictionInput",
    "load_training_data",
    "build_model",
    "train_model",
    "predict_yield",
]
