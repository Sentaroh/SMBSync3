## 1. Collected data  
### 1.1.Data provided to SMBSync3 from users.  

The following data provided by the user to use SMBSync3 will be stored in the memory area of the application.  

- File information (directory name, file name)  
- SMB server information if using SMB server (host name/IP address, port number, account name(**<u>\*1</u>**), account password(**<u>\*1</u>**))  
- ZIP file information if using a ZIP file (compression method, encryption method, encryption password(**<u>\*1</u>**))  
- App setting options (warning messages, language and font size, etc.)  
- Application passwords(**<u>\*1</u>**)  

**<u>\*1</u>**Data is encrypted and stored.  

### 1.2.Execution result of SMBSync3  

Save the data to the storage area in the application so that the user can check the execution result of SMBSync3.  

- Directory name, file name, execution status  
- File size of synchronized files, file update date and time  
- Error information  

### 1.3.Activity record of SMBSync3  

Enabling logging will save the activity record data in the app's memory area for verification of the app's execution results and technical support. When logging is disabled, data recording will be stopped. However, the data already recorded will not be deleted, so you can delete it if you need to.  

- Device information (manufacturer name, model name, OS version, mount point, app-specific directory, StorageAccessFramework, Storage manager, IP address, WiFi enable/disable, WiFi link speed)  
- SMBSync3 version, SMBSync3 execution options  
- Directory name, file name, execution status  
- File size of synchronized files, file modification date and time  
- Debugging information  
- Error information  

### 1.4.Sending or writing data outside SMBSync3  

<span style="color: red;"><u></u></span>Data held by SMBSync3 cannot be sent or written out to outside unless operated by the user.  

- Press "Share button" from History tab.  
- Click "Send to Developer" button from System Information.  
- Click the "Share" button from the log management.  
- Click the "Send to Developer" button from the log management.  
- Export to external storage by clicking "Export log file" button from Log Management.  
- By executing "Export settings" from the menu, "1.1. Data provided to SMBSync3 from users" will be exported.  
The information will be encrypted by specifying a password when exporting.  

### 1.5.Delete the data stored in SMBSync3  

By uninstalling SMBSync3, the saved data ("1.1. Data provided by user to SMBSync3", "1.2. Execution result of SMBSync3", "1.3. Activity record of SMBSync3") will be deleted from the device.  
<span style="color: red;"><u>However, data stored in external storage due to user interaction will not be deleted.</u></span>  

### 2.Permissions required to run the application  

### 2.1.Storage  

### 2.1.1.Android 11 or later  
**<u>All file access</u>**  
Used for file synchronization and reading/writing management files.  

### 2.1.2.Android 10 or before  

### 2.1.Photos, Media and Files  
**<u>read the contents of your USB storage</u>**  
**<u>modify or delete the contents of your USB storage</u>**  
Used for file synchronization and reading/writing management files.  

### 2.2.Wi-Fi connection information  
**view Wi-Fi connections**.  
Use this to check network connections when starting synchronization.  

### 2.3.Others  
### 2.3.1.View network connections  
Use this to check network connections when starting synchronization.  
### 2.3.2.connect and disconnect from Wi-Fi  
This function is used to turn Wi-Fi on and off for scheduled synchronization in Andoid 8/9.  
### 2.3.3.Full network access  
Use this to synchronize via SMB protocol through the network.  
### 2.3.4.Run at startup  
Used to initialize scheduled synchronization upon device reboot.  
### 2.3.5.Control vibration  
Used to notify the user when synchronization is finished.  
### 2.3.6.Prevent device from sleeping  
Used to prevent the device from going to sleep during synchronization.  
### 2.3.7.Install shortcuts  
Used to add sync start shortcuts to the desktop.  
