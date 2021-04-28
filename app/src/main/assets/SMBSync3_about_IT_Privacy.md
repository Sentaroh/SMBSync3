## 1.Dati raccolti
### 1.1.Dati forniti dall'utente a SMBSync3

I dati forniti dall'utente per utilizzare SMBSync3 saranno memorizzati nell'area di archiviazione nell'applicazione.
Tuttavia, il nome dell'account SMB, la password dell'account SMB, la password ZIP e la password dell'applicazione saranno criptati e memorizzati.
<span style="color: red;"><u>I dati non saranno inviati all'esterno a meno che non venga eseguita l'operazione "1.4.Invio o scrittura di dati al di fuori di SMBSync3".</u></span>.

- Informazioni sul file (nome della directory, nome del file)
- Informazioni sul server SMB (nome host/indirizzo IP, numero di porta, nome dell'account, password dell'account)
- Informazioni sul file ZIP (metodo di compressione, metodo di crittografia, password di crittografia)
- Opzioni di impostazione delle applicazioni (messaggi di avviso, lingua e dimensione dei caratteri, ecc.)
- Password dell'applicazione (password usata per l'autenticazione all'avvio dell'applicazione, autenticazione quando si cambiano le impostazioni di sicurezza, ecc.)

### 1.2.Risultato dell'esecuzione di SMBSync3

Salva i dati nell'area di memorizzazione dell'applicazione in modo che l'utente possa controllare il risultato dell'esecuzione di SMBSync3.
<span style="color: red;"><u>I dati non saranno inviati all'esterno a meno che non venga eseguita l'operazione "1.4.Invio o scrittura di dati al di fuori di SMBSync3".</u></span>.

- Nome della directory, nome del file, stato di esecuzione
- Dimensione dei file sincronizzati, data e ora di aggiornamento dei file
- Informazioni di errore

### 1.3.Registro di attività di SMBSync3

Salva i dati nell'area di memorizzazione dell'applicazione per verificare il risultato dell'esecuzione di SMBSync3 e per informare lo sviluppatore.
<span style="color: red;"><u>I dati non saranno inviati all'esterno a meno che non venga eseguita l'operazione "1.4.Invio o scrittura di dati al di fuori di SMBSync3".</u></span>.

- Informazioni sul dispositivo (nome del produttore, nome del modello, versione del sistema operativo, punto di montaggio, directory specifica dell'applicazione, StorageAccessFramework, gestore dello storage, indirizzo IP, abilitazione/disab
- Versione SMBSync3, opzioni di esecuzione SMBSync3
- Nome della directory, nome del file, stato di esecuzione
- Dimensione dei file sincronizzati, data e ora di aggiornamento dei file
- Informazioni di debug
- Informazioni di errore

### 1.4.Invio o scrittura di dati al di fuori di SMBSync3

I dati di SMBSync3 non possono essere inviati o scritti all'esterno a meno che non sia l'utente a farlo.

- Premi [Pulsante di condivisione] dalla scheda Storia.
- Fare clic sul pulsante "Invia allo sviluppatore" da Informazioni di sistema.
- Cliquez sur le bouton "Envoyer au développeur" à partir de la gestion du journal.
- Fare clic sul pulsante "Export log file" dalla gestione dei registri per esportare su una memoria esterna.
- Eseguendo "Impostazioni di esportazione" dal menu, "1.1.Dati forniti dall'utente a SMBSync3" saranno esportati.
Specificando una password durante l'esportazione, le informazioni vengono criptate e salvate nel file.

### 1.5.Cancellare i dati memorizzati in SMBSync3

Disinstallando SMBSync3, i dati salvati ("1.1.Dati forniti dall'utente a SMBSync3", "1.2.Risultato dell'esecuzione di SMBSync3", "1.3.Registro di attività di SMBSync3") saranno cancellati dal dispositivo.
<span style="color: red;"><u>Tuttavia, i dati memorizzati nella memoria esterna a causa dell'interazione dell'utente non saranno cancellati.</u></span>

### 2.Permessi richiesti per eseguire l'applicazione

### 2.1.Foto, media, file
**leggere il contenuto della vostra memoria USB**.
**modificare o cancellare il contenuto della vostra memoria USB**.
Utilizzato per la sincronizzazione dei file e la lettura/scrittura di file di gestione.

### 2.2.Stoccaggio

### 2.2.1.Android11 o successivo.
**Tutti gli accessi ai file**.

Tutti gli accessi ai file** Usato per la sincronizzazione dei file e la gestione dei file in lettura/scrittura.

### 2.2.2.Android 10 o prima
**leggere il contenuto della vostra memoria USB**.
**modificare o cancellare il contenuto della vostra memoria USB**.
Utilizzato per la sincronizzazione dei file e la lettura/scrittura di file di gestione.

### 2.3.Informazioni sulla connessione Wi-Fi
**vedi le connessioni Wi-Fi**.
Utilizzato per controllare lo stato del Wi-Fi quando inizia la sincronizzazione.

### 2.4.Altri
### 2.4.1.View network connections
Usalo per controllare le connessioni di rete quando viene avviata la sincronizzazione.
### 2.4.2.connect and disconnect from Wi-Fi
Questa funzione è usata per attivare/disattivare il Wi-Fi per la sincronizzazione programmata su Andoid 8/9.
### 2.4.3.Full network access
Questo è usato per sincronizzare tramite il protocollo SMB attraverso la rete.
### 2.4.4.Run at startup
Utilizzato per eseguire la sincronizzazione programmata.
### 2.4.5.Control vibration
Questo è usato per notificare all'utente quando la sincronizzazione è finita.
### 2.4.6.Prevent device from sleeping
Utilizzato per avviare la sincronizzazione da una pianificazione o da un'applicazione esterna.
### 2.4.7.Install shortcuts
Utilizzato per aggiungere un collegamento di avvio della sincronizzazione al desktop.
