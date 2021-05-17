## Funktion  
SMBSync3 ist ein Tool zum Synchronisieren von Dateien zwischen dem internen Speicher des Android-Geräts, MicroSD, USB-Flash und PC/NAS über WLAN mit SMB1-, SMB2- oder SMB3-Protokoll. <span style="color: red;"><u>Die Synchronisation erfolgt unidirektional</u></span> vom Quellordner zum Zielordner und kann gespiegelt, verschoben, kopiert oder archiviert werden.  
Die periodische Synchronisation kann durch die Zeitplanungsfunktion von SMBSync3 oder durch externe Anwendungen (Tasker, AutoMagic, etc.) initiiert werden.  
- Spiegeln  
Das Quellverzeichnis und die Dateien werden differenziert (<u>***1**</u>) auf das Ziel kopiert, und nach Abschluss des Kopiervorgangs werden die Dateien und Verzeichnisse, die auf der Quellseite nicht vorhanden sind, gelöscht.  
- Verschieben  
Das Quellverzeichnis und die Dateien werden differenziell (<u>***1**</u>) auf die Zielseite kopiert, und die Dateien auf der Quellseite werden gelöscht, wenn der Kopiervorgang abgeschlossen ist. (Die Datei mit dem gleichen Namen, die Dateigröße und das Änderungsdatum sind jedoch in der Quelle und im Ziel gleich, und die Datei wird nicht kopiert, und das Quellverzeichnis und die Datei werden nach Abschluss des Kopiervorgangs gelöscht. seite der Datei).  
- Kopieren  
Delta-Kopiert(<u>***1**</u>) die im Quellverzeichnis enthaltenen Dateien in das Ziel.  
- Archivieren  
Fotos und Videos, die im Quellverzeichnis enthalten sind, mit einem Datum und einer Uhrzeit von 7 Tagen ab dem Ausführungsdatum des Archivs gehen zum Ziel vor oder vor 30 Tagen, usw. (Sie können aber kein ZIP für das Ziel verwenden.)  

<u>***1**</u> Die Differenzdatei ist eine der folgenden drei Bedingungen.  

1. Datei ist nicht vorhanden  
2. Unterschiedliche Dateigrößen  
3. Unterschiedlich über wenn letzte Aktualisierung 3 Sekunden  

### Manuals  
[FAQs](https://sentaroh.github.io/Documents/SMBSync3/SMBSync3_FAQ_EN.htm)  
[Description](https://sentaroh.github.io/Documents/SMBSync3/SMBSync3_Desc_EN.htm)  
