## 1.Aufgezeichnete Daten von der App senden

Die von der App aufgezeichneten Daten können mit den folgenden App-Vorgängen über E-Mail und Sharing-Tools extern versendet werden. <span style="color: red;"><u>Die App sendet die aufgezeichneten Daten nur dann extern, wenn der Benutzer dies tut.</u></span>

- Drücken Sie die Schaltfläche "Teilen" auf der Registerkarte "Verlauf".

- Drücken Sie die Schaltfläche "An Entwickler senden" aus den Systeminformationen

- Drücken Sie die "Teilen-Schaltfläche" oder die "An Entwickler senden"-Schaltfläche aus der Protokollverwaltung

## 2.Von der App aufgezeichnete Daten

### 2.1.Aufgabenliste der Synchronisation

Die App zeichnet die notwendigen Daten auf, um die Synchronisation durchzuführen.

- Verzeichnisname, Dateiname, SMB-Server-Hostname, IP-Adresse, Portnummer, Kontoname (***1**), Passwort (***1**)

- App-Passwort (***1**) zum Schutz des App-Starts und der Einstellungsänderung

- App-Einstellungswert

***1** Verschlüsselt mit systemgeneriertem Schlüssel, der im AndroidKeystore gespeichert ist.

 

### 2.2.App-Aktivitätsaufzeichnung

Die App zeichnet die folgenden Daten auf, wenn Sie die Protokollierung aktivieren, um die Synchronisierungsergebnisse zu überprüfen und Fehler zu beheben.

- Android-Version, Terminalhersteller, Terminalname, Terminalmodell, Anwendungsversion

- Verzeichnisname, Dateiname, Dateigröße, Zeit der letzten Änderung der Datei

- SMB-Server-Hostname, IP-Adresse, Port-Nummer, Kontoname

- Name der Netzwerkschnittstelle, IP-Adresse

- Systemeinstellungswert (WiFi Sleep policy, Storage info)

- App-Einstellungswert

### 2.3.Sync-Aufgabenliste exportieren

Die App kann die "2.1 Synchronisationsaufgabenliste" in eine Datei exportieren. Sie können den Export mit einem Passwort schützen.

- Verzeichnisname, Dateiname
- SMB-Server Hostname, IP-Adresse, Portnummer, Kontoname, Passwort
- App-Einstellungswert 

## 3.Verwendungszweck der an den App-Entwickler gesendeten Daten

Die von den Benutzern der App an den Entwickler gesendeten Daten werden nur zur Lösung von Problemen mit der App verwendet und werden nicht an andere Personen als den Entwickler weitergegeben.

## 4.Berechtigungen

Die App benötigt die folgenden Berechtigungen.

### 4.1.Photos/Media/Files

**read the contents of your USB storage  
modify or delete the contents of your USB storage** 
Wird für die Dateisynchronisierung mit dem internen/externen Speicher und das Lesen/Schreiben der Verwaltungsdatei verwendet.


### 4.2.Storage

**read the contents of your USB storage  
modify or delete the contents of your USB storage**   
Dient zur Dateisynchronisation mit dem USB-Speicher und zum Lesen/Schreiben der Verwaltungsdatei.

### 4.3.Wi-Fi Verbindungsinformationen

**view Wi-Fi connections**  
Dient zur Überprüfung des Wi-Fi-Status beim Start der Synchronisation.

### 4.4.Sonstiges

### 4.4.1.view network connections

Wird verwendet, um zu bestätigen, dass beim Start der Synchronisierung eine Verbindung zum Netzwerk besteht.

### 4.4.2.connect and disconnect from Wi-Fi

Dient zum Ein- und Ausschalten von Wi-Fi bei der Zeitplansynchronisation in Andoid 8/9.

### 4.4.3.full network access

Wird verwendet, um die Synchronisierung mit dem SMB-Protokoll über das Netzwerk durchzuführen.

### 4.4.4.run at startup

Wird verwendet, um eine Zeitplansynchronisation durchzuführen.

### 4.4.5.control vibration

Wird verwendet, um den Benutzer am Ende der Synchronisation zu benachrichtigen.

### 4.4.6.prevent device from sleeping

Wird verwendet, um die Synchronisation von einem Zeitplan oder einer externen Anwendung aus zu starten.

### 4.4.7.install shortcuts

Wird verwendet, um eine Verknüpfung zum Starten der Synchronisierung auf dem Desktop hinzuzufügen.

 

 