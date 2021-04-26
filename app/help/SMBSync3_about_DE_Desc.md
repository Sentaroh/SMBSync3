## Funktion
SMBSync3 ist ein Android-Gerät den internen Speicher, MicroSD, USB-Flash und PC/NAS über WLAN mit SMBv1, SMBv2 oder SMBv3-Protokolle. Es ist ein Werkzeug zum Synchronisieren von Dateien. <u>**Die Synchronisation erfolgt in einer Richtung von der Quelle zum Ziel**</u> und kann gespiegelt, verschoben, kopiert oder archiviert werden. Eine Kombination von Local storage(<u>***1**</u>), SMB und ZIP ist möglich).  
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

## FAQs
https://sentaroh.github.io/Documents/SMBSync3/SMBSync3_FAQ_EN.htm

## Dokument

https://sentaroh.github.io/Documents/SMBSync3/SMBSync3_Desc_EN.htm