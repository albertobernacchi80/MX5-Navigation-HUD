package com.mx5.navhud;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;

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

    @NonNull
    @Override
    public Template onGetTemplate() {
        // NavigationTemplate richiede un ActionStrip con almeno 1 azione (un ActionStrip vuoto
        // fa fallire la validazione lato host non appena questa schermata viene mostrata, con
        // il classico "si è verificato un errore imprevisto"): qui l'unica azione utile è una
        // scorciatoia verso il permesso "Accesso alle notifiche", nel caso venga revocato.
        ActionStrip actionStrip = new ActionStrip.Builder()
                .addAction(new Action.Builder()
                        .setTitle("Notifiche")
                        .setOnClickListener(this::openNotificationAccessSettings)
                        .build())
                .build();

        return new NavigationTemplate.Builder()
                .setActionStrip(actionStrip)
                .build();
    }

    private void openNotificationAccessSettings() {
        getCarContext().startActivity(
                new Intent(getCarContext(), NavAccessSettingsActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    private void draw(Canvas canvas, Rect visibleArea) {
        NavInstruction instr = NavHudState.currentInstruction();
        boolean stale = NavHudState.hasEverReceivedData() && NavHudState.isStale();
        RectF area = new RectF(visibleArea);
        NavGlyphRenderer.draw(canvas, area, instr, stale);
    }

    @Override
    public void onStart(@NonNull androidx.lifecycle.LifecycleOwner owner) {
        NavSurfaceRenderer.getInstance().start(getCarContext(), this::draw);
    }

    @Override
    public void onStop(@NonNull androidx.lifecycle.LifecycleOwner owner) {
        NavSurfaceRenderer.getInstance().stop();
    }
}
