package com.mx5.navhud;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import com.mx5.navhud.nav.NavGlyphRenderer;
import com.mx5.navhud.nav.NavHudState;
import com.mx5.navhud.nav.NavInstruction;

/**
 * Anteprima dell'HUD direttamente sullo schermo del telefono, senza bisogno di essere collegati
 * ad Android Auto: mostra la stessa identica schermata (stesso codice di disegno,
 * NavGlyphRenderer, e stesso stato in memoria, NavHudState) che comparirebbe sullo schermo
 * dell'auto. Serve solo per verificare comodamente, dal telefono in mano, se il servizio di
 * ascolto delle notifiche di Google Maps sta funzionando e cosa sta effettivamente leggendo —
 * comprese le righe diagnostiche della schermata "in attesa" — senza dover essere in auto e
 * collegati ad Android Auto per ogni prova. Compare come una seconda icona a parte nell'elenco
 * app del telefono; non è la modalità d'uso normale dell'app (quella resta lo schermo dell'auto).
 */
public final class NavPreviewActivity extends Activity {

    private static final String TAG = "NavPreviewActivity";
    private static final long TICK_MS = 300;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = this::tick;
    private PreviewView view;
    private boolean running = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            view = new PreviewView(this);
            setContentView(view);
        } catch (Throwable t) {
            Log.e(TAG, "errore nella creazione dell'anteprima", t);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        running = true;
        try {
            handler.post(tick);
        } catch (Throwable t) {
            Log.e(TAG, "errore nell'avvio dell'aggiornamento dell'anteprima", t);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        running = false;
        try {
            handler.removeCallbacks(tick);
        } catch (Throwable t) {
            Log.e(TAG, "errore nell'arresto dell'aggiornamento dell'anteprima", t);
        }
    }

    private void tick() {
        if (!running || view == null) {
            return;
        }
        try {
            view.invalidate();
        } catch (Throwable t) {
            Log.e(TAG, "errore nell'aggiornamento dell'anteprima", t);
        } finally {
            handler.postDelayed(tick, TICK_MS);
        }
    }

    /** Disegna con lo stesso NavGlyphRenderer usato per lo schermo dell'auto: qualsiasi
     *  differenza vista qui è quindi rappresentativa di quello che si vedrebbe in auto. */
    private static final class PreviewView extends View {
        PreviewView(Context context) {
            super(context);
            setBackgroundColor(Color.parseColor("#09090F"));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            // Un'anteprima di debug non deve mai bloccare il resto del telefono: un problema
            // nel disegno qui si traduce solo in uno sfondo nero fermo, non in un crash.
            try {
                NavInstruction instr = NavHudState.currentInstruction();
                boolean stale = NavHudState.hasEverReceivedData() && NavHudState.isStale();
                RectF area = new RectF(0, 0, getWidth(), getHeight());
                NavGlyphRenderer.draw(canvas, area, instr, stale);
            } catch (Throwable t) {
                Log.e(TAG, "errore nel disegno dell'anteprima", t);
            }
        }
    }
}
