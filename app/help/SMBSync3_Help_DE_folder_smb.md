### Suchen und konfigurieren Sie den SMB-Server.

Scannen Sie das Netzwerk und wählen Sie aus der SMB-Server-Liste den zu konfigurierenden aus. 

### SMB-Server-Parameter bearbeiten

Legen Sie die Parameter für den SMB-Server manuell fest. 

### Server-Hostname/IP-Adresse

So geben Sie den SMB-Servernamen oder die IP-Adresse an 

### SMB-Protokoll

Sie können das SMB-Protokoll angeben.

- SMB V1 verwenden

- SMB V2/3 verwenden

### Portn-ummer verwenden

Geben Sie an, wenn die Portnummer des Standards nicht verfügbar ist. Die Standard-Portnummer ist 139/tcp und 445/tcp. 

### Kontoname und Passwort verwenden

Legt fest, ob der Kontoname und das Passwort des SMB-Servers verwendet werden sollen

Der Kontoname ist ein lokales Konto auf dem SMB-Server. Sie können kein Microsoft-Konto verwenden. 

### Kontoname

Zur Angabe des Kontonamens für den SMB-Server. 

### Kennwort

Zur Angabe des Passworts für den SMB-Server. 

### Liste  der Aktien

Zeigt den Freigabenamen für den SMB-Server an.  

### Verzeichnisse auflisten

Zeigt die Verzeichnisliste des SMB-Servers an.  

### Parameter für Verzeichnisnamen bearbeiten

Das Datum und die Uhrzeit **<u>können in das Zielverzeichnis</u>** aufgenommen werden. Die Variablen werden beim Start der Synchronisation in das Datum umgewandelt. Bitte bestätigen Sie die Angaben der Variablen mit "Verzeichnisnamensparameter bearbeiten". 

### Verzeichnis

Zur Angabe des Verzeichnisses für den SMB-Host.Wenn das Zielverzeichnis nicht existiert, wird es zum Zeitpunkt der Synchronisation erstellt.

Hinweis: Unter den folgenden Bedingungen wird es zu einer zirkulären Referenz und bildet eine Schleife. Geben Sie einen Verzeichnisfilter an oder geben Sie ein Verzeichnis auf der Masterseite an, das sich vom Ziel unterscheidet.

- Kein Verzeichnis angegeben, wenn derselbe SMB-Server für Master und Ziel angegeben ist

- Kein Verzeichnisfilter angegeben

### <u>Das Folgende wird nur angezeigt, wenn der Synchronisationstyp Archiv ist.</u>

### Alle  Dateien im Zielverzeichnis speichern, ohne Unterverzeichnisse zu erstellen

Wenn diese Option aktiviert ist, wird im Zielverzeichnis kein Unterverzeichnis im Quellverzeichnis angelegt.

### Zum Archivieren der

Wählen Sie eine Datei mit einem Aufnahmedatum und einer Aufnahmezeit, die älter sind als das Datum und die Uhrzeit der Archivausführung. (Unabhängig von Aufnahmedatum und -zeit ist das Aufnahmedatum 7 Tage oder älter, das Aufnahmedatum 30 Tage oder älter, das Aufnahmedatum 60 Tage oder älter, das Aufnahmedatum 90 Tage oder älter, das Aufnahmedatum 180 Tage oder älter, das Aufnahmedatum ist Sie können aus mehr als ein Jahr alt wählen) 

### Laufende  Nummer

Sie können dem Dateinamen eine Sequenznummer hinzufügen. 

### Parameter Dateiname bearbeiten

Um das Datum und die Uhrzeit in den Dateinamen aufzunehmen, tippen Sie auf die Schaltfläche und bearbeiten Sie sie.