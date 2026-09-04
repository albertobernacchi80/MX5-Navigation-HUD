package com.mx5.navhud.nav;

import android.app.Notification;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

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
    private static final String TAG = "GMapsNotifListener";

    // Oltre a reagire agli eventi onNotificationPosted() (che dipendono dal sistema e possono,
    // per motivi vari non diagnosticabili da qui senza log del telefono, non arrivare sempre:
    // notifica aggiornata "silenziosamente", evento perso durante un rebind del servizio,
    // particolarità del produttore del telefono), l'app ora controlla ANCHE per conto proprio,
    // a intervalli regolari, le notifiche di Google Maps attualmente presenti. È un secondo
    // canale, indipendente dal primo: se uno dei due si inceppa, l'altro copre comunque
    // l'aggiornamento entro pochi secondi. Il costo (leggere l'elenco delle notifiche attive
    // ogni pochi secondi) è trascurabile.
    private static final long POLL_INTERVAL_MS = 4000;
    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private final Runnable pollTask = this::pollActiveNotifications;

    /**
     * Chiamato dal sistema quando il servizio si collega e comincia a ricevere le notifiche.
     * Se la navigazione su Google Maps era già stata avviata PRIMA che questo servizio si
     * collegasse (es. permesso appena concesso, telefono riavviato, servizio riavviato dal
     * sistema per risparmio batteria), la notifica di navigazione esiste già ma
     * onNotificationPosted() non verrà richiamato per essa finché Maps non la aggiorna di
     * nuovo: senza questo controllo iniziale sulle notifiche già attive, l'HUD potrebbe restare
     * bloccato su "in attesa" anche con una navigazione già in corso, finché non arriva il
     * primo aggiornamento successivo (che comunque avviene entro pochi secondi). Da qui parte
     * anche il controllo periodico (vedi pollActiveNotifications()).
     */
    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        NavHudState.setListenerConnected(true);
        // pollActiveNotifications() si ripianifica da sé (vedi il suo blocco finally): questa
        // prima chiamata avvia la catena di controlli periodici.
        pollHandler.removeCallbacks(pollTask);
        pollActiveNotifications();
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        NavHudState.setListenerConnected(false);
        pollHandler.removeCallbacks(pollTask);
    }

    private void pollActiveNotifications() {
        try {
            StatusBarNotification[] active = getActiveNotifications();
            if (active != null) {
                for (StatusBarNotification sbn : active) {
                    handleNotificationPosted(sbn);
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "Errore nel controllo periodico delle notifiche attive", t);
        } finally {
            // Si ripianifica da sé finché il servizio resta collegato: se il collegamento cade
            // (onListenerDisconnected), il removeCallbacks() lì sopra interrompe la catena.
            pollHandler.postDelayed(pollTask, POLL_INTERVAL_MS);
        }
    }

    @Override
    public void onNotificationPosted(@NonNull StatusBarNotification sbn) {
        // Qualsiasi eccezione qui (formato di notifica inatteso, campo mancante, ecc.) non deve
        // far crashare l'intero processo dell'app: si registra nel log e si ignora quella
        // notifica, mantenendo l'ultima istruzione valida mostrata sullo schermo.
        try {
            handleNotificationPosted(sbn);
        } catch (Throwable t) {
            Log.e(TAG, "Errore nella lettura della notifica di Google Maps", t);
        }
    }

    private void handleNotificationPosted(@NonNull StatusBarNotification sbn) {
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

        // Registrata SEMPRE, anche se risulterà vuota: è ciò che permette alla schermata "in
        // attesa" di mostrare "notifiche Maps ricevute: N" e distinguere "non arriva nessuna
        // notifica" da "arrivano ma senza testo utile" (vedi NavGlyphRenderer.drawWaiting).
        NavHudState.recordMapsNotificationSeen(title, text);

        if (title.isEmpty() && text.isEmpty()) {
            return;
        }
        NavHudState.update(ManeuverTextParser.parse(title, text));
    }

    @Override
    public void onNotificationRemoved(@NonNull StatusBarNotification sbn) {
        try {
            if (MAPS_PACKAGE.equals(sbn.getPackageName())) {
                NavHudState.clear();
            }
        } catch (Throwable t) {
            Log.e(TAG, "Errore nella rimozione della notifica di Google Maps", t);
        }
    }

    /** Rete di sicurezza in più: se il servizio viene distrutto dal sistema senza passare da
     *  onListenerDisconnected() (capita su alcuni telefoni durante un riavvio aggressivo del
     *  servizio per risparmio batteria), questo evita comunque che il controllo periodico
     *  continui a essere ripianificato all'infinito su un servizio ormai morto. */
    @Override
    public void onDestroy() {
        try {
            NavHudState.setListenerConnected(false);
            pollHandler.removeCallbacks(pollTask);
        } catch (Throwable t) {
            Log.e(TAG, "Errore nella pulizia alla distruzione del servizio", t);
        } finally {
            super.onDestroy();
        }
    }
}
