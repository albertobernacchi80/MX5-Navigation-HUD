package com.mx5.navhud;

import android.content.Intent;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.car.app.CarAppService;
import androidx.car.app.validation.HostValidator;
import androidx.car.app.Session;

public final class NavCarAppService extends CarAppService {

    private static final String TAG = "NavCarAppService";
    private static boolean uncaughtHandlerInstalled = false;

    @Override
    public void onCreate() {
        super.onCreate();
        installUncaughtExceptionLogger();
    }

    /**
     * Non cambia il comportamento in caso di crash (l'eccezione, dopo essere stata registrata
     * nel log, viene comunque passata al gestore di sistema, che chiude l'app come sempre):
     * serve solo ad avere, nel Logcat/bug report del telefono, una riga ben visibile con il
     * tag di questa app anche per un eventuale crash che sfugge a tutti i try/catch mirati
     * già presenti nel resto del codice (disegno, notifiche, template).
     */
    private static synchronized void installUncaughtExceptionLogger() {
        if (uncaughtHandlerInstalled) {
            return;
        }
        uncaughtHandlerInstalled = true;
        final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                Log.e(TAG, "CRASH non gestito in " + thread.getName(), throwable);
            } catch (Throwable ignored) {
                // Non deve mai impedire la normale gestione del crash qui sotto.
            }
            if (previous != null) {
                previous.uncaughtException(thread, throwable);
            }
        });
    }

    @NonNull @Override public HostValidator createHostValidator() {
        // Va bene per test locali su Android Auto. Prima di un'eventuale pubblicazione,
        // sostituire con la strategia ufficiale di allow-list consigliata da Android for Cars.
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR;
    }

    @NonNull @Override public Session onCreateSession() {
        return new NavSession();
    }
}
