## 1.Erfasste Daten
### 1.1.Vom Benutzer an SMBSync3 bereitgestellte Daten

Die vom Benutzer für die Verwendung von SMBSync3 bereitgestellten Daten werden im Speicherbereich in der Anwendung gespeichert.
Der SMB-Kontoname, das SMB-Kontopasswort, das ZIP-Passwort und das App-Passwort werden jedoch verschlüsselt gespeichert.
<span style="color: red;"><u>Daten werden nicht extern gesendet, es sei denn, der Vorgang "1.4.das Senden oder Schreiben von Daten außerhalb von SMBSync3" wird ausgeführt.</u></span>

- Dateiinformationen (Verzeichnisname, Dateiname)
- SMB-Serverinformationen (Hostname/IP-Adresse, Portnummer, Kontoname, Kontopasswort)
- Informationen zur ZIP-Datei (Komprimierungsmethode, Verschlüsselungsmethode, Verschlüsselungskennwort)
- App-Einstellungsoptionen (Warnmeldungen, Sprache und Schriftgröße usw.)
- Anwendungskennwort (Kennwort zur Authentifizierung beim Starten der Anwendung, Authentifizierung beim Ändern von Sicherheitseinstellungen usw.)

### 1.2.Ausführungsergebnis von SMBSync3

Speichern Sie die Daten im Speicherbereich in der Anwendung, damit der Benutzer das Ausführungsergebnis von SMBSync3 überprüfen kann.
<span style="color: red;"><u>Daten werden nicht extern gesendet, es sei denn, der Vorgang "1.4.das Senden oder Schreiben von Daten außerhalb von SMBSync3" wird ausgeführt.</u></span>

- Verzeichnisname, Dateiname, Ausführungsstatus
- Dateigröße der synchronisierten Dateien, Datum und Uhrzeit der Dateiaktualisierung
- Fehlerinformation

### 1.3.Aktivitätsprotokoll von SMBSync3

Speichern Sie die Daten in den Speicherbereich der Anwendung, um das Ausführungsergebnis von SMBSync3 zu überprüfen und den Entwickler zu befragen.
<span style="color: red;"><u>Daten werden nicht extern gesendet, es sei denn, der Vorgang "1.4.das Senden oder Schreiben von Daten außerhalb von SMBSync3" wird ausgeführt.</u></span>

- Geräteinformationen (Herstellername, Modellname, OS-Version, Mount-Point, App-spezifisches Verzeichnis, StorageAccessFramework, Speichermanager, IP-Adresse, WiFi enable/disable, WiFi link speed)
- SMBSync3-Version, SMBSync3-Ausführungsoptionen
- Verzeichnisname, Dateiname, Ausführungsstatus
- Dateigröße der synchronisierten Dateien, Datum und Uhrzeit der Dateiaktualisierung
- Informationen zur Fehlersuche
- Fehlerinformation

### 1.4.das Senden oder Schreiben von Daten außerhalb von SMBSync3

SMBSync3-Daten können nur dann nach außen gesendet oder geschrieben werden, wenn der Benutzer sie bedient.

- Drücken Sie die Taste [Freigeben] auf der Registerkarte Verlauf.
- Klicken Sie in den Systeminformationen auf die Schaltfläche "An Entwickler senden".
- Klicken Sie in der Protokollverwaltung auf die Schaltfläche "An Entwickler senden".
- Klicken Sie in der Protokollverwaltung auf die Schaltfläche "Protokolldatei exportieren", um in einen externen Speicher zu exportieren.
- Durch Ausführen von "Einstellungen exportieren" aus dem Menü wird "1.1.Vom Benutzer an SMBSync3 bereitgestellte Daten" exportiert werden.
Durch Angabe eines Kennworts beim Exportieren werden die Informationen verschlüsselt in der Datei gespeichert.

### 1.5.die in SMBSync3 gespeicherten Daten löschen

Durch die Deinstallation von SMBSync3 werden die gespeicherten Daten ("1.1.Vom Benutzer an SMBSync3 bereitgestellte Daten", "1.2.Ausführungsergebnis von SMBSync3", "1.3.Aktivitätsprotokoll von SMBSync3") vom Gerät gelöscht.
<span style="color: red;"><u>Allerdings werden Daten, die aufgrund von Benutzerinteraktionen im externen Speicher gespeichert wurden, nicht gelöscht.</u></span>

### 2.erforderliche Berechtigungen zum Ausführen der Anwendung

### 2.1.Fotos, Medien, Dateien
**Lesen Sie den Inhalt Ihres USB-Speichers**.
**Ändern oder löschen Sie den Inhalt Ihres USB-Speichers**.
Wird für die Dateisynchronisation und das Lesen/Schreiben von Verwaltungsdateien verwendet.

### 2.2.Speicherung

### 2.2.1.Android11 oder höher.
**Alle Dateizugriffe**.

Alle Dateizugriffe** Wird für die Dateisynchronisation und das Lesen/Schreiben von Verwaltungsdateien verwendet.

### 2.2.2.Android 10 oder früher
**Lesen Sie den Inhalt Ihres USB-Speichers**.
**Ändern oder löschen Sie den Inhalt Ihres USB-Speichers**.
Wird für die Dateisynchronisation und das Lesen/Schreiben von Verwaltungsdateien verwendet.

### 2.3.Wi-Fi Verbindungsinformationen
**Wi-Fi-Verbindungen anzeigen**.
Wird verwendet, um den Wi-Fi-Status zu prüfen, wenn die Synchronisierung beginnt.

### 2.4.Andere
### 2.4.1.View network connections
Verwenden Sie dies, um Netzwerkverbindungen zu prüfen, wenn die Synchronisierung gestartet wird.
### 2.4.2.connect and disconnect from Wi-Fi
Diese Funktion wird verwendet, um Wi-Fi für die geplante Synchronisierung auf Andoid 8/9 ein-/auszuschalten.
### 2.4.3.Full network access
Dies wird zur Synchronisierung über das SMB-Protokoll durch das Netzwerk verwendet.
### 2.4.4.Run at startup
Wird verwendet, um eine geplante Synchronisierung durchzuführen.
### 2.4.5.Control vibration
Dies wird verwendet, um den Benutzer zu benachrichtigen, wenn die Synchronisierung beendet ist.
### 2.4.6.Prevent device from sleeping
Dient zum Starten der Synchronisierung aus einem Zeitplan oder einer externen App.
### 2.4.7.Install shortcuts
Dient zum Hinzufügen einer Sync-Start-Verknüpfung auf dem Desktop.
