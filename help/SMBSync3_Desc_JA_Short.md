## 機能  
SMBSync3はAndroid端末の内部ストレージ, MicroSD, USB FlashとPC/NASの間でSMB1, SMB2またはSMB3プロトコルを使用し無線LAN経由でファイルの同期を行うためのツールです。<span style="color: red; "><u>同期はソースから宛先への一方向</u></span>で、ミラー、移動、コピー、アーカイブが使用できます。ローカルストレージ(***<u>1</u>**)、SMB、ZIPの組み合わせが可能です）  

SMBSync3のスケジュール機能または外部アプリケーション（TaskerやAutoMagicなど）により定期的に同期を開始する事が可能です。   

- ミラー  
  ソース側のディレクトリーとファイルを宛先側に差分コピー(***2**)し、コピー終了後にソース側に存在しない宛先側のファイルとディレクトリーを削除する。  
- 移動  
  ソース側のディレクトリーとファイルを宛先側に差分コピー(***2**)し、コピー終了後にソース側のファイルを削除する。（ただし、ソースと宛先に同名でファイルサイズと更新日時が同じファイルはコピーせずソース側のファイルを削除）  
- コピー  
  ソース側のディレクトリーに含まれるファイルを宛先側に差分コピー(***2**)する。  
- アーカイブ  
  ソース側のディレクトリーに含まれる写真とビデオをアーカイブ実行日時より撮影日時が７日以前や30日以前などの条件で宛先に移動する。（ただし、宛先にZIPは使用できません）

***<u>1</u>** ローカルストレージは内部ストレージ、MicroSD、USB Flashの何れかです。  

***2** 下記の３条件のうちいずれかが成立した場合に差分ファイルと判定し、コピーや移動を行います。また、同期タスクのオプションでファイルサイズと最終更新時間を無視することができます。  

1. ファイルが存在しない  
1. ファイルサイズが違う  
1. ファイルの最終更新日時が3秒以上違う(秒数は同期タスクのオプションにより変更可能)

## FAQ

[PDFを参照ください。](https://drive.google.com/file/d/16ahhKVE8jSLwidHyIKKfDUXV3ogXa2-6/view?usp=sharing)  

## 使用ライブラリー

- [jcifs-ng](https://github.com/AgNO3/jcifs-ng)
- [Bouncy Castle Provider](https://mvnrepository.com/artifact/org.bouncycastle/bcprov-jdk15to18/1.66)  
- [jcifs-1.3.17](https://jcifs.samba.org/)
- [Zip4J 2.2.3](http://www.lingala.net/zip4j.html)
- [juniversalchardet-1.0.3](https://code.google.com/archive/p/juniversalchardet/)
- [Metadata-extractor](https://github.com/drewnoakes/metadata-extractor)


## マニュアル

[PDFを参照ください。](https://drive.google.com/file/d/1AfPsJbV7H5WHF7ZcvzVOJ4-e0SzVrA-p/view?usp=sharing)

