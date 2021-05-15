## Funktion<br>
SMBSync3 ist ein Tool zum Synchronisieren von Dateien zwischen dem internen Speicher des Android-Geräts, MicroSD, USB-Flash und PC/NAS über WLAN mit SMB1-, SMB2- oder SMB3-Protokoll. <span style="color: red;"><u>Die Synchronisation erfolgt unidirektional</u></span> vom Quellordner zum Zielordner und kann gespiegelt, verschoben, kopiert oder archiviert werden.<br>
Die periodische Synchronisation kann durch die Zeitplanungsfunktion von SMBSync3 oder durch externe Anwendungen (Tasker, AutoMagic, etc.) initiiert werden.<br>

- Spiegeln<br>
Das Quellverzeichnis und die Dateien werden differenziert (<u>***1**</u>) auf das Ziel kopiert, und nach Abschluss des Kopiervorgangs werden die Dateien und Verzeichnisse, die auf der Quellseite nicht vorhanden sind, gelöscht.<br>
- Verschieben<br>
Das Quellverzeichnis und die Dateien werden differenziell (<u>***1**</u>) auf die Zielseite kopiert, und die Dateien auf der Quellseite werden gelöscht, wenn der Kopiervorgang abgeschlossen ist. (Die Datei mit dem gleichen Namen, die Dateigröße und das Änderungsdatum sind jedoch in der Quelle und im Ziel gleich, und die Datei wird nicht kopiert, und das Quellverzeichnis und die Datei werden nach Abschluss des Kopiervorgangs gelöscht. seite der Datei).<br>
- Kopieren<br>
Delta-Kopiert(<u>***1**</u>) die im Quellverzeichnis enthaltenen Dateien in das Ziel.<br>
- Archivieren<br>
Fotos und Videos, die im Quellverzeichnis enthalten sind, mit einem Datum und einer Uhrzeit von 7 Tagen ab dem Ausführungsdatum des Archivs gehen zum Ziel vor oder vor 30 Tagen, usw. (Sie können aber kein ZIP für das Ziel verwenden.)<br>

<u>***1**</u> Die Differenzdatei ist eine der folgenden drei Bedingungen.<br>

1. Datei ist nicht vorhanden<br>
2. Unterschiedliche Dateigrößen<br>
3. Unterschiedlich über wenn letzte Aktualisierung 3 Sekunden<br>

### Manuals<br>
[FAQs](https://sentaroh.github.io/Documents/SMBSync3/SMBSync3_FAQ_EN.htm)<br>
[Description](https://sentaroh.github.io/Documents/SMBSync3/SMBSync3_Desc_EN.htm)<br>
