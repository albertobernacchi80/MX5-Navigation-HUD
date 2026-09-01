package com.mx5.navhud.nav;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import androidx.annotation.NonNull;

/**
 * Legge le notifiche di navigazione dell'app Google Maps (com.google.android.apps.maps) per
 * ricavarne l'istruzione di svolta corrente, e la scrive in NavHudState.
 *
 * PERCHÉ QUESTA STRADA E NON UNA API DI GOOGLE MAPS: Google non offre alcuna API pubblica per
 * leggere in tempo reale l'istruzione di navigazione mentre l'utente guida dentro l'app Google
 * Maps — l'unico canale "ufficiale" per una navigazione turn-by-turn propria è la Navigation
 * SDK di Google Maps Platform, che richiede una chiave API a pagamento oltre la soglia gratuita
 * e sostituisce interamente il routing (non si appoggia a Google Maps). Leggere la notifica di
 * navigazione è quindi l'unico modo gratuito e senza chiave per "agganciarsi" a una navigazione
 * già in corso su Google Maps — ma è un meccanismo non ufficiale, non documentato da Google e
 * non garantito: può smettere di funzionare con un aggiornamento dell'app Maps, va riautorizzato
 * manualmente dall'utente (permesso "Accesso alle notifiche") ed è ai margini di quanto previsto
 * dai termini di servizio di Google Maps, che non contemplano l'estrazione automatica dei dati
 * di navigazione da parte di app terze. Va trattato come funzione sperimentale, non come base
 * affidabile per la sicurezza alla guida.
 *
 * Serve il permesso di sistema "Accesso alle notifiche", che l'utente deve concedere a mano
 * dalle Impostazioni del telefono (Android non permette di chiederlo con un semplice pop-up):
 * vedi NavAccessSettingsActivity per la scorciatoia che apre la schermata giusta.
 */
public final class GoogleMapsNotificationListener extends NotificationListenerService {

    private static final String MAPS_PACKAGE = "com.google.android.apps.maps";

    @Override
    public void onNotificationPosted(@NonNull StatusBarNotification sbn) {
        if (!MAPS_PACKAGE.equals(sbn.getPackageName())) {
            return;
        }
        Notification n = sbn.getNotification();
        if (n == null) {
            return;
        }
        Bundle extras = n.extras;
        if (extras == null) {
            return;
        }
        CharSequence titleCs = extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence textCs = extras.getCharSequence(Notification.EXTRA_TEXT);
        CharSequence bigTextCs = extras.getCharSequence(Notification.EXTRA_BIG_TEXT);

        String title = titleCs != null ? titleCs.toString() : "";
        String text = bigTextCs != null ? bigTextCs.toString()
                : (textCs != null ? textCs.toString() : "");

        if (title.isEmpty() && text.isEmpty()) {
            return;
        }
        NavHudState.update(ManeuverTextParser.parse(title, text));
    }

    @Override
    public void onNotificationRemoved(@NonNull StatusBarNotification sbn) {
        if (MAPS_PACKAGE.equals(sbn.getPackageName())) {
            NavHudState.clear();
        }
    }
}
