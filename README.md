# IoT Sensor Network - Backend Java MQTT

Versión MQTT del backend. Comunicación bidireccional en tiempo real con los dispositivos Pico W via broker Mosquitto.

## Diferencias con backend-java (HTTP)

| Aspecto | HTTP | MQTT |
|---------|------|------|
| Protocolo dispositivo → backend | HTTP POST cada 5 min | MQTT publish (conexión persistente) |
| Backend → dispositivo | No posible | Publica comandos en topic |
| Medición bajo demanda | No | Sí (POST /request-measurement) |
| Latencia | ~500ms por request | ~50ms por mensaje |
| Broker requerido | No | Mosquitto (contenedor) |

## Arquitectura

```
┌──────────┐     ┌──────────────┐     ┌───────────┐     ┌──────────┐
│ Frontend │────▶│ Backend Java │◄───▶│ Mosquitto │◄───▶│ Pico W   │
│          │     │              │     │  (MQTT)   │     │          │
└──────────┘     └──────┬───────┘     └───────────┘     └──────────┘
                        │
                        ▼
                 ┌──────────────┐
                 │   DynamoDB   │
                 └──────────────┘
```

## Topics MQTT

| Topic | Dirección | Contenido |
|-------|-----------|-----------|
| `devices/{device_id}/telemetry` | Pico W → Backend | Payload de medición (JSON + HMAC) |
| `devices/{device_id}/command` | Backend → Pico W | Comandos (ej: `{"action": "measure_now"}`) |

## Endpoints REST

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/v1/measurements` | Recibe medición HTTP (fallback) |
| POST | `/api/v1/devices` | Registrar dispositivo |
| GET | `/api/v1/devices/{deviceId}` | Info de un dispositivo |
| DELETE | `/api/v1/devices/{deviceId}` | Eliminar dispositivo |
| GET | `/api/v1/sensors` | Sensores agrupados con última medición |
| **POST** | **`/api/v1/devices/{deviceId}/request-measurement`** | **Solicitar medición en tiempo real via MQTT** |
| GET | `/actuator/health` | Health check |

## Desarrollo local

```bash
# 1. Levantar Mosquitto + DynamoDB local
docker compose up -d

# 2. Construir y correr el backend
docker build -t iot-sensor-mqtt .
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=local \
  -e MQTT_BROKER_URL=tcp://localhost:1883 \
  --network=host \
  iot-sensor-mqtt
```

## Variables de entorno

| Variable | Default | Descripción |
|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | `local` | Perfil (local/prod) |
| `MQTT_BROKER_URL` | `tcp://localhost:1883` | URL del broker MQTT |
| `MQTT_CLIENT_ID` | `iot-backend-server` | Client ID del backend |
| `AWS_REGION` | `us-east-1` | Región DynamoDB |
| `PORT` | `8080` | Puerto del servidor |

## Flujo: Medición bajo demanda

```
1. Frontend: POST /api/v1/devices/pico-mx-000001/request-measurement
2. Backend:  publica MQTT → devices/pico-mx-000001/command → {"action":"measure_now"}
3. Pico W:   recibe comando → lee sensor → publica MQTT → devices/pico-mx-000001/telemetry
4. Backend:  recibe telemetry → valida HMAC → guarda en DynamoDB
5. Frontend: consulta GET /api/v1/sensors para obtener la nueva medición
```

## Archivos nuevos respecto a backend-java

```
+ mosquitto.conf                          ← Config del broker
+ src/.../config/MqttConfig.java          ← Bean del cliente MQTT
+ src/.../service/MqttSubscriberService.java  ← Escucha telemetry
+ src/.../service/MqttPublisherService.java   ← Publica commands
```
