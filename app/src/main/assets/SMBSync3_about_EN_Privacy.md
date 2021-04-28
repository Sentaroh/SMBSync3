## 1. Collected data
### 1.1.Data provided to SMBSync3 from users.

The data provided by the user to use SMBSync3 will be stored in the storage area in the application.
However, the SMB account name, SMB account password, ZIP password, and app password will be encrypted and stored.
<span style="color: red;"><u>Data will not be sent externally unless the operation "1.4.Sending or writing data outside SMBSync3" is performed.</u></span>

- File information (directory name, file name)
- SMB server information (host name/IP address, port number, account name, account password)
- ZIP file information (compression method, encryption method, encryption password)
- App setting options (warning messages, language and font size, etc.)
- Application passwords (passwords used for authentication when launching applications, changing security settings, etc.)

### 1.2.Execution result of SMBSync3

Save the data to the storage area in the application so that the user can check the execution result of SMBSync3.
<span style="color: red;"><u>Data will not be sent externally unless the operation "1.4.Sending or writing data outside SMBSync3" is performed.</u></span>

- Directory name, file name, execution status
- File size of synchronized files, file update date and time
- Error information

### 1.3.Activity record of SMBSync3

Save the data to the storage area in the application to verify the execution results of SMBSync3 and to query the developer.
<span style="color: red;"><u>Data will not be sent externally unless the operation "1.4.Sending or writing data outside SMBSync3" is performed.</u></span>

- Device information (manufacturer name, model name, OS version, mount point, app-specific directory, StorageAccessFramework, Storage manager, IP address, WiFi enable/disable, WiFi link speed)
- SMBSync3 version, SMBSync3 execution options
- Directory name, file name, execution status
- File size of synchronized files, file modification date and time
- Debugging information
- Error information

### 1.4.Sending or writing data outside SMBSync3

Data held by SMBSync3 cannot be sent or written out to outside unless operated by the user.

- Press "Share button" from History tab.
- Click "Send to Developer" button from System Information.
- Click the "Send to Developer" button from the log management.
- Export to external storage by clicking "Export log file" button from Log Management.
- By executing "Export settings" from the menu, "1.1. Data provided to SMBSync3 from users" will be exported.
By specifying a password when exporting, the information is encrypted and saved in the file.

### 1.5.Delete the data stored in SMBSync3

By uninstalling SMBSync3, the saved data ("1.1. Data provided by user to SMBSync3", "1.2. Execution result of SMBSync3", "1.3. Activity record of SMBSync3") will be deleted from the device.
<span style="color: red;"><u>However, data stored in external storage due to user interaction will not be deleted.</u></span>

### 2.Permissions required to run the application

### 2.1.Photos, Media and Files
**read the contents of your USB storage**.
**modify or delete the contents of your USB storage**.
Used for file synchronization and reading/writing management files.

### 2.2.Storage

### 2.2.1.Android 11 or later
**All file access**.

Used for file synchronization and reading/writing management files.

### 2.2.2.Android 10 or before
**read the contents of your USB storage**.
**modify or delete the contents of your USB storage**.
Used for file synchronization and reading/writing management files.

### 2.3.Wi-Fi connection information
**view Wi-Fi connections**.
Used to check the Wi-Fi status when synchronization starts.

### 2.4.Others
### 2.4.1.View network connections
Use this to check network connections when starting synchronization.
### 2.4.2.connect and disconnect from Wi-Fi
This function is used to turn Wi-Fi on and off for scheduled synchronization in Andoid 8/9.
### 2.4.3.Full network access
Use this to synchronize via SMB protocol through the network.
### 2.4.4.Run at startup
Used to perform scheduled synchronization.
### 2.4.5.Control vibration
Used to notify the user when synchronization is finished.
### 2.4.6.Prevent device from sleeping
Used to start synchronization from a schedule or external app.
### 2.4.7.Install shortcuts
Used to add sync start shortcuts to the desktop.
