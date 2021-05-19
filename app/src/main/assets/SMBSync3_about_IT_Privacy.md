## 1.Dati raccolti  
### 1.1.Dati forniti dall'utente a SMBSync3  

I seguenti dati forniti dall'utente per utilizzare SMBSync3 saranno memorizzati nell'area di memoria dell'applicazione.  

- Informazioni sul file (nome della directory, nome del file)  
- Informazioni sul server SMB se si usa un server SMB (nome host/indirizzo IP, numero di porta, nome dell'account(**<u>\*1</u>**), password dell'account(**<u>\*1</u>**))  
- Informazioni sul file ZIP se si usa un file ZIP (metodo di compressione, metodo di crittografia, password di crittografia(**<u>\*1</u>**))  
- Opzioni di impostazione delle applicazioni (messaggi di avviso, lingua e dimensione dei caratteri, ecc.)  
- Password dell'applicazione(**<u>\*1</u>**)  

**<u>\*1</u>**I dati sono criptati e conservati.  

### 1.2.Risultato dell'esecuzione di SMBSync3  

Salva i dati nell'area di memorizzazione dell'applicazione in modo che l'utente possa controllare il risultato dell'esecuzione di SMBSync3.  

- Nome della directory, nome del file, stato di esecuzione  
- Dimensione dei file sincronizzati, data e ora di aggiornamento dei file  
- Informazioni di errore  

### 1.3.Registro di attività di SMBSync3  

Abilitando la registrazione verranno salvati i dati di registrazione dell'attività nell'area di memoria dell'app per la verifica dei risultati di esecuzione dell'app e per il supporto tecnico. Quando la registrazione è disabilitata, la registrazione dei dati verrà interrotta. Tuttavia, i dati già registrati non saranno cancellati, quindi puoi cancellarli se ne hai bisogno.  

- Informazioni sul dispositivo (nome del produttore, nome del modello, versione del sistema operativo, punto di montaggio, directory specifica dell'applicazione, StorageAccessFramework, gestore dello storage, indirizzo IP, abilitazione/disab  
- Versione SMBSync3, opzioni di esecuzione SMBSync3  
- Nome della directory, nome del file, stato di esecuzione  
- Dimensione dei file sincronizzati, data e ora di aggiornamento dei file  
- Informazioni di debug  
- Informazioni di errore  

### 1.4.Invio o scrittura di dati al di fuori di SMBSync3  

<span style="color: red;"><u>I dati di SMBSync3 non possono essere inviati o scritti all'esterno a meno che non sia l'utente a farlo.</u></span>  

- Premi [Pulsante di condivisione] dalla scheda Storia.  
- Fare clic sul pulsante "Invia allo sviluppatore" da Informazioni di sistema.  
- Cliquez sur le bouton "Envoyer au développeur" à partir de la gestion du journal.  
- Fare clic sul pulsante "Export log file" dalla gestione dei registri per esportare su una memoria esterna.  
- Eseguendo "Impostazioni di esportazione" dal menu, "1.1.Dati forniti dall'utente a SMBSync3" saranno esportati.  
Le informazioni saranno criptate specificando una password durante l'esportazione.  

### 1.5.Cancellare i dati memorizzati in SMBSync3  

Disinstallando SMBSync3, i dati salvati ("1.1.Dati forniti dall'utente a SMBSync3", "1.2.Risultato dell'esecuzione di SMBSync3", "1.3.Registro di attività di SMBSync3") saranno cancellati dal dispositivo.  
<span style="color: red;"><u>Tuttavia, i dati memorizzati nella memoria esterna a causa dell'interazione dell'utente non saranno cancellati.</u></span>  

### 2.Permessi richiesti per eseguire l'applicazione  

### 2.1.Stoccaggio  

### 2.1.1.Android11 o successivo.  
**Tutti gli accessi ai file**.  

Tutti gli accessi ai file** Usato per la sincronizzazione dei file e la gestione dei file in lettura/scrittura.  

### 2.1.2.Android 10 o prima  

#### 2.1.2.1.Foto, media, file  
**"read the contents of your USB storage"**  
**"modify or delete the contents of your USB storage"**  
Utilizzato per la sincronizzazione dei file e la lettura/scrittura di file di gestione.  

### 2.2.Informazioni sulla connessione Wi-Fi  
**vedi le connessioni Wi-Fi**.  
Utilizzato per controllare lo stato del Wi-Fi quando inizia la sincronizzazione.  

### 2.3.Altri  
### 2.3.1.View network connections  
Usalo per controllare le connessioni di rete quando viene avviata la sincronizzazione.  
### 2.3.2.connect and disconnect from Wi-Fi  
Questa funzione è usata per attivare/disattivare il Wi-Fi per la sincronizzazione programmata su Andoid 8/9.  
### 2.3.3.Full network access  
Questo è usato per sincronizzare tramite il protocollo SMB attraverso la rete.  
### 2.3.4.Run at startup  
Utilizzato per eseguire la sincronizzazione programmata.  
### 2.3.5.Control vibration  
Questo è usato per notificare all'utente quando la sincronizzazione è finita.  
### 2.3.6.Prevent device from sleeping  
Utilizzato per avviare la sincronizzazione da una pianificazione o da un'applicazione esterna.  
### 2.3.7.Install shortcuts  
Utilizzato per aggiungere un collegamento di avvio della sincronizzazione al desktop.  
