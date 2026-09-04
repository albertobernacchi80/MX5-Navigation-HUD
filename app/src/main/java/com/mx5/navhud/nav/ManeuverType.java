package com.mx5.navhud.nav;

/**
 * Tipo di manovra da mostrare come simbolo grande sullo schermo dell'auto.
 * Ricavato per parole chiave dal testo (italiano) delle notifiche di navigazione
 * di Google Maps: NON è un dato strutturato fornito da Google (che non espone
 * un'API pubblica per leggere la manovra corrente dell'app Maps), quindi va
 * considerato "best effort" — vedi ManeuverTextParser.
 */
public enum ManeuverType {
    WAITING,        // Nessuna notifica di navigazione ricevuta ancora / navigazione non attiva.
    STRAIGHT,       // Prosegui dritto.
    SLIGHT_LEFT,
    LEFT,
    SHARP_LEFT,
    UTURN_LEFT,
    SLIGHT_RIGHT,
    RIGHT,
    SHARP_RIGHT,
    UTURN_RIGHT,
    ROUNDABOUT,     // Rotonda (direzione/uscita non distinta in v1).
    FORK_LEFT,
    FORK_RIGHT,
    MERGE,          // Immissione in autostrada/superstrada.
    ARRIVE,         // Destinazione raggiunta.
    UNKNOWN         // Notifica di navigazione ricevuta ma testo non riconosciuto: si mostra
                     // comunque la distanza e il testo grezzo, con una freccia generica.
}
