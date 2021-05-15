## Function<br>
SMBSync3 is a tool to synchronize files between Android device's internal storage, MicroSD, USB Flash and PC/NAS via wireless LAN using SMB1, SMB2 or SMB3 protocol. <span style="color: red;"><u>Synchronization is unidirectional</u></span> from the source folder to the destination folder and can be mirrored, moved, copied or archived.<br>
Periodic synchronization can be initiated by SMBSync3's scheduling feature or external applications (Tasker, AutoMagic, etc.).<br>

- Mirror<br>
The source directory and files are differential copied (<u>***1**</u>) to the destination, and after the copying is completed, the files and directories that do not exist on the source side are deleted.<br>
- Move<br>
The source directory and files are differential copied(<u>***1**</u>) to the destination side, and the files on the source side are deleted when the copying is completed.(However, the file with the same name, the file size, and the modified date are the same in the source and the destination, and the file is not copied, and the source directory and the file are deleted after the copy is finished. side of the file).<br>
- Copy<br>
Make a differential copy (***1**) of the files contained in the directory on the source side to the destination side.<br>
- Archive<br>
Photos and videos contained in the source directory with a date and time of 7 days from the archive's execution date Go to the destination before or before 30 days, etc. (But you can't use ZIP for the destination.)<br>

<u>***1**</u> The difference file is one of the following three conditions.<br>

1. File does not exist<br>
2. Different file sizes<br>
3. Different over when last updated 3 seconds<br>

### Manuals<br>
[FAQs](https://sentaroh.github.io/Documents/SMBSync3/SMBSync3_FAQ_EN.htm)<br>
[Description](https://sentaroh.github.io/Documents/SMBSync3/SMBSync3_Desc_EN.htm)<br>
