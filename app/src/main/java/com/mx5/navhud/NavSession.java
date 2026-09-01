package com.mx5.navhud;

import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.car.app.Session;
import androidx.car.app.Screen;

/** Una breve schermata di avvio (SplashScreen), poi l'HUD di navigazione: non c'è altro da
 *  scegliere (nessun menu gauge/telemetria/report: quest'app fa solo navigazione). */
public final class NavSession extends Session {
    @NonNull @Override public Screen onCreateScreen(@NonNull Intent intent) {
        return new SplashScreen(getCarContext());
    }
}
