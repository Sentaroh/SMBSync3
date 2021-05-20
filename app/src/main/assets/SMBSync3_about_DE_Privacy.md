## 1.Erfasste Daten  
### 1.1.Vom Benutzer an SMBSync3 bereitgestellte Daten  

Die folgenden Daten, die der Benutzer zur Verwendung von SMBSync3 bereitstellt, werden im Speicherbereich der Anwendung gespeichert.  

- Dateiinformationen (Verzeichnisname, Dateiname)  
- SMB-Server-Informationen, wenn Sie einen SMB-Server verwenden (Hostname/IP-Adresse, Portnummer, Kontoname(**<u>\*1</u>**), Kontopasswort(**<u>\*1</u>**))  
- ZIP-Datei-Informationen, wenn eine ZIP-Datei verwendet wird (Komprimierungsmethode, Verschlüsselungsmethode, Verschlüsselungspasswort)  
- App-Einstellungsoptionen (Warnmeldungen, Sprache und Schriftgröße usw.)  
- Anwendungskennwort(**<u>\*1</u>**)  

**<u>\*1</u>**Die Daten werden verschlüsselt gespeichert.  

### 1.2.Ausführungsergebnis von SMBSync3  

Speichern Sie die Daten im Speicherbereich in der Anwendung, damit der Benutzer das Ausführungsergebnis von SMBSync3 überprüfen kann.  

- Verzeichnisname, Dateiname, Ausführungsstatus  
- Dateigröße der synchronisierten Dateien, Datum und Uhrzeit der Dateiaktualisierung  
- Fehlerinformation  

### 1.3.Aktivitätsprotokoll von SMBSync3  

Wenn Sie die Protokollierung aktivieren, werden die Daten der Aktivitätsaufzeichnung im Speicherbereich der App zur Überprüfung der Ausführungsergebnisse der App und für den technischen Support gespeichert. Wenn die Protokollierung deaktiviert ist, wird die Datenaufzeichnung gestoppt. Die bereits aufgezeichneten Daten werden jedoch nicht gelöscht, so dass Sie sie bei Bedarf löschen können.  

- Geräteinformationen (Herstellername, Modellname, OS-Version, Mount-Point, App-spezifisches Verzeichnis, StorageAccessFramework, Speichermanager, IP-Adresse, WiFi enable/disable, WiFi link speed)  
- SMBSync3-Version, SMBSync3-Ausführungsoptionen  
- Verzeichnisname, Dateiname, Ausführungsstatus  
- Dateigröße der synchronisierten Dateien, Datum und Uhrzeit der Dateiaktualisierung  
- Informationen zur Fehlersuche  
- Fehlerinformation  

### 1.4.das Senden oder Schreiben von Daten außerhalb von SMBSync3  

<span style="color: red;"><u>SMBSync3-Daten können nur dann nach außen gesendet oder geschrieben werden, wenn der Benutzer sie bedient.</u></span>  

- Drücken Sie die Taste [Freigeben] auf der Registerkarte Verlauf.  
- Klicken Sie in den Systeminformationen auf die Schaltfläche "An Entwickler senden".  
- Klicken Sie in der Protokollverwaltung auf die Schaltfläche "Freigeben".  
- Klicken Sie in der Protokollverwaltung auf die Schaltfläche "An Entwickler senden".  
- Klicken Sie in der Protokollverwaltung auf die Schaltfläche "Protokolldatei exportieren", um in einen externen Speicher zu exportieren.  
- Durch Ausführen von "Einstellungen exportieren" aus dem Menü wird "1.1.Vom Benutzer an SMBSync3 bereitgestellte Daten" exportiert werden.  
Die Informationen werden verschlüsselt, indem Sie beim Exportieren ein Passwort angeben.  

### 1.5.die in SMBSync3 gespeicherten Daten löschen  

Durch die Deinstallation von SMBSync3 werden die gespeicherten Daten ("1.1.Vom Benutzer an SMBSync3 bereitgestellte Daten", "1.2.Ausführungsergebnis von SMBSync3", "1.3.Aktivitätsprotokoll von SMBSync3") vom Gerät gelöscht.  
<span style="color: red;"><u>Allerdings werden Daten, die aufgrund von Benutzerinteraktionen im externen Speicher gespeichert wurden, nicht gelöscht.</u></span>  

### 2.erforderliche Berechtigungen zum Ausführen der Anwendung  

### 2.1.Speicherung  

### 2.1.1.Android11 oder höher.  
**<u>All file access</u>**  
Alle Dateizugriffe** Wird für die Dateisynchronisation und das Lesen/Schreiben von Verwaltungsdateien verwendet.  

### 2.1.2.Android 10 oder früher  

#### 2.1.2.1.Fotos, Medien, Dateien  
**<u>read the contents of your USB storage</u>**  
**<u>modify or delete the contents of your USB storage</u>**  
Wird für die Dateisynchronisation und das Lesen/Schreiben von Verwaltungsdateien verwendet.  

### 2.2.Wi-Fi Verbindungsinformationen  
**Wi-Fi-Verbindungen anzeigen**.  
Verwenden Sie dies, um Netzwerkverbindungen zu prüfen, wenn die Synchronisierung gestartet wird.  

### 2.3.Andere  
### 2.3.1.View network connections  
Verwenden Sie dies, um Netzwerkverbindungen zu prüfen, wenn die Synchronisierung gestartet wird.  
### 2.3.2.connect and disconnect from Wi-Fi  
Diese Funktion wird verwendet, um Wi-Fi für die geplante Synchronisierung auf Andoid 8/9 ein-/auszuschalten.  
### 2.3.3.Full network access  
Dies wird zur Synchronisierung über das SMB-Protokoll durch das Netzwerk verwendet.  
### 2.3.4.Run at startup  
Wird verwendet, um die geplante Synchronisierung beim Neustart des Geräts zu initialisieren.  
### 2.3.5.Control vibration  
Dies wird verwendet, um den Benutzer zu benachrichtigen, wenn die Synchronisierung beendet ist.  
### 2.3.6.Prevent device from sleeping  
Wird verwendet, um zu verhindern, dass das Gerät während der Synchronisierung in den Ruhezustand geht.  
### 2.3.7.Install shortcuts  
Dient zum Hinzufügen einer Sync-Start-Verknüpfung auf dem Desktop.  
