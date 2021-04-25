## 1.Send recorded data from the app

The data recorded by the application can be sent externally via email and sharing tools by the following app operations. <span style="color: red; "><u>The application will not send the recorded data externally unless the user does so.</u></span>

- Press "Share button" from History tab

- Press the "Send to Developer" button from the system information

- Press "Share button" or "Send to developer" button from log management

## 2.Data recorded by the app

### 2.1.Synchronization task list

The app records the necessary data to perform the synchronization.

- Directory name, file name, SMB server host name, IP address, port number, account name (***1**), password (***1**)

- App password (***1**) to protect app launch and setting change

- App setting value

***1** Encrypted with system generated key stored in AndroidKeystore.

 

### 2.2.App activity record

The app will record the following data when you enable logging to verify and troubleshoot the synchronization results.

- Android version, terminal maker, terminal name, terminal model, application version

- Directory name, file name, file size, file last modified time

- SMB server host name, IP address, port number, account name

- Network interface name, IP address

- System setting value(WiFi Sleep policy, Storage info)

- App setting value

### 2.3.Sync task list exported

The app can export "2.1 Synchronization task list" to a file. You can password protect on export.
- Directory name, file name
- SMB server host name, IP address, port number, account name, password
- App setting value 

## 3.Purpose of use of data sent to app developers

The data sent by the users of the app to the developer will be used only to solve problems with the app and will not be disclosed to anyone other than the developer.

## 4.Permissions

The app requires the following permissions.

### 4.1.Photos/Media/Files

**read the contents of your USB storage  
modify or delete the contents of your USB storage**  
Used for file synchronization to internal/external storage and read/write of management file.

### 4.2.Storage

### 4.2.1.Android11から  
**Manage external storage**  

USBストレージへのファイル同期と管理ファイルの読み書きで使用します。

### 4.2.2.Android10まで  
**read the contents of your USB storage  
modify or delete the contents of your USB storage**  
Used for file synchronization to USB storage and read/write of management file.

### 4.3.Wi-Fi Connection infomation

**view Wi-Fi connections**  
Used to check the status of Wi-Fi at the start of synchronization.

### 4.4.Other

### 4.4.1.view network connections

Used to confirm that it is connected to the network at the start of synchronization.

### 4.4.2.connect and disconnect from Wi-Fi

Used to turn on / off Wi-Fi in schedule synchronization in Andoid 8/9.

### 4.4.3.full network access

Used to perform synchronization with the SMB protocol through the network.

### 4.4.4.run at startup

Used to perform schedule synchronization.

### 4.4.5.control vibration

Used to notify the user at the end of synchronization.

### 4.4.6.prevent device from sleeping

Use it to start synchronization from a schedule or external application.

### 4.4.7.install shortcuts

Used to add a synchronization start shortcut to the desktop.

 

 