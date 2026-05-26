# Guide de Configuration

Guide de configuration complet pour le projet Gas Leak Detector. Ce document couvre les quatre composants - backend serveur, Supabase, firmware ESP8266 et application Android - dans l'ordre dans lequel ils doivent être configurés.

<div align="center">
	<p><a target="_blank" href="README.md">English</a>&nbsp;&nbsp;|&nbsp;&nbsp;
	    <a target="_blank" href="README-vi.md">Tiếng Việt</a>&nbsp;&nbsp;|&nbsp;&nbsp;
	    <a target="_blank" href="README-de.md">Deutsch</a>&nbsp;&nbsp;|&nbsp;&nbsp;
	    <a target="_blank" href="README-es.md">Español</a>&nbsp;&nbsp;|&nbsp;&nbsp;
	    Français&nbsp;&nbsp;|&nbsp;&nbsp;
	    <a target="_blank" href="README-ja.md">日本語</a>&nbsp;&nbsp;|&nbsp;&nbsp;
	    <a target="_blank" href="README-ko.md">한국어</a>&nbsp;&nbsp;|&nbsp;&nbsp;
	    <a target="_blank" href="README-zh.md">中文</a>
</div>

## Prérequis

### Matériel

| Composant | Notes |
|-----------|-------|
| ESP8266 | NodeMCU ou équivalent |
| Capteur de gaz MQ-6 | Détection LPG / propane |
| OLED SSD1306 0.96" | Optionnel - pour affichage sur l'appareil |
| Buzzer | Actif ou passif |

### Comptes

- [Vercel](https://vercel.com) - déploiement serverless
- [Supabase](https://supabase.com) - base de données et temps réel
- [Resend](https://resend.com) - alertes par e-mail (optionnel)

---

## 1. Backend Serveur

### Déployer sur Vercel

**Étape 1.** Accédez au dépôt [gasleakdetector-server](https://github.com/gasleakdetector/gasleakdetector-server) et cliquez sur le bouton **Deploy**.

![Déployer sur Vercel](../images/vercel_deploy_button.jpg)

**Étape 2.** Ouvrez votre projet dans le tableau de bord Vercel et allez dans **Settings**.

![Paramètres du projet Vercel](../images/vercel_project_settings.jpg)

**Étape 3.** Naviguez vers **Environment Variables**.

![Onglet Environment Variables](../images/vercel_environment_variables.jpg)

**Étape 4.** Cliquez sur **Add Environment Variable**.

![Ajouter une variable d'environnement](../images/vercel_add_variable.jpg)

**Étape 5.** Ajoutez chacune des variables suivantes :

![Remplir les valeurs des variables](../images/vercel_fill_variables.jpg)

| Variable | Description |
|----------|-------------|
| `SUPABASE_URL` | L'URL de votre projet Supabase |
| `SUPABASE_ANON_KEY` | Supabase anonymous key - utilisé par le WebSocket de l'app Android |
| `SUPABASE_SERVICE_KEY` | Supabase service role key - utilisé pour toutes les écritures côté serveur |
| `VALID_API_KEY` | Secret partagé envoyé dans le header `x-api-key` par l'ESP et l'app |
| `RESEND_API_KEY` | API key Resend pour les alertes par e-mail |
| `ALERT_EMAIL` | Adresse de destination pour les alertes de niveau danger |
| `DANGER_THRESHOLD` | Niveau PPM déclenchant le statut `danger`. Recommandé : `800` pour MQ-6 |
| `WARNING_THRESHOLD` | Niveau PPM déclenchant le statut `warning`. Recommandé : `300` pour MQ-6 |
| `EMAIL_COOLDOWN_MINUTES` | Minutes minimum entre les e-mails d'alerte répétés. Par défaut : `2` |

### API Key

`VALID_API_KEY` est un secret que vous définissez vous-même - il est partagé entre le serveur, le firmware ESP et l'app Android. Aucune inscription ni service tiers n'est nécessaire.

Recommandé : 8–10 caractères alphanumériques. Exemple : `Abc12345`.

### Resend (Alertes par E-mail)

Cette section est optionnelle. Ignorez-la si vous n'avez pas besoin d'alertes par e-mail.

**Étape 1.** Connectez-vous ou créez un compte sur [resend.com](https://resend.com/login).

**Étape 2.** Allez dans **API Keys** et cliquez sur **Create API Key**.

![Page API Keys de Resend](../images/resend_api_keys_page.jpg)

**Étape 3.** Renseignez le nom de la clé et cliquez sur **Add**.

![Formulaire de création de l'API Key](../images/resend_create_api_key.jpg)

**Étape 4.** Copiez la clé générée.

![Copier la clé](../images/resend_copy_key.jpg)

Assignez-la à `RESEND_API_KEY` dans Vercel. Définissez `ALERT_EMAIL` sur l'adresse qui doit recevoir les alertes (p. ex. `you@gmail.com`).

---

## 2. Supabase

**Étape 1.** Créez un nouveau projet dans Supabase.

**Étape 2.** Ouvrez l'éditeur SQL, collez le contenu de [`supabase/schema.sql`](https://github.com/gasleakdetector/gasleakdetector-server/blob/main/supabase/schema.sql) et exécutez-le une seule fois.

![Éditeur SQL Supabase](../images/supabase_sql_editor.jpg)

Cela crée toutes les tables (`gas_logs_raw`, `gas_logs_minute`, `gas_logs_hour`, `devices`), les fonctions d'agrégation, les jobs `pg_cron` et les politiques de Row-Level Security en une seule exécution.

**Étape 3.** Récupérez vos informations d'identification API. Allez dans **Settings > API Keys > Legacy anon, service_role API keys** et copiez les clés `anon` et `service_role`.

![API Keys Supabase](../images/supabase_api_keys.jpg)

Assignez-les à `SUPABASE_ANON_KEY` et `SUPABASE_SERVICE_KEY` dans Vercel. Votre `SUPABASE_URL` est disponible dans **Project Overview**.

---

## 3. Firmware ESP8266

### Câblage

Connectez tous les composants à l'ESP8266 selon les tableaux ci-dessous avant de l'alimenter.

**Capteur de gaz MQ-6**

| MQ-6 | ESP8266 |
|------|---------|
| VCC | VIN (5V) |
| GND | GND |
| AO | A0 |

**OLED SSD1306 (optionnel)**

| OLED | ESP8266 |
|------|---------|
| VCC | 3.3V |
| GND | GND |
| SCL | D1 / GPIO5 |
| SDA | D2 / GPIO4 |

**Buzzer**

| Buzzer | ESP8266 |
|--------|---------|
| + | D5 / GPIO14 |
| − | GND |

### Flasher le Firmware

Téléchargez le dernier fichier `.bin` depuis la page [gasleakdetector-esp releases](https://github.com/gasleakdetector/gasleakdetector-esp/releases) et flashez-le sur l'ESP8266 avec votre outil préféré (esptool, Arduino IDE ou ESP Flash Download Tool).

La procédure de flashage n'est pas couverte en détail ici - référez-vous au dépôt du firmware pour les instructions.

### Configuration Initiale

**Étape 1.** Mettez l'ESP8266 sous tension. Il diffusera un point d'accès Wi-Fi. Connectez-vous y.

![Point d'accès Wi-Fi de l'ESP](../images/esp_wifi_ap.jpg)

**Étape 2.** Ouvrez un navigateur et accédez à `http://192.168.4.1`. Le portail captif s'ouvrira automatiquement sur la plupart des appareils. Entrez vos identifiants :

- **SSID / Mot de passe** - vos identifiants Wi-Fi domestiques
- **API KEY** - le `VALID_API_KEY` configuré dans Vercel

![Portail captif de l'ESP](../images/esp_captive_portal.jpg)

**Étape 3.** Allez dans **Settings** (en haut à droite). Entrez l'**API Host** avec votre URL de déploiement Vercel (p. ex. `https://your-app.vercel.app`), puis cliquez sur **Save Config**.

![Formulaire de configuration de l'ESP](../images/esp_config_form.jpg)

**Étape 4.** Réinitialisez l'ESP pour appliquer la nouvelle configuration. L'appareil se connectera à votre Wi-Fi et commencera à envoyer des données.

---

## 4. Application Android

### Installation

Téléchargez le dernier APK depuis la [page des releases](https://github.com/gasleakdetector/gasleakdetector/releases/latest) et installez-le sur votre appareil.

### Configuration

**Étape 1.** Ouvrez l'application et appuyez sur l'icône crayon en haut à droite de l'écran d'accueil.

![Bouton de modification de la configuration](../images/app_edit_config.jpg)

**Étape 2.** Renseignez vos paramètres :

![Formulaire de configuration de l'app](../images/app_config_form.jpg)

| Champ | Valeur |
|-------|--------|
| API URL | L'URL de votre déploiement Vercel |
| API Key | Le `VALID_API_KEY` que vous avez défini |
| Device ID | Le `device_id` de votre ESP (p. ex. `ESP_GASLEAK_01`). Laissez vide pour surveiller tous les appareils. |

Appuyez sur **Save**. L'application se connectera et commencera à afficher les lectures en direct.

---

> ❕ Dans la version stable actuelle, l'application ne prend en charge les **alertes push que lorsqu'elle tourne en arrière-plan**.
> 
> Si tu veux recevoir des notifications **même quand l'application est complètement fermée**, consulte les branches de développement ci-dessous :
> 
> * [Branche Android avec FCM](https://github.com/gasleakdetector/gasleakdetector/tree/feature/fcm-push-notification)
> * [Branche serveur avec support FCM](https://github.com/gasleakdetector/gasleakdetector-server/tree/feature/fcm-push-notification)
> 
> **Remarque :** Pour des raisons de sécurité et de configuration (un fichier `google-services.json` personnel est requis), ces branches ne peuvent pas être fusionnées dans la branche principale. La fonctionnalité est encore en développement. Tu peux ignorer ceci si ce n'est pas pertinent pour toi 😊.

---

## Dépannage

**L'ESP ne se connecte pas au Wi-Fi** - vérifiez le SSID et le mot de passe dans le portail captif. Maintenez le bouton appuyé pendant 5 secondes pour réinitialiser aux paramètres d'usine et reconfigurer.

**L'app n'affiche aucune donnée** - vérifiez que l'API URL et l'API Key correspondent exactement à ce qui est configuré dans Vercel. Consultez les logs des fonctions Vercel pour détecter des erreurs.

**Pas d'alertes par e-mail** - confirmez que `RESEND_API_KEY` et `ALERT_EMAIL` sont définis dans Vercel. Les alertes ne se déclenchent que lorsque le statut atteint `danger` et que la fenêtre de cooldown est écoulée.

**Erreurs de schéma Supabase** - assurez-vous que le SQL a été exécuté dans le bon projet et que l'extension `pg_cron` est activée (Supabase l'active par défaut sur les plans payants ; les plans gratuits peuvent nécessiter une activation manuelle).

---

*Pour toute question ou problème, ouvrez un [issue GitHub](https://github.com/gasleakdetector/gasleakdetector/issues) ou contactez [pan2512811@gmail.com](mailto:pan2512811@gmail.com). Les contributions sont les bienvenues 😊*
