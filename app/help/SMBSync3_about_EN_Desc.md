## Function
SMBSync3 is an Android device's internal storage, MicroSD, USB Flash and PC/NAS via wireless LAN using SMBv1, SMBv2 or SMBv3 protocols. It is a tool for synchronizing files.   <u>**Synchronization is one-way from source to destination**</u> and can be mirrored, moved, copied or archived. A combination of Local storage(<u>***1**</u>), SMB and ZIP is possible.)  
Periodic synchronization can be initiated by SMBSync3's scheduling feature or external applications (Tasker, AutoMagic, etc.).

- Mirror  
  The source directory and files are differential copied (<u>***1**</u>) to the destination, and after the copying is completed, the files and directories that do not exist on the source side are deleted.
- Move  
  The source directory and files are differential copied(<u>***1**</u>) to the destination side, and the files on the source side are deleted when the copying is completed.(However, the file with the same name, the file size, and the modified date are the same in the source and the destination, and the file is not copied, and the source directory and the file are deleted after the copy is finished. side of the file).
- Copy  
  Make a differential copy (***1**) of the files contained in the directory on the source side to the destination side.
- Archive  
  Photos and videos contained in the source directory with a date and time of 7 days from the archive's execution date Go to the destination before or before 30 days, etc. (But you can't use ZIP for the destination.)

<u>***1**</u> The difference file is one of the following three conditions.  

1. File does not exist  
2. Different file sizes  
3. Different over when last updated 3 seconds

## FAQs
https://sentaroh.github.io/Documents/SMBSync3/SMBSync3_FAQ_EN.htm

## Document

https://sentaroh.github.io/Documents/SMBSync3/SMBSync3_Desc_EN.htm