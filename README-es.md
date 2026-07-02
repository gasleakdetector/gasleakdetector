<p align="center">    
  <img src=".github/assets/cover.png" alt="Cover" />    
</p>

<p align="center">
  <img alt="License" src="https://img.shields.io/github/license/gasleakdetector/gasleakdetector?color=04A8F4&style=flat-square"/>
  <img alt="Version" src="https://img.shields.io/badge/version-1.1.0-04A8F4?style=flat-square"/>
  <img alt="Min SDK" src="https://img.shields.io/badge/min%20SDK-21-04A8F4?style=flat-square&logo=android"/>
  <img alt="Build" src="https://img.shields.io/github/actions/workflow/status/gasleakdetector/gasleakdetector/build.yml?style=flat-square&color=04A8F4"/>
  <img alt="Last commit" src="https://img.shields.io/github/last-commit/gasleakdetector/gasleakdetector?color=04A8F4&style=flat-square"/>
</p>

<div align="center">
  <h1>Gas Leak Detector - App</h1>
  <p><a target="_blank" href="README.md">English</a>&nbsp;&nbsp;|&nbsp;&nbsp;
    <a target="_blank" href="README-vi.md">Tiếng Việt</a>&nbsp;&nbsp;|&nbsp;&nbsp;
    <a target="_blank" href="README-de.md">Deutsch</a>&nbsp;&nbsp;|&nbsp;&nbsp;
    Español&nbsp;&nbsp;|&nbsp;&nbsp;
    <a target="_blank" href="README-fr.md">Français</a>&nbsp;&nbsp;|&nbsp;&nbsp;
    <a target="_blank" href="README-ja.md">日本語</a>&nbsp;&nbsp;|&nbsp;&nbsp;
    <a target="_blank" href="README-ko.md">한국어</a>&nbsp;&nbsp;|&nbsp;&nbsp;
    <a target="_blank" href="README-zh.md">中文</a>
  </p>
  <p align="center">Monitoreo de gas en tiempo real para Android - impulsado por ESP8266, Supabase y una API serverless edge.</p>
  <img src=".github/assets/intro.png" width="19.2%" alt="intro" />    
  <img src=".github/assets/config.png" width="19.2%" alt="config" />    
  <img src=".github/assets/home.png" width="19.2%" alt="home" />    
  <img src=".github/assets/stats.png" width="19.2%" alt="stats" />    
  <br/>    
  <br/>    
</div>

## Descripción General

Gas Leak Detector es un sistema de seguridad IoT completo. Un sensor MQ-6 en un ESP8266 muestrea continuamente los niveles de gas ambiental y envía lecturas a una API serverless de Vercel. Los datos se almacenan en Supabase y se transmiten en tiempo real a la aplicación Android via WebSocket - sin polling, sin demora.

El sistema está compuesto por tres repositorios independientes que forman un pipeline:

| Capa | Repositorio | Stack |
|---|---|---|
| Firmware | [gasleakdetector-esp](https://github.com/gasleakdetector/gasleakdetector-esp) | C++ / Arduino / ESP8266 |
| Backend | [gasleakdetector-server](https://github.com/gasleakdetector/gasleakdetector-server) | Node.js / Vercel / Supabase |
| Mobile | **gasleakdetector** *(este repositorio)* | Android / Java |

## Demo

[Ver Video Demo](https://www.youtube.com/watch?v=RLNf9Zphb1I)

## Configuración del Proyecto Completo

Puede ver una guía detallada sobre cómo configurar todo el proyecto [aquí](Tutorial/README-es.md)

## Flujo del Sistema

<p align="center">
  <img src=".github/assets/flow.png" alt="System flow" width="100%"/>
</p>

1. ESP8266 lee el sensor MQ-6 cada 400 ms y hace POST a `/api/ingest` con autenticación por API key.
2. La función edge de Vercel clasifica la lectura (`normal / warning / danger`) y escribe en Supabase. Las alertas de correo electrónico se activan con `danger` con un tiempo de enfriamiento configurable.
3. `pg_cron` dentro de Supabase agrega filas crudas en buckets por minuto y por hora automáticamente - no se necesita planificador externo.
4. La aplicación Android obtiene las credenciales de Supabase desde `/api/realtime-config` y abre una suscripción WebSocket directamente a `gas_logs_raw` para actualizaciones en vivo sin latencia.
5. Los gráficos históricos leen de buckets de horas pre-agregados, manteniendo las consultas rápidas independientemente del tiempo de funcionamiento del dispositivo.

## Características

- [x] Medidor PPM en vivo con valor animado y actualizaciones WebSocket en tiempo real
- [x] Clasificación de estado - Normal / Advertencia / Peligro con retroalimentación de color
- [x] Notificación de peligro persistente mientras los niveles de gas siguen siendo críticos
- [x] Gráfico histórico - datos agregados por hora con eje Y dinámico
- [x] Soporte multi-nodo - cambiar entre dispositivos ESP por `device_id`
- [x] Paginación basada en cursor - obtiene hasta 1.000 filas por solicitud con compresión gzip
- [x] Resiliencia offline - ESP encola hasta 60 lecturas localmente; la app muestra datos en caché
- [x] Pantalla de introducción de primer inicio - mostrada una vez, omitida en todos los lanzamientos posteriores
- [x] Retroalimentación - correo electrónico de un toque prellenado con la versión de la app en el asunto
- [x] Internacionalización - 8 idiomas: Inglés, Vietnamita, Alemán, Español, Francés, Japonés, Coreano, Chino
- [ ] Widget
- [ ] Configuración de múltiples umbrales por dispositivo
- [ ] Notificaciones push via FCM

## Gestión de Datos

El núcleo de este proyecto es un **pipeline de almacenamiento de tres niveles** construido completamente dentro de Supabase, diseñado para manejar la ingestión de sensores de alta frecuencia manteniendo el almacenamiento acotado y las consultas rápidas a cualquier escala.

**Nivel 1 - `gas_logs_raw`**
Cada lectura del sensor se escribe aquí al llegar. Supabase Realtime transmite cada inserción sobre WebSocket a la aplicación Android al instante. Esta tabla es intensiva en escritura y crece rápido - un solo dispositivo enviando al intervalo predeterminado produce miles de filas por hora.

**Nivel 2 - `gas_logs_minute`**
`pg_cron` agrega filas crudas en buckets por minuto cada minuto, calculando `avg / min / max / sample_count` por dispositivo. El estado usa lógica de peor caso: una sola lectura `danger` en cualquier lugar del bucket marca todo el bucket como `danger`. Esta es la capa intermedia - suficientemente granular para vistas de corto alcance, suficientemente pequeña para consultas rápidas.

**Nivel 3 - `gas_logs_hour`**
Cada hora, los buckets de minutos se consolidan en buckets de horas. El gráfico de estadísticas en la app lee exclusivamente de esta tabla - nunca toca datos crudos. El tiempo de consulta es constante independientemente del tiempo de funcionamiento de los dispositivos.

**Limpieza automática**
`pg_cron` ejecuta una purga diaria a las 03:00 UTC que elimina todas las filas `normal` de `gas_logs_raw` con más de 48 horas. En ese momento cada lectura ya ha sido capturada en los agregados de minuto y hora, por lo que no se pierde información histórica. Las filas con estado `warning` o `danger` se conservan indefinidamente para fines de auditoría.

Sin esto, un dispositivo enviando a 5 lecturas/segundo acumularía ~430.000 filas crudas por día. Con la purga en su lugar, `gas_logs_raw` se mantiene acotado a aproximadamente las últimas 48 horas de lecturas normales - los costos de almacenamiento se mantienen estables a medida que aumenta el tiempo de actividad.

## Comenzando

### Requisitos Previos

- Android Studio Flamingo o posterior
- JDK 17
- Una instancia en ejecución de [gasleakdetector-server](https://github.com/gasleakdetector/gasleakdetector-server)

### Compilar

```shell
git clone https://github.com/gasleakdetector/gasleakdetector.git
cd gasleakdetector
./gradlew assembleDebug
```

### Configuración

Abra **Ajustes** en la app y complete:

| Campo | Descripción |
|---|---|
| API URL | Su URL de despliegue de Vercel, ej. `https://your-app.vercel.app` |
| API Key | El `VALID_API_KEY` configurado en sus variables de entorno de Vercel |
| Device ID | El `device_id` que su ESP está enviando, ej. `ESP_GASLEAK_01` (Deje este campo en blanco si desea incluir todos los dispositivos.) |

La app obtiene las credenciales de Supabase automáticamente desde `/api/realtime-config` - no es necesario ingresar claves de Supabase manualmente.

## Descargar

- **Última Build Alpha**: Descargar desde [Actions](https://github.com/gasleakdetector/gasleakdetector/actions/)
- **Última Build Estable**: Descargar desde [Releases](https://github.com/gasleakdetector/gasleakdetector/releases)

[<img src="https://raw.githubusercontent.com/Kunzisoft/Github-badge/main/get-it-on-github.png" alt="Get it on GitHub" height="80">](https://github.com/gasleakdetector/gasleakdetector/releases/latest)

## Repositorios Relacionados

| Repositorio | Descripción |
|---|---|
| [gasleakdetector-server](https://github.com/gasleakdetector/gasleakdetector-server) | API serverless de Vercel - ingestión, consulta histórica, estadísticas, alertas de correo electrónico |
| [gasleakdetector-esp](https://github.com/gasleakdetector/gasleakdetector-esp) | Firmware ESP8266 - lector MQ-6, portal cautivo WiFi, cola offline |

## Licencia

Apache 2.0 © [Gas Leak Detector](LICENSE)
