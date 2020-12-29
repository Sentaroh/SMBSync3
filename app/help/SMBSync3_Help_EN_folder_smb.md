### Find and configure the SMB server

Scan the network and select from the SMB server list to configure. 

### Edit SMB server parameters

Manually set the parameters for the SMB server. 

### Server host name/IP address

To specify SMB server name or IP address 

### SMB Protocol

You can specify SMB protocol.

- Use SMB V1

- Use SMB V2/3(2.14) 

### Use port number

Specify if the port number of the standard is not available. The standard port number is 139/tcp and 445/tcp. 

### Use Account name and password

It specifies whether to use the SMB server account name and password

The account name is a local account on the SMB server. You cannot use a Microsoft account. 

### Account name

To specify account name for SMB server. 

### Password

To specify password for SMB server. 

### List share

Show share name for SMB server.  

### List Directories

Show SMB server directory list.  

### Edit directory name parameters(Hidden in master directory)

The date and time **can be included in the target directory**. Variables are converted to date when synchronization starts. Please confirm the details of the variable by pressing "Edit directory name parameter". 

### Directory

To specify directory for SMB host. If the target directory does not exist, it will be created at the time of synchronization.

Note: Under the following conditions, it becomes a circular reference and loops. Specify a directory filter or specify a directory on the master side that is different from the target.

- No directory specified when the same SMB server is specified for master and target

- No directory filter specified

**The following is only displayed when the synchronization type is Archive.**

### Save all  files in the destination directory without creating subdirectories

If checked, the destination directory will not create a subdirectory in the source directory.

### To archive the

Select a file with a shooting date and time older than the archive execution date and time. (Regardless of the shooting date and time, the shooting date is 7 days or older, the shooting date is 30 days or older, the shooting date is 60 days or older, the shooting date is 90 days or older, the shooting date is 180 days or older, the shooting date is You can choose from more than one year old) 

### Increment  file names by appending

You can add a sequence number to the file name. 

### Edit file name parameter

To include the date and time in the file name, tap the button and edit.