### Test mode
It does not perform overwrite and delete the file if you check. Please be tested when you create a synchronization task, files that are copied or deleted can be found in the message tab.

### Auto sync task
If checked the task to the automatic. Tasks that are set to automatic synchronization will start when you press the sync button.

### Task name
Specify task name.

### Sync type
Select a method from Mirror, Copy, Move and Archive. <span style="color: red;"><u>Synchronization is done in one direction, from the source folder to the destination folder.</u></span> 

- Mirror
Make a differential copy (**<u>*1</u>**) of directories and files on the source side to the destination side, and delete files and directories on the destination side that do not exist on the source side after the copy is completed.
- Move
Make a differential copy of the source side directory and file to the destination side, and delete the source side file copied to the destination side. However, the file with the same name as the source and the destination but with the same file size and modification date is not copied and the file on the source side is deleted.
- Copy
Make a differential copy of the files contained in the source directory to the destination side.
- Archive
Move the photos and videos contained in the source directory to the destination with the condition that the shooting date and time is earlier than 7 days or 30 days from the archive execution date and time. (However, ZIP cannot be used as the destination.)

**<u>*1</u>** If any of the following three conditions are met, the file is judged to be a difference file and is copied or moved. However, the file size and last modified time can be ignored in the options of the synchronization task.

1. file does not exist
2. file size is different
3. last modified date and time is different by more than 3 seconds (the number of seconds can be changed by the option of the synchronization task)

### Swap the source and destination
Swap the contents of the source folder and the destination folder 

### Source folder

Tap the button to edit source folder

### Destination folder

Tap the button to edit destination folder

### Filters
You can select files to synchronize by file name, file size, file modification date, and directory name.

- File name filter
You can register the names and extensions of files to be synchronized.
- File size filter
You can select the files to synchronize by file size.
- File modification date filter
You can select the files to synchronize by their last modified date.
- Directory filter
You can select the name of the directory you want to synchronize.

### To archive the

Select the criteria for the photos or videos to be archived.

- Any date (All)
- Older than 7 days
- Older than 30 days
- Older than 60 days
- Older than 90 days
- Older than 180 days
- Older than 1 year

### Execute sync task only when charging
If checked, you can start sync only while charging. If sync is started when not charging it will result in an error.

### Syncincludes the files located directly in root of the source directory 

Ifunchecked, only folders and their files/subfolders are synchronized

### Confirm before override copy or delete

It will display a confirmation dialog when you want to overwrite and delete the file if you have checked.

### Error Options

You can specify the behavior when an error occurs.

- Stop synchronization
- Ignore all errors and start subsequent tasks
Use this option if you want to make sure that subsequent tasks are executed. 
- Start subsequent tasks if network options result in errors
Use this if you want to run subsequent tasks when the address is not private or when it is not the specified IP address.

### Network Option
You can set whether synchronization can start or not based on the network status.

- Run even when off
You can always start syncing
- When connected to AP
Sync can start if the wireless LAN is connected to the any access point.
- Private IP address only
Synchronization can be started when the IP address is a private address
- Registered in the IP address list
You can start synchronization when the IP address is registered in the IP address list.
You can use wildcards for the filter. (e.g: 192.168.100.\*, 192.168.\*.\*.)

### Allowsync with all IP addresses (include public)

Enables synchronization on all IP addresses. However, SMB server scan cannot be performed. 

### Shows advanced options

**Please use it when setting detailed options.**

### Include subdirectories
It will recursively include subdirectories under the specified source folder. 

### Include empty directories
Synchronizes the empty directories (even if a directory is empty on the source, it will be created on the destination). If unchecked, empty directories on the source are ignored. 

### Include hidden directories
When checked, Sync will include the hidden linux folders (those with a name starting with a dot). Note that in Windows and Samba, the hidden attribute is not set by the folder name. Thus, the synchronized folder on the SMB/Windows destination won’t have the host hidden attribute. 

### Include hidden files
When checked, Sync will include the hidden linux files (those with a name starting with a dot). Note that in Windows and Samba, the hidden attribute is not set by the file name. Thus, the synchronized file on the SMB/Windows destination won’t have the host hidden attribute.

### Overwrite destination files
If unchecked, files on the destination will never be overwritten even if the compare criteria by size and time are different. 

### Retry on network error (only for SMB shares)
On server-side connection errors, SMBSync3 will try again the synchronization for a maximum of 3 times at a 30 seconds interval. 

### Limit SMB I/O write buffer to 16KB (only for SMB shares)
Please try if you get an "Access is denied" error when writing to the PC/NAS folder. When checked, it will limit I/O buffer to 16KB for writing operations to the SMB host. 

### Delete files prior to sync (Mirror mode only)

When checked, the directories and files that are present on the destination folder but that do not exist on the source, will be first deleted. After that, files and folders that are different will be copied to the destination.
If the source folder is SMB, the processing time will be longer because the directory structure and their contents is scanned through the network. It is strongly recommended to enable the option " Use SMB2 negotiation" because SMB1 will be very slow.

### Remove directories and files excluded by the filters

If enabled, **it removes directories/files that are excluded from the filter.** 

### Do not set last modified time of destination file to match source file

Please enable if you get an error like SmbFile.setLastModified()/File.setLastModified() fails. It means that the remote host doesn’t allow setting file last modified time. If unchecked, the last modified time of the copied file on the destination will be set to the time it was copied / synchronized. This means that the destination file will appear newer than the source. 

### Use file size to determine if files are different

When checked, files are considered different if they differ by size. 
### Size only compare
Files are considered different only if size of the source is larger than the destination. This will disable compare by file time. 

### Use time of last modification to determine if files are different 
When checked, files are considered different based on their last modification time 

### Min allowed time difference (in seconds) between source and destination files
Files are considered identical if the difference between their last modified times is less or equal to the selected time in seconds. They are considered different if the time difference between the files is superior to the selected time. FAT and ExFAT need a minimum of 2 seconds tolerance. If 0 seconds is selected, the files must have exactly the same time to be considered similar.

### Do not overwrite destination file if it is newer than source file
If checked, the file will be overwritten only when the source file is newer than the destination file even if the file sizes and the last update times are different. Keep in mind that if you change time zones or if the files are modified during the interval period of the Day Light Saving Time change, the last modified file could appear older than the non-updated file. This is related to the file system differences and only a manual check before overwriting the file will avoid data loss. It is generally recommended to not modify files during the interval of day light saving time change if they are meant to be auto-synchronized. 

###Ignore Day Light Saving Time difference between files
Let you select the time difference in minutes between summer and winter time. Files are considered different if the time difference is not exactly equal to the specified interval (+/- the “Min allowed time difference (in seconds)” specified in previous option)

###Skip directory and file names that contain invalid characters(", :, \, *, <, >, |)
If checked, it will display a warning message and the sync will continue without processing the directories/files containing invalid characters. 

###Delete the source directory when it is empty (only when Sync option is Move)
When sync mode is Move, after the files are moved to the destination, the Source folder is also deleted. 

### If the date and time cannot be determined by EXIF data, a confirmation message is displayed
Display a confirmation message when the taken date and time cannot be obtained from Exif.

### Ignore source files that are larger than 4 GB when sync to external storage.
If checked, you can prevent I/O errors when syncing to a MicroSD card by ignoring source files larger than 4 GB in size to be synced to local storage.

### Ignore files whose file name exceed 255 bytes
If checked, ignore files with file names longer than 255 bytes.

### Manuals
[FAQs](https://sentaroh.github.io/Documents/SMBSync3/SMBSync3_FAQ_EN.htm)
[Description](https://sentaroh.github.io/Documents/SMBSync3/SMBSync3_Desc_EN.htm)
