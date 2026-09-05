# 🏦 Motor Transaccional Bancario Distribuido en GCP

Diseñé e implementé un sistema bancario distribuido y tolerante a fallos capaz de procesar operaciones para 820,000 cuentas manteniendo un 100% de consistencia de saldos bajo pruebas de estrés continuo.

## 🚀 Arquitectura y Tecnologías
- **Backend:** Java (Programación Concurrente, `ReentrantLock`, `ConcurrentHashMap`).
- **Infraestructura Cloud (GCP):** 
  - **Google Cloud Storage:** Implementación de *Event Sourcing* (Append-Only Log) para persistencia inmutable y recuperación ante desastres (reemplazando BD relacionales).
  - **Google Cloud Pub/Sub:** Replicación asíncrona de eventos para sincronización de réplicas en tiempo real.
- **Observabilidad:** Dashboard de monitoreo en tiempo real construido con JavaScript y HTML (Fetch API).
- **Seguridad:** Autenticación de endpoints mediante JSON Web Tokens (JWT).

## ⚙️ Características Principales
- **Tolerancia a Fallos:** Capacidad de reconstrucción de estado (Cold Start) descargando y procesando el historial de transacciones desde la nube tras la caída total de los nodos.
- **Alta Concurrencia (Thread-Safety):** Soporte validado para carga masiva mediante un generador de estrés (50 hilos simultáneos con ratio 80/20 de lectura-escritura) sin colisiones de memoria ni bloqueos mutuos (*deadlocks*).
- **Modelo Líder-Réplica:** Arquitectura orientada a eventos para distribuir la carga de trabajo y garantizar alta disponibilidad.
