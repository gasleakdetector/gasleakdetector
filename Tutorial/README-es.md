# Guía de Configuración

Guía completa de configuración para el proyecto Gas Leak Detector. Este documento cubre los cuatro componentes - backend del servidor, Supabase, firmware ESP8266 y la app de Android - en el orden en que deben configurarse.

<div align="center">
	<p><a target="_blank" href="README.md">English</a>&nbsp;&nbsp;|&nbsp;&nbsp;
	    <a target="_blank" href="README-vi.md">Tiếng Việt</a>&nbsp;&nbsp;|&nbsp;&nbsp;
	    <a target="_blank" href="README-de.md">Deutsch</a>&nbsp;&nbsp;|&nbsp;&nbsp;
	    Español&nbsp;&nbsp;|&nbsp;&nbsp;
	    <a target="_blank" href="README-fr.md">Français</a>&nbsp;&nbsp;|&nbsp;&nbsp;
	    <a target="_blank" href="README-ja.md">日本語</a>&nbsp;&nbsp;|&nbsp;&nbsp;
	    <a target="_blank" href="README-ko.md">한국어</a>&nbsp;&nbsp;|&nbsp;&nbsp;
	    <a target="_blank" href="README-zh.md">中文</a>
</div>

## Requisitos

### Hardware

| Componente | Notas |
|-----------|-------|
| ESP8266 | NodeMCU o equivalente |
| Sensor de gas MQ-6 | Detección de LPG / propano |
| OLED SSD1306 0.96" | Opcional - para visualización en el dispositivo |
| Zumbador | Activo o pasivo |

### Cuentas

- [Vercel](https://vercel.com) - despliegue serverless
- [Supabase](https://supabase.com) - base de datos y tiempo real
- [Resend](https://resend.com) - alertas por correo electrónico (opcional)

---

## 1. Backend del Servidor

### Desplegar en Vercel

**Paso 1.** Ve al repositorio [gasleakdetector-server](https://github.com/gasleakdetector/gasleakdetector-server) y haz clic en el botón **Deploy**.

![Desplegar en Vercel](../images/vercel_deploy_button.jpg)

**Paso 2.** Abre tu proyecto en el panel de Vercel y ve a **Settings**.

![Configuración del proyecto Vercel](../images/vercel_project_settings.jpg)

**Paso 3.** Navega a **Environment Variables**.

![Pestaña Environment Variables](../images/vercel_environment_variables.jpg)

**Paso 4.** Haz clic en **Add Environment Variable**.

![Agregar variable de entorno](../images/vercel_add_variable.jpg)

**Paso 5.** Agrega cada una de las siguientes variables:

![Completar valores de variables](../images/vercel_fill_variables.jpg)

| Variable | Descripción |
|----------|-------------|
| `SUPABASE_URL` | URL de tu proyecto Supabase |
| `SUPABASE_ANON_KEY` | Supabase anonymous key - usado por el WebSocket de la app Android |
| `SUPABASE_SERVICE_KEY` | Supabase service role key - usado para todas las escrituras del lado del servidor |
| `VALID_API_KEY` | Secreto compartido enviado en el header `x-api-key` por el ESP y la app |
| `RESEND_API_KEY` | API key de Resend para alertas por correo electrónico |
| `ALERT_EMAIL` | Dirección de correo para las alertas de nivel de peligro |
| `DANGER_THRESHOLD` | Nivel de PPM que activa el estado `danger`. Recomendado: `800` para MQ-6 |
| `WARNING_THRESHOLD` | Nivel de PPM que activa el estado `warning`. Recomendado: `300` para MQ-6 |
| `EMAIL_COOLDOWN_MINUTES` | Minutos mínimos entre correos de alerta repetidos. Predeterminado: `2` |

### API Key

`VALID_API_KEY` es un secreto que defines tú mismo - se comparte entre el servidor, el firmware ESP y la app Android. No se requiere registro ni servicio de terceros.

Recomendado: 8–10 caracteres alfanuméricos. Ejemplo: `Abc12345`.

### Resend (Alertas por Correo Electrónico)

Esta sección es opcional. Omítela si no necesitas alertas por correo electrónico.

**Paso 1.** Inicia sesión o crea una cuenta en [resend.com](https://resend.com/login).

**Paso 2.** Ve a **API Keys** y haz clic en **Create API Key**.

![Página de API Keys de Resend](../images/resend_api_keys_page.jpg)

**Paso 3.** Ingresa el nombre del key y haz clic en **Add**.

![Formulario de creación de API Key](../images/resend_create_api_key.jpg)

**Paso 4.** Copia el key generado.

![Copiar key](../images/resend_copy_key.jpg)

Asígnalo a `RESEND_API_KEY` en Vercel. Establece `ALERT_EMAIL` con la dirección que debe recibir las alertas (p. ej. `you@gmail.com`).

---

## 2. Supabase

**Paso 1.** Crea un nuevo proyecto en Supabase.

**Paso 2.** Abre el Editor SQL, pega el contenido de [`supabase/schema.sql`](https://github.com/gasleakdetector/gasleakdetector-server/blob/main/supabase/schema.sql) y ejecútalo una vez.

![Editor SQL de Supabase](../images/supabase_sql_editor.jpg)

Esto crea todas las tablas (`gas_logs_raw`, `gas_logs_minute`, `gas_logs_hour`, `devices`), funciones de agregación, trabajos de `pg_cron` y políticas de Row-Level Security en una sola ejecución.

**Paso 3.** Recupera tus credenciales de API. Ve a **Settings > API Keys > Legacy anon, service_role API keys** y copia las claves `anon` y `service_role`.

![API Keys de Supabase](../images/supabase_api_keys.jpg)

Asígnalas a `SUPABASE_ANON_KEY` y `SUPABASE_SERVICE_KEY` en Vercel. Tu `SUPABASE_URL` está disponible en **Project Overview**.

---

## 3. Firmware ESP8266

### Cableado

Conecta todos los componentes al ESP8266 según las tablas siguientes antes de encenderlo.

**Sensor de Gas MQ-6**

| MQ-6 | ESP8266 |
|------|---------|
| VCC | VIN (5V) |
| GND | GND |
| AO | A0 |

**OLED SSD1306 (opcional)**

| OLED | ESP8266 |
|------|---------|
| VCC | 3.3V |
| GND | GND |
| SCL | D1 / GPIO5 |
| SDA | D2 / GPIO4 |

**Zumbador**

| Zumbador | ESP8266 |
|----------|---------|
| + | D5 / GPIO14 |
| − | GND |

### Flashear el Firmware

Descarga el archivo `.bin` más reciente desde la página de [gasleakdetector-esp releases](https://github.com/gasleakdetector/gasleakdetector-esp/releases) y flashéalo al ESP8266 usando tu herramienta preferida (esptool, Arduino IDE o ESP Flash Download Tool).

El procedimiento de flasheo no se cubre en detalle aquí - consulta el repositorio del firmware para instrucciones.

### Configuración Inicial

**Paso 1.** Enciende el ESP8266. Transmitirá un punto de acceso Wi-Fi. Conéctate a él.

![Punto de acceso Wi-Fi del ESP](../images/esp_wifi_ap.jpg)

**Paso 2.** Abre un navegador y navega a `http://192.168.4.1`. El portal cautivo se abrirá automáticamente en la mayoría de los dispositivos. Ingresa tus credenciales:

- **SSID / Contraseña** - credenciales de tu Wi-Fi doméstico
- **API KEY** - el `VALID_API_KEY` configurado en Vercel

![Portal cautivo del ESP](../images/esp_captive_portal.jpg)

**Paso 3.** Ve a **Settings** (arriba a la derecha). Ingresa el **API Host** con la URL de tu despliegue en Vercel (p. ej. `https://your-app.vercel.app`), luego haz clic en **Save Config**.

![Formulario de configuración del ESP](../images/esp_config_form.jpg)

**Paso 4.** Reinicia el ESP para aplicar la nueva configuración. El dispositivo se conectará a tu Wi-Fi y comenzará a enviar datos.

---

## 4. App Android

### Instalación

Descarga el APK más reciente desde la [página de releases](https://github.com/gasleakdetector/gasleakdetector/releases/latest) e instálalo en tu dispositivo.

### Configuración

**Paso 1.** Abre la app y toca el ícono de lápiz en la esquina superior derecha de la pantalla de inicio.

![Botón de editar configuración](../images/app_edit_config.jpg)

**Paso 2.** Completa tu configuración:

![Formulario de configuración de la app](../images/app_config_form.jpg)

| Campo | Valor |
|-------|-------|
| API URL | URL de tu despliegue en Vercel |
| API Key | El `VALID_API_KEY` que definiste |
| Device ID | El `device_id` de tu ESP (p. ej. `ESP_GASLEAK_01`). Déjalo en blanco para monitorear todos los dispositivos. |

Toca **Save**. La app se conectará y comenzará a mostrar lecturas en vivo.

---

> ❕ En la versión estable actual, la app solo admite **alertas push mientras la app está activa en segundo plano**.
> 
> Si deseas recibir notificaciones **incluso cuando la app está completamente cerrada**, consulta las ramas de desarrollo a continuación:
> 
> * [Rama Android con FCM](https://github.com/gasleakdetector/gasleakdetector/tree/feature/fcm-push-notification)
> * [Rama del servidor con soporte FCM](https://github.com/gasleakdetector/gasleakdetector-server/tree/feature/fcm-push-notification)
> 
> **Nota:** Por razones de seguridad y configuración del servicio (se necesita un archivo `google-services.json` propio), estas ramas no pueden fusionarse con la rama principal. La función sigue en desarrollo. Puedes ignorar esto si no lo necesitas 😊.

---

## Solución de Problemas

**El ESP no se conecta a Wi-Fi** - verifica el SSID y la contraseña en el portal cautivo. Mantén presionado el botón durante 5 segundos para restablecer de fábrica y reconfigurar.

**La app no muestra datos** - verifica que la API URL y el API Key coincidan exactamente con lo configurado en Vercel. Revisa los logs de funciones de Vercel para ver errores.

**Sin alertas por correo electrónico** - confirma que `RESEND_API_KEY` y `ALERT_EMAIL` estén configurados en Vercel. Las alertas solo se envían cuando el estado alcanza `danger` y ha pasado la ventana de cooldown.

**Errores de esquema de Supabase** - asegúrate de que el SQL se ejecutó en el proyecto correcto y que la extensión `pg_cron` esté habilitada (Supabase la habilita por defecto en planes de pago; los planes gratuitos pueden requerir activación manual).

---

*Para preguntas o problemas, abre un [issue en GitHub](https://github.com/gasleakdetector/gasleakdetector/issues) o contacta a [pan2512811@gmail.com](mailto:pan2512811@gmail.com). ¡Las contribuciones son bienvenidas! 😊*
