package com.mx5.navhud;

import android.content.Intent;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.car.app.Session;
import androidx.car.app.Screen;

/** Una breve schermata di avvio (SplashScreen), poi l'HUD di navigazione: non c'è altro da
 *  scegliere (nessun menu gauge/telemetria/report: quest'app fa solo navigazione). */
public final class NavSession extends Session {

    private static final String TAG = "NavSession";

    @NonNull @Override public Screen onCreateScreen(@NonNull Intent intent) {
        // Se per qualsiasi motivo la costruzione dello splash fallisse, si salta direttamente
        // alla schermata di navigazione vera e propria invece di lasciar risalire l'eccezione
        // (che qui chiuderebbe l'app prima ancora che compaia una schermata qualsiasi).
        try {
            return new SplashScreen(getCarContext());
        } catch (Throwable t) {
            Log.e(TAG, "impossibile creare lo SplashScreen, avvio direttamente l'HUD", t);
            return new NavHudScreen(getCarContext());
        }
    }
}
