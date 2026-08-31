package com.mx5.navhud.nav;

import androidx.annotation.NonNull;

import java.text.Normalizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interpreta per parole chiave il testo (titolo + corpo) delle notifiche di navigazione di
 * Google Maps in italiano, per ricavarne un ManeuverType, la distanza e il nome della strada.
 *
 * QUESTO È UN "BEST EFFORT", NON UN'API UFFICIALE: Google non pubblica una struttura dati per
 * le manovre correnti dell'app Maps, quindi l'unico modo per un'app terza di sapere "cosa sta
 * per succedere" senza fare essa stessa il routing è leggere il testo che Maps scrive nella
 * propria notifica di navigazione e indovinarne il significato. Se Google cambia le frasi
 * usate, o l'utente cambia la lingua del telefono, il riconoscimento smette di funzionare
 * (si torna a ManeuverType.UNKNOWN, che mostra comunque distanza e testo grezzo). Funziona
 * solo quando la navigazione turn-by-turn di Google Maps è attiva e la sua notifica è
 * visibile (non silenziata, non raggruppata/nascosta dal sistema).
 */
public final class ManeuverTextParser {

    private ManeuverTextParser() {
    }

    private static final Pattern DISTANCE =
            Pattern.compile("(\\d+[.,]?\\d*)\\s*(metri|metro|m|chilometri|chilometro|km)\\b");

    // "su Via Roma", "su via Dante Alighieri" -> "Via Roma" / "Dante Alighieri" grezzo.
    private static final Pattern ROAD =
            Pattern.compile("\\bsu\\s+(.+?)\\s*$");

    // "2ª uscita", "2a uscita", "2° uscita" -> 2. Solo cifre: le parole ("seconda uscita")
    // sono gestite a parte in extractRoundaboutExit, sono più comuni nel parlato di Maps.
    private static final Pattern EXIT_DIGIT =
            Pattern.compile("(\\d+)\\s*[ªa°]?\\s*uscita");

    private static final String[] ORDINAL_WORDS = {
            "prima", "seconda", "terza", "quarta", "quinta", "sesta"
    };

    @NonNull
    public static NavInstruction parse(@NonNull String title, @NonNull String text) {
        String raw = (title + " " + text).trim();
        if (raw.isEmpty()) {
            return NavInstruction.WAITING;
        }
        String norm = normalize(raw);

        String distance = "";
        Matcher dm = DISTANCE.matcher(norm);
        if (dm.find()) {
            String num = dm.group(1);
            String unit = dm.group(2);
            boolean isKm = unit.startsWith("km") || unit.startsWith("chilomet");
            distance = num + " " + (isKm ? "km" : "m");
        }

        String road = "";
        Matcher rm = ROAD.matcher(raw); // sul testo originale, per mantenere maiuscole del nome via
        if (rm.find()) {
            road = rm.group(1).trim();
            // Toglie un'eventuale distanza rimasta agganciata in fondo per errore di match.
            road = road.replaceAll("[.,;]+$", "");
        }

        ManeuverType type = classify(norm);
        int exit = (type == ManeuverType.ROUNDABOUT) ? extractRoundaboutExit(norm) : 0;
        return new NavInstruction(type, distance, road, raw, exit);
    }

    /** Numero dell'uscita alla rotonda (1, 2, 3, ...), 0 se il testo non lo specifica.
     *  Riconosce sia le parole ("prendi la seconda uscita") sia le cifre ("2ª uscita"). */
    private static int extractRoundaboutExit(@NonNull String t) {
        for (int i = 0; i < ORDINAL_WORDS.length; i++) {
            if (t.contains(ORDINAL_WORDS[i])) {
                return i + 1;
            }
        }
        Matcher em = EXIT_DIGIT.matcher(t);
        if (em.find()) {
            try {
                return Integer.parseInt(em.group(1));
            } catch (NumberFormatException ignored) {
                // resta 0: si userà il simbolo generico.
            }
        }
        return 0;
    }

    private static ManeuverType classify(@NonNull String t) {
        boolean left = t.contains("sinistra");
        boolean right = t.contains("destra");

        if (t.contains("sei arrivato") || t.contains("hai raggiunto") ||
                (t.contains("destinazione") && (t.contains("arriv") || t.contains("raggiunt")))) {
            return ManeuverType.ARRIVE;
        }
        if (t.contains("rotonda") || t.contains("rotatoria")) {
            return ManeuverType.ROUNDABOUT;
        }
        if (t.contains("inversione") || t.contains("inverti il senso") || t.contains("inverti la marcia")) {
            return left ? ManeuverType.UTURN_LEFT : ManeuverType.UTURN_RIGHT;
        }
        if (t.contains("immettiti") || t.contains("entra in autostrada") || t.contains("entra nella superstrada")) {
            return ManeuverType.MERGE;
        }
        if (t.contains("biforcazione") || t.contains("allo svincolo") || t.contains("mantieniti")) {
            if (left) return ManeuverType.FORK_LEFT;
            if (right) return ManeuverType.FORK_RIGHT;
        }
        boolean sharp = t.contains("decisa") || t.contains("stretta");
        boolean slight = t.contains("leggermente") || t.contains("tieni la");
        if (t.contains("svolta") || t.contains("gira") || t.contains("prendi") || t.contains("esci")) {
            if (left) {
                if (sharp) return ManeuverType.SHARP_LEFT;
                if (slight) return ManeuverType.SLIGHT_LEFT;
                return ManeuverType.LEFT;
            }
            if (right) {
                if (sharp) return ManeuverType.SHARP_RIGHT;
                if (slight) return ManeuverType.SLIGHT_RIGHT;
                return ManeuverType.RIGHT;
            }
        }
        if (slight && left) return ManeuverType.SLIGHT_LEFT;
        if (slight && right) return ManeuverType.SLIGHT_RIGHT;
        if (t.contains("prosegui dritto") || t.contains("continua dritto") ||
                t.contains("vai dritto") || t.contains("prosegui per") || t.contains("continua per")) {
            return ManeuverType.STRAIGHT;
        }
        if (left) return ManeuverType.LEFT;
        if (right) return ManeuverType.RIGHT;
        return ManeuverType.UNKNOWN;
    }

    /** Minuscolo e senza accenti, per matching più robusto (es. "svolta a sinistra" vs "SVOLTA..."). */
    private static String normalize(@NonNull String s) {
        String lower = s.toLowerCase(java.util.Locale.ITALIAN);
        String stripped = Normalizer.normalize(lower, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return stripped;
    }
}
