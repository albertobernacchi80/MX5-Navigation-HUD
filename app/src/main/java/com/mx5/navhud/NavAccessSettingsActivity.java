package com.mx5.navhud;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

/**
 * Attività "ponte", senza interfaccia propria: apre direttamente la schermata di sistema
 * "Accesso alle notifiche" e si chiude. Android non permette a un'app di chiedere questo
 * permesso con un normale pop-up: l'utente deve concederlo a mano dalle Impostazioni,
 * cercando questa app nell'elenco e attivandola. Va lanciata una sola volta, dal telefono
 * (compare nell'elenco app come "Attiva notifiche Google Maps - MX-5 Nav HUD").
 */
public final class NavAccessSettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            Toast.makeText(this,
                    "Attiva \"MX-5 Nav HUD\" nell'elenco per usare l'HUD di navigazione",
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Apri manualmente: Impostazioni > App > Accesso speciale > Accesso alle notifiche",
                    Toast.LENGTH_LONG).show();
        }
        finish();
    }
}
