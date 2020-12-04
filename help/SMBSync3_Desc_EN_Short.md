## Function

SMBSync3 is an Android device's internal storage, MicroSD, USB Flash and PC/NAS via wireless LAN using SMBv1, SMBv2 or SMBv3 protocols. It is a tool for synchronizing files. 

<u>**Synchronization is one-way from source to destination**</u> and can be mirrored, moved, copied or archived. A combination of Local storage(<u>***1**</u>), SMB and ZIP is possible.)  

Periodic synchronization can be initiated by SMBSync3's scheduling feature or external applications (Tasker, AutoMagic, etc.).

- Mirror

  The source directory and files are delta-copied (<u>***2**</u>) to the destination, and after the copying is completed, the files and directories that do not exist on the source side are deleted.

- Move

  The source directory and files are delta-copied(<u>***2**</u>) to the destination side, and the files on the source side are deleted when the copying is completed.(However, the file with the same name, the file size, and the modified date are the same in the source and the destination, and the file is not copied, and the source directory and the file are deleted after the copy is finished. side of the file).

- Copy

  Delta-copies(<u>***2**</u>) the files contained in the source directory to the destination.

- Archive

  Photos and videos contained in the source directory with a date and time of 7 days from the archive's execution date Go to the destination before or before 30 days, etc. (But you can't use ZIP for the destination.)

<u>***1**</u> Local storage can be either internal storage, MicroSD or USB Flash. 

<u>***2**</u> The difference file is one of the following three conditions.  

1. File does not exist  
2. Different file sizes  
3. Different over when last updated 3 seconds

If it is not permitted to change the last update time of the file by the application, the last update time of the file is recorded in the management file and it is used to judge the difference file. Therefore, if you copy a file other than SMBSync3 or there is no management file, the file will be copied.

## FAQs

Please refer to the PDF link below.

https://drive.google.com/file/d/1v4-EIWuucUErSg9uYZtycsGGn9o-T_2t/view?usp=sharing

## Library

- [jcifs-ng ClientLibrary](https://github.com/AgNO3/jcifs-ng)
- [jcifs-1.3.17](https://jcifs.samba.org/)
- [Zip4J 2.2.3](http://www.lingala.net/zip4j.html)
- [juniversalchardet-1.0.3](https://code.google.com/archive/p/juniversalchardet/)
- [Metadata-extractor](https://github.com/drewnoakes/metadata-extractor)


## Document

Please refer to the PDF link below.

https://drive.google.com/file/d/1gIsulxyGBY-Fl0Ki7BJ50gPFWx0iQ9Tm/view?usp=sharing

##  