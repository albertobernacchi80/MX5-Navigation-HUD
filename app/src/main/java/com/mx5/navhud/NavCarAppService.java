package com.mx5.navhud;

import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.car.app.CarAppService;
import androidx.car.app.validation.HostValidator;
import androidx.car.app.Session;

public final class NavCarAppService extends CarAppService {
    @NonNull @Override public HostValidator createHostValidator() {
        // Va bene per test locali su Android Auto. Prima di un'eventuale pubblicazione,
        // sostituire con la strategia ufficiale di allow-list consigliata da Android for Cars.
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR;
    }

    @NonNull @Override public Session onCreateSession() {
        return new NavSession();
    }
}
