## 1. Collected data<br>
### 1.1.Data provided to SMBSync3 from users.<br>

The data provided by the user to use SMBSync3 will be saved in the storage area of the application.<br>
When storing the data, the SMB account name, SMB account password, ZIP password, and application password will be encrypted with the system-generated password.<br>
<span style="color: red;"><u>Data will not be sent externally unless the operation "1.4.Sending or writing data outside SMBSync3" is performed.</u></span><br>

- File information (directory name, file name)<br>
- SMB server information (host name/IP address, port number, account name, account password)<br>
- ZIP file information (compression method, encryption method, encryption password)<br>
- App setting options (warning messages, language and font size, etc.)<br>
- Application passwords (passwords used for authentication when launching applications, changing security settings, etc.)<br>

### 1.2.Execution result of SMBSync3<br>

Save the data to the storage area in the application so that the user can check the execution result of SMBSync3.<br>
<span style="color: red;"><u>Data will not be sent externally unless the operation "1.4.Sending or writing data outside SMBSync3" is performed.</u></span><br>

- Directory name, file name, execution status<br>
- File size of synchronized files, file update date and time<br>
- Error information<br>

### 1.3.Activity record of SMBSync3<br>

Save the data to the storage area in the application to verify the execution results of SMBSync3 and to query the developer.<br>
<span style="color: red;"><u>Data will not be sent externally unless the operation "1.4.Sending or writing data outside SMBSync3" is performed.</u></span><br>

- Device information (manufacturer name, model name, OS version, mount point, app-specific directory, StorageAccessFramework, Storage manager, IP address, WiFi enable/disable, WiFi link speed)<br>
- SMBSync3 version, SMBSync3 execution options<br>
- Directory name, file name, execution status<br>
- File size of synchronized files, file modification date and time<br>
- Debugging information<br>
- Error information<br>

### 1.4.Sending or writing data outside SMBSync3<br>

Data held by SMBSync3 cannot be sent or written out to outside unless operated by the user.<br>

- Press "Share button" from History tab.<br>
- Click "Send to Developer" button from System Information.<br>
- Click the "Send to Developer" button from the log management.<br>
- Export to external storage by clicking "Export log file" button from Log Management.<br>
- By executing "Export settings" from the menu, "1.1. Data provided to SMBSync3 from users" will be exported.<br>
By specifying a password when exporting, the information is encrypted and saved in the file.<br>

### 1.5.Delete the data stored in SMBSync3<br>

By uninstalling SMBSync3, the saved data ("1.1. Data provided by user to SMBSync3", "1.2. Execution result of SMBSync3", "1.3. Activity record of SMBSync3") will be deleted from the device.<br>
<span style="color: red;"><u>However, data stored in external storage due to user interaction will not be deleted.</u></span><br>

### 2.Permissions required to run the application<br>

### 2.1.Photos, Media and Files<br>
**read the contents of your USB storage**.<br>
**modify or delete the contents of your USB storage**.<br>
Used for file synchronization and reading/writing management files.<br>

### 2.2.Storage<br>

### 2.2.1.Android 11 or later<br>
**All file access**.<br>

Used for file synchronization and reading/writing management files.<br>

### 2.2.2.Android 10 or before<br>
**read the contents of your USB storage**.<br>
**modify or delete the contents of your USB storage**.<br>
Used for file synchronization and reading/writing management files.<br>

### 2.3.Wi-Fi connection information<br>
**view Wi-Fi connections**.<br>
Used to check the Wi-Fi status when synchronization starts.<br>

### 2.4.Others<br>
### 2.4.1.View network connections<br>
Use this to check network connections when starting synchronization.<br>
### 2.4.2.connect and disconnect from Wi-Fi<br>
This function is used to turn Wi-Fi on and off for scheduled synchronization in Andoid 8/9.<br>
### 2.4.3.Full network access<br>
Use this to synchronize via SMB protocol through the network.<br>
### 2.4.4.Run at startup<br>
Used to perform scheduled synchronization.<br>
### 2.4.5.Control vibration<br>
Used to notify the user when synchronization is finished.<br>
### 2.4.6.Prevent device from sleeping<br>
Used to start synchronization from a schedule or external app.<br>
### 2.4.7.Install shortcuts<br>
Used to add sync start shortcuts to the desktop.<br>
