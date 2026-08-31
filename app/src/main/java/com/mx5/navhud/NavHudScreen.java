package com.mx5.navhud;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
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
        // Nessun'altra schermata da raggiungere: niente Back, niente menu.
        return new NavigationTemplate.Builder()
                .setActionStrip(new ActionStrip.Builder().build())
                .build();
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
