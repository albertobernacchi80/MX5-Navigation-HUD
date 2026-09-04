package com.mx5.navhud.nav;

import androidx.annotation.NonNull;

/**
 * Ultimo stato di navigazione noto, scritto da GoogleMapsNotificationListener (che gira nel
 * processo dell'app ogni volta che il sistema notifica un cambiamento nelle notifiche attive)
 * e letto periodicamente da NavHudScreen per ridisegnare la freccia grande sullo schermo
 * dell'auto. Singleton in memoria, come SensorHub.state: nessuna persistenza, si azzera a ogni
 * riavvio del processo.
 */
public final class NavHudState {

    private static volatile NavInstruction current = NavInstruction.WAITING;
    private static volatile long lastUpdateMs = 0L;

    // --- Diagnostica per la schermata "in attesa" -------------------------------------------
    // Senza queste informazioni, se l'app resta bloccata su "in attesa" non c'è modo di capire
    // da una foto dello schermo dell'auto SE il problema è "il servizio di ascolto notifiche
    // non è collegato" (permesso non concesso/revocato), "nessuna notifica di Google Maps è mai
    // arrivata" (navigazione non avviata, o notifica soppressa dal sistema), oppure "la notifica
    // arriva ma il testo estratto è vuoto" (formato della notifica cambiato/diverso dal
    // previsto). Questi contatori/flag, mostrati da NavGlyphRenderer.drawWaiting, distinguono
    // questi tre casi senza bisogno di un logcat o di un bug report del telefono.
    private static volatile boolean listenerConnected = false;
    private static volatile int mapsNotificationsSeen = 0;
    private static volatile String lastRawTitle = "";
    private static volatile String lastRawText = "";

    /** Dopo quanto tempo senza nuove notifiche l'istruzione mostrata si considera "vecchia"
     *  (l'app resta in ascolto, ma segnala visivamente che il dato potrebbe non essere più
     *  valido: notifica raggruppata, navigazione interrotta, telefono bloccato l'app, ecc.). */
    public static final long STALE_AFTER_MS = 12_000;

    private NavHudState() {
    }

    public static void update(@NonNull NavInstruction instruction) {
        current = instruction;
        lastUpdateMs = System.currentTimeMillis();
    }

    public static void setListenerConnected(boolean connected) {
        listenerConnected = connected;
    }

    public static boolean isListenerConnected() {
        return listenerConnected;
    }

    /** Da chiamare per OGNI notifica di Google Maps intercettata, anche se poi risulta senza
     *  testo utile: serve solo a contare quante ne sono arrivate e a ricordare l'ultimo
     *  titolo/testo grezzo visto, indipendentemente dal fatto che siano stati interpretabili. */
    public static void recordMapsNotificationSeen(@NonNull String title, @NonNull String text) {
        mapsNotificationsSeen++;
        lastRawTitle = title;
        lastRawText = text;
    }

    public static int mapsNotificationsSeenCount() {
        return mapsNotificationsSeen;
    }

    @NonNull
    public static String lastRawTitle() {
        return lastRawTitle;
    }

    @NonNull
    public static String lastRawText() {
        return lastRawText;
    }

    /** Da chiamare quando la notifica di navigazione di Google Maps sparisce (es. navigazione
     *  terminata o interrotta dall'utente): torna allo stato "in attesa" invece di restare
     *  bloccato sull'ultima svolta mostrata. */
    public static void clear() {
        current = NavInstruction.WAITING;
        lastUpdateMs = 0L;
    }

    @NonNull
    public static NavInstruction currentInstruction() {
        return current;
    }

    public static boolean isStale() {
        return lastUpdateMs != 0L && System.currentTimeMillis() - lastUpdateMs > STALE_AFTER_MS;
    }

    public static boolean hasEverReceivedData() {
        return lastUpdateMs != 0L;
    }
}
