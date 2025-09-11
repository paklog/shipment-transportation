# Grafana Dashboard for Shipment & Transportation Service

This document contains the JSON definition for a Grafana dashboard designed to monitor the key business and infrastructure metrics of the Shipment & Transportation service.

## How to Use

1.  **Copy the JSON:** Copy the entire JSON code block below.
2.  **Import into Grafana:**
    - In your Grafana instance, navigate to the **Dashboards** section.
    - Click the **New** button and select **Import**.
    - Paste the copied JSON into the **"Import via panel json"** text area.
    - Click **Load**.
3.  **Select Datasource:** Choose your configured Prometheus data source from the dropdown menu.
4.  **Import:** Click the **Import** button to create the dashboard.

---

## Dashboard JSON

```json
{
  "__inputs": [
    {
      "name": "DS_PROMETHEUS",
      "label": "Prometheus",
      "description": "",
      "type": "datasource",
      "pluginId": "prometheus",
      "pluginName": "Prometheus"
    }
  ],
  "__requires": [
    {
      "type": "grafana",
      "id": "grafana",
      "name": "Grafana",
      "version": "8.0.0"
    },
    {
      "type": "datasource",
      "id": "prometheus",
      "name": "Prometheus",
      "version": "1.0.0"
    }
  ],
  "annotations": {
    "list": [
      {
        "builtIn": 1,
        "datasource": {
          "type": "grafana",
          "uid": "-- Grafana --"
        },
        "enable": true,
        "hide": true,
        "iconColor": "rgba(0, 211, 255, 1)",
        "name": "Annotations & Alerts",
        "type": "dashboard"
      }
    ]
  },
  "editable": true,
  "fiscalYearStartMonth": 0,
  "graphTooltip": 0,
  "links": [],
  "liveNow": false,
  "panels": [
    {
      "type": "row",
      "title": "Business Metrics",
      "gridPos": {
        "h": 1,
        "w": 24,
        "x": 0,
        "y": 0
      },
      "collapsed": false
    },
    {
      "title": "Shipments Created",
      "type": "stat",
      "datasource": {
        "type": "prometheus",
        "uid": "${DS_PROMETHEUS}"
      },
      "gridPos": {
        "h": 8,
        "w": 6,
        "x": 0,
        "y": 1
      },
      "options": {
        "reduceOptions": {
          "values": false,
          "calcs": [
            "lastNotNull"
          ],
          "fields": ""
        },
        "orientation": "auto",
        "text": {},
        "textMode": "auto",
        "colorMode": "value",
        "graphMode": "area",
        "justifyMode": "auto"
      },
      "targets": [
        {
          "expr": "sum(rate(shipments_created_total{application=\"shipment-transportation\"}[5m]))",
          "legendFormat": "per second"
        }
      ]
    },
    {
      "title": "Load Funnel",
      "type": "barchart",
      "datasource": {
        "type": "prometheus",
        "uid": "${DS_PROMETHEUS}"
      },
      "gridPos": {
        "h": 8,
        "w": 18,
        "x": 6,
        "y": 1
      },
      "options": {
        "orientation": "horizontal",
        "showValue": "auto",
        "legend": {
          "displayMode": "hidden"
        }
      },
      "targets": [
        {
          "expr": "sum(loads_created_total) or vector(0)",
          "legendFormat": "Created"
        },
        {
          "expr": "sum(loads_tendered_total) or vector(0)",
          "legendFormat": "Tendered"
        },
        {
          "expr": "sum(loads_booked_total) or vector(0)",
          "legendFormat": "Booked"
        }
      ]
    },
    {
      "type": "row",
      "title": "Infrastructure Metrics",
      "gridPos": {
        "h": 1,
        "w": 24,
        "x": 0,
        "y": 9
      },
      "collapsed": false
    },
    {
      "title": "External API Calls (by Carrier)",
      "type": "timeseries",
      "datasource": {
        "type": "prometheus",
        "uid": "${DS_PROMETHEUS}"
      },
      "gridPos": {
        "h": 9,
        "w": 12,
        "x": 0,
        "y": 10
      },
      "options": {
        "legend": {
          "calcs": [],
          "displayMode": "list",
          "placement": "bottom"
        }
      },
      "targets": [
        {
          "expr": "sum(rate(carrier_api_calls_total{status=\"success\"}[5m])) by (carrier, operation)",
          "legendFormat": "{{carrier}} - {{operation}}"
        }
      ]
    },
    {
      "title": "Kafka Events Consumed",
      "type": "timeseries",
      "datasource": {
        "type": "prometheus",
        "uid": "${DS_PROMETHEUS}"
      },
      "gridPos": {
        "h": 9,
        "w": 12,
        "x": 12,
        "y": 10
      },
      "options": {
        "legend": {
          "calcs": [],
          "displayMode": "list",
          "placement": "bottom"
        }
      },
      "targets": [
        {
          "expr": "sum(rate(kafka_events_consumed_total[5m]))"
        }
      ]
    }
  ],
  "schemaVersion": 36,
  "style": "dark",
  "tags": [],
  "templating": {
    "list": [
      {
        "current": {
          "selected": true,
          "text": "Prometheus",
          "value": "Prometheus"
        },
        "hide": 0,
        "includeAll": false,
        "label": "Datasource",
        "multi": false,
        "name": "datasource",
        "options": [],
        "query": "prometheus",
        "queryValue": "",
        "refresh": 1,
        "regex": "",
        "skipUrlSync": false,
        "type": "datasource"
      }
    ]
  },
  "time": {
    "from": "now-6h",
    "to": "now"
  },
  "timepicker": {},
  "timezone": "",
  "title": "Shipment & Transportation Service",
  "uid": "c1a2b3d4-e5f6-a7b8-c9d0-e1f2a3b4c5d6",
  "version": 1,
  "weekStart": ""
}
```