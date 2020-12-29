### Trovare e configurare il server SMB

Eseguire la scansione della rete e selezionare dall'elenco dei server SMB da configurare. 

### Modifica i parametri del server SMB

Impostare manualmente i parametri per il server SMB. 

### Nome host del server/indirizzo IP

Per specificare il nome del server SMB o l'indirizzo IP 

### Protocollo SMB

È possibile specificare il protocollo SMB.

- Utilizzare SMB V1

- Utilizzare SMB V2/3

### Utilizzare il numero di porta

Specificare se il numero di porta dello standard non è disponibile. Il numero di porta standard è 139/tcp e 445/tcp. 

### Utilizzare il nome del conto e la password

Specifica se utilizzare il nome del conto del server SMB e la password

Il nome del conto è un conto locale sul server SMB. Non è possibile utilizzare un account Microsoft. 

### Nome dell'account

Per specificare il nome dell'account per il server SMB. 

### Password

Per specificare la password per il server SMB. 

### Condividi la lista

Mostra il nome della condivisione per il server SMB.  

### Elenca le directory

Mostra l'elenco delle directory del server SMB.  

### Modifica i parametri del nome della directory

La data e l'ora **<u>può essere inclusa nella directory di destinazione</u>**. Le variabili vengono convertite in data all'inizio della sincronizzazione. Si prega di confermare i dettagli della variabile premendo "Modifica parametro nome della directory". 

### Directory

Per specificare la directory per l'host SMB. Se la directory di destinazione non esiste, verrà creata al momento della sincronizzazione.

Nota: Nelle seguenti condizioni, essa diventa un riferimento circolare ed è in loop. Specificare un filtro di directory o specificare una directory sul lato master che sia diversa dalla destinazione.

- Nessuna directory specificata quando lo stesso server SMB è specificato per il master e il target

- Nessun filtro di directory specificato

### <u>Il seguente viene visualizzato solo quando il tipo di sincronizzazione è Archive.</u>

### Salvare  tutti i file nella directory di destinazione senza creare sottodirectory

Se spuntata, la directory di destinazione non creerà una sottodirectory nella directory di origine.

### Per archiviare il file

Selezionare un file con data e ora di ripresa più vecchie della data e ora di esecuzione dell'archivio. (Indipendentemente dalla data e dall'ora di ripresa, la data di ripresa è di 7 giorni o più vecchia, la data di ripresa è di 30 giorni o più vecchia, la data di ripresa è di 60 giorni o più vecchia, la data di ripresa è di 90 giorni o più vecchia, la data di ripresa è di 180 giorni o più vecchia, la data di ripresa è di più di un anno) 

### Incrementare  i nomi dei file aggiungendo

È possibile aggiungere un numero di sequenza al nome del file. 

### Modifica il parametro del nome del file

Per includere la data e l'ora nel nome del file, toccare il pulsante e modificare.