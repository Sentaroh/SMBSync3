### Testmodus<br>
Es wird kein Überschreiben und Löschen der Datei durchgeführt, wenn Sie dies prüfen. Testen Sie bitte, wenn Sie eine Synchronisationsaufgabe erstellen, Dateien, die kopiert oder gelöscht werden, finden Sie in der Registerkarte Nachricht.<br>

### Automatische Synchronisierung<br>
Wenn dieses Kontrollkästchen aktiviert ist, wird die Aufgabe auf die automatische. Aufgaben, die auf automatische Synchronisierung eingestellt sind, werden gestartet, wenn Sie die Synchronisierungstaste drücken.<br>

### Aufgabenname<br>
Geben Sie den Aufgabennamen an.<br>

### Sync-Typ<br>
Wählen Sie eine Methode aus Spiegeln, Kopieren, Verschieben und Archivieren. <span style="color: red;"><u>Die Synchronisierung erfolgt in eine Richtung, vom Quellordner zum Zielordner.</u></span> <br>

- Spiegel<br>
Erstellen Sie eine differenzielle Kopie (**<u>*1</u>**) von Verzeichnissen und Dateien auf der Quellseite auf die Zielseite und löschen Sie Dateien und Verzeichnisse auf der Zielseite, die auf der Quellseite nicht vorhanden sind, nachdem die Kopie abgeschlossen ist.<br>
- Verschieben<br>
Erstellen Sie eine Differenzkopie des quellseitigen Verzeichnisses und der Datei auf der Zielseite und löschen Sie die auf die Zielseite kopierte quellseitige Datei. Die quell- und zielseitige Datei mit gleichem Namen, aber gleicher Dateigröße und gleichem Änderungsdatum wird jedoch nicht kopiert und die quellseitige Datei wird gelöscht.<br>
- Kopie<br>
Erstellen Sie eine differenzielle Kopie der im Quellverzeichnis enthaltenen Dateien auf der Zielseite.<br>
- Archiv<br>
Verschieben Sie die im Quellverzeichnis enthaltenen Fotos und Videos in das Ziel unter der Bedingung, dass Aufnahmedatum und -zeit vor 7 Tagen oder 30 Tagen ab dem Datum und der Uhrzeit der Archivausführung liegen. (ZIP kann jedoch nicht als Ziel verwendet werden.)<br>

**<u>*1</u>** Wenn eine der folgenden drei Bedingungen erfüllt ist, wird die Datei als Differenzdatei eingestuft und kopiert oder verschoben. Die Dateigröße und die letzte Änderungszeit können jedoch in den Optionen der Synchronisationsaufgabe ignoriert werden.<br>

1. Datei ist nicht vorhanden<br>
2. Dateigröße ist unterschiedlich<br>
3. Datum und Uhrzeit der letzten Änderung unterscheiden sich um mehr als 3 Sekunden (die Anzahl der Sekunden kann über die Option der Synchronisationsaufgabe geändert werden).<br>

### Quelle und Ziel vertauschen<br>
Tauschen Sie den Inhalt des Quellordners und des Zielordners <br>

### Quellordner<br>

Tippen Sie auf die Schaltfläche, um den Quellordner zu bearbeiten<br>

### Zielordner<br>

Tippen Sie auf die Schaltfläche, um den Zielordner zu bearbeiten<br>

### Filter<br>
Sie können die zu synchronisierenden Dateien nach Dateiname, Dateigröße, Änderungsdatum der Datei und Verzeichnisname auswählen.<br>

- File name filter<br>
You can register the names and extensions of files to be synchronized.<br>
- Dateigrößenfilter<br>
Sie können die zu synchronisierenden Dateien nach Dateigröße auswählen.<br>
- Filter für das Änderungsdatum der Datei<br>
Sie können die zu synchronisierende Datei anhand des letzten Änderungsdatums der Datei auswählen.<br>
- Verzeichnisfilter<br>
Sie können den Namen des Verzeichnisses auswählen, das Sie synchronisieren möchten.<br>

### Zum Archivieren der<br>

Wählen Sie die Kriterien für die zu archivierenden Fotos oder Videos.<br>

- Sofort alle<br>
- Älter als 7 Tage<br>
- Älter als 30 Tage<br>
- Älter als 60 Tage<br>
- Älter als 90 Tage<br>
- Älter als 180 Tage<br>
- Älter als 1 Jahr<br>

### Starten Sie die Synchronisation nur, wenn der Akku geladen wird.<br>
Wenn diese Option aktiviert ist, kann die Synchronisation nur gestartet werden, wenn der Akku geladen wird. Wenn Sie die Synchronisation starten, wenn der Akku nicht geladen ist, tritt ein Fehler auf.<br>

### Bestätigenvor dem Überschreiben/Löschen<br>

Es wird ein Bestätigungsdialog angezeigt, wenn Sie die Datei überschreiben und löschen wollen, wenn Sie das Häkchen gesetzt haben.<br>

### Bestätigen Sie vor dem Überschreiben Kopieren oder Löschen<br>

Es wird ein Bestätigungsdialog angezeigt, wenn Sie die Datei überschreiben und löschen wollen, wenn Sie das Häkchen gesetzt haben.<br>

### Fehler-Option<br>

Sie können das Verhalten beim Auftreten eines Fehlers festlegen.<br>

- Synchronisation stoppen<br>
- Alle Fehler ignorieren und nachfolgende Aufgaben starten<br>
Verwenden Sie diese Option, wenn Sie sicherstellen möchten, dass nachfolgende Aufgaben ausgeführt werden. <br>
- Folgeaufgaben starten, wenn Netzwerkoptionen zu Fehlern führen<br>
Verwenden Sie diese Option, wenn Sie nachfolgende Aufgaben ausführen möchten, wenn die Adresse nicht privat ist oder wenn es sich nicht um die angegebene IP-Adresse handelt.<br>

### Netzwerk-Option<br>
Sie können einstellen, ob die Synchronisierung basierend auf dem Netzwerkstatus starten kann oder nicht.<br>

- Auch im ausgeschalteten Zustand ausführen<br>
Sie können immer die Synchronisierung starten<br>
- Wenn mit AP verbunden<br>
Die Synchronisierung kann starten, wenn das drahtlose LAN mit einem beliebigen Zugangspunkt verbunden ist.<br>
- Nur private IP-Adresse<br>
Die Synchronisation kann gestartet werden, wenn die IP-Adresse eine private Adresse ist<br>
- Registriert in der IP-Adressliste<br>
Sie können die Synchronisation starten, wenn die IP-Adresse in der IP-Adressliste registriert ist.<br>
Sie können Wildcards für den Filter verwenden. (z. B.: 192.168.100.\*, 192.168.\*.\*.)<br>

### Synchronisierungmit allen IP-Adressen zulassen (einschließlich öffentlich)<br>

Aktiviert die Synchronisierung auf allen IP-Adressen. Es kann jedoch kein SMB-Server-Scan durchgeführt werden.<br>

### Erweiterte Optionen anzeigen<br>

**Bitte verwenden Sie es, wenn Sie detaillierte Optionen einstellen.**<br>

### Unterverzeichnisse synchronisieren<br>
Es werden rekursiv Unterverzeichnisse unter dem angegebenen Quellordner einbezogen. <br>

### Leere Verzeichnisse synchronisieren<br>
Synchronisiert die leeren Verzeichnisse (auch wenn ein Verzeichnis auf der Quelle leer ist, wird es auf dem Ziel erstellt). Wenn nicht markiert, werden leere Verzeichnisse auf der Quelle ignoriert. <br>

### Versteckte Verzeichnisse synchronisieren<br>
Wenn dieses Kontrollkästchen aktiviert ist, schließt Sync die versteckten Linux-Ordner ein (die mit einem Namen, der mit einem Punkt beginnt). Beachten Sie, dass unter Windows und Samba das Attribut "versteckt" nicht durch den Ordnernamen festgelegt wird. Der synchronisierte Ordner auf dem SMB/Windows-Ziel hat also nicht das Attribut "Versteckt" des Hosts. <br>

### Versteckte Dateien synchronisieren<br>
Wenn dieses Kontrollkästchen aktiviert ist, schließt Sync die versteckten Linux-Dateien ein (die mit einem Namen, der mit einem Punkt beginnt). Beachten Sie, dass unter Windows und Samba das Attribut "versteckt" nicht durch den Dateinamen festgelegt wird. Die synchronisierte Datei auf dem SMB/Windows-Ziel hat also nicht das Attribut "Host versteckt".<br>

### Zieldatei(en) überschreiben<br>
Wenn diese Option nicht aktiviert ist, werden die Dateien auf dem Ziel niemals überschrieben, auch wenn die Vergleichskriterien nach Größe und Zeit unterschiedlich sind. <br>

### Wiederholung bei Netzwerkfehler (nur für SMB-Freigaben)<br>
Bei serverseitigen Verbindungsfehlern wird SMBSync3 die Synchronisation maximal 3 Mal im Abstand von 30 Sekunden erneut versuchen. <br>

### SMB-E/A-Schreibpufferauf 16 KB begrenzen<br>
Bitte versuchen Sie es, wenn Sie beim Schreiben in den PC/NAS-Ordner die Fehlermeldung "Access is denied" erhalten.　Wenn diese Option aktiviert ist, wird der I/O-Puffer für Schreibvorgänge auf dem SMB-Host auf 16 KB begrenzt. <br>

### Dateienvor der Synchronisierung löschen (nur Spiegelmethode)<br>

Wenn dieses Kontrollkästchen aktiviert ist, werden zuerst die Verzeichnisse und Dateien gelöscht, die im Zielordner vorhanden sind, aber nicht in der Quelle existieren. Danach werden die Dateien und Ordner, die anders sind, in den Zielordner kopiert.<br>
Wenn der Quellordner SMB ist, verlängert sich die Verarbeitungszeit, da die Verzeichnisstruktur und deren Inhalt über das Netzwerk gescannt wird. Es wird dringend empfohlen, die Option " SMB2-Verhandlung verwenden" zu aktivieren, da SMB1 dann sehr langsam ist.<br>

### EntferntVerzeichnisse und Dateien, die vom Filter ausgeschlossen wurden<br>

Wenn diese Option aktiviert ist, **entfernt sie Verzeichnisse/Dateien, die von den Filtern ausgeschlossen sind**. <br>

### Aktualisierungszeitder Zieldatei nicht auf Übereinstimmung mit der Quelldatei einstellen<br>

Bitte aktivieren Sie, wenn Sie eine Fehlermeldung wie SmbFile.setLastModified()/File.setLastModified() erhalten, die fehlschlägt. Das bedeutet, dass der Remote-Host das Setzen der letzten Änderungszeit der Datei nicht zulässt. Wenn diese Option nicht aktiviert ist, wird die Zeit der letzten Änderung der kopierten Datei auf dem Zielhost auf die Zeit gesetzt, zu der sie kopiert / synchronisiert wurde. Dies bedeutet, dass die Zieldatei neuer erscheint als die Quelle. <br>

### Verwendungder Dateigröße zur Bestimmung von Dateiänderungen<br>

Wenn diese Option aktiviert ist, werden Dateien als unterschiedlich betrachtet, wenn sie sich in der Größe unterscheiden. <br>
### DieDateien unterscheiden sich nur dann, wenn die Quelle größer als das Ziel ist<br>
Dateien werden nur dann als unterschiedlich betrachtet, wenn die Größe der Quelle größer ist als die des Ziels. Damit wird der Vergleich nach Dateizeit deaktiviert. <br>

### Verwendungder Dateigröße zur Bestimmung von Dateiänderungen <br>
Wenn diese Option aktiviert ist, werden Dateien anhand des Zeitpunkts der letzten Änderung als unterschiedlich betrachtet <br>

### Min.erlaubter Zeitunterschied (in Sekunden) zwischen Quell- und Zieldatei für dieSynchronisierung<br>
Dateien werden als identisch betrachtet, wenn der Unterschied zwischen ihren letzten Änderungszeiten kleiner oder gleich der gewählten Zeit in Sekunden ist. Sie gelten als unterschiedlich, wenn die Zeitdifferenz zwischen den Dateien größer als die gewählte Zeit ist. FAT und ExFAT benötigen eine Mindesttoleranz von 2 Sekunden. Wenn 0 Sekunden gewählt wird, müssen die Dateien genau die gleiche Zeit haben, um als ähnlich zu gelten.<br>

### ÜberschreibenSie die Zieldatei nicht, wenn sie neuer als die Quelldatei ist<br>
Wenn diese Option aktiviert ist, wird die Datei nur überschrieben, wenn die Quelldatei neuer ist als die Zieldatei, auch wenn die Dateigrößen und die letzten Aktualisierungszeiten unterschiedlich sind. Beachten Sie, dass bei einem Wechsel der Zeitzonen oder wenn die Dateien im Intervall der Sommerzeitumstellung geändert werden, die zuletzt geänderte Datei älter erscheinen könnte als die nicht aktualisierte Datei. Dies hängt mit den Unterschieden im Dateisystem zusammen, und nur eine manuelle Prüfung vor dem Überschreiben der Datei kann Datenverluste vermeiden. Es wird allgemein empfohlen, Dateien während des Intervalls der Sommerzeitumstellung nicht zu ändern, wenn sie für die automatische Synchronisierung vorgesehen sind. <br>

### Ignorierender Sommerzeitdifferenz zwischen Dateien<br>
Hier können Sie den Zeitunterschied in Minuten zwischen Sommer- und Winterzeit einstellen. Dateien werden als unterschiedlich betrachtet, wenn die Zeitdifferenz nicht genau dem angegebenen Intervall entspricht (+/- der in der vorherigen Option angegebenen "Min. erlaubten Zeitdifferenz (in Sekunden)")<br>

### Überspringenvon Verzeichnis- und Dateinamen, die ungültige Zeichen enthalten (\", :,\\, *, &lt;, &gt;, \|)<br>
Wenn diese Option aktiviert ist, wird eine Warnmeldung angezeigt und die Synchronisierung wird fortgesetzt, ohne die Verzeichnisse/Dateien zu verarbeiten, die ungültige Zeichen enthalten. <br>

### Löschen Sie das Quellverzeichnis, wenn es leer ist (nur wenn die Sync-Option "Verschieben" ist)<br>
Wenn der Synchronisationsmodus "Verschieben" ist, wird nach dem Verschieben der Dateien zum Ziel auch der Quellordner gelöscht. <br>

### Wenn das Datum und die Uhrzeit nicht über die EXIF-Daten ermittelt werden können, wird eine Bestätigungsmeldung angezeigt<br>
Anzeige einer Bestätigungsmeldung, wenn das übernommene Datum und die Uhrzeit nicht aus den Exif-Daten ermittelt werden können.<br>

### IgnorierenSie Quelldateien, die größer als 4 GB sind, wenn sie mit einem externenSpeicher synchronisiert werden<br>
Wenn dieses Kontrollkästchen aktiviert ist, können Sie E/A-Fehler bei der Synchronisierung mit einer MicroSD-Karte vermeiden, indem Quelldateien, die größer als 4 GB sind, bei der Synchronisierung mit dem lokalen Speicher ignoriert werden.<br>

### Ignoriere Dateien, deren Dateiname 255 Bytes überschreitet<br>
Wenn diese Option aktiviert ist, werden Dateien mit Dateinamen, die länger als 255 Byte sind, ignoriert.<br>

### Manuals<br>
[FAQs](https://sentaroh.github.io/Documents/SMBSync3/SMBSync3_FAQ_EN.htm)<br>
[Description](https://sentaroh.github.io/Documents/SMBSync3/SMBSync3_Desc_EN.htm)<br>
