## 1.Dati raccolti<br>
### 1.1.Dati forniti dall'utente a SMBSync3<br>

I dati forniti dall'utente per usare SMBSync3 saranno salvati nell'area di archiviazione dell'applicazione.<br>
Quando si memorizzano i dati, il nome dell'account SMB, la password dell'account SMB, la password ZIP e la password dell'applicazione saranno criptati con la password generata dal sistema.<br>
<span style="color: red;"><u>I dati non saranno inviati all'esterno a meno che non venga eseguita l'operazione "1.4.Invio o scrittura di dati al di fuori di SMBSync3".</u></span>.<br>

- Informazioni sul file (nome della directory, nome del file)<br>
- Informazioni sul server SMB (nome host/indirizzo IP, numero di porta, nome dell'account, password dell'account)<br>
- Informazioni sul file ZIP (metodo di compressione, metodo di crittografia, password di crittografia)<br>
- Opzioni di impostazione delle applicazioni (messaggi di avviso, lingua e dimensione dei caratteri, ecc.)<br>
- Password dell'applicazione (password usata per l'autenticazione all'avvio dell'applicazione, autenticazione quando si cambiano le impostazioni di sicurezza, ecc.)<br>

### 1.2.Risultato dell'esecuzione di SMBSync3<br>

Salva i dati nell'area di memorizzazione dell'applicazione in modo che l'utente possa controllare il risultato dell'esecuzione di SMBSync3.<br>
<span style="color: red;"><u>I dati non saranno inviati all'esterno a meno che non venga eseguita l'operazione "1.4.Invio o scrittura di dati al di fuori di SMBSync3".</u></span>.<br>

- Nome della directory, nome del file, stato di esecuzione<br>
- Dimensione dei file sincronizzati, data e ora di aggiornamento dei file<br>
- Informazioni di errore<br>

### 1.3.Registro di attività di SMBSync3<br>

Salva i dati nell'area di memorizzazione dell'applicazione per verificare il risultato dell'esecuzione di SMBSync3 e per informare lo sviluppatore.<br>
<span style="color: red;"><u>I dati non saranno inviati all'esterno a meno che non venga eseguita l'operazione "1.4.Invio o scrittura di dati al di fuori di SMBSync3".</u></span>.<br>

- Informazioni sul dispositivo (nome del produttore, nome del modello, versione del sistema operativo, punto di montaggio, directory specifica dell'applicazione, StorageAccessFramework, gestore dello storage, indirizzo IP, abilitazione/disab<br>
- Versione SMBSync3, opzioni di esecuzione SMBSync3<br>
- Nome della directory, nome del file, stato di esecuzione<br>
- Dimensione dei file sincronizzati, data e ora di aggiornamento dei file<br>
- Informazioni di debug<br>
- Informazioni di errore<br>

### 1.4.Invio o scrittura di dati al di fuori di SMBSync3<br>

I dati di SMBSync3 non possono essere inviati o scritti all'esterno a meno che non sia l'utente a farlo.<br>

- Premi [Pulsante di condivisione] dalla scheda Storia.<br>
- Fare clic sul pulsante "Invia allo sviluppatore" da Informazioni di sistema.<br>
- Cliquez sur le bouton "Envoyer au développeur" à partir de la gestion du journal.<br>
- Fare clic sul pulsante "Export log file" dalla gestione dei registri per esportare su una memoria esterna.<br>
- Eseguendo "Impostazioni di esportazione" dal menu, "1.1.Dati forniti dall'utente a SMBSync3" saranno esportati.<br>
Specificando una password durante l'esportazione, le informazioni vengono criptate e salvate nel file.<br>

### 1.5.Cancellare i dati memorizzati in SMBSync3<br>

Disinstallando SMBSync3, i dati salvati ("1.1.Dati forniti dall'utente a SMBSync3", "1.2.Risultato dell'esecuzione di SMBSync3", "1.3.Registro di attività di SMBSync3") saranno cancellati dal dispositivo.<br>
<span style="color: red;"><u>Tuttavia, i dati memorizzati nella memoria esterna a causa dell'interazione dell'utente non saranno cancellati.</u></span><br>

### 2.Permessi richiesti per eseguire l'applicazione<br>

### 2.1.Foto, media, file<br>
**leggere il contenuto della vostra memoria USB**.<br>
**modificare o cancellare il contenuto della vostra memoria USB**.<br>
Utilizzato per la sincronizzazione dei file e la lettura/scrittura di file di gestione.<br>

### 2.2.Stoccaggio<br>

### 2.2.1.Android11 o successivo.<br>
**Tutti gli accessi ai file**.<br>

Tutti gli accessi ai file** Usato per la sincronizzazione dei file e la gestione dei file in lettura/scrittura.<br>

### 2.2.2.Android 10 o prima<br>
**leggere il contenuto della vostra memoria USB**.<br>
**modificare o cancellare il contenuto della vostra memoria USB**.<br>
Utilizzato per la sincronizzazione dei file e la lettura/scrittura di file di gestione.<br>

### 2.3.Informazioni sulla connessione Wi-Fi<br>
**vedi le connessioni Wi-Fi**.<br>
Utilizzato per controllare lo stato del Wi-Fi quando inizia la sincronizzazione.<br>

### 2.4.Altri<br>
### 2.4.1.View network connections<br>
Usalo per controllare le connessioni di rete quando viene avviata la sincronizzazione.<br>
### 2.4.2.connect and disconnect from Wi-Fi<br>
Questa funzione è usata per attivare/disattivare il Wi-Fi per la sincronizzazione programmata su Andoid 8/9.<br>
### 2.4.3.Full network access<br>
Questo è usato per sincronizzare tramite il protocollo SMB attraverso la rete.<br>
### 2.4.4.Run at startup<br>
Utilizzato per eseguire la sincronizzazione programmata.<br>
### 2.4.5.Control vibration<br>
Questo è usato per notificare all'utente quando la sincronizzazione è finita.<br>
### 2.4.6.Prevent device from sleeping<br>
Utilizzato per avviare la sincronizzazione da una pianificazione o da un'applicazione esterna.<br>
### 2.4.7.Install shortcuts<br>
Utilizzato per aggiungere un collegamento di avvio della sincronizzazione al desktop.<br>
