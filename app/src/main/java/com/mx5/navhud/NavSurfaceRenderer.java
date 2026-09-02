package com.mx5.navhud;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.car.app.AppManager;
import androidx.car.app.CarContext;
import androidx.car.app.SurfaceCallback;
import androidx.car.app.SurfaceContainer;

/**
 * Gestisce l'unica Surface di disegno personalizzato che l'host mette a disposizione alle
 * schermate NavigationTemplate, e la ridisegna periodicamente. Serve per disegnare la freccia
 * di svolta a schermo intero, molto più grande di quanto permettano le icone dei template
 * standard (limitate a 44dp per specifica di design di Android Auto, indipendentemente dalla
 * risoluzione dei bitmap forniti dall'app).
 *
 * Singleton perché l'host fornisce una sola Surface per l'intera app. Con una sola schermata
 * (NavHudScreen) in questa versione dell'app non è strettamente necessario che sia un
 * singleton "multi-schermata" come nel progetto gauge da cui deriva, ma la struttura è
 * mantenuta identica per coerenza e per poter aggiungere in futuro altre schermate senza
 * doverla riscrivere.
 */
final class NavSurfaceRenderer {

    interface DrawCallback {
        void draw(Canvas canvas, Rect visibleArea);
    }

    private static final NavSurfaceRenderer INSTANCE = new NavSurfaceRenderer();
    private static final long TICK_MS = 300;
    private static final int BACKGROUND = Color.parseColor("#09090F");

    static NavSurfaceRenderer getInstance() {
        return INSTANCE;
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private volatile Surface surface;
    private volatile Rect visibleArea;
    private volatile DrawCallback drawCallback;
    private boolean registered = false;
    private int epoch = 0;

    private final class Tick implements Runnable {
        private final int myEpoch;

        Tick(int myEpoch) {
            this.myEpoch = myEpoch;
        }

        @Override
        public void run() {
            if (myEpoch != epoch) {
                return;
            }
            render();
            handler.postDelayed(this, TICK_MS);
        }
    }

    private final SurfaceCallback surfaceCallback = new SurfaceCallback() {
        @Override
        public void onSurfaceAvailable(@NonNull SurfaceContainer surfaceContainer) {
            surface = surfaceContainer.getSurface();
            render();
        }

        @Override
        public void onVisibleAreaChanged(@NonNull Rect visible) {
            visibleArea = visible;
            render();
        }

        @Override
        public void onStableAreaChanged(@NonNull Rect stableArea) {
            render();
        }

        @Override
        public void onSurfaceDestroyed(@NonNull SurfaceContainer surfaceContainer) {
            surface = null;
        }
    };

    private NavSurfaceRenderer() {
    }

    void start(@NonNull CarContext carContext, @NonNull DrawCallback callback) {
        this.drawCallback = callback;
        if (!registered) {
            carContext.getCarService(AppManager.class).setSurfaceCallback(surfaceCallback);
            registered = true;
        }
        epoch++;
        handler.post(new Tick(epoch));
    }

    void stop() {
        // Intenzionalmente vuoto: vedi il commento sul campo epoch in start().
    }

    private static final String TAG = "NavSurfaceRenderer";

    private void render() {
        Surface s = surface;
        DrawCallback cb = drawCallback;
        if (s == null || !s.isValid() || cb == null) {
            return;
        }
        Canvas canvas;
        try {
            canvas = s.lockCanvas(null);
        } catch (Exception e) {
            return;
        }
        try {
            canvas.drawColor(BACKGROUND);
            Rect area = visibleArea != null ? visibleArea : new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
            // Un'eccezione qui (es. nel disegno del simbolo di manovra) non deve far crashare
            // l'intero processo dell'app — cosa che l'host di Android Auto mostrerebbe come
            // "si è verificato un errore imprevisto" e chiuderebbe l'app. La si intercetta e si
            // disegna al suo posto, direttamente sullo schermo, cosa è andato storto: così il
            // problema resta visibile e diagnosticabile (basta una foto) invece di un crash
            // totale che non lascia traccia utile senza collegare un PC con adb.
            try {
                cb.draw(canvas, area);
            } catch (Throwable t) {
                Log.e(TAG, "Errore nel disegno della schermata", t);
                drawErrorOverlay(canvas, area, t);
            }
        } finally {
            s.unlockCanvasAndPost(canvas);
        }
    }

    private void drawErrorOverlay(Canvas canvas, Rect area, Throwable t) {
        canvas.drawColor(Color.parseColor("#09090F"));
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.parseColor("#FF4D4D"));
        p.setTextAlign(Paint.Align.CENTER);
        p.setFakeBoldText(true);
        float textSize = Math.min(area.width(), area.height()) * 0.045f;
        p.setTextSize(textSize);
        float cx = area.centerX();
        float y = area.centerY() - textSize * 2;
        canvas.drawText("ERRORE DI DISEGNO (MX-5 Nav HUD)", cx, y, p);

        Paint p2 = new Paint(p);
        p2.setFakeBoldText(false);
        p2.setTextSize(textSize * 0.8f);
        p2.setColor(Color.parseColor("#F4F4FA"));
        String msg = t.getClass().getSimpleName() + ": " + t.getMessage();
        canvas.drawText(msg, cx, y + textSize * 1.4f, p2);

        StackTraceElement[] trace = t.getStackTrace();
        if (trace.length > 0) {
            String where = trace[0].getClassName() + "." + trace[0].getMethodName()
                    + ":" + trace[0].getLineNumber();
            canvas.drawText(where, cx, y + textSize * 2.4f, p2);
        }
    }
}
