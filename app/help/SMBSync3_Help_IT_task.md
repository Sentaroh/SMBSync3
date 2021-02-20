### Modalità di test

Non esegue la sovrascrittura e la cancellazione del file se si controlla. Si prega di essere testato quando si crea un'attività di sincronizzazione, i file che vengono copiati o cancellati possono essere trovati nella scheda dei messaggi.

### Sincronizzazione  automatica

Se si seleziona l'attività all'automatico. Le attività che sono impostate sulla sincronizzazione automatica si avvieranno quando si preme il pulsante sync.

### Nome dell'attività

Specificare il nome dell'attività.

### Tipo di sincronizzazione
Selezionare un metodo tra Mirror, Copy, Move e Archive. <span style="color: red;"><u>La sincronizzazione viene effettuata in una direzione, dalla cartella sorgente alla cartella di destinazione.</u></span></span   
- Mirror  
  Fare una copia differenziale (**<u>*1</u>**) delle directory e dei file sul lato sorgente verso il lato destinazione, ed eliminare i file e le directory sul lato destinazione che non esistono sul lato sorgente dopo che la copia è stata completata.

- Spostare  

  Fare una copia differenziale della directory lato sorgente e del file sul lato di destinazione e cancellare il file lato sorgente copiato sul lato di destinazione. Tuttavia, il file con lo stesso nome dell'origine e della destinazione, ma con le stesse dimensioni del file e la stessa data di modifica, non viene copiato e il file sul lato origine viene cancellato.

- Copia  
Fare una copia differenziale dei file contenuti nella directory di origine sul lato di destinazione.

- Archivio  
Spostare le foto e i video contenuti nella directory di origine a destinazione con la condizione che la data e l'ora di ripresa siano anteriori a 7 giorni o 30 giorni dalla data e dall'ora di esecuzione dell'archivio. (Tuttavia, ZIP non può essere utilizzato come destinazione).

**<u>*1</u>** Se una delle tre condizioni seguenti è soddisfatta, il file viene giudicato come un file di differenza e viene copiato o spostato. Tuttavia, la dimensione del file e l'ultimo tempo modificato possono essere ignorati nelle opzioni del task di sincronizzazione.  

1. il file non esiste
2. la dimensione del file è diversa
3. l'ultima data e ora modificata è diversa di oltre 3 secondi (il numero di secondi può essere cambiato con l'opzione del task di sincronizzazione)

### Scambiare  fonte e destinazione
Scambiare il contenuto della cartella di origine e della cartella di destinazione 

### Cartella di origine

Toccare il pulsante per modificare la cartella sorgente

### Cartella di destinazione

Toccare il pulsante per modificare la cartella di destinazione

### Selezionare i file per la sincronizzazione
Se non si controlla e poi si sincronizza tutto il file. Se si controlla per vedere i dettagli. 
- Sincronizzare i file audio  
  Se si verifica la sincronizzazione dei file delle seguenti estensioni.  
aac, aif, aifc, aiff, kar, flac, m3u, m4a, mid, midi, mp2, mp3, mpga, ogg, ra, ram, ram, wav  
- Sincronizzare i file di immagine  
  Se si verifica la sincronizzazione dei file delle seguenti estensioni.  
bmp, cgm, djv, djvu, gif, ico,ief, jpe, jpeg, jpg, pbm, pgm, png,pnm, ppm, ras, rgb, svg, tif, tiff, wbmp, xbm, xpm, xwd
- Sincronizzare i file video  
  Se si verifica la sincronizzazione dei file delle seguenti estensioni.  
avi, m4u, mov, mp4, movie, mpe, mpeg, mpg, mxu, qt, wmv  
- Filtro file  
  È possibile selezionare il nome e l'estensione del file con cui si desidera sincronizzarsi, oltre a quanto sopra.

### Seleziona cosa archiviare

Selezionare i criteri per le foto o i video da archiviare.

- Qualsiasi data (Tutti)
- Più vecchio di 7 giorni
- Più vecchio di 30 giorni
- Più vecchio di 60 giorni
- Più vecchio di 90 giorni
- Più di 180 giorni
- Più vecchio di 1 anno

### Selezionare  le sottodirectory

Se non si controlla e poi si sincronizza tutta la sottocartella. Se si controlla per mostrare il pulsante del filtro della directory.

- Filtro della directory
  È possibile selezionare il nome della directory che si desidera sincronizzare.
### Eseguire  le operazioni di sincronizzazione solo in fase di ricarica

Se l'opzione è selezionata, è possibile avviare la sincronizzazione solo durante la carica. Se la sincronizzazione viene avviata quando non è in carica, si verificherà un errore.

### La  sincronizzazione include i file che si trovano direttamente nella root della  directory di origine  

si décoché, seuls les  dossiers et leurs fichiers/sous-dossiers seront synchronisés

### Confermare  prima di sovrascrivere/cancellare

Quando si desidera sovrascrivere e cancellare il file, viene visualizzata una finestra di dialogo di conferma se è stato selezionato.

### Opzioni di errore  

È possibile specificare il comportamento quando si verifica un errore.
- Interrompere la sincronizzazione
- Ignorare  tutti gli errori e iniziare le attività successive  
  Utilizzare questa opzione se si vuole essere sicuri che le attività successive vengano eseguite. 
- Avviare  le attività successive anche se non si adattano alle opzioni di rete  
  Utilizzare questa funzione se si desidera eseguire attività successive quando l'indirizzo non è privato o quando non è l'indirizzo IP specificato.  

### Rete

- Esegui anche quando sei fuori  
È sempre possibile iniziare la sincronizzazione
- Quando è collegato all'AP  
La sincronizzazione può iniziare se la LAN wireless è collegata a qualsiasi punto di accesso.
- Solo indirizzo IP privato  
La sincronizzazione può essere avviata quando l'indirizzo IP è un indirizzo privato
- Registrato nell'elenco degli indirizzi IP  
È possibile avviare la sincronizzazione quando l'indirizzo IP è registrato nell'elenco degli indirizzi IP.  
  È possibile utilizzare i caratteri jolly per il filtro. (ad esempio: 192.168.100.\*, 192.168.\*.\*.*).

### Permette  la sincronizzazione con tutti gli indirizzi IP (includono pubblico) 

Permette la sincronizzazione su tutti gli indirizzi IP. Tuttavia, la scansione del server SMB non può essere eseguita.
### Mostra le opzioni avanzate

**Si prega di utilizzarlo quando si impostano opzioni dettagliate.**
### Includi le sottodirectory
Includerà ricorsivamente le sottocartelle sotto la cartella sorgente specificata. 

### Includere le directory vuote
Sincronizza le directory vuote (anche se una directory è vuota sul sorgente, verrà creata sulla destinazione). Se deselezionata, le directory vuote sul sorgente vengono ignorate. 

### Includi le directory nascoste
Quando spuntata, Sync includerà le cartelle linux nascoste (quelle con un nome che inizia con un punto). Si noti che in Windows e Samba, l'attributo nascosto non è impostato dal nome della cartella. Pertanto, la cartella sincronizzata sulla destinazione SMB/Windows non avrà l'attributo nascosto dell'host. 

### Includere i file nascosti
Quando spuntata, Sync includerà i file linux nascosti (quelli con un nome che inizia con un punto). Si noti che in Windows e Samba, l'attributo nascosto non è impostato dal nome del file. Pertanto, il file sincronizzato sulla destinazione SMB/Windows non avrà l'attributo nascosto dell'host.

### Sovrascrivere i file di destinazione
Se deselezionati, i file sulla destinazione non verranno mai sovrascritti anche se i criteri di confronto per dimensioni e tempo sono diversi. 

### Riprova su errore di rete (solo per le condivisioni SMB)
In caso di errori di connessione lato server, SMBSync3 riproverà la sincronizzazione per un massimo di 3 volte ad un intervallo di 30 secondi. 

### Limitare il buffer di scrittura SMB I/O a 16KB (solo per le condivisioni SMB)
Provare se si ottiene un errore di "Accesso negato" quando si scrive nella cartella PC/NAS. Quando viene selezionato, limiterà il buffer I/O a 16KB per le operazioni di scrittura sull'host SMB. 

### Cancellare i file prima della sincronizzazione (solo modalità Mirror)

Una volta spuntate, le directory e i file che sono presenti nella cartella di destinazione ma che non esistono nella sorgente, verranno prima cancellati. Dopo di che, i file e le cartelle che sono diversi saranno copiati nella destinazione.
Se la cartella di origine è SMB, il tempo di elaborazione sarà più lungo perché la struttura delle directory e il loro contenuto viene scansionato attraverso la rete. Si raccomanda vivamente di abilitare l'opzione "Usa la negoziazione SMB2" perché SMB1 sarà molto lenta.

### Rimuovere le directory e i file esclusi dai filtri

Se abilitato, **rimuove le directory/file che sono escluse dal filtro.** 

### Non impostare l'ultimo orario modificato del file di destinazione per far corrispondere il file sorgente

Si prega di abilitare se si ottiene un errore come SmbFile#setLastModified()/File#setLastModified() fallisce. Significa che l'host remoto non permette l'impostazione del file modificato l'ultima volta. Se deselezionato, l'ultimo orario modificato del file copiato sulla destinazione sarà impostato all'orario in cui è stato copiato / sincronizzato. Ciò significa che il file di destinazione apparirà più nuovo del sorgente. 

### Utilizzare la dimensione del file per determinare se i file sono diversi

Quando viene selezionato, i file sono considerati diversi se differiscono per dimensioni. 
### Solo le dimensioni confrontare
I file sono considerati diversi solo se la dimensione della fonte è maggiore della destinazione. Questo disabiliterà il confronto in base al tempo del file. 

### Utilizzare il tempo dell'ultima modifica per determinare se i file sono diversi 
Quando si seleziona, i file sono considerati diversi in base al loro ultimo tempo di modifica 

### Differenza di tempo minima consentita (in secondi) tra i file sorgente e quelli di destinazione
I file sono considerati identici se la differenza tra i loro ultimi tempi modificati è inferiore o uguale al tempo selezionato in secondi. Sono considerati diversi se la differenza di tempo tra i file è superiore al tempo selezionato. FAT e ExFAT richiedono una tolleranza minima di 2 secondi. Se si seleziona 0 secondi, i file devono avere esattamente lo stesso tempo per essere considerati simili.

### Non sovrascrivere il file di destinazione se è più nuovo del file sorgente
Se spuntata, il file verrà sovrascritto solo quando il file di origine è più nuovo del file di destinazione, anche se le dimensioni del file e gli ultimi tempi di aggiornamento sono diversi. Tenere presente che se si cambiano i fusi orari o se i file vengono modificati durante il periodo di intervallo del cambio dell'ora legale, l'ultimo file modificato potrebbe apparire più vecchio del file non aggiornato. Ciò è legato alle differenze del file system e solo un controllo manuale prima di sovrascrivere il file eviterà la perdita di dati. Si raccomanda generalmente di non modificare i file durante l'intervallo del cambio dell'ora legale se sono destinati ad essere auto-sincronizzati. 

### Ignora la differenza di fuso orario tra i file
Permette di selezionare la differenza di fuso orario in minuti tra l'ora legale e quella invernale. I file sono considerati diversi se la differenza di orario non è esattamente uguale all'intervallo specificato (+/- la "differenza di orario minima consentita (in secondi)" specificata nell'opzione precedente)

### Salta i nomi di directory e file che contengono caratteri non validi(", :, \, \, *, <, >, |)
Se spuntata, visualizzerà un messaggio di avviso e la sincronizzazione continuerà senza elaborare le directory/file contenenti caratteri non validi. 

### Cancellare la directory di origine quando è vuota (solo quando l'opzione Sync è Move)
Quando la modalità di sincronizzazione è "Spostare", dopo che i file sono stati spostati nella destinazione, viene cancellata anche la cartella Source. 

### Se la data e l'ora non possono essere determinate dai dati EXIF, viene visualizzato un messaggio di conferma
Visualizzare un messaggio di conferma quando non è possibile ottenere da Exif la data e l'ora prese.

### Ignora i file sorgente che sono più grandi di 4 GB quando la sincronizzazione con la memoria esterna.
Se l'opzione è selezionata, è possibile evitare errori I/O durante la sincronizzazione su una scheda MicroSD ignorando i file sorgente di dimensioni superiori a 4 GB da sincronizzare sulla memoria locale.

### Ignora i file il cui nome file supera i 255 byte
Se spuntata, ignorare i file con nomi di file più lunghi di 255 byte.
