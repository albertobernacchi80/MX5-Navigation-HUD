# MX-5 Nav HUD

## Cos'è

Un'app per Android Auto che, mentre navighi con Google Maps sul telefono, mostra sullo
schermo dell'auto un'indicazione di svolta grande e semplice — una freccia, la distanza, il
nome della strada — invece della mappa completa. Nessun gauge di velocità o accelerazione,
nessuna registrazione dati: questa app fa solo questo.

Il percorso e la voce guida restano quelli di Google Maps, esattamente come oggi: questa app
non li sostituisce, si limita a "tradurre" l'istruzione di svolta in un simbolo grande e
leggibile in mezzo secondo mentre guidi.

## Come si usa

1. In Android Auto apri **Google Maps**, imposta la destinazione e avvia la navigazione, come fai di solito.
2. Torna alla schermata principale di Android Auto e apri **MX-5 Nav HUD**.
3. Metti l'app a schermo intero: da questo momento continui a sentire la voce guida di Google
   Maps, ma sullo schermo vedi solo la freccia della prossima svolta, con distanza e nome strada.

Per tornare alla mappa completa (per guardare il traffico, cambiare percorso, ecc.) basta
riaprire Google Maps da Android Auto in qualsiasi momento.

## Impostazione (una volta sola, dal telefono)

Prima di poter usare l'app serve concedere un permesso che Android non chiede da solo con un
pop-up:

1. Installa l'app sul telefono (vedi sotto).
2. Dal launcher del telefono, apri l'icona **"Attiva notifiche Google Maps - MX-5 Nav HUD"**:
   si apre direttamente la schermata giusta nelle Impostazioni di sistema.
3. Nell'elenco che compare, trova **"MX-5 Nav HUD"** e attivalo.
4. Fatto: non serve ripetere questo passaggio, a meno che il telefono non revochi da solo il
   permesso (vedi il punto sulla batteria qui sotto).

### Batteria: un'impostazione importante (per due app, non una sola)

Alcuni telefoni, con l'ottimizzazione batteria molto aggressiva, limitano le app che restano
in background. Qui ne sono coinvolte **due**, ed entrambe vanno escluse dall'ottimizzazione
batteria, non solo Google Maps:

- **Google Maps**, che deve restare attivo in background mentre guardi l'HUD e non la mappa,
  per continuare a generare le indicazioni che l'app legge;
- **MX-5 Nav HUD** stessa, perché il suo servizio di ascolto notifiche gira anch'esso in
  background: se il telefono lo "addormenta" per risparmiare batteria, l'HUD smette di
  ricevere aggiornamenti anche se Google Maps sta funzionando perfettamente.

Per entrambe le app, vai su **Impostazioni del telefono > App > [nome app] > Batteria** e
imposta **"Nessuna restrizione"** (su alcuni telefoni si chiama "Non ottimizzare" o "Consenti
attività in background"). Su telefoni Samsung, Xiaomi, Huawei e simili, controlla anche che non
esista un elenco separato tipo "Gestione batteria" / "App messe in pausa automaticamente" /
"Avvio automatico" in cui entrambe le app debbano essere aggiunte manualmente: questi produttori
spesso applicano restrizioni proprie, più aggressive di quelle standard di Android. Va fatto una
sola volta per app.

Se noti che le indicazioni sull'HUD si bloccano o restano indietro mentre l'audio di Google Maps
continua regolarmente, è quasi sempre uno di questi due interruttori non ancora impostati.

## Cosa aspettarsi (limiti da conoscere)

- Funziona solo se il telefono ha **Google Maps in italiano**: l'app riconosce le istruzioni
  in base alle parole usate da Maps ("svolta a destra", "rotonda", "prosegui dritto", ecc.).
  Con Maps in un'altra lingua l'app mostra comunque la distanza, ma non sempre indovina il
  simbolo giusto.
- È una funzione **non ufficiale**: si appoggia al modo in cui Google Maps mostra le sue
  notifiche di navigazione, non a un collegamento diretto con l'app. Se in futuro Google Maps
  cambia il modo in cui scrive quelle notifiche, l'HUD potrebbe smettere di riconoscerle
  correttamente — non è quindi da considerare uno strumento di sicurezza a cui affidarsi in
  modo assoluto, ma un supporto visivo in più.
- Se per più di una decina di secondi non arriva nessun aggiornamento da Google Maps (percorso
  interrotto, telefono che limita l'app, ecc.), l'HUD lo segnala a schermo scrivendo "DATO NON
  AGGIORNATO", invece di continuare a mostrare l'ultima svolta come se fosse ancora valida.
- L'app non richiede GPS, sensori né connessione a internet: non misura né invia nulla, legge
  solo le notifiche di navigazione di Google Maps già presenti sul telefono.

## Installazione

Non è disponibile su Google Play: l'installazione avviene manualmente.

### Scaricare l'APK già pronto

Se è già disponibile una build, scarica **MX-5 Nav HUD.apk** dalla sezione **Releases** di
questo repository, oppure dagli **Artifacts** dell'ultima esecuzione del workflow **Build APK**
in **Actions**, e installala manualmente sul telefono (serve Android Auto in modalità
sviluppatore e un installatore APK come KingInstaller).

### Oppure, compilare l'APK da soli (senza Android Studio)

1. Crea un account gratuito su GitHub e un nuovo repository con il contenuto di questa cartella.
2. Vai su **Actions → Build APK → Run workflow**.
3. A esecuzione completata, apri quell'esecuzione e scarica l'artifact: contiene
   **MX-5 Nav HUD.apk**, pronta da installare sul telefono.

## Licenza

Applicazione per uso personale, non destinata alla distribuzione o vendita a terzi.
© 2026 Alberto Bernacchi. Tutti i diritti riservati. "Mazda" e "MX-5" sono marchi registrati
dei rispettivi proprietari, citati in questo repository e nell'app solo a scopo descrittivo.
"Google Maps" è un marchio di Google LLC, citato solo a scopo descrittivo: questa app non è
un prodotto Google e non ne usa alcuna API ufficiale.
