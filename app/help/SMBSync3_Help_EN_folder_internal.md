### Local storage
Select the storage to use.

### Allow use external storage
Request permission to use external storage. If you format the MicroSD card, the UUID will change, so request permission to use it.

### List Files
Show directory list for local storage. 

### Edit directory name parameters
The date and time can be included in the target directory. Variables are converted to date when synchronization starts. Please confirm the details of the variable by pressing "Edit directory name parameter".

### Directory
To specify directory for local storage. If the target directory does not exist, it will be created at the time of synchronization.
Note: Under the following conditions, it becomes a circular reference and loops. Specify a directory filter or specify a directory on the master side that is different from the target.

- No directory specified for master and target
- No directory filter specified

### <u>The following is only displayed when the synchronization type is Archive.</u>
### Do not create a source directory in the destination directory
If checked, the destination directory will not create a subdirectory in the source directory.
### To archive the
Select a file with a shooting date and time older than the archive execution date and time. (Regardless of the shooting date and time, the shooting date is 7 days or older, the shooting date is 30 days or older, the shooting date is 60 days or older, the shooting date is 90 days or older, the shooting date is 180 days or older, the shooting date is You can choose from more than one year old)

### Sequence number
You can add a sequence number to the file name.

### Edit file name parameter
To include the date and time in the file name, tap the button and edit.
