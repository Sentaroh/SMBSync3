### Test mode
It does not perform overwrite and delete the file if you check. Please be tested when you create a synchronization task, files that are copied or deleted can be found in the message tab.

### Auto sync task
If checked the task to the automatic. Tasks that are set to automatic synchronization will start when you press the sync button.

### Task name
Specify task name.

### Sync type
The sync method is selected from mirror, copy, move, archive. Sync is done from master to target in one direction.
- Mirror
- Move
- Copy
- Archive

### Swap the source and destination
Swap the contents of the source folder and the destination folder 

### Source folder
Tap the button to edit source folder

### Destination folder
Tap the button to edit destination folder

### Select files for sync
If you do not check and then sync all of the file. If you check to see details. 
- Sync audio files  
If you check to synchronize the files of the following extensions.  
aac, aif, aifc, aiff, kar, flac, m3u, m4a, mid, midi, mp2, mp3, mpga, ogg, ra, ram, wav  
- Sync image files  
  If you check to synchronize the files of the following extensions.  
  bmp, cgm, djv, djvu, gif, ico, ief, jpe, jpeg, jpg, pbm, pgm, png, tif, tiff
- Sync video files  
  If you check to synchronize the files of the following extensions.  
  avi, m4u, mov, mp4, movie, mpe, mpeg, mpg, mxu, qt, wmv  
- File filter  
You can select the name and extension of the file you want to synchronize with other than the above.

### Select sub directories for sync
If you do not check and then sync all of the sub directory. If you check to show directory filter button.
- Directory filter
You can select the name of the directory you want to synchronize.

### Execute sync task only when charging
If checked, you can start sync only while charging. If sync is started when not charging it will result in an error.

### Confirm before override copy or delete
It will display a confirmation dialog when you want to overwrite and delete the file if you have checked.

### Error Options  

You can specify the behavior when an error occurs.

- Stop synchronization
- Ignore all errors and start subsequent tasks
  Use this option if you want to make sure that subsequent tasks are executed. 
- Start subsequent tasks if network options result in errors  
  Use this if you want to run subsequent tasks when the address is not private or when it is not the specified IP address.  

### Network

- Run even when off  
You can always start syncing
- Conn to any AP  
Sync can start if the wireless LAN is connected to the any access point.
- Has private address  
You can start synchronization when the IP address is a private address
- IP Address List  
You can start syncing only if the WiFi IP address matches one of the specified addresses. You can also directly add the current IP address your device is connected to through the IP selection list.  
You can use wildcards for the filter. (e.g: 192.168.100.\*, 192.168.\*.\*.)

### Shows advanced options

**Please use it when setting detailed options.**

### Include subdirectories

It will recursively include subdirectories under the specified master folder. 

### Include empty directories

Synchronizes the empty directories (even if a directory is empty on the master, it will be created on the target). If unchecked, empty directories on the master are ignored. 

### Include hidden directories

When checked, Sync will include the hidden linux folders (those with a name starting with a dot). Note that in Windows and Samba, the hidden attribute is not set by the folder name. Thus, the synchronized folder on the SMB/Windows target won’t have the host hidden attribute. 

### Include hidden files

When checked, Sync will include the hidden linux files (those with a name starting with a dot). Note that in Windows and Samba, the hidden attribute is not set by the file name. Thus, the synchronized file on the SMB/Windows target won’t have the host hidden attribute.

### Overwrite destination files

If unchecked, files on the target will never be overwritten even if the compare criteria by size and time are different. 

### Retry on network error (only for SMB shares)

On server-side connection errors, SMBSync3 will try again the synchronization for a maximum of 3 times at a 30 seconds interval. 

### Limit SMB I/O write buffer to 16KB (only for SMB shares)

**Please try if you get an "Access is denied" error when writing to the PC/NAS folder.**

When checked, it will limit I/O buffer to 16KB for writing operations to the SMB host. 

### Delete files prior to sync (Mirror mode only)

When checked, the directories and files that are present on the target folder but that do not exist on the master, will be first deleted. After that, files and folders that are different will be copied to the target.

If the master folder is SMB, the processing time will be longer because the directory structure and their contents is scanned through the network. It is strongly recommended to enable the option " Use SMB2 negotiation" because SMB1 will be very slow.

### Remove directories and files excluded by the filters

If enabled, **it removes directories/files that are excluded from the filter.** 

### Do not set last modified time of destination file to match source file

Please enable if you get an error like SmbFile#setLastModified()/File#setLastModified() fails. It means that the remote host doesn’t allow setting file last modified time. If unchecked, the last modified time of the copied file on the target will be set to the time it was copied / synchronized. This means that the target file will appear newer than the master. 

For next synchronizations, you can:

- stick to compare by size only, or

- you can enable the option “Do not overwrite destination file if it is newer than source file” to only copy files modified later on the master, or

- you can enable the task option “Obtain last modification time of files from SMBSync3 application custom list”. However, this option is currently not available if the target is SMB. Most SMB hosts support setting the last modified time. 

See below for a detailed info on each option. 

### Use file size to determine if files are different

When checked, files are considered different if they differ by size. 

### Size only compare

Files are considered different only if size of the source is larger than the destination. This will disable compare by file time. 

### Use time of last modification to determine if files are different 

When checked, files are considered different based on their last modification time 

### Min allowed time difference (in seconds) between source and destination files

Files are considered identical if the difference between their last modified times is less or equal to the selected time in seconds. They are considered different if the time difference between the files is superior to the selected time. FAT and ExFAT need a minimum of 2 seconds tolerance. If 0 seconds is selected, the files must have exactly the same time to be considered similar.

### Do not overwrite destination file if it is newer than source file

If checked, the file will be overwritten only when the master file is newer than the target file even if the file sizes and the last update times are different. Keep in mind that if you change time zones or if the files are modified during the interval period of the Day Light Saving Time change, the last modified file could appear older than the non-updated file. This is related to the file system differences and only a manual check before overwriting the file will avoid data loss. It is generally recommended to not modify files during the interval of day light saving time change if they are meant to be auto-synchronized 

###  Ignore Day Light Saving Time difference between files

Let you select the time difference in minutes between summer and winter time. Files are considered different if the time difference is not exactly equal to the specified interval (+/- the “Min allowed time difference (in seconds)” specified in previous option)

###  Skip directory and file names that contain invalid characters(", :, \, *, <, >, |)

If checked, it will display a warning message and the sync will continue without processing the directories/files containing invalid characters. 

###  Delete the master directory when it is empty (only when Sync option is Move)

When sync mode is Move, after the files are moved to the target, the Master folder is also deleted. 

### If the date and time cannot be determined by EXIF data, a confirmation message is displayed

Display a confirmation message when the taken date and time cannot be obtained from Exif.

### Ignore source files that are larger than 4 GB when sync to external storage.
If checked, you can prevent I/O errors when syncing to a MicroSD card by ignoring source files larger than 4 GB in size to be synced to local storage.

### Ignore files whose file name exceed 255 bytes

If checked, ignore files with file names longer than 255 bytes.