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

### If it is not connected to the specified access point Skip the task and start the next task
If not connected to a specific access point, start the next synchronization task without error.

### Allow sync with global IP addresses
Enable sync with global IP addresses.

### Stop syncing when an error occurs
If it is not checked, sync continue when sync task ended with error.

### Sync subdirectory
Sync the subdirectory if you check.

### Sync a empty directory
Sync the empty directory if you check.(Create a empty directory on the target) 

### Sync a hidden directory
Sync the hidden directory if you check.( The name of the hidden directory starts from "." In Android, since it is represented by a bit in the directory and not the name of the directory on Samba and Windows, the directory that was created will not be in a hidden directory)

### Sync a hidden file
Sync the hidden file if you check.( The name of the hidden file begins with "." In Android, since it is represented by a bit in the directory and not the name of the file with Samba and Windows, the file that is created will not be hidden files)

### Overwrite destination file(s)
Overwrite the file if same file exists.

### Delete files prior to sync(Mirror method only)
If checked, delete the target file that does not exist on the source, and then copy the difference file with the source.

### Maintain Destination directory as an exact image of the Source (Mirror mode only)
If checked, all files and folders that do not exist in the Source directory and those that are not included by the defined filters will be deleted from the Target directory !

### Retry on network error
Retry the sync only error on the remote side. Retry is done up to three times, is carried out after 30 seconds from the error occurred retry each.

### Do not set the last update time of destination file to match source file
If checked, the last update time of the file copied from the source will be the time the file was copied. The difference judgment is judged by the existence of the existence of the file and the file size.

### Limit the SMB I/O write buffer to 16KB
If you check to limit the IO buffer at the time of remote file writing to 16KB. 

### Use file size to determine file difference
If checked, if the last update time of the file is different, it is determined as a difference file.
### Files are different only when the source is larger than the destination
If checked, the file is overwritten only when the source file is newer than the destination file, even if the file size and last update time are different.

### Minimum age period(in seconds) between source and destination file for synchronization
No change within the specified time difference.

### Do not overwrite destination file if newer than source file
If checked, the file will be overwritten only when the source file is newer than the destination file even if the file size and the last update time are different.

### Skipping directories or files name that contain invalid characters (", :, \, *, <, >, |)
If checked, it will display a warning message without processing directories/files containing unusable characters and process the following directories/files.

### (Sync type only moves) Delete the source directory when the source directory is empty
If the source directory is empty after moving from the source directory to the destination directory, delete the source directory.

### If the date and time cannot be determined by EXIF data, a confirmation message is displayed
Display a confirmation message when the taken date and time cannot be obtained from Exif.

### Ignore source files that are larger than 4 GB when sync to external storage.
If checked, you can prevent I/O errors when syncing to a MicroSD card by ignoring source files larger than 4 GB in size to be synced to local storage.
