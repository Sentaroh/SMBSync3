## 功能  
SMBSync3是一款通过无线局域网使用SMB1、SMB2或SMB3协议在Android设备的内部存储、MicroSD、USB闪存和PC/NAS之间同步文件的工具。<span style="color: red;"><u>同步是单向的</u></span>从源文件夹到目标文件夹，可以进行镜像、移动、复制或存档。  
定期同步可以由SMBSync3的调度功能或外部应用程序（Tasker，AutoMagic等）启动。  
- 镜像  
将源目录和文件进行差分复制（<u>***1**</u>）到目的地，复制完成后，删除源端不存在的文件和目录。  
- 移动  
将源目录和文件进行差分复制(<u>***1**</u>)到目的侧，复制完成后删除源侧的文件(但源侧和目的侧的文件名称相同、文件大小相同、修改日期相同，文件不复制，复制完成后删除源目录和文件。侧的文件)。  
- 拷贝  
将源端目录中包含的文件进行差分复制（***1**）到目的端。  
- 归档  
源目录中包含的照片和视频，日期和时间为档案执行日期后7天 去目的地之前或30天之前等。但不能用ZIP作为目的地）。  

<u>***1**</u> 差别文件是以下三种情况之一。   

1. 文件不存在  
2. 不同的文件大小  
3. 与上次更新3秒时不同  

### 使用说明书  
[FAQs](https://sentaroh.github.io/Documents/SMBSync3/SMBSync3_FAQ_EN.htm)  
[Description](https://sentaroh.github.io/Documents/SMBSync3/SMBSync3_Desc_EN.htm)  
