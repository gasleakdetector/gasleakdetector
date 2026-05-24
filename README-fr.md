<p align="center">    
  <img src=".github/assets/cover.png" alt="Cover" />    
</p>

<p align="center">
  <img alt="License" src="https://img.shields.io/github/license/gasleakdetector/gasleakdetector?color=04A8F4&style=flat-square"/>
  <img alt="Version" src="https://img.shields.io/badge/version-1.0.3-04A8F4?style=flat-square"/>
  <img alt="Min SDK" src="https://img.shields.io/badge/min%20SDK-21-04A8F4?style=flat-square&logo=android"/>
  <img alt="Build" src="https://img.shields.io/github/actions/workflow/status/gasleakdetector/gasleakdetector/build.yml?style=flat-square&color=04A8F4"/>
  <img alt="Last commit" src="https://img.shields.io/github/last-commit/gasleakdetector/gasleakdetector?color=04A8F4&style=flat-square"/>
</p>

<div align="center">
  <h1>Gas Leak Detector — App</h1>
  <p><a target="_blank" href="README.md">English</a>&nbsp;&nbsp;|&nbsp;&nbsp;
    <a target="_blank" href="README-vi.md">Tiếng Việt</a>&nbsp;&nbsp;|&nbsp;&nbsp;
    <a target="_blank" href="README-de.md">Deutsch</a>&nbsp;&nbsp;|&nbsp;&nbsp;
    <a target="_blank" href="README-es.md">Español</a>&nbsp;&nbsp;|&nbsp;&nbsp;
    Français&nbsp;&nbsp;|&nbsp;&nbsp;
    <a target="_blank" href="README-ja.md">日本語</a>&nbsp;&nbsp;|&nbsp;&nbsp;
    <a target="_blank" href="README-ko.md">한국어</a>&nbsp;&nbsp;|&nbsp;&nbsp;
    <a target="_blank" href="README-zh.md">中文</a>
  </p>
  <p align="center">Surveillance de gaz en temps réel pour Android — propulsé par ESP8266, Supabase et une API serverless edge.</p>
  <img src=".github/assets/intro.png" width="19.2%" alt="intro" />    
  <img src=".github/assets/config.png" width="19.2%" alt="config" />    
  <img src=".github/assets/home.png" width="19.2%" alt="home" />    
  <img src=".github/assets/stats.png" width="19.2%" alt="stats" />    
  <br/>    
  <br/>    
</div>

## Présentation

Gas Leak Detector est un système de sécurité IoT complet. Un capteur MQ-6 sur un ESP8266 échantillonne en continu les niveaux de gaz ambiant et envoie les lectures à une API serverless Vercel. Les données sont persistées dans Supabase et transmises en temps réel à l'application Android via WebSocket — sans polling, sans délai.

Le système est composé de trois dépôts indépendants formant un pipeline :

| Couche | Dépôt | Stack |
|---|---|---|
| Firmware | [gasleakdetector-esp](https://github.com/gasleakdetector/gasleakdetector-esp) | C++ / Arduino / ESP8266 |
| Backend | [gasleakdetector-server](https://github.com/gasleakdetector/gasleakdetector-server) | Node.js / Vercel / Supabase |
| Mobile | **gasleakdetector** *(ce dépôt)* | Android / Java |

## Démo

[Regarder la vidéo de démonstration](https://www.youtube.com/watch?v=RLNf9Zphb1I)

## Configuration du Projet Complet

Vous pouvez consulter un guide détaillé sur la configuration de l'ensemble du projet [ici](Tutorial/README-fr.md)

## Flux du Système

<p align="center">
  <img src=".github/assets/flow.png" alt="System flow" width="100%"/>
</p>

1. ESP8266 lit le capteur MQ-6 toutes les 400 ms et envoie des requêtes POST à `/api/ingest` avec authentification par clé API.
2. La fonction edge Vercel classifie la lecture (`normal / warning / danger`) et écrit dans Supabase. Des alertes e-mail se déclenchent sur `danger` avec un délai de refroidissement configurable.
3. `pg_cron` dans Supabase agrège automatiquement les lignes brutes en buckets par minute et par heure — aucun planificateur externe n'est nécessaire.
4. L'application Android récupère les identifiants Supabase depuis `/api/realtime-config` et ouvre un abonnement WebSocket directement à `gas_logs_raw` pour des mises à jour en direct sans latence.
5. Les graphiques historiques lisent depuis des buckets d'heures pré-agrégés, gardant les requêtes rapides quelle que soit la durée de fonctionnement de l'appareil.

## Fonctionnalités

- [x] Jauge PPM en direct avec valeur animée et mises à jour WebSocket en temps réel
- [x] Classification de statut — Normal / Avertissement / Danger avec retour coloré
- [x] Notification de danger persistante tant que les niveaux de gaz restent critiques
- [x] Graphique historique — données agrégées par heure avec axe Y dynamique
- [x] Support multi-nœuds — basculer entre les appareils ESP par `device_id`
- [x] Pagination basée sur le curseur — récupère jusqu'à 1 000 lignes par requête avec compression gzip
- [x] Résilience hors ligne — l'ESP met en file jusqu'à 60 lectures localement ; l'app affiche les données en cache
- [x] Écran d'introduction au premier démarrage — affiché une fois, ignoré lors de tous les démarrages ultérieurs
- [x] Retour d'information — e-mail en un seul clic pré-rempli avec la version de l'app dans l'objet
- [x] Internationalisation — 8 langues : Anglais, Vietnamien, Allemand, Espagnol, Français, Japonais, Coréen, Chinois
- [ ] Widget
- [ ] Configuration multi-seuils par appareil
- [ ] Notifications push via FCM

## Gestion des Données

Le cœur de ce projet est un **pipeline de stockage à trois niveaux** entièrement construit dans Supabase, conçu pour gérer l'ingestion de capteurs à haute fréquence tout en maintenant le stockage limité et les requêtes rapides à n'importe quelle échelle.

**Niveau 1 — `gas_logs_raw`**
Chaque lecture de capteur est écrite ici à l'arrivée. Supabase Realtime diffuse chaque insertion via WebSocket à l'application Android instantanément. Cette table est intensive en écriture et grossit vite — un seul appareil envoyant à l'intervalle par défaut produit des milliers de lignes par heure.

**Niveau 2 — `gas_logs_minute`**
`pg_cron` agrège les lignes brutes en buckets par minute chaque minute, calculant `avg / min / max / sample_count` par appareil. Le statut utilise la logique du pire cas : une seule lecture `danger` n'importe où dans le bucket marque l'ensemble du bucket comme `danger`. C'est la couche intermédiaire — suffisamment granulaire pour les vues à court terme, suffisamment petite pour des requêtes rapides.

**Niveau 3 — `gas_logs_hour`**
Chaque heure, les buckets de minutes sont consolidés en buckets d'heures. Le graphique de statistiques dans l'app lit exclusivement depuis cette table — il ne touche jamais aux données brutes. Le temps de requête est constant quelle que soit la durée de fonctionnement des appareils.

**Nettoyage automatique**
`pg_cron` effectue une purge quotidienne à 03h00 UTC qui supprime toutes les lignes `normal` de `gas_logs_raw` datant de plus de 48 heures. À ce moment, chaque lecture a déjà été capturée dans les agrégats de minutes et d'heures, donc aucune information historique n'est perdue. Les lignes avec le statut `warning` ou `danger` sont conservées indéfiniment à des fins d'audit.

Sans cela, un appareil envoyant à 5 lectures/seconde accumulerait ~430 000 lignes brutes par jour. Avec la purge en place, `gas_logs_raw` reste limité aux environ 48 dernières heures de lectures normales — les coûts de stockage restent stables à mesure que la durée de fonctionnement augmente.

## Démarrage

### Prérequis

- Android Studio Flamingo ou ultérieur
- JDK 17
- Une instance en cours d'exécution de [gasleakdetector-server](https://github.com/gasleakdetector/gasleakdetector-server)

### Compilation

```shell
git clone https://github.com/gasleakdetector/gasleakdetector.git
cd gasleakdetector
./gradlew assembleDebug
```

### Configuration

Ouvrez **Paramètres** dans l'app et remplissez :

| Champ | Description |
|---|---|
| API URL | Votre URL de déploiement Vercel, ex. `https://your-app.vercel.app` |
| API Key | Le `VALID_API_KEY` défini dans vos variables d'environnement Vercel |
| Device ID | Le `device_id` que votre ESP envoie, ex. `ESP_GASLEAK_01` (Laissez ce champ vide si vous souhaitez inclure tous les appareils.) |

L'app récupère automatiquement les identifiants Supabase depuis `/api/realtime-config` — aucune clé Supabase ne doit être saisie manuellement.

## Téléchargement

- **Dernière Build Alpha** : Télécharger depuis [Actions](https://github.com/gasleakdetector/gasleakdetector/actions/)
- **Dernière Build Stable** : Télécharger depuis [Releases](https://github.com/gasleakdetector/gasleakdetector/releases)

[<img src="https://raw.githubusercontent.com/Kunzisoft/Github-badge/main/get-it-on-github.png" alt="Get it on GitHub" height="80">](https://github.com/gasleakdetector/gasleakdetector/releases/latest)

## Dépôts Associés

| Dépôt | Description |
|---|---|
| [gasleakdetector-server](https://github.com/gasleakdetector/gasleakdetector-server) | API serverless Vercel — ingestion, requête historique, statistiques, alertes e-mail |
| [gasleakdetector-esp](https://github.com/gasleakdetector/gasleakdetector-esp) | Firmware ESP8266 — lecteur MQ-6, portail captif WiFi, file d'attente hors ligne |

## Licence

Apache 2.0 © [Gas Leak Detector](LICENSE)
