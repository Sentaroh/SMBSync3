## 1.Inviare i dati registrati dall'app

I dati registrati dall'app possono essere inviati all'esterno tramite e-mail e strumenti di condivisione dalle seguenti operazioni dell'app. <span style="color: rosso; "><u>L'app non invierà i dati registrati all'esterno a meno che l'utente non lo faccia.</u></span></span

- Premere il "Pulsante Condividi" dalla scheda Cronologia

- Premere il pulsante "Invia allo sviluppatore" dalle informazioni del sistema

- Premere il pulsante "Condividi" o "Invia allo sviluppatore" dalla gestione dei log

## 2.Dati registrati dall'app

### 2.1.Lista dei compiti di sincronizzazione

L'app registra i dati necessari per eseguire la sincronizzazione.

- Nome della directory, nome del file, nome host del server SMB, indirizzo IP, numero di porta, nome dell'account (***1**), password (***1**)

- Password dell'applicazione (***1**) per proteggere il lancio dell'applicazione e la modifica delle impostazioni

- Valore di impostazione dell'applicazione

***1** Crittografato con chiave generata dal sistema memorizzata in AndroidKeystore.

 

### 2.2.Record di attività dell'applicazione

L'applicazione registrerà i seguenti dati quando si abilita la registrazione per verificare e risolvere i risultati della sincronizzazione.

- Versione Android, produttore del terminale, nome del terminale, modello del terminale, versione dell'applicazione

- Nome della directory, nome del file, dimensione del file, ultima modifica del file

- Nome host del server SMB, indirizzo IP, numero di porta, nome del conto

- Nome dell'interfaccia di rete, indirizzo IP

- Valore di impostazione del sistema (WiFi Sleep policy, Informazioni sulla memorizzazione)

- Valore di impostazione dell'applicazione

### 2.3.Sync elenco attività esportato

L'app può esportare "2.1 Lista dei compiti di sincronizzazione" in un file. È possibile proteggere con password l'esportazione.

- Nome della directory, nome del file
- Nome host del server SMB, indirizzo IP, numero di porta, nome del conto, password
- Valore di impostazione dell'applicazione 

## 3.Scopo dell'utilizzo dei dati inviati agli sviluppatori di app

I dati inviati dagli utenti dell'app allo sviluppatore saranno utilizzati solo per risolvere i problemi dell'app e non saranno comunicati a nessun altro che allo sviluppatore.

## 4.Autorizzazioni

L'applicazione richiede le seguenti autorizzazioni.

### 4.1.Photos/Media/Files

**read the contents of your USB storage  
modify or delete the contents of your USB storage**  
Utilizzato per la sincronizzazione dei file con la memorizzazione interna/esterna e la lettura/scrittura dei file di gestione.

### 4.2.Storage

### 4.2.1.Android11から  
**Manage external storage**  

USBストレージへのファイル同期と管理ファイルの読み書きで使用します。

### 4.2.2.Android10まで  
**read the contents of your USB storage  
modify or delete the contents of your USB storage**  
Utilizzato per la sincronizzazione dei file su memoria USB e la lettura/scrittura di file di gestione.

### 4.3.Wi-Fi Connection infomation

**view Wi-Fi connections**  
Utilizzato per controllare lo stato del Wi-Fi all'inizio della sincronizzazione.

### 4.4.Altro

### 4.4.1.view network connections

Utilizzato per confermare che è collegato alla rete all'inizio della sincronizzazione.

### 4.4.2.connect and disconnect from Wi-Fi

Utilizzato per accendere/spegnere il Wi-Fi nella sincronizzazione del programma in Andoid 8/9.

### 4.4.3.full network access

Utilizzato per eseguire la sincronizzazione con il protocollo SMB attraverso la rete.

### 4.4.4.run at startup

Utilizzato per eseguire la sincronizzazione della programmazione.

### 4.4.5.control vibration

Utilizzato per notificare all'utente la fine della sincronizzazione.

### 4.4.6.prevent device from sleeping

Utilizzatelo per avviare la sincronizzazione da un programma o da un'applicazione esterna.

### 4.4.7.install shortcuts

Utilizzato per aggiungere una scorciatoia di avvio della sincronizzazione al desktop.


