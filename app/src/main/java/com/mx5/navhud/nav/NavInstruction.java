package com.mx5.navhud.nav;

import androidx.annotation.NonNull;

/** Un'istruzione di navigazione così come ricavata dall'ultima notifica di Google Maps. */
public final class NavInstruction {

    @NonNull public final ManeuverType type;
    /** Es. "300 m", "1,2 km" — già formattata, testo grezzo estratto dalla notifica. */
    @NonNull public final String distanceText;
    /** Es. "Via Roma" — vuoto se non riconosciuto. */
    @NonNull public final String roadName;
    /** Testo completo della notifica, per il caso UNKNOWN (mostrato piccolo sotto la freccia). */
    @NonNull public final String rawText;
    /** Numero dell'uscita alla rotonda (1, 2, 3, ...), 0 se non riconosciuto nel testo o se
     *  type non è ROUNDABOUT: in quel caso il simbolo disegnato è quello generico. */
    public final int roundaboutExit;

    public NavInstruction(@NonNull ManeuverType type, @NonNull String distanceText,
                           @NonNull String roadName, @NonNull String rawText, int roundaboutExit) {
        this.type = type;
        this.distanceText = distanceText;
        this.roadName = roadName;
        this.rawText = rawText;
        this.roundaboutExit = roundaboutExit;
    }

    static final NavInstruction WAITING =
            new NavInstruction(ManeuverType.WAITING, "", "", "", 0);
}
