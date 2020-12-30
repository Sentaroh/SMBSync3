### Modalità di test

Non esegue la sovrascrittura e la cancellazione del file se si controlla. Si prega di essere testato quando si crea un'attività di sincronizzazione, i file che vengono copiati o cancellati possono essere trovati nella scheda dei messaggi.

### Sincronizzazione  automatica

Se si seleziona l'attività all'automatico. Le attività che sono impostate sulla sincronizzazione automatica si avvieranno quando si preme il pulsante sync.

### Nome dell'attività

Specificare il nome dell'attività.

### Tipo di sincronizzazione

Il metodo di sincronizzazione viene selezionato da mirror, copia, sposta, archivia. La sincronizzazione viene effettuata dal master al bersaglio in una direzione.

- Mirror
- Spostare
- Copia
- Archivio

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
  aac, aif, aifc, aiff, kar, flac, m3u, m4a, mid, midi, mp2, mp3, mp3, mpga, ogg, ra, ra, ram, wav  
- Sincronizzare i file di immagine  
  Se si verifica la sincronizzazione dei file delle seguenti estensioni.  
  bmp, cgm, djv, djvu, gif, ico, ief, jpe, jpeg, jpg, pbm, pgm, png, tif, tiff
- Sincronizzare i file video  
  Se si verifica la sincronizzazione dei file delle seguenti estensioni.  
  avi, m4u, mov, mp4, film, mpe, mpeg, mpg, mxu, qt, wmv  
- Filtro file  
  È possibile selezionare il nome e l'estensione del file con cui si desidera sincronizzarsi, oltre a quanto sopra.

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
- Connessione a qualsiasi AP  
  La sincronizzazione può iniziare se la LAN wireless è collegata a qualsiasi punto di accesso.
- Ha un indirizzo privato  
  È possibile avviare la sincronizzazione quando l'indirizzo IP è un indirizzo privato
- Elenco indirizzi IP  
  La sincronizzazione può iniziare solo se l'indirizzo IP WiFi corrisponde a uno degli indirizzi specificati. È inoltre possibile aggiungere direttamente l'indirizzo IP corrente al quale il dispositivo è collegato attraverso l'elenco di selezione IP.  
  È possibile utilizzare i caratteri jolly per il filtro. (ad esempio: 192.168.100.\*, 192.168.\*.\*.*).

### Permette  la sincronizzazione con tutti gli indirizzi IP (includono pubblico) 

Permette la sincronizzazione su tutti gli indirizzi IP. Tuttavia, la scansione del server SMB non può essere eseguita.

### Mostra le opzioni avanzate

**Si prega di utilizzarlo quando si impostano opzioni dettagliate.**

### Includi le sottodirectory

Includerà ricorsivamente le sottodirectory sotto la cartella master specificata. 

### Includere le directory vuote

Sincronizza le directory vuote (anche se una directory è vuota sul master, verrà creata sul target). Se deselezionata, le directory vuote sul master vengono ignorate. 

### Includi le directory nascoste

Quando spuntata, Sync includerà le cartelle linux nascoste (quelle con un nome che inizia con un punto). Si noti che in Windows e Samba, l'attributo nascosto non è impostato dal nome della cartella. Pertanto, la cartella sincronizzata sulla destinazione SMB/Windows non avrà l'attributo nascosto dell'host. 

### Includere i file nascosti

Quando spuntata, Sync includerà i file linux nascosti (quelli con un nome che inizia con un punto). Si noti che in Windows e Samba, l'attributo nascosto non è impostato dal nome del file. Quindi, il file sincronizzato sulla destinazione SMB/Windows non avrà l'attributo nascosto dell'host.

### Sovrascrivere i file di destinazione

Se deselezionati, i file sul target non saranno mai sovrascritti anche se i criteri di confronto per dimensioni e tempo sono diversi. 

### Riprova su errore di rete (solo per le condivisioni SMB)

In caso di errori di connessione lato server, SMBSync3 riproverà la sincronizzazione per un massimo di 3 volte ad un intervallo di 30 secondi. 

### Limitare il buffer di scrittura SMB I/O a 16KB (solo per le condivisioni SMB)

Provare se si ottiene un errore di "Accesso negato" quando si scrive nella cartella PC/NAS.

Quando viene selezionato, limiterà il buffer I/O a 16KB per le operazioni di scrittura sull'host SMB. 

### Cancellare i file prima della sincronizzazione (solo modalità Mirror)

Una volta spuntate, le directory e i file che sono presenti nella cartella di destinazione ma che non esistono sul master, verranno prima cancellati. Dopo di che, i file e le cartelle che sono diversi saranno copiati nella cartella di destinazione.

Se la cartella master è SMB, il tempo di elaborazione sarà più lungo perché la struttura delle directory e il loro contenuto viene scansionato attraverso la rete. Si raccomanda vivamente di attivare l'opzione "Usa la negoziazione SMB2" perché SMB1 sarà molto lenta.

### Rimuovere le directory e i file esclusi dai filtri

Se abilitato, **rimuove le directory/file che sono escluse dal filtro.** 

### Non impostare l'ultimo orario modificato del file di destinazione per far corrispondere il file sorgente

Si prega di abilitare se si ottiene un errore come SmbFile#setLastModified()/File#setLastModified() fallisce. Significa che l'host remoto non permette l'impostazione del file modificato l'ultima volta. Se deselezionato, l'ultimo tempo modificato del file copiato sul target sarà impostato all'ora in cui è stato copiato / sincronizzato. Ciò significa che il file di destinazione apparirà più nuovo del master. 

Per le sincronizzazioni successive, è possibile:

- attenersi a confrontare solo in base alle dimensioni, oppure

- è possibile attivare l'opzione "Non sovrascrivere il file di destinazione se è più recente del file sorgente" per copiare solo i file modificati in seguito sul master, oppure

- è possibile attivare l'opzione task "Ottenere l'ora dell'ultima modifica dei file dalla lista personalizzata dell'applicazione SMBSync3". Tuttavia, questa opzione non è attualmente disponibile se l'obiettivo è SMB. La maggior parte degli host SMB supporta l'impostazione dell'ultimo orario di modifica. 

Vedere sotto per informazioni dettagliate su ogni opzione. 

### Utilizzare la dimensione del file per determinare se i file sono diversi

Quando viene selezionato, i file sono considerati diversi se differiscono per dimensioni. 

### Solo le dimensioni confrontare

I file sono considerati diversi solo se la dimensione della fonte è maggiore della destinazione. Questo disabiliterà il confronto in base al tempo del file. 

### Utilizzare il tempo dell'ultima modifica per determinare se i file sono diversi 

Quando si seleziona, i file sono considerati diversi in base al loro ultimo tempo di modifica 

### Differenza di tempo minima consentita (in secondi) tra i file sorgente e quelli di destinazione

I file sono considerati identici se la differenza tra i loro ultimi tempi modificati è inferiore o uguale al tempo selezionato in secondi. Sono considerati diversi se la differenza di tempo tra i file è superiore al tempo selezionato. FAT e ExFAT richiedono una tolleranza minima di 2 secondi. Se si seleziona 0 secondi, i file devono avere esattamente lo stesso tempo per essere considerati simili.

### Non sovrascrivere il file di destinazione se è più nuovo del file sorgente

Se spuntata, il file viene sovrascritto solo quando il file master è più nuovo del file di destinazione, anche se le dimensioni del file e gli ultimi tempi di aggiornamento sono diversi. Tenere presente che se si cambiano i fusi orari o se i file vengono modificati durante il periodo di intervallo della modifica dell'ora legale, l'ultimo file modificato potrebbe apparire più vecchio del file non aggiornato. Ciò è legato alle differenze del file system e solo un controllo manuale prima di sovrascrivere il file eviterà la perdita di dati. Si raccomanda generalmente di non modificare i file durante l'intervallo del cambio dell'ora legale se sono destinati ad essere auto-sincronizzati 

### Ignora la differenza di fuso orario tra i file

Permette di selezionare la differenza di fuso orario in minuti tra l'ora legale e quella invernale. I file sono considerati diversi se la differenza di orario non è esattamente uguale all'intervallo specificato (+/- la "differenza di orario minima consentita (in secondi)" specificata nell'opzione precedente)

### Salta i nomi di directory e file che contengono caratteri non validi(", :, \, \, *, <, >, |)

Se spuntata, visualizzerà un messaggio di avviso e la sincronizzazione continuerà senza elaborare le directory/file contenenti caratteri non validi. 

### Cancella la directory master quando è vuota (solo quando l'opzione Sync è Spostare)

Quando la modalità di sincronizzazione è "Spostare", dopo che i file sono stati spostati nella destinazione, viene cancellata anche la cartella Source. 

### Se la data e l'ora non possono essere determinate dai dati EXIF, viene visualizzato un messaggio di conferma

Visualizzare un messaggio di conferma quando non è possibile ottenere da Exif la data e l'ora prese.

### Ignora i file sorgente che sono più grandi di 4 GB quando la sincronizzazione con la memoria esterna.

Se l'opzione è selezionata, è possibile evitare errori I/O durante la sincronizzazione su una scheda MicroSD ignorando i file sorgente di dimensioni superiori a 4 GB da sincronizzare sulla memoria locale.

### Ignora i file il cui nome file supera i 255 byte

Se spuntata, ignorare i file con nomi di file più lunghi di 255 byte.

