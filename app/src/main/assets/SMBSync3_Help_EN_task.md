### Test mode<br>
It does not perform overwrite and delete the file if you check. Please be tested when you create a synchronization task, files that are copied or deleted can be found in the message tab.<br>

### Auto sync task<br>
If checked the task to the automatic. Tasks that are set to automatic synchronization will start when you press the sync button.<br>

### Task name<br>
Specify task name.<br>

### Sync type<br>
Select a method from Mirror, Copy, Move and Archive. <span style="color: red;"><u>Synchronization is done in one direction, from the source folder to the destination folder.</u></span> <br>

- Mirror<br>
Make a differential copy (**<u>*1</u>**) of directories and files on the source side to the destination side, and delete files and directories on the destination side that do not exist on the source side after the copy is completed.<br>
- Move<br>
Make a differential copy of the source side directory and file to the destination side, and delete the source side file copied to the destination side. However, the file with the same name as the source and the destination but with the same file size and modification date is not copied and the file on the source side is deleted.<br>
- Copy<br>
Make a differential copy of the files contained in the source directory to the destination side.<br>
- Archive<br>
Move the photos and videos contained in the source directory to the destination with the condition that the shooting date and time is earlier than 7 days or 30 days from the archive execution date and time. (However, ZIP cannot be used as the destination.)<br>

**<u>*1</u>** If any of the following three conditions are met, the file is judged to be a difference file and is copied or moved. However, the file size and last modified time can be ignored in the options of the synchronization task.<br>

1. file does not exist<br>
2. file size is different<br>
3. last modified date and time is different by more than 3 seconds (the number of seconds can be changed by the option of the synchronization task)<br>

### Swap the source and destination<br>
Swap the contents of the source folder and the destination folder <br>

### Source folder<br>

Tap the button to edit source folder<br>

### Destination folder<br>

Tap the button to edit destination folder<br>

### Filters<br>
You can select files to synchronize by file name, file size, file modification date, and directory name.<br>

- File name filter<br>
You can register the names and extensions of files to be synchronized.<br>
- File size filter<br>
You can select the files to synchronize by file size.<br>
- File modification date filter<br>
You can select the files to synchronize by their last modified date.<br>
- Directory filter<br>
You can select the name of the directory you want to synchronize.<br>

### To archive the<br>

Select the criteria for the photos or videos to be archived.<br>

- Any date (All)<br>
- Older than 7 days<br>
- Older than 30 days<br>
- Older than 60 days<br>
- Older than 90 days<br>
- Older than 180 days<br>
- Older than 1 year<br>

### Execute sync task only when charging<br>
If checked, you can start sync only while charging. If sync is started when not charging it will result in an error.<br>

### Syncincludes the files located directly in root of the source directory <br>

Ifunchecked, only folders and their files/subfolders are synchronized<br>

### Confirm before override copy or delete<br>

It will display a confirmation dialog when you want to overwrite and delete the file if you have checked.<br>

### Error Options<br>

You can specify the behavior when an error occurs.<br>

- Stop synchronization<br>
- Ignore all errors and start subsequent tasks<br>
Use this option if you want to make sure that subsequent tasks are executed. <br>
- Start subsequent tasks if network options result in errors<br>
Use this if you want to run subsequent tasks when the address is not private or when it is not the specified IP address.<br>

### Network Option<br>
You can set whether synchronization can start or not based on the network status.<br>

- Run even when off<br>
You can always start syncing<br>
- When connected to AP<br>
Sync can start if the wireless LAN is connected to the any access point.<br>
- Private IP address only<br>
Synchronization can be started when the IP address is a private address<br>
- Registered in the IP address list<br>
You can start synchronization when the IP address is registered in the IP address list.<br>
You can use wildcards for the filter. (e.g: 192.168.100.\*, 192.168.\*.\*.)<br>

### Allowsync with all IP addresses (include public)<br>

Enables synchronization on all IP addresses. However, SMB server scan cannot be performed. <br>

### Shows advanced options<br>

**Please use it when setting detailed options.**<br>

### Include subdirectories<br>
It will recursively include subdirectories under the specified source folder. <br>

### Include empty directories<br>
Synchronizes the empty directories (even if a directory is empty on the source, it will be created on the destination). If unchecked, empty directories on the source are ignored. <br>

### Include hidden directories<br>
When checked, Sync will include the hidden linux folders (those with a name starting with a dot). Note that in Windows and Samba, the hidden attribute is not set by the folder name. Thus, the synchronized folder on the SMB/Windows destination won’t have the host hidden attribute. <br>

### Include hidden files<br>
When checked, Sync will include the hidden linux files (those with a name starting with a dot). Note that in Windows and Samba, the hidden attribute is not set by the file name. Thus, the synchronized file on the SMB/Windows destination won’t have the host hidden attribute.<br>

### Overwrite destination files<br>
If unchecked, files on the destination will never be overwritten even if the compare criteria by size and time are different. <br>

### Retry on network error (only for SMB shares)<br>
On server-side connection errors, SMBSync3 will try again the synchronization for a maximum of 3 times at a 30 seconds interval. <br>

### Limit SMB I/O write buffer to 16KB (only for SMB shares)<br>
Please try if you get an "Access is denied" error when writing to the PC/NAS folder. When checked, it will limit I/O buffer to 16KB for writing operations to the SMB host. <br>

### Delete files prior to sync (Mirror mode only)<br>

When checked, the directories and files that are present on the destination folder but that do not exist on the source, will be first deleted. After that, files and folders that are different will be copied to the destination.<br>
If the source folder is SMB, the processing time will be longer because the directory structure and their contents is scanned through the network. It is strongly recommended to enable the option " Use SMB2 negotiation" because SMB1 will be very slow.<br>

### Remove directories and files excluded by the filters<br>

If enabled, **it removes directories/files that are excluded from the filter.** <br>

### Do not set last modified time of destination file to match source file<br>

Please enable if you get an error like SmbFile.setLastModified()/File.setLastModified() fails. It means that the remote host doesn’t allow setting file last modified time. If unchecked, the last modified time of the copied file on the destination will be set to the time it was copied / synchronized. This means that the destination file will appear newer than the source. <br>

### Use file size to determine if files are different<br>

When checked, files are considered different if they differ by size. <br>
### Size only compare<br>
Files are considered different only if size of the source is larger than the destination. This will disable compare by file time. <br>

### Use time of last modification to determine if files are different <br>
When checked, files are considered different based on their last modification time <br>

### Min allowed time difference (in seconds) between source and destination files<br>
Files are considered identical if the difference between their last modified times is less or equal to the selected time in seconds. They are considered different if the time difference between the files is superior to the selected time. FAT and ExFAT need a minimum of 2 seconds tolerance. If 0 seconds is selected, the files must have exactly the same time to be considered similar.<br>

### Do not overwrite destination file if it is newer than source file<br>
If checked, the file will be overwritten only when the source file is newer than the destination file even if the file sizes and the last update times are different. Keep in mind that if you change time zones or if the files are modified during the interval period of the Day Light Saving Time change, the last modified file could appear older than the non-updated file. This is related to the file system differences and only a manual check before overwriting the file will avoid data loss. It is generally recommended to not modify files during the interval of day light saving time change if they are meant to be auto-synchronized. <br>

###Ignore Day Light Saving Time difference between files<br>
Let you select the time difference in minutes between summer and winter time. Files are considered different if the time difference is not exactly equal to the specified interval (+/- the “Min allowed time difference (in seconds)” specified in previous option)<br>

###Skip directory and file names that contain invalid characters(", :, \, *, <, >, |)<br>
If checked, it will display a warning message and the sync will continue without processing the directories/files containing invalid characters. <br>

###Delete the source directory when it is empty (only when Sync option is Move)<br>
When sync mode is Move, after the files are moved to the destination, the Source folder is also deleted. <br>

### If the date and time cannot be determined by EXIF data, a confirmation message is displayed<br>
Display a confirmation message when the taken date and time cannot be obtained from Exif.<br>

### Ignore source files that are larger than 4 GB when sync to external storage.<br>
If checked, you can prevent I/O errors when syncing to a MicroSD card by ignoring source files larger than 4 GB in size to be synced to local storage.<br>

### Ignore files whose file name exceed 255 bytes<br>
If checked, ignore files with file names longer than 255 bytes.<br>

### Manuals<br>
[FAQs](https://sentaroh.github.io/Documents/SMBSync3/SMBSync3_FAQ_EN.htm)<br>
[Description](https://sentaroh.github.io/Documents/SMBSync3/SMBSync3_Desc_EN.htm)<br>
