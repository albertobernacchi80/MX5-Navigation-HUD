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
