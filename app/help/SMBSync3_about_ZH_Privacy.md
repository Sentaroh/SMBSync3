 ## 1.从应用程序发送记录的数据。

应用程序记录的数据可以通过以下应用程序操作，通过电子邮件和共享工具对外发送。<span style="color: red;"><u>应用程序不会对外发送记录的数据，除非用户这样做</u></span>。

- 从 "历史 "标签中按 "分享 "按钮

- 按系统信息中的 "发送给开发人员 "按钮。

- 在日志管理中按 "分享按钮 "或 "发送至开发者 "按钮。

## 2.应用程序记录的数据

### 2.1.同步任务列表

应用程序记录执行同步所需的数据。

- 目录名、文件名、SMB服务器主机名、IP地址、端口号、账户名（***1**）、密码（***1**）。

- 应用密码(***1**)保护应用的启动和设置的更改

- 应用程序设置值

***1**用系统生成的密钥存储在AndroidKeystore中加密。

 

### 2.2.应用活动记录

启用日志记录功能后，应用会记录以下数据，以验证和排查同步结果。

- Android版本、终端制造商、终端名称、终端型号、应用程序版本。

- 目录名、文件名、文件大小、文件最后修改时间。

- SMB服务器的主机名、IP地址、端口号、账户名。

- 网络接口名称、IP地址

- 系统设置值(WiFi睡眠策略、存储信息)

- 应用程序设置值

### 2.3.同步导出任务列表

该应用可以将 "2.1同步任务列表 "导出为文件。导出时可以进行密码保护。

- 目录名、文件名
- SMB服务器主机名、IP地址、端口号、账户名、密码。
- 应用程序设置值 

## 3. 发送给应用开发者的数据的使用目的

应用程序的用户向开发者发送的数据将仅用于解决应用程序的问题，不会向开发者以外的任何人透露。

## 4. 权限

该应用需要以下权限。

### 4.1.Photos/Media/Files

**read the contents of your USB storage  
modify or delete the contents of your USB storage** 
用于内部/外部存储的文件同步和管理文件的读/写。

### 4.2.Storage

### 4.2.1.Android11から  
**Manage external storage**  

USBストレージへのファイル同期と管理ファイルの読み書きで使用します。

### 4.2.2.Android10まで  
**read the contents of your USB storage  
modify or delete the contents of your USB storage** 
用于USB存储的文件同步和管理文件的读/写。

### 4.3.Wi-Fi Connection infomation

**view Wi-Fi connections**  
用于检查同步开始时Wi-Fi的状态。

### 4.4.其他

### 4.4.1.view network connections

用于在同步开始时确认是否连接到网络。

### 4.4.2.connect and disconnect from Wi-Fi

用于在Andoid 8/9中开启/关闭日程同步的Wi-Fi。

### 4.4.3.full network access

用于通过网络与SMB协议进行同步。

### 4.4.4.run at startup

用于执行计划同步。

### 4.4.5.control vibration

用于在同步结束时通知用户。

### 4.4.6.prevent device from sleeping

使用它从日程表或外部应用程序开始同步。

### 4.4.7.install shortcuts

用于在桌面上添加同步启动快捷方式。

 

 