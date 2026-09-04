package com.mx5.navhud.nav;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import androidx.annotation.NonNull;

import java.util.Locale;

/**
 * Disegna, direttamente su un Canvas a schermo intero, il simbolo grande della manovra di
 * svolta corrente, nello stesso linguaggio grafico degli altri gauge dell'app: sfondo quasi
 * nero, tratto bianco per la forma della strada/freccia, rosso MX5 come accento per la parte
 * che indica la direzione, nessun altro colore. Nessuna mappa disegnata: solo il simbolo
 * essenziale, pensato per essere letto in mezzo secondo mentre si guida.
 */
public final class NavGlyphRenderer {

    private static final int WHITE = Color.parseColor("#F4F4FA");
    private static final int RED = Color.parseColor("#FF4D4D");
    private static final int DIM = Color.parseColor("#8888A0");
    private static final int TRACK = Color.parseColor("#2A2A3C");
    private static final int BG = Color.parseColor("#09090F");

    private NavGlyphRenderer() {
    }

    /** Simbolo "di marchio" usato dallo SplashScreen: la rotonda, il simbolo più riconoscibile
     *  e distintivo dell'app, senza dipendere da un'istruzione di navigazione reale. */
    public static void drawBrandGlyph(@NonNull Canvas c, float cx, float cy, float size, int accent) {
        Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(size * 0.10f);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        stroke.setColor(accent);

        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill.setStyle(Paint.Style.FILL);
        fill.setColor(accent);

        drawRoundabout(c, cx, cy, size, stroke, fill, 3);
    }

    /** Disegna la scena completa: freccia grande centrata nella parte alta dell'area, distanza
     *  e nome strada sotto, in stile coerente con GaugeIcon.drawTile. */
    public static void draw(@NonNull Canvas c, @NonNull RectF area, @NonNull NavInstruction instr,
                             boolean stale) {
        // Difesa contro un'area non valida (larghezza/altezza zero o negativa, capita per un
        // singolo frame durante un cambio di layout dell'host): non c'è nulla di sensato da
        // disegnare in quel caso, meglio saltare il frame che rischiare calcoli con numeri
        // degeneri più avanti.
        if (!(area.width() > 0) || !(area.height() > 0)) {
            return;
        }
        float pad = Math.min(area.width(), area.height()) * 0.04f;
        RectF inner = new RectF(area.left + pad, area.top + pad, area.right - pad, area.bottom - pad);

        if (instr.type == ManeuverType.WAITING) {
            drawWaiting(c, inner);
            return;
        }

        float textBlockH = inner.height() * 0.30f;
        RectF glyphBox = new RectF(inner.left, inner.top, inner.right, inner.bottom - textBlockH);
        RectF textBox = new RectF(inner.left, inner.bottom - textBlockH, inner.right, inner.bottom);

        int accent = stale ? DIM : RED;
        drawGlyph(c, glyphBox, instr, accent);
        drawText(c, textBox, instr, stale);
    }

    private static void drawWaiting(Canvas c, RectF inner) {
        Paint txt = new Paint(Paint.ANTI_ALIAS_FLAG);
        txt.setColor(DIM);
        txt.setTextAlign(Paint.Align.CENTER);
        txt.setFakeBoldText(true);
        txt.setTextSize(Math.min(inner.width(), inner.height()) * 0.07f);
        float cy = inner.centerY();
        c.drawText("IN ATTESA DI NAVIGAZIONE GOOGLE MAPS", inner.centerX(), cy - txt.getTextSize(), txt);

        Paint sub = new Paint(txt);
        sub.setTextSize(txt.getTextSize() * 0.72f);
        sub.setFakeBoldText(false);
        sub.setColor(TRACK);
        c.drawText("avvia un percorso su Google Maps (anche da qui in Android Auto)", inner.centerX(), cy, sub);
        c.drawText("(HUD sperimentale, non ufficiale)", inner.centerX(), cy + sub.getTextSize() * 1.4f, sub);

        drawWaitingDiagnostics(c, inner, cy + sub.getTextSize() * 3.2f, sub.getTextSize() * 0.9f);
    }

    /**
     * Messaggio sotto al testo "in attesa", mostrato solo quando il servizio di ascolto delle
     * notifiche risulta davvero non collegato (es. permesso "Accesso alle notifiche" revocato):
     * un unico avviso generico, non i dettagli tecnici usati durante la messa a punto — quelli
     * servivano per la diagnosi iniziale, non hanno senso da mostrare al conducente durante
     * l'uso normale.
     */
    private static void drawWaitingDiagnostics(Canvas c, RectF inner, float y, float textSize) {
        if (NavHudState.isListenerConnected()) {
            return;
        }
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(textSize);
        p.setColor(RED);
        c.drawText("Connessione persa", inner.centerX(), y, p);
    }

    private static void drawText(Canvas c, RectF box, NavInstruction instr, boolean stale) {
        float cx = box.centerX();
        int primary = stale ? DIM : WHITE;

        Paint distPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        distPaint.setColor(primary);
        distPaint.setTextAlign(Paint.Align.CENTER);
        distPaint.setFakeBoldText(true);
        distPaint.setTextSize(box.height() * 0.42f);
        String dist = !instr.distanceText.isEmpty() ? instr.distanceText : "—";
        float distY = box.top + box.height() * 0.44f;
        c.drawText(dist, cx, distY, distPaint);

        Paint roadPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        roadPaint.setColor(DIM);
        roadPaint.setTextAlign(Paint.Align.CENTER);
        roadPaint.setTextSize(box.height() * 0.20f);
        String label = instr.type == ManeuverType.UNKNOWN
                ? truncate(instr.rawText, 42)
                : (!instr.roadName.isEmpty() ? instr.roadName : maneuverLabel(instr));
        c.drawText(label, cx, distY + box.height() * 0.30f, roadPaint);

        if (stale) {
            Paint staleP = new Paint(roadPaint);
            staleP.setColor(RED);
            staleP.setTextSize(box.height() * 0.14f);
            c.drawText("DATO NON AGGIORNATO", cx, box.bottom - box.height() * 0.04f, staleP);
        }
    }

    private static String maneuverLabel(NavInstruction instr) {
        switch (instr.type) {
            case STRAIGHT: return "Prosegui dritto";
            case SLIGHT_LEFT: return "Tieni la sinistra";
            case LEFT: return "Svolta a sinistra";
            case SHARP_LEFT: return "Svolta decisa a sinistra";
            case UTURN_LEFT: case UTURN_RIGHT: return "Inversione a U";
            case SLIGHT_RIGHT: return "Tieni la destra";
            case RIGHT: return "Svolta a destra";
            case SHARP_RIGHT: return "Svolta decisa a destra";
            case ROUNDABOUT:
                return instr.roundaboutExit >= 1
                        ? "Rotonda — " + instr.roundaboutExit + "ª uscita"
                        : "Rotonda";
            case FORK_LEFT: case FORK_RIGHT: return "Biforcazione";
            case MERGE: return "Immissione";
            case ARRIVE: return "Destinazione";
            default: return "";
        }
    }

    private static String truncate(String s, int max) {
        s = s.trim();
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    private static void drawGlyph(Canvas c, RectF box, NavInstruction instr, int accent) {
        ManeuverType type = instr.type;
        float cx = box.centerX(), cy = box.centerY();
        float size = Math.min(box.width(), box.height()) * 0.86f;

        Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(size * 0.11f);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        stroke.setColor(accent);

        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill.setStyle(Paint.Style.FILL);
        fill.setColor(accent);

        switch (type) {
            case ARRIVE:
                drawArrive(c, cx, cy, size, accent);
                return;
            case ROUNDABOUT:
                drawRoundabout(c, cx, cy, size, stroke, fill, instr.roundaboutExit);
                return;
            case UTURN_LEFT:
                drawUturn(c, cx, cy, size, stroke, fill, true);
                return;
            case UTURN_RIGHT:
                drawUturn(c, cx, cy, size, stroke, fill, false);
                return;
            case MERGE:
                drawMerge(c, cx, cy, size, stroke, fill);
                return;
            case FORK_LEFT:
                drawFork(c, cx, cy, size, stroke, fill, true);
                return;
            case FORK_RIGHT:
                drawFork(c, cx, cy, size, stroke, fill, false);
                return;
            case UNKNOWN:
                drawTurnArrow(c, cx, cy, size, 0, stroke, fill);
                return;
            default:
                break;
        }

        boolean left = type == ManeuverType.SLIGHT_LEFT || type == ManeuverType.LEFT || type == ManeuverType.SHARP_LEFT;
        float bend; // gradi di curvatura della freccia rispetto alla verticale
        if (type == ManeuverType.STRAIGHT) bend = 0;
        else if (type == ManeuverType.SLIGHT_LEFT || type == ManeuverType.SLIGHT_RIGHT) bend = 40;
        else if (type == ManeuverType.SHARP_LEFT || type == ManeuverType.SHARP_RIGHT) bend = 130;
        else bend = 85; // LEFT / RIGHT
        if (left) bend = -bend;

        drawTurnArrow(c, cx, cy, size, bend, stroke, fill);
    }

    /**
     * Freccia di svolta: un "gambo" dritto e verticale (la direzione attuale di marcia) che poi
     * curva nettamente verso la direzione della svolta, con la punta orientata di conseguenza —
     * lo stesso linguaggio grafico delle frecce di svolta della segnaletica e delle altre app di
     * navigazione (un tratto dritto seguito da un gomito, non una semplice diagonale, che da
     * sola non comunica "sto girando" in modo chiaro).
     * bendDeg = 0 -> dritto in su; positivo -> verso destra; negativo -> verso sinistra;
     * ±180 non è usato qui (l'inversione a U ha il suo disegno dedicato, drawUturn).
     */
    private static void drawTurnArrow(Canvas c, float cx, float cy, float size, float bendDeg,
                                       Paint stroke, Paint fill) {
        float half = size / 2f;
        float startX = cx, startY = cy + half * 0.75f;
        double rad = Math.toRadians(bendDeg);
        float endX = cx + (float) Math.sin(rad) * half * 0.85f;
        float endY = cy - half * 0.75f + (float) (1 - Math.cos(rad)) * half * 0.55f;

        // Tangente forzata verticale in partenza (il gambo dritto) e nella direzione finale in
        // arrivo (la freccia esce già orientata come l'ultimo tratto della svolta): una Bezier
        // cubica con le maniglie di controllo allineate a queste due tangenti, invece di una
        // semplice curva dal punto A al punto B, è ciò che rende il disegno leggibile come
        // "svolta" anche a un'occhiata di mezzo secondo.
        float h1 = half * 0.55f;
        float h2 = half * 0.42f;
        float c1x = startX, c1y = startY - h1;
        float c2x = endX - (float) Math.sin(rad) * h2;
        float c2y = endY + (float) Math.cos(rad) * h2;

        Path path = new Path();
        path.moveTo(startX, startY);
        path.cubicTo(c1x, c1y, c2x, c2y, endX, endY);
        c.drawPath(path, stroke);

        drawArrowHead(c, endX, endY, bendDeg, size, fill);
    }

    private static void drawArrowHead(Canvas c, float tipX, float tipY, float headingDeg, float size, Paint fill) {
        // headingDeg: 0 = punta verso l'alto, positivo = ruotato in senso orario.
        double rad = Math.toRadians(headingDeg);
        float aw = size * 0.22f;
        float ah = size * 0.26f;
        // Vettore "avanti" (direzione della punta) e "laterale" (perpendicolare), ruotati di headingDeg.
        float fx = (float) Math.sin(rad), fy = (float) -Math.cos(rad);
        float sx = (float) Math.cos(rad), sy = (float) Math.sin(rad);

        Path head = new Path();
        head.moveTo(tipX + fx * ah * 0.6f, tipY + fy * ah * 0.6f);
        head.lineTo(tipX - fx * ah * 0.5f + sx * aw * 0.5f, tipY - fy * ah * 0.5f + sy * aw * 0.5f);
        head.lineTo(tipX - fx * ah * 0.5f - sx * aw * 0.5f, tipY - fy * ah * 0.5f - sy * aw * 0.5f);
        head.close();
        c.drawPath(head, fill);
    }

    /** Bandiera a scacchi: simbolo di arrivo/traguardo, immediatamente riconoscibile e con
     *  proporzioni equilibrate (a differenza di un segnaposto a goccia, che risulta stretto
     *  e allungato in un riquadro pensato per un simbolo largo quanto alto). */
    private static void drawArrive(Canvas c, float cx, float cy, float size, int accent) {
        Paint pole = new Paint(Paint.ANTI_ALIAS_FLAG);
        pole.setColor(accent);
        pole.setStyle(Paint.Style.STROKE);
        pole.setStrokeWidth(size * 0.06f);
        pole.setStrokeCap(Paint.Cap.ROUND);

        float poleX = cx - size * 0.20f;
        float poleTop = cy - size * 0.36f;
        float poleBottom = cy + size * 0.36f;
        c.drawLine(poleX, poleBottom, poleX, poleTop, pole);

        float flagW = size * 0.52f;
        float flagH = size * 0.34f;
        int cols = 4, rows = 3;
        float cellW = flagW / cols, cellH = flagH / rows;

        Paint accentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        accentPaint.setColor(accent);
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(BG);

        for (int r = 0; r < rows; r++) {
            for (int col = 0; col < cols; col++) {
                float x0 = poleX + col * cellW;
                float y0 = poleTop + r * cellH;
                boolean isAccent = (r + col) % 2 == 0;
                c.drawRect(x0, y0, x0 + cellW, y0 + cellH, isAccent ? accentPaint : bgPaint);
            }
        }
        Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(size * 0.025f);
        border.setColor(accent);
        c.drawRect(poleX, poleTop, poleX + flagW, poleTop + flagH, border);
    }

    // Quanti gradi si percorrono lungo la rotonda per raggiungere la 1ª, 2ª, 3ª, 4ª uscita,
    // percorrendo la rotonda in senso ANTIORARIO tenendo la destra, come si fa realmente in
    // Italia (e in generale con la guida a destra): valori crescenti, così ogni uscita ha una
    // forma visivamente diversa dalla precedente (più si esce "lontano", più il tratto
    // percorso intorno al cerchio è lungo). Il segno negativo è ciò che fa percorrere il
    // cerchio in senso antiorario invece che orario (Canvas.drawArc: sweep positivo = orario,
    // negativo = antiorario).
    private static final float[] EXIT_SWEEP_DEG = {-100f, -170f, -240f, -300f};
    private static final float DEFAULT_SWEEP_DEG = -260f; // uscita non specificata dal testo

    /** Rotonda: ingresso sempre dal basso, percorsa in senso antiorario (tenendo la destra);
     *  il tratto percorso intorno al cerchio e l'angolo di uscita cambiano in base al numero
     *  di uscita (instr.roundaboutExit), con il numero scritto anche al centro per togliere
     *  ogni ambiguità quando le forme si assomigliano. */
    private static void drawRoundabout(Canvas c, float cx, float cy, float size, Paint stroke, Paint fill, int exitNumber) {
        float r = size * 0.32f;
        RectF oval = new RectF(cx - r, cy - r, cx + r, cy + r);

        float startAngle = 90f; // punto più basso del cerchio: l'ingresso
        float sweep = (exitNumber >= 1 && exitNumber <= EXIT_SWEEP_DEG.length)
                ? EXIT_SWEEP_DEG[exitNumber - 1] : DEFAULT_SWEEP_DEG;

        c.drawArc(oval, startAngle, sweep, false, stroke);

        float exitAngle = ((startAngle + sweep) % 360f + 360f) % 360f;
        double rad = Math.toRadians(exitAngle);
        float tipX = (float) (cx + r * Math.cos(rad));
        float tipY = (float) (cy + r * Math.sin(rad));
        // Tangente nel verso di percorrenza (arco disegnato in senso antiorario, sweep
        // negativo): a differenza del senso orario, qui la tangente coincide con l'angolo
        // stesso, senza bisogno di aggiungere 180°.
        float headingDeg = exitAngle;
        double headingRad = Math.toRadians(headingDeg);
        // Segmento di uscita che PROSEGUE dritto nella stessa direzione in cui l'arco stava
        // già andando in quel punto (la tangente), non che si allontana in linea radiale dal
        // centro del cerchio: un segmento radiale forma un angolo innaturale con la curva
        // (la freccia sembra "spuntare di lato" invece che continuare il movimento), mentre
        // proseguire lungo la tangente dà l'effetto di un'unica linea fluida che esce dalla
        // rotonda e si conclude con la punta — coerente con come è disegnata la freccia di
        // svolta (drawTurnArrow), dove il tratto finale è anch'esso dritto nella direzione
        // di arrivo.
        float tanX = (float) Math.sin(headingRad);
        float tanY = (float) -Math.cos(headingRad);
        float extLen = size * 0.20f;
        float tipOutX = tipX + tanX * extLen;
        float tipOutY = tipY + tanY * extLen;
        c.drawLine(tipX, tipY, tipOutX, tipOutY, stroke);
        drawArrowHead(c, tipOutX, tipOutY, headingDeg, size, fill);

        // Ingresso: piccolo segmento dal basso verso il cerchio.
        c.drawLine(cx, cy + size * 0.48f, cx, cy + r, stroke);

        if (exitNumber >= 1) {
            Paint badge = new Paint(Paint.ANTI_ALIAS_FLAG);
            badge.setColor(fill.getColor());
            badge.setTextAlign(Paint.Align.CENTER);
            badge.setFakeBoldText(true);
            badge.setTextSize(size * 0.26f);
            c.drawText(String.valueOf(exitNumber), cx, cy + size * 0.14f, badge);
        }
    }

    /** Inversione a U: la lettera "U" disegnata per intero (due gambe verticali unite da una
     *  curva in basso), con la freccia sulla gamba di uscita rivolta verso l'alto — l'esatto
     *  simbolo di "fai un'inversione a U", non un accenno di gancio poco leggibile. */
    private static void drawUturn(Canvas c, float cx, float cy, float size, Paint stroke, Paint fill, boolean toLeft) {
        float dir = toLeft ? -1f : 1f;
        float r = size * 0.22f;
        float topY = cy - size * 0.30f;
        float bottomY = topY + size * 0.50f;

        float entryX = cx - dir * r;
        float exitX = cx + dir * r;
        float arrowGap = size * 0.16f;

        // Gamba di ingresso (nessuna freccia: è il tratto da cui si arriva).
        c.drawLine(entryX, bottomY, entryX, topY, stroke);
        // Curva in basso, a chiudere la "U".
        RectF oval = new RectF(entryX, bottomY - r, exitX, bottomY + r);
        c.drawArc(oval, 0, 180, false, stroke);
        // Gamba di uscita, più corta per lasciare spazio alla punta della freccia.
        c.drawLine(exitX, bottomY, exitX, topY + arrowGap, stroke);
        drawArrowHead(c, exitX, topY + arrowGap, 0, size, fill);
    }

    private static void drawMerge(Canvas c, float cx, float cy, float size, Paint stroke, Paint fill) {
        float half = size / 2f;
        Path main = new Path();
        main.moveTo(cx - size * 0.16f, cy + half * 0.75f);
        main.quadTo(cx - size * 0.05f, cy - half * 0.1f, cx, cy - half * 0.75f);
        c.drawPath(main, stroke);

        Paint mergeStroke = new Paint(stroke);
        mergeStroke.setColor(DIM);
        mergeStroke.setStrokeWidth(size * 0.07f);
        Path side = new Path();
        side.moveTo(cx + size * 0.30f, cy + half * 0.6f);
        side.quadTo(cx + size * 0.10f, cy + half * 0.05f, cx, cy - half * 0.2f);
        c.drawPath(side, mergeStroke);

        drawArrowHead(c, cx, cy - half * 0.75f, 0, size, fill);
    }

    private static void drawFork(Canvas c, float cx, float cy, float size, Paint stroke, Paint fill, boolean left) {
        float half = size / 2f;
        float dir = left ? -1f : 1f;

        Paint dim = new Paint(stroke);
        dim.setColor(DIM);
        Path other = new Path();
        other.moveTo(cx, cy + half * 0.75f);
        other.quadTo(cx, cy, cx - dir * size * 0.28f, cy - half * 0.6f);
        c.drawPath(other, dim);

        Path main = new Path();
        main.moveTo(cx, cy + half * 0.75f);
        main.quadTo(cx, cy, cx + dir * size * 0.28f, cy - half * 0.7f);
        c.drawPath(main, stroke);
        drawArrowHead(c, cx + dir * size * 0.28f, cy - half * 0.7f, dir * 35, size, fill);
    }
}
