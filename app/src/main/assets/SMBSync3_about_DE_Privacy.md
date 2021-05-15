## 1.Erfasste Daten<br>
### 1.1.Vom Benutzer an SMBSync3 bereitgestellte Daten<br>

Die Daten, die der Benutzer zur Verwendung von SMBSync3 bereitstellt, werden im Speicherbereich der Anwendung gespeichert.<br>
Beim Speichern der Daten werden der SMB-Kontoname, das SMB-Kontopasswort, das ZIP-Passwort und das Anwendungspasswort mit dem vom System generierten Passwort verschlüsselt.<br>
<span style="color: red;"><u>Daten werden nicht extern gesendet, es sei denn, der Vorgang "1.4.das Senden oder Schreiben von Daten außerhalb von SMBSync3" wird ausgeführt.</u></span><br>

- Dateiinformationen (Verzeichnisname, Dateiname)<br>
- SMB-Serverinformationen (Hostname/IP-Adresse, Portnummer, Kontoname, Kontopasswort)<br>
- Informationen zur ZIP-Datei (Komprimierungsmethode, Verschlüsselungsmethode, Verschlüsselungskennwort)<br>
- App-Einstellungsoptionen (Warnmeldungen, Sprache und Schriftgröße usw.)<br>
- Anwendungskennwort (Kennwort zur Authentifizierung beim Starten der Anwendung, Authentifizierung beim Ändern von Sicherheitseinstellungen usw.)<br>

### 1.2.Ausführungsergebnis von SMBSync3<br>

Speichern Sie die Daten im Speicherbereich in der Anwendung, damit der Benutzer das Ausführungsergebnis von SMBSync3 überprüfen kann.<br>
<span style="color: red;"><u>Daten werden nicht extern gesendet, es sei denn, der Vorgang "1.4.das Senden oder Schreiben von Daten außerhalb von SMBSync3" wird ausgeführt.</u></span><br>

- Verzeichnisname, Dateiname, Ausführungsstatus<br>
- Dateigröße der synchronisierten Dateien, Datum und Uhrzeit der Dateiaktualisierung<br>
- Fehlerinformation<br>

### 1.3.Aktivitätsprotokoll von SMBSync3<br>

Speichern Sie die Daten in den Speicherbereich der Anwendung, um das Ausführungsergebnis von SMBSync3 zu überprüfen und den Entwickler zu befragen.<br>
<span style="color: red;"><u>Daten werden nicht extern gesendet, es sei denn, der Vorgang "1.4.das Senden oder Schreiben von Daten außerhalb von SMBSync3" wird ausgeführt.</u></span><br>

- Geräteinformationen (Herstellername, Modellname, OS-Version, Mount-Point, App-spezifisches Verzeichnis, StorageAccessFramework, Speichermanager, IP-Adresse, WiFi enable/disable, WiFi link speed)<br>
- SMBSync3-Version, SMBSync3-Ausführungsoptionen<br>
- Verzeichnisname, Dateiname, Ausführungsstatus<br>
- Dateigröße der synchronisierten Dateien, Datum und Uhrzeit der Dateiaktualisierung<br>
- Informationen zur Fehlersuche<br>
- Fehlerinformation<br>

### 1.4.das Senden oder Schreiben von Daten außerhalb von SMBSync3<br>

SMBSync3-Daten können nur dann nach außen gesendet oder geschrieben werden, wenn der Benutzer sie bedient.<br>

- Drücken Sie die Taste [Freigeben] auf der Registerkarte Verlauf.<br>
- Klicken Sie in den Systeminformationen auf die Schaltfläche "An Entwickler senden".<br>
- Klicken Sie in der Protokollverwaltung auf die Schaltfläche "An Entwickler senden".<br>
- Klicken Sie in der Protokollverwaltung auf die Schaltfläche "Protokolldatei exportieren", um in einen externen Speicher zu exportieren.<br>
- Durch Ausführen von "Einstellungen exportieren" aus dem Menü wird "1.1.Vom Benutzer an SMBSync3 bereitgestellte Daten" exportiert werden.<br>
Durch Angabe eines Kennworts beim Exportieren werden die Informationen verschlüsselt in der Datei gespeichert.<br>

### 1.5.die in SMBSync3 gespeicherten Daten löschen<br>

Durch die Deinstallation von SMBSync3 werden die gespeicherten Daten ("1.1.Vom Benutzer an SMBSync3 bereitgestellte Daten", "1.2.Ausführungsergebnis von SMBSync3", "1.3.Aktivitätsprotokoll von SMBSync3") vom Gerät gelöscht.<br>
<span style="color: red;"><u>Allerdings werden Daten, die aufgrund von Benutzerinteraktionen im externen Speicher gespeichert wurden, nicht gelöscht.</u></span><br>

### 2.erforderliche Berechtigungen zum Ausführen der Anwendung<br>

### 2.1.Fotos, Medien, Dateien<br>
**Lesen Sie den Inhalt Ihres USB-Speichers**.<br>
**Ändern oder löschen Sie den Inhalt Ihres USB-Speichers**.<br>
Wird für die Dateisynchronisation und das Lesen/Schreiben von Verwaltungsdateien verwendet.<br>

### 2.2.Speicherung<br>

### 2.2.1.Android11 oder höher.<br>
**Alle Dateizugriffe**.<br>

Alle Dateizugriffe** Wird für die Dateisynchronisation und das Lesen/Schreiben von Verwaltungsdateien verwendet.<br>

### 2.2.2.Android 10 oder früher<br>
**Lesen Sie den Inhalt Ihres USB-Speichers**.<br>
**Ändern oder löschen Sie den Inhalt Ihres USB-Speichers**.<br>
Wird für die Dateisynchronisation und das Lesen/Schreiben von Verwaltungsdateien verwendet.<br>

### 2.3.Wi-Fi Verbindungsinformationen<br>
**Wi-Fi-Verbindungen anzeigen**.<br>
Wird verwendet, um den Wi-Fi-Status zu prüfen, wenn die Synchronisierung beginnt.<br>

### 2.4.Andere<br>
### 2.4.1.View network connections<br>
Verwenden Sie dies, um Netzwerkverbindungen zu prüfen, wenn die Synchronisierung gestartet wird.<br>
### 2.4.2.connect and disconnect from Wi-Fi<br>
Diese Funktion wird verwendet, um Wi-Fi für die geplante Synchronisierung auf Andoid 8/9 ein-/auszuschalten.<br>
### 2.4.3.Full network access<br>
Dies wird zur Synchronisierung über das SMB-Protokoll durch das Netzwerk verwendet.<br>
### 2.4.4.Run at startup<br>
Wird verwendet, um eine geplante Synchronisierung durchzuführen.<br>
### 2.4.5.Control vibration<br>
Dies wird verwendet, um den Benutzer zu benachrichtigen, wenn die Synchronisierung beendet ist.<br>
### 2.4.6.Prevent device from sleeping<br>
Dient zum Starten der Synchronisierung aus einem Zeitplan oder einer externen App.<br>
### 2.4.7.Install shortcuts<br>
Dient zum Hinzufügen einer Sync-Start-Verknüpfung auf dem Desktop.<br>
