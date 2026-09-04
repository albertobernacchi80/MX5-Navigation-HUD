package com.mx5.navhud;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.NavigationTemplate;

import com.mx5.navhud.nav.NavGlyphRenderer;
import com.mx5.navhud.nav.NavHudState;
import com.mx5.navhud.nav.NavInstruction;

/**
 * Unica schermata dell'app (è anche la Home): schermo intero con il solo simbolo essenziale
 * della prossima svolta (freccia, distanza, nome strada), stile MX-5 (rosso/bianco su nero).
 *
 * SPERIMENTALE: il percorso lo decide e lo calcola Google Maps sul telefono, esattamente come
 * oggi — questa schermata non fa routing proprio, non ha una mappa e non sa dove si trova
 * l'auto. Legge solo il testo dell'ultima notifica di navigazione di Google Maps
 * (GoogleMapsNotificationListener) e lo traduce nel simbolo grande più vicino. Serve quindi:
 * 1) avviare la navigazione dentro l'app Google Maps sul telefono, come sempre;
 * 2) aver concesso una volta il permesso "Accesso alle notifiche" a questa app (vedi
 *    NavAccessSettingsActivity);
 * 3) tenere Google Maps in esecuzione (anche in background) mentre si guarda questa schermata.
 * Vedi docs/NAV_HUD.md per i limiti (perché non una mappa 3D o vista stile Street View).
 */
public final class NavHudScreen extends Screen implements androidx.lifecycle.DefaultLifecycleObserver {

    public NavHudScreen(@NonNull CarContext context) {
        super(context);
        getLifecycle().addObserver(this);
    }

    private static final String TAG = "NavHudScreen";

    @NonNull
    @Override
    public Template onGetTemplate() {
        // onGetTemplate() viene chiamato dall'host: se lancia un'eccezione non gestita, l'host
        // considera l'intera app "rotta" e la chiude con l'errore generico. Qui costruiamo il
        // template "vero" (con il pulsante Notifiche) dentro un try/catch e, in caso di
        // qualunque problema, torniamo comunque un NavigationTemplate valido e minimo — mai
        // null, mai un'eccezione che risale all'host — così la schermata resta comunque in
        // piedi anche se l'azione più ricca fallisce.
        try {
            ActionStrip actionStrip = new ActionStrip.Builder()
                    // NavigationTemplate richiede un ActionStrip con almeno 1 azione: un
                    // ActionStrip vuoto fa fallire la validazione lato host appena questa
                    // schermata viene mostrata (causa già individuata e corretta una volta).
                    .addAction(new Action.Builder()
                            .setTitle("Notifiche")
                            .setOnClickListener(this::openNotificationAccessSettings)
                            .build())
                    .build();

            return new NavigationTemplate.Builder()
                    .setActionStrip(actionStrip)
                    .build();
        } catch (Throwable t) {
            Log.e(TAG, "onGetTemplate fallito, uso il template minimo di sicurezza", t);
            return safeMinimalTemplate();
        }
    }

    /** Template di riserva, costruito con il minimo indispensabile richiesto dalla libreria
     *  (un solo pulsante senza azione associata a rischio): usato solo se il template "vero"
     *  sopra fallisce per qualche motivo imprevisto. */
    private Template safeMinimalTemplate() {
        try {
            ActionStrip minimal = new ActionStrip.Builder()
                    .addAction(new Action.Builder().setTitle("MX-5").setOnClickListener(() -> {
                    }).build())
                    .build();
            return new NavigationTemplate.Builder().setActionStrip(minimal).build();
        } catch (Throwable t) {
            // Se anche questo fallisse, non c'è più nulla di sicuro da restituire: qui la
            // libreria stessa avrebbe un problema serio, non il nostro codice.
            Log.e(TAG, "anche il template minimo di sicurezza è fallito", t);
            throw t instanceof RuntimeException ? (RuntimeException) t : new RuntimeException(t);
        }
    }

    private void openNotificationAccessSettings() {
        // Avviata da un pulsante nell'ActionStrip: un problema qui (host che rifiuta di aprire
        // l'Activity, permesso mancante, ecc.) non deve mai far crashare l'intera schermata di
        // navigazione, quindi resta isolato in try/catch.
        try {
            getCarContext().startActivity(
                    new Intent(getCarContext(), NavAccessSettingsActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Throwable t) {
            Log.e(TAG, "impossibile aprire le impostazioni di accesso alle notifiche", t);
        }
    }

    private void draw(Canvas canvas, Rect visibleArea) {
        NavInstruction instr = NavHudState.currentInstruction();
        boolean stale = NavHudState.hasEverReceivedData() && NavHudState.isStale();
        RectF area = new RectF(visibleArea);
        NavGlyphRenderer.draw(canvas, area, instr, stale);
    }

    @Override
    public void onStart(@NonNull androidx.lifecycle.LifecycleOwner owner) {
        // Callback del ciclo di vita chiamato dal framework: un'eccezione qui non è come
        // un'eccezione dentro un try/catch nostro, risale direttamente e chiude l'app.
        try {
            NavSurfaceRenderer.getInstance().start(getCarContext(), this::draw);
        } catch (Throwable t) {
            Log.e(TAG, "errore nell'avvio del disegno della schermata", t);
        }
    }

    @Override
    public void onStop(@NonNull androidx.lifecycle.LifecycleOwner owner) {
        try {
            NavSurfaceRenderer.getInstance().stop();
        } catch (Throwable t) {
            Log.e(TAG, "errore nell'arresto del disegno della schermata", t);
        }
    }
}
