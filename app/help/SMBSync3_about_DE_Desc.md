## Funktion

SMBSync3 ist ein Android-Gerät den internen Speicher, MicroSD, USB-Flash und PC/NAS über WLAN mit SMBv1, SMBv2 oder SMBv3-Protokolle. Es ist ein Werkzeug zum Synchronisieren von Dateien. 

<u>**Die Synchronisation erfolgt in einer Richtung von der Quelle zum Ziel**</u> und kann gespiegelt, verschoben, kopiert oder archiviert werden. Eine Kombination von Local storage(<u>***1**</u>), SMB und ZIP ist möglich).  

Die periodische Synchronisation kann durch die Zeitplanungsfunktion von SMBSync3 oder durch externe Anwendungen (Tasker, AutoMagic, etc.) initiiert werden.

- Spiegeln

  Das Quellverzeichnis und die Dateien werden differenziert (<u>***2**</u>) auf das Ziel kopiert, und nach Abschluss des Kopiervorgangs werden die Dateien und Verzeichnisse, die auf der Quellseite nicht vorhanden sind, gelöscht.

- Verschieben

  Das Quellverzeichnis und die Dateien werden differenziell (<u>***2**</u>) auf die Zielseite kopiert, und die Dateien auf der Quellseite werden gelöscht, wenn der Kopiervorgang abgeschlossen ist. (Die Datei mit dem gleichen Namen, die Dateigröße und das Änderungsdatum sind jedoch in der Quelle und im Ziel gleich, und die Datei wird nicht kopiert, und das Quellverzeichnis und die Datei werden nach Abschluss des Kopiervorgangs gelöscht. seite der Datei).

- Kopieren

  Delta-Kopiert(<u>***2**</u>) die im Quellverzeichnis enthaltenen Dateien in das Ziel.

- Archivieren

  Fotos und Videos, die im Quellverzeichnis enthalten sind, mit einem Datum und einer Uhrzeit von 7 Tagen ab dem Ausführungsdatum des Archivs gehen zum Ziel vor oder vor 30 Tagen, usw. (Sie können aber kein ZIP für das Ziel verwenden.)

<u>***1**</u> Der lokale Speicher kann entweder interner Speicher, MicroSD oder USB-Flash sein. 

<u>***2**</u> Die Differenzdatei ist eine der folgenden drei Bedingungen.  

1. Datei ist nicht vorhanden  
2. Unterschiedliche Dateigrößen  
3. Unterschiedlich über wenn letzte Aktualisierung 3 Sekunden

Wenn es nicht erlaubt ist, die letzte Aktualisierungszeit der Datei durch die Anwendung zu ändern, wird die letzte Aktualisierungszeit der Datei in der Verwaltungsdatei aufgezeichnet und zur Beurteilung der Differenzdatei verwendet. Wenn Sie also eine andere Datei als SMBSync3 kopieren oder keine Verwaltungsdatei vorhanden ist, wird die Datei kopiert.

## FAQs

[Bitte beachten Sie das PDF](https://drive.google.com/file/d/1v4-EIWuucUErSg9uYZtycsGGn9o-T_2t/view?usp=sharing)

## Dokument

[Bitte beachten Sie das PDF](https://drive.google.com/file/d/1gIsulxyGBY-Fl0Ki7BJ50gPFWx0iQ9Tm/view?usp=sharing)
