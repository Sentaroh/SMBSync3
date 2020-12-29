## Funzione

SMBSync3 è un dispositivo di memorizzazione interna di un dispositivo Android, MicroSD, USB Flash e PC/NAS via LAN wireless utilizzando i protocolli SMBv1, SMBv2 o SMBv3. È uno strumento per la sincronizzazione dei file. 

<u>**La sincronizzazione è unidirezionale dalla sorgente alla destinazione**</u> e può essere riflessa, spostata, copiata o archiviata. E' possibile una combinazione di Archiviazione locale (<u>***1**</u>), SMB e ZIP).  

La sincronizzazione periodica può essere avviata dalla funzione di programmazione di SMBSync3 o da applicazioni esterne (Tasker, AutoMagic, ecc.).

- Specchio

  La directory di origine e i file vengono copiati a delta (<u>***2**</u>) fino alla destinazione e, una volta completata la copia, i file e le directory che non esistono sul lato di origine vengono cancellati.

- Spostare

  La directory di origine e i file vengono copiati a delta(<u>***2**</u>) sul lato di destinazione, e i file sul lato di origine vengono cancellati al termine della copia.(Tuttavia, il file con lo stesso nome, la dimensione del file e la data modificata sono gli stessi nel lato di origine e di destinazione, e il file non viene copiato, e la directory di origine e il file vengono cancellati al termine della copia. lato del file).

- Copia

  Delta-copia(<u>***2**</u>) i file contenuti nella directory di origine verso la destinazione.

- Archivio

  Foto e video contenuti nella directory dei sorgenti con data e ora di 7 giorni dalla data di esecuzione dell'archivio Vai a destinazione prima o prima di 30 giorni, ecc. (Ma non è possibile utilizzare lo ZIP per la destinazione).

<u>***1**</u> La memoria locale può essere interna, MicroSD o USB Flash. 

<u>***2**</u> Il file di differenza è una delle tre condizioni seguenti.  

1. Il file non esiste  
2. Diverse dimensioni dei file  
3. Diverso rispetto all'ultimo aggiornamento di 3 secondi

Se non è consentito modificare l'ultimo tempo di aggiornamento del file da parte dell'applicazione, l'ultimo tempo di aggiornamento del file viene registrato nel file di gestione e viene utilizzato per giudicare il file delle differenze. Pertanto, se si copia un file diverso da SMBSync3 o non esiste un file di gestione, il file viene copiato.

## FAQs

[Si prega di consultare il PDF](https://drive.google.com/file/d/1v4-EIWuucUErSg9uYZtycsGGn9o-T_2t/view?usp=sharing)

## Documento

[Si prega di consultare il PDF](https://drive.google.com/file/d/1gIsulxyGBY-Fl0Ki7BJ50gPFWx0iQ9Tm/view?usp=sharing)

## Biblioteca

- [jcifs-ng ClientLibrary](https://github.com/AgNO3/jcifs-ng)
- [jcifs-1.3.17](https://jcifs.samba.org/)
- [Zip4J 2.2.3](http://www.lingala.net/zip4j.html)
- [juniversalchardet-1.0.3](https://code.google.com/archive/p/juniversalchardet/)
- [Metadata-extractor](https://github.com/drewnoakes/metadata-extractor)
