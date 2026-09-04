package com.mx5.navhud;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Attività "ponte": prova ad aprire direttamente la schermata di sistema "Accesso alle
 * notifiche" e si chiude. Android non permette a un'app di chiedere questo permesso con un
 * normale pop-up: l'utente deve concederlo a mano dalle Impostazioni, cercando questa app
 * nell'elenco e attivandola. Va lanciata una sola volta, dal telefono (compare nell'elenco app
 * come "Attiva notifiche Google Maps - MX-5 Nav HUD").
 *
 * L'intent standard di Android per questa schermata (usato qui sotto) non è implementato allo
 * stesso modo su tutte le personalizzazioni Android (su alcune, come MIUI, può risolversi in
 * una schermata sbagliata o non risolversi affatto): in quel caso, prima si prova un ripiego
 * più generico, e se anche quello fallisce si mostrano istruzioni testuali a schermo intero
 * invece di chiudersi silenziosamente — un Toast da solo è facile da perdere proprio nel
 * momento in cui l'attività sparisce, e "non succede nulla" è la cosa peggiore da mostrare a
 * chi sta cercando di attivare un permesso.
 */
public final class NavAccessSettingsActivity extends Activity {

    private static final String TAG = "NavAccessSettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean opened;
        try {
            opened = tryOpenNotificationAccessSettings();
        } catch (Throwable t) {
            Log.e(TAG, "errore nel tentativo di aprire le impostazioni di accesso alle notifiche", t);
            opened = false;
        }
        if (opened) {
            Toast.makeText(this,
                    "Cerca \"MX-5 Nav HUD\" nell'elenco e attivalo",
                    Toast.LENGTH_LONG).show();
            finish();
        } else {
            showManualInstructions();
        }
    }

    /** Prova prima l'intent specifico per "Accesso alle notifiche", poi un ripiego più
     *  generico. Restituisce true solo se una delle due schermate si è aperta davvero. */
    private boolean tryOpenNotificationAccessSettings() {
        if (tryStartActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))) {
            return true;
        }
        return tryStartActivity(new Intent(Settings.ACTION_SETTINGS));
    }

    private boolean tryStartActivity(Intent intent) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (intent.resolveActivity(getPackageManager()) == null) {
                return false;
            }
            startActivity(intent);
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "impossibile avviare " + intent.getAction(), t);
            return false;
        }
    }

    /** Ripiego finale se nessuna scorciatoia automatica ha funzionato: una schermata a tutto
     *  schermo con le istruzioni per arrivarci a mano, che resta visibile finché l'utente non
     *  la tocca (non si chiude da sola), invece di un Toast facile da non notare. */
    private void showManualInstructions() {
        TextView tv = new TextView(this);
        tv.setText("Non sono riuscito ad aprire automaticamente le impostazioni su questo telefono.\n\n"
                + "Apri a mano: Impostazioni > Notifiche > Accesso speciale > Accesso alle notifiche\n"
                + "(su alcuni telefoni: Impostazioni > App > Autorizzazioni speciali > Accesso alle notifiche).\n\n"
                + "Nell'elenco, cerca \"MX-5 Nav HUD\" e attivalo.\n\n"
                + "Tocca lo schermo per chiudere questa schermata.");
        tv.setTextColor(Color.parseColor("#F4F4FA"));
        tv.setBackgroundColor(Color.parseColor("#09090F"));
        tv.setTextSize(18);
        tv.setGravity(Gravity.CENTER);
        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        tv.setPadding(pad, pad, pad, pad);
        tv.setOnClickListener(v -> finish());
        setContentView(tv);
    }
}
