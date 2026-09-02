package com.mx5.navhud;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.NavigationTemplate;

import com.mx5.navhud.nav.NavGlyphRenderer;

/**
 * Schermata di avvio a schermo intero, coerente con quello che l'app fa davvero: non la
 * silhouette dell'auto (qui non ci sono gauge di guida), ma la freccia di svolta stessa —
 * il simbolo su cui è costruita l'intera app — più il nome. Disegnata direttamente sulla
 * Surface, come l'HUD vero e proprio, non con un'icona di template limitata a 44dp.
 *
 * Dopo una breve pausa passa automaticamente all'HUD di navigazione e si rimuove dallo
 * stack, così il tasto indietro non torna qui. C'è comunque un pulsante "Salta" nell'
 * ActionStrip, selezionabile con la rotella, per chi non vuole aspettare la pausa.
 */
public final class SplashScreen extends Screen implements androidx.lifecycle.DefaultLifecycleObserver {

    private static final int RED = Color.parseColor("#FF4D4D");
    private static final int WHITE = Color.parseColor("#F4F4FA");
    private static final int DIM = Color.parseColor("#8888A0");

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable goToHud = this::goToHud;
    // Sia il timer automatico sia il pulsante "Salta" chiamano goToHud(): questo flag evita
    // un doppio push se arrivano quasi insieme (stessa idea usata nell'app gauge).
    private boolean navigated = false;

    public SplashScreen(@NonNull CarContext context) {
        super(context);
        getLifecycle().addObserver(this);
    }

    private static final String TAG = "SplashScreen";

    private void goToHud() {
        if (navigated) return;
        navigated = true;
        // Chiamata sia dal timer automatico sia dal tasto "Salta": un'eccezione qui (host che
        // rifiuta il push, CarContext non più valido perché l'utente è già uscito, ecc.) non
        // deve far crashare l'app. Se fallisce, si annulla la navigazione e si resta sullo
        // splash: l'utente può comunque riprovare con "Salta", invece di trovarsi l'app chiusa.
        try {
            getScreenManager().push(new NavHudScreen(getCarContext()));
            finish();
        } catch (Throwable t) {
            Log.e(TAG, "impossibile passare all'HUD di navigazione", t);
            navigated = false;
        }
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        // Vedi il commento equivalente in NavHudScreen.onGetTemplate(): mai lasciar risalire
        // un'eccezione all'host, altrimenti l'intera app viene chiusa con l'errore generico.
        try {
            ActionStrip actionStrip = new ActionStrip.Builder()
                    .addAction(new Action.Builder()
                            .setTitle("Salta")
                            .setOnClickListener(this::goToHud)
                            .build())
                    .build();

            return new NavigationTemplate.Builder()
                    .setActionStrip(actionStrip)
                    .build();
        } catch (Throwable t) {
            Log.e(TAG, "onGetTemplate fallito, uso il template minimo di sicurezza", t);
            try {
                ActionStrip minimal = new ActionStrip.Builder()
                        .addAction(new Action.Builder().setTitle("MX-5").setOnClickListener(() -> {
                        }).build())
                        .build();
                return new NavigationTemplate.Builder().setActionStrip(minimal).build();
            } catch (Throwable t2) {
                Log.e(TAG, "anche il template minimo di sicurezza è fallito", t2);
                throw t2 instanceof RuntimeException ? (RuntimeException) t2 : new RuntimeException(t2);
            }
        }
    }

    private void draw(Canvas canvas, Rect visibleArea) {
        float cx = visibleArea.centerX();
        float cy = visibleArea.centerY();
        float areaW = visibleArea.width();
        float areaH = visibleArea.height();

        float glyphSize = Math.min(areaW, areaH) * 0.34f;
        NavGlyphRenderer.drawBrandGlyph(canvas, cx, cy - areaH * 0.08f, glyphSize, RED);

        String mx5 = "MX-5";
        String rest = " Nav HUD";
        float titleY = cy + areaH * 0.26f;
        float titleSize = Math.min(areaW, areaH) * 0.09f;

        Paint mx5Paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mx5Paint.setColor(RED);
        mx5Paint.setFakeBoldText(true);
        mx5Paint.setTextSize(titleSize);

        Paint restPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        restPaint.setColor(WHITE);
        restPaint.setFakeBoldText(true);
        restPaint.setTextSize(titleSize);

        float mx5W = mx5Paint.measureText(mx5);
        float restW = restPaint.measureText(rest);
        float startX = cx - (mx5W + restW) / 2f;

        canvas.drawText(mx5, startX, titleY, mx5Paint);
        canvas.drawText(rest, startX + mx5W, titleY, restPaint);

        Paint subPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        subPaint.setColor(DIM);
        subPaint.setTextAlign(Paint.Align.CENTER);
        subPaint.setTextSize(titleSize * 0.34f);
        canvas.drawText("indicazioni di navigazione", cx, titleY + titleSize * 0.7f, subPaint);
    }

    @Override
    public void onStart(@NonNull androidx.lifecycle.LifecycleOwner owner) {
        NavSurfaceRenderer.getInstance().start(getCarContext(), this::draw);
        handler.postDelayed(goToHud, 2000);
    }

    @Override
    public void onStop(@NonNull androidx.lifecycle.LifecycleOwner owner) {
        handler.removeCallbacks(goToHud);
        NavSurfaceRenderer.getInstance().stop();
    }
}
