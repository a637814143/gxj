export const spatialMapDefinitions = {
  macro: {
    mapKey: 'macro-agri-clusters',
    label: '示范片区',
    description: '跨区域对比示范片区产量、预测与风险分布',
    geoJson: {
      type: 'FeatureCollection',
      features: [
        {
          type: 'Feature',
          properties: { name: '北部示范区', childKey: 'northBelt' },
          geometry: {
            type: 'Polygon',
            coordinates: [
              [
                [101.2, 31.6],
                [101.2, 34.4],
                [104.6, 34.4],
                [104.6, 31.6],
                [101.2, 31.6]
              ]
            ]
          }
        },
        {
          type: 'Feature',
          properties: { name: '中部平原', childKey: 'centralPlain' },
          geometry: {
            type: 'Polygon',
            coordinates: [
              [
                [104.8, 30.2],
                [104.8, 33.1],
                [108.1, 33.1],
                [108.1, 30.2],
                [104.8, 30.2]
              ]
            ]
          }
        },
        {
          type: 'Feature',
          properties: { name: '南部丘陵', childKey: 'southHills' },
          geometry: {
            type: 'Polygon',
            coordinates: [
              [
                [101.6, 27.7],
                [101.6, 30.6],
                [104.9, 30.6],
                [104.9, 27.7],
                [101.6, 27.7]
              ]
            ]
          }
        }
      ]
    },
    regions: [
      {
        name: '北部示范区',
        production: 6.2,
        forecastMin: 5.7,
        forecastMax: 6.9,
        risk: 0.72,
        center: [102.8, 33],
        childKey: 'northBelt'
      },
      {
        name: '中部平原',
        production: 7.1,
        forecastMin: 6.8,
        forecastMax: 7.5,
        risk: 0.31,
        center: [106.5, 31.7],
        childKey: 'centralPlain'
      },
      {
        name: '南部丘陵',
        production: 5.4,
        forecastMin: 5.1,
        forecastMax: 5.8,
        risk: 0.55,
        center: [103.5, 29.2],
        childKey: 'southHills'
      }
    ],
    heatmapPoints: [
      { name: '北部示范区', value: [102.8, 33, 0.72] },
      { name: '中部平原', value: [106.5, 31.7, 0.31] },
      { name: '南部丘陵', value: [103.5, 29.2, 0.55] }
    ]
  },
  northBelt: {
    mapKey: 'north-belt-detail',
    parentKey: 'macro',
    label: '北部示范区',
    description: '川北高原粮仓分区的田块级观测',
    geoJson: {
      type: 'FeatureCollection',
      features: [
        {
          type: 'Feature',
          properties: { name: '川北粮仓' },
          geometry: {
            type: 'Polygon',
            coordinates: [
              [
                [101.4, 32],
                [101.4, 33.7],
                [102.8, 33.7],
                [102.8, 32],
                [101.4, 32]
              ]
            ]
          }
        },
        {
          type: 'Feature',
          properties: { name: '宁通灌区' },
          geometry: {
            type: 'Polygon',
            coordinates: [
              [
                [102.9, 32.2],
                [102.9, 33.9],
                [104.3, 33.9],
                [104.3, 32.2],
                [102.9, 32.2]
              ]
            ]
          }
        }
      ]
    },
    regions: [
      {
        name: '川北粮仓',
        production: 6.5,
        forecastMin: 6.1,
        forecastMax: 7.2,
        risk: 0.68,
        center: [102.1, 32.9]
      },
      {
        name: '宁通灌区',
        production: 5.8,
        forecastMin: 5.4,
        forecastMax: 6.4,
        risk: 0.48,
        center: [103.6, 33.1]
      }
    ],
    heatmapPoints: [
      { name: '川北粮仓', value: [102.1, 32.9, 0.68] },
      { name: '宁通灌区', value: [103.6, 33.1, 0.48] }
    ]
  },
  centralPlain: {
    mapKey: 'central-plain-detail',
    parentKey: 'macro',
    label: '中部平原',
    description: '中部产粮核心区的预测与风险对比',
    geoJson: {
      type: 'FeatureCollection',
      features: [
        {
          type: 'Feature',
          properties: { name: '河套冲积带' },
          geometry: {
            type: 'Polygon',
            coordinates: [
              [
                [105.1, 30.6],
                [105.1, 32.6],
                [106.6, 32.6],
                [106.6, 30.6],
                [105.1, 30.6]
              ]
            ]
          }
        },
        {
          type: 'Feature',
          properties: { name: '渠化稻作区' },
          geometry: {
            type: 'Polygon',
            coordinates: [
              [
                [106.7, 30.8],
                [106.7, 32.8],
                [107.9, 32.8],
                [107.9, 30.8],
                [106.7, 30.8]
              ]
            ]
          }
        }
      ]
    },
    regions: [
      {
        name: '河套冲积带',
        production: 7.4,
        forecastMin: 7,
        forecastMax: 7.9,
        risk: 0.22,
        center: [105.8, 31.8]
      },
      {
        name: '渠化稻作区',
        production: 6.9,
        forecastMin: 6.6,
        forecastMax: 7.3,
        risk: 0.37,
        center: [107.3, 31.9]
      }
    ],
    heatmapPoints: [
      { name: '河套冲积带', value: [105.8, 31.8, 0.22] },
      { name: '渠化稻作区', value: [107.3, 31.9, 0.37] }
    ]
  },
  southHills: {
    mapKey: 'south-hills-detail',
    parentKey: 'macro',
    label: '南部丘陵',
    description: '丘陵地带复合风险热力分布',
    geoJson: {
      type: 'FeatureCollection',
      features: [
        {
          type: 'Feature',
          properties: { name: '云岭梯田' },
          geometry: {
            type: 'Polygon',
            coordinates: [
              [
                [101.9, 28.2],
                [101.9, 29.8],
                [103.2, 29.8],
                [103.2, 28.2],
                [101.9, 28.2]
              ]
            ]
          }
        },
        {
          type: 'Feature',
          properties: { name: '南溪旱作区' },
          geometry: {
            type: 'Polygon',
            coordinates: [
              [
                [103.3, 28.4],
                [103.3, 29.9],
                [104.6, 29.9],
                [104.6, 28.4],
                [103.3, 28.4]
              ]
            ]
          }
        }
      ]
    },
    regions: [
      {
        name: '云岭梯田',
        production: 5.2,
        forecastMin: 4.9,
        forecastMax: 5.6,
        risk: 0.63,
        center: [102.5, 29.1]
      },
      {
        name: '南溪旱作区',
        production: 5.6,
        forecastMin: 5.2,
        forecastMax: 6,
        risk: 0.58,
        center: [103.9, 29.2]
      }
    ],
    heatmapPoints: [
      { name: '云岭梯田', value: [102.5, 29.1, 0.63] },
      { name: '南溪旱作区', value: [103.9, 29.2, 0.58] }
    ]
  }
}
