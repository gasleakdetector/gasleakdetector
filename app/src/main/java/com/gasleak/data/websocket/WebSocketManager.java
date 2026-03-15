/*
 * Copyright 2026 Phuc An
 * Licensed under the Apache License, Version 2.0
 *
 * Project : Gas Leak Detector
 * Author  : Phuc An <pan2512811@gmail.com>
 * Email   : pan2512811@gmail.com
 * GitHub  : https://github.com/traitimtrongvag/gasleakdetector-app
 * Modified: 2026-03-15
 */
package com.gasleak.data.websocket;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.gasleak.data.model.RealtimeConfig;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages the Supabase Realtime WebSocket connection.
 *
 * Key design decisions:
 * - Uses an incrementing AtomicInteger ref so Supabase doesn't discard duplicate messages.
 * - Heartbeat every 10 s — Supabase drops the channel after ~25 s of silence.
 * - Auto-reconnects 2 s after a drop so no data points are missed.
 */
public class WebSocketManager {

    private static final String TAG = "WebSocketManager";

    private static final int HEARTBEAT_MS   = 10_000; // Supabase drops channel after ~25 s idle
    private static final int RECONNECT_MS   =  2_000; // fast enough to avoid missing live inserts
    private static final int CONNECT_TIMEOUT = 10_000;

    /* Supabase Realtime endpoint constants. */
    private static final String CONFIG_ENDPOINT  = "/api/realtime-config";
    private static final String REALTIME_TABLE   = "gas_logs_raw";
    private static final String REALTIME_SCHEMA  = "public";
    private static final String CHANNEL_TOPIC    = "realtime:" + REALTIME_SCHEMA + ":" + REALTIME_TABLE;

    public interface Callback {
        void onConnected();
        void onDisconnected();
        void onDataReceived(int gasPpm, String status, String timestamp);
        void onError(String error);
    }

    private WebSocketClient               client;
    private final WeakReference<Callback> callbackRef;
    private final Handler                 mainHandler;
    private final Handler                 heartbeatHandler;
    private final Handler                 reconnectHandler;
    private RealtimeConfig                config;
    private volatile boolean              isDestroyed     = false;
    private volatile boolean              shouldReconnect = false;
    private volatile boolean              everConnected   = false;
    private String                        cachedWsUrl     = null;

    /* Incremented with each message so Supabase never sees a duplicate ref. */
    private final AtomicInteger ref = new AtomicInteger(1);

    private Thread fetchThread;

    public WebSocketManager(Callback callback) {
        this.callbackRef     = new WeakReference<>(callback);
        this.mainHandler     = new Handler(Looper.getMainLooper());
        this.heartbeatHandler = new Handler(Looper.getMainLooper());
        this.reconnectHandler = new Handler(Looper.getMainLooper());
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void connect(final RealtimeConfig config) {
        if (isDestroyed) return;
        this.config     = config;
        shouldReconnect = true;
        everConnected   = false;
        cachedWsUrl     = null; // force a fresh config fetch for the new connection

        stopHeartbeat();
        closeClient();

        if (fetchThread != null && fetchThread.isAlive()) fetchThread.interrupt();

        fetchThread = new Thread(new Runnable() {
            @Override public void run() { fetchConfigAndConnect(); }
        });
        fetchThread.setDaemon(true);
        fetchThread.start();
    }

    public void disconnect() {
        shouldReconnect = false;
        stopHeartbeat();
        stopReconnect();
        closeClient();
    }

    public boolean isConnected() {
        return client != null && client.isOpen();
    }

    public void destroy() {
        isDestroyed = true;
        disconnect();
        if (fetchThread != null) { fetchThread.interrupt(); fetchThread = null; }
    }

    // ── Step 1: Fetch Supabase WS URL and anon key from the backend config endpoint ──

    private void fetchConfigAndConnect() {
        if (Thread.interrupted() || isDestroyed) return;
        try {
            /* Reuse the cached WS URL on reconnects to avoid hammering the config endpoint. */
            if (cachedWsUrl == null) {
                String apiUrl = config.getApiUrl() + CONFIG_ENDPOINT;
                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("x-api-key", config.getApiKey());
                conn.setConnectTimeout(CONNECT_TIMEOUT);
                conn.setReadTimeout(CONNECT_TIMEOUT);

                int code = conn.getResponseCode();
                if (code != 200) throw new Exception("Config HTTP " + code);

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                conn.disconnect();

                JSONObject json    = new JSONObject(sb.toString());
                String supabaseUrl = json.getString("url");
                String anonKey     = json.getString("anonKey");

                cachedWsUrl = supabaseUrl
                    .replace("https://", "wss://")
                    .replace("http://",  "ws://")
                    + "/realtime/v1/websocket?apikey=" + anonKey + "&vsn=1.0.0";
            }

            connectWebSocket(cachedWsUrl);

        } catch (final Exception e) {
            if (isDestroyed) return;
            Log.e(TAG, "fetchConfig error", e);
            postError("Connection failed: " + e.getMessage());
            scheduleReconnect();
        }
    }

    // ── Step 2: Open the WebSocket connection ─────────────────────────────────

    private void connectWebSocket(final String wsUrl) {
        if (isDestroyed) return;
        try {
            URI uri = new URI(wsUrl);
            client = new WebSocketClient(uri) {

                @Override
                public void onOpen(ServerHandshake hs) {
                    if (isDestroyed) return;
                    Log.d(TAG, "WS opened");
                    /* Short delay before joining; the socket isn't always ready immediately after onOpen. */
                    mainHandler.postDelayed(new Runnable() {
                        @Override public void run() { if (!isDestroyed) joinChannel(); }
                    }, 300);
                }

                @Override
                public void onMessage(String message) {
                    if (!isDestroyed) handleMessage(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    Log.d(TAG, "WS closed code=" + code + " reason=" + reason);
                    stopHeartbeat();
                    if (isDestroyed) return;
                    /* Only propagate disconnect when the user explicitly stopped monitoring. */
                    if (!shouldReconnect) postDisconnected();
                    scheduleReconnect();
                }

                @Override
                public void onError(Exception ex) {
                    Log.e(TAG, "WS error", ex);
                    if (!isDestroyed && ex != null) postError(ex.getMessage());
                }
            };
            client.connect();

        } catch (final Exception e) {
            Log.e(TAG, "connectWebSocket error", e);
            postError("WebSocket error: " + e.getMessage());
            scheduleReconnect();
        }
    }

    // ── Step 3: Subscribe to the Postgres CDC channel after the socket opens ──

    private void joinChannel() {
        if (client == null || !client.isOpen()) return;
        try {
            JSONObject postgresChanges = new JSONObject();
            postgresChanges.put("event",  "INSERT");
            postgresChanges.put("schema", REALTIME_SCHEMA);
            postgresChanges.put("table",  REALTIME_TABLE);
            if (config.getDeviceId() != null && !config.getDeviceId().isEmpty()) {
                postgresChanges.put("filter", "device_id=eq." + config.getDeviceId());
            }

            JSONObject configObj = new JSONObject();
            configObj.put("postgres_changes", new JSONArray().put(postgresChanges));

            JSONObject msg = new JSONObject();
            msg.put("topic",   CHANNEL_TOPIC);
            msg.put("event",   "phx_join");
            msg.put("payload", new JSONObject().put("config", configObj));
            msg.put("ref",     String.valueOf(ref.getAndIncrement()));

            client.send(msg.toString());
            Log.d(TAG, "Sent phx_join");

        } catch (JSONException e) {
            Log.e(TAG, "joinChannel error", e);
        }
    }

    // ── Heartbeat — keeps the Supabase channel alive ──────────────────────────

    private void startHeartbeat() {
        stopHeartbeat();
        heartbeatHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isDestroyed || !isConnected()) return;
                try {
                    JSONObject msg = new JSONObject();
                    msg.put("topic",   "phoenix");
                    msg.put("event",   "heartbeat");
                    msg.put("payload", new JSONObject());
                    msg.put("ref",     String.valueOf(ref.getAndIncrement()));
                    client.send(msg.toString());
                    Log.d(TAG, "Heartbeat sent");
                } catch (JSONException e) {
                    Log.e(TAG, "Heartbeat error", e);
                }
                heartbeatHandler.postDelayed(this, HEARTBEAT_MS);
            }
        }, HEARTBEAT_MS);
    }

    private void stopHeartbeat() {
        heartbeatHandler.removeCallbacksAndMessages(null);
    }

    // ── Auto-reconnect ────────────────────────────────────────────────────────

    private void scheduleReconnect() {
        if (!shouldReconnect || isDestroyed) return;
        Log.d(TAG, "Reconnect in " + RECONNECT_MS + " ms");
        reconnectHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!shouldReconnect || isDestroyed) return;
                closeClient();
                connectWebSocket(cachedWsUrl != null ? cachedWsUrl : "");
            }
        }, RECONNECT_MS);
    }

    private void stopReconnect() {
        reconnectHandler.removeCallbacksAndMessages(null);
    }

    // ── Handle incoming messages ──────────────────────────────────────────────

    private void handleMessage(String message) {
        try {
            JSONObject json  = new JSONObject(message);
            String     event = json.optString("event", "");

            if ("phx_reply".equals(event)) {
                JSONObject payload = json.optJSONObject("payload");
                if (payload != null && "ok".equals(payload.optString("status"))) {
                    Log.d(TAG, "Channel joined OK");
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            startHeartbeat();
                            /* postConnected only on first join to avoid duplicate toasts on reconnect. */
                            if (!everConnected) {
                                everConnected = true;
                                postConnected();
                            }
                        }
                    });
                }

            } else if ("postgres_changes".equals(event)) {
                JSONObject payload = json.optJSONObject("payload");
                if (payload == null) return;
                JSONObject data   = payload.optJSONObject("data");
                if (data == null) return;
                JSONObject record = data.optJSONObject("record");
                if (record == null) return;

                final int    gasPpm    = record.optInt("gas_ppm", 0);
                final String status    = record.optString("status", "normal");
                final String timestamp = record.optString("created_at", "");

                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Callback cb = callbackRef.get();
                        if (cb != null) cb.onDataReceived(gasPpm, status, timestamp);
                    }
                });

            } else if ("phx_error".equals(event) || "phx_close".equals(event)) {
                Log.w(TAG, "Channel error/close, reconnecting");
                stopHeartbeat();
                scheduleReconnect();
            }

        } catch (JSONException e) {
            Log.e(TAG, "handleMessage error", e);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void closeClient() {
        if (client != null) {
            try { client.close(); } catch (Exception ignored) {}
            client = null;
        }
    }

    private void postConnected() {
        mainHandler.post(new Runnable() {
            @Override public void run() { Callback cb = callbackRef.get(); if (cb != null) cb.onConnected(); }
        });
    }

    private void postDisconnected() {
        mainHandler.post(new Runnable() {
            @Override public void run() { Callback cb = callbackRef.get(); if (cb != null) cb.onDisconnected(); }
        });
    }

    private void postError(final String msg) {
        mainHandler.post(new Runnable() {
            @Override public void run() { Callback cb = callbackRef.get(); if (cb != null) cb.onError(msg); }
        });
    }
}
