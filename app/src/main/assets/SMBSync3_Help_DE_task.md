### Testmodus  
Es wird kein Überschreiben und Löschen der Datei durchgeführt, wenn Sie dies prüfen. Testen Sie bitte, wenn Sie eine Synchronisationsaufgabe erstellen, Dateien, die kopiert oder gelöscht werden, finden Sie in der Registerkarte Nachricht.  

### Automatische Synchronisierung  
Wenn dieses Kontrollkästchen aktiviert ist, wird die Aufgabe auf die automatische. Aufgaben, die auf automatische Synchronisierung eingestellt sind, werden gestartet, wenn Sie die Synchronisierungstaste drücken.  

### Aufgabenname  
Geben Sie den Aufgabennamen an.  

### Sync-Typ  
Wählen Sie eine Methode aus Spiegeln, Kopieren, Verschieben und Archivieren. <span style="color: red;"><u>Die Synchronisierung erfolgt in eine Richtung, vom Quellordner zum Zielordner.</u></span>   

- Spiegel  
Erstellen Sie eine differenzielle Kopie (**<u>*1</u>**) von Verzeichnissen und Dateien auf der Quellseite auf die Zielseite und löschen Sie Dateien und Verzeichnisse auf der Zielseite, die auf der Quellseite nicht vorhanden sind, nachdem die Kopie abgeschlossen ist.  
- Verschieben  
Erstellen Sie eine Differenzkopie des quellseitigen Verzeichnisses und der Datei auf der Zielseite und löschen Sie die auf die Zielseite kopierte quellseitige Datei. Die quell- und zielseitige Datei mit gleichem Namen, aber gleicher Dateigröße und gleichem Änderungsdatum wird jedoch nicht kopiert und die quellseitige Datei wird gelöscht.  
- Kopie  
Erstellen Sie eine differenzielle Kopie der im Quellverzeichnis enthaltenen Dateien auf der Zielseite.  
- Archiv  
Verschieben Sie die im Quellverzeichnis enthaltenen Fotos und Videos in das Ziel unter der Bedingung, dass Aufnahmedatum und -zeit vor 7 Tagen oder 30 Tagen ab dem Datum und der Uhrzeit der Archivausführung liegen. (ZIP kann jedoch nicht als Ziel verwendet werden.)  

**<u>*1</u>** Wenn eine der folgenden drei Bedingungen erfüllt ist, wird die Datei als Differenzdatei eingestuft und kopiert oder verschoben. Die Dateigröße und die letzte Änderungszeit können jedoch in den Optionen der Synchronisationsaufgabe ignoriert werden.  

1. Datei ist nicht vorhanden  
2. Dateigröße ist unterschiedlich  
3. Datum und Uhrzeit der letzten Änderung unterscheiden sich um mehr als 3 Sekunden (die Anzahl der Sekunden kann über die Option der Synchronisationsaufgabe geändert werden).  

### Quelle und Ziel vertauschen  
Tauschen Sie den Inhalt des Quellordners und des Zielordners   

### Quellordner  

Tippen Sie auf die Schaltfläche, um den Quellordner zu bearbeiten  

### Zielordner  

Tippen Sie auf die Schaltfläche, um den Zielordner zu bearbeiten  

### Filter  
Sie können die zu synchronisierenden Dateien nach Dateiname, Dateigröße, Änderungsdatum der Datei und Verzeichnisname auswählen.  

- File name filter  
You can register the names and extensions of files to be synchronized.  
- Dateigrößenfilter  
Sie können die zu synchronisierenden Dateien nach Dateigröße auswählen.  
- Filter für das Änderungsdatum der Datei  
Sie können die zu synchronisierende Datei anhand des letzten Änderungsdatums der Datei auswählen.  
- Verzeichnisfilter  
Sie können den Namen des Verzeichnisses auswählen, das Sie synchronisieren möchten.  

### Zum Archivieren der(Wird nur angezeigt, wenn die Synchronisationsmethode Archiv ist)  

Wählen Sie die Kriterien für die zu archivierenden Fotos oder Videos.  

- Sofort alle  
- Älter als 7 Tage  
- Älter als 30 Tage  
- Älter als 60 Tage  
- Älter als 90 Tage  
- Älter als 180 Tage  
- Älter als 1 Jahr  

### Starten Sie die Synchronisation nur, wenn der Akku geladen wird.  
Wenn diese Option aktiviert ist, kann die Synchronisation nur gestartet werden, wenn der Akku geladen wird. Wenn Sie die Synchronisation starten, wenn der Akku nicht geladen ist, tritt ein Fehler auf.  

### Bestätigenvor dem Überschreiben/Löschen  

Es wird ein Bestätigungsdialog angezeigt, wenn Sie die Datei überschreiben und löschen wollen, wenn Sie das Häkchen gesetzt haben.  

### Bestätigen Sie vor dem Überschreiben Kopieren oder Löschen  

Es wird ein Bestätigungsdialog angezeigt, wenn Sie die Datei überschreiben und löschen wollen, wenn Sie das Häkchen gesetzt haben.  

### Fehler-Option  

Sie können das Verhalten beim Auftreten eines Fehlers festlegen.  

- Synchronisation stoppen  
- Alle Fehler ignorieren und nachfolgende Aufgaben starten  
Verwenden Sie diese Option, wenn Sie sicherstellen möchten, dass nachfolgende Aufgaben ausgeführt werden.   
- Folgeaufgaben starten, wenn Netzwerkoptionen zu Fehlern führen  
Verwenden Sie diese Option, wenn Sie nachfolgende Aufgaben ausführen möchten, wenn die Adresse nicht privat ist oder wenn es sich nicht um die angegebene IP-Adresse handelt.  

### Netzwerk-Option  
Sie können einstellen, ob die Synchronisierung basierend auf dem Netzwerkstatus starten kann oder nicht.  

- Auch im ausgeschalteten Zustand ausführen  
Sie können immer die Synchronisierung starten  
- Wenn mit AP verbunden  
Die Synchronisierung kann starten, wenn das drahtlose LAN mit einem beliebigen Zugangspunkt verbunden ist.  
- Nur private IP-Adresse  
Die Synchronisation kann gestartet werden, wenn die IP-Adresse eine private Adresse ist  
- Registriert in der IP-Adressliste  
Sie können die Synchronisation starten, wenn die IP-Adresse in der IP-Adressliste registriert ist.  
Sie können Wildcards für den Filter verwenden. (z. B.: 192.168.100.\*, 192.168.\*.\*.)  

### Synchronisierungmit allen IP-Adressen zulassen (einschließlich öffentlich)  

Aktiviert die Synchronisierung auf allen IP-Adressen. Es kann jedoch kein SMB-Server-Scan durchgeführt werden.  

### Erweiterte Optionen anzeigen  

**Bitte verwenden Sie es, wenn Sie detaillierte Optionen einstellen.**  

### Unterverzeichnisse synchronisieren  
Es werden rekursiv Unterverzeichnisse unter dem angegebenen Quellordner einbezogen.   

### Leere Verzeichnisse synchronisieren  
Synchronisiert die leeren Verzeichnisse (auch wenn ein Verzeichnis auf der Quelle leer ist, wird es auf dem Ziel erstellt). Wenn nicht markiert, werden leere Verzeichnisse auf der Quelle ignoriert.   

### Versteckte Verzeichnisse synchronisieren  
Wenn dieses Kontrollkästchen aktiviert ist, schließt Sync die versteckten Linux-Ordner ein (die mit einem Namen, der mit einem Punkt beginnt). Beachten Sie, dass unter Windows und Samba das Attribut "versteckt" nicht durch den Ordnernamen festgelegt wird. Der synchronisierte Ordner auf dem SMB/Windows-Ziel hat also nicht das Attribut "Versteckt" des Hosts.   

### Versteckte Dateien synchronisieren  
Wenn dieses Kontrollkästchen aktiviert ist, schließt Sync die versteckten Linux-Dateien ein (die mit einem Namen, der mit einem Punkt beginnt). Beachten Sie, dass unter Windows und Samba das Attribut "versteckt" nicht durch den Dateinamen festgelegt wird. Die synchronisierte Datei auf dem SMB/Windows-Ziel hat also nicht das Attribut "Host versteckt".  

### Zieldatei(en) überschreiben  
Wenn diese Option nicht aktiviert ist, werden die Dateien auf dem Ziel niemals überschrieben, auch wenn die Vergleichskriterien nach Größe und Zeit unterschiedlich sind.   

### Wiederholung bei Netzwerkfehler (nur für SMB-Freigaben)  
Bei serverseitigen Verbindungsfehlern wird SMBSync3 die Synchronisation maximal 3 Mal im Abstand von 30 Sekunden erneut versuchen.   

### SMB-E/A-Schreibpufferauf 16 KB begrenzen  
Bitte versuchen Sie es, wenn Sie beim Schreiben in den PC/NAS-Ordner die Fehlermeldung "Access is denied" erhalten.　Wenn diese Option aktiviert ist, wird der I/O-Puffer für Schreibvorgänge auf dem SMB-Host auf 16 KB begrenzt.   

### Dateienvor der Synchronisierung löschen (nur Spiegelmethode)  

Wenn dieses Kontrollkästchen aktiviert ist, werden zuerst die Verzeichnisse und Dateien gelöscht, die im Zielordner vorhanden sind, aber nicht in der Quelle existieren. Danach werden die Dateien und Ordner, die anders sind, in den Zielordner kopiert.  
Wenn der Quellordner SMB ist, verlängert sich die Verarbeitungszeit, da die Verzeichnisstruktur und deren Inhalt über das Netzwerk gescannt wird. Es wird dringend empfohlen, die Option " SMB2-Verhandlung verwenden" zu aktivieren, da SMB1 dann sehr langsam ist.  

### EntferntVerzeichnisse und Dateien, die vom Filter ausgeschlossen wurden  

Wenn diese Option aktiviert ist, **entfernt sie Verzeichnisse/Dateien, die von den Filtern ausgeschlossen sind**.   

### Aktualisierungszeitder Zieldatei nicht auf Übereinstimmung mit der Quelldatei einstellen  

Bitte aktivieren Sie, wenn Sie eine Fehlermeldung wie SmbFile.setLastModified()/File.setLastModified() erhalten, die fehlschlägt. Das bedeutet, dass der Remote-Host das Setzen der letzten Änderungszeit der Datei nicht zulässt. Wenn diese Option nicht aktiviert ist, wird die Zeit der letzten Änderung der kopierten Datei auf dem Zielhost auf die Zeit gesetzt, zu der sie kopiert / synchronisiert wurde. Dies bedeutet, dass die Zieldatei neuer erscheint als die Quelle.   

### Verwendungder Dateigröße zur Bestimmung von Dateiänderungen  

Wenn diese Option aktiviert ist, werden Dateien als unterschiedlich betrachtet, wenn sie sich in der Größe unterscheiden.   
### DieDateien unterscheiden sich nur dann, wenn die Quelle größer als das Ziel ist  
Dateien werden nur dann als unterschiedlich betrachtet, wenn die Größe der Quelle größer ist als die des Ziels. Damit wird der Vergleich nach Dateizeit deaktiviert.   

### Verwendungder Dateigröße zur Bestimmung von Dateiänderungen   
Wenn diese Option aktiviert ist, werden Dateien anhand des Zeitpunkts der letzten Änderung als unterschiedlich betrachtet   

### Min.erlaubter Zeitunterschied (in Sekunden) zwischen Quell- und Zieldatei für dieSynchronisierung  
Dateien werden als identisch betrachtet, wenn der Unterschied zwischen ihren letzten Änderungszeiten kleiner oder gleich der gewählten Zeit in Sekunden ist. Sie gelten als unterschiedlich, wenn die Zeitdifferenz zwischen den Dateien größer als die gewählte Zeit ist. FAT und ExFAT benötigen eine Mindesttoleranz von 2 Sekunden. Wenn 0 Sekunden gewählt wird, müssen die Dateien genau die gleiche Zeit haben, um als ähnlich zu gelten.  

### ÜberschreibenSie die Zieldatei nicht, wenn sie neuer als die Quelldatei ist  
Wenn diese Option aktiviert ist, wird die Datei nur überschrieben, wenn die Quelldatei neuer ist als die Zieldatei, auch wenn die Dateigrößen und die letzten Aktualisierungszeiten unterschiedlich sind. Beachten Sie, dass bei einem Wechsel der Zeitzonen oder wenn die Dateien im Intervall der Sommerzeitumstellung geändert werden, die zuletzt geänderte Datei älter erscheinen könnte als die nicht aktualisierte Datei. Dies hängt mit den Unterschieden im Dateisystem zusammen, und nur eine manuelle Prüfung vor dem Überschreiben der Datei kann Datenverluste vermeiden. Es wird allgemein empfohlen, Dateien während des Intervalls der Sommerzeitumstellung nicht zu ändern, wenn sie für die automatische Synchronisierung vorgesehen sind.   

### Ignorierender Sommerzeitdifferenz zwischen Dateien  
Hier können Sie den Zeitunterschied in Minuten zwischen Sommer- und Winterzeit einstellen. Dateien werden als unterschiedlich betrachtet, wenn die Zeitdifferenz nicht genau dem angegebenen Intervall entspricht (+/- der in der vorherigen Option angegebenen "Min. erlaubten Zeitdifferenz (in Sekunden)")  

### Überspringenvon Verzeichnis- und Dateinamen, die ungültige Zeichen enthalten (\", :,\\, *, &lt;, &gt;, \|)  
Wenn diese Option aktiviert ist, wird eine Warnmeldung angezeigt und die Synchronisierung wird fortgesetzt, ohne die Verzeichnisse/Dateien zu verarbeiten, die ungültige Zeichen enthalten.   

### Löschen Sie das Quellverzeichnis, wenn es leer ist (nur wenn die Sync-Option "Verschieben" ist)  
Wenn der Synchronisationsmodus "Verschieben" ist, wird nach dem Verschieben der Dateien zum Ziel auch der Quellordner gelöscht.   

### Wenn das Datum und die Uhrzeit nicht über die EXIF-Daten ermittelt werden können, wird eine Bestätigungsmeldung angezeigt  
Anzeige einer Bestätigungsmeldung, wenn das übernommene Datum und die Uhrzeit nicht aus den Exif-Daten ermittelt werden können.  

### Quelldateien, die größer als 4 GB sind, beim Synchronisieren auf SDCARD/USB-Speicher ignorieren  
Wenn dieses Kontrollkästchen aktiviert ist, können Sie E/A-Fehler bei der Synchronisierung mit einer MicroSD-Karte vermeiden, indem Quelldateien, die größer als 4 GB sind, bei der Synchronisierung mit dem lokalen Speicher ignoriert werden.  

### Dateien ignorieren, deren Dateinamen länger als die angegebene Länge sind  
Wenn die Länge des Dateinamens den auf der rechten Seite des Bildschirms eingegebenen Wert überschreitet, wird die Datei ignoriert.  

### Manuals  
[FAQs](https://sentaroh.github.io/Documents/SMBSync3/SMBSync3_FAQ_EN.htm)  
[Description](https://sentaroh.github.io/Documents/SMBSync3/SMBSync3_Desc_EN.htm)  
