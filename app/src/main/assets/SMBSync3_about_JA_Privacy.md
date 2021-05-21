## 1.収集データ  
### 1.1.ユーザーからSMBSync3に提供されるデータ  

#### 1.1.1.保存するデータ  
下記のデータはアプリ内に保存されます。  

- ファイル情報(ディレクトリー名、ファイル名)  
- SMBサーバーを使用する場合はSMBサーバー情報(ホスト名/IPアドレス、ポート番号、アカウント名(**<u>\*1</u>**)、アカウントパスワード(**<u>\*1</u>**))  
- ZIPファイルを使用する場合はZIPファイル情報(圧縮方式、暗号化方式、暗号化パスワード(**<u>\*1</u>**))  
- アプリ設定オプション(警告メッセージ表示、言語や文字サイズなど)  
- アプリパスワード(**<u>\*1</u>**)  
ユーザーが作成したパスワードでアプリの起動・設定変更等の認証に使用します。  

**<u>\*1</u>** 暗号化して保存します。  

#### 1.1.2.保存しないデータ  
下記のデータは保存しません。  

- 「1.4.SMBSync3外へのデータの送信または書出し」でデータを保護するためのパスワード  
パスワードは処理が終了した時点で廃棄され保存しません。  

### 1.2.SMBSync3の実行結果  

SMBSync3の実行結果をユーザーが確認できるようにデータをアプリ内の記憶領域に保存します。  

- ディレクトリー名、ファイル名、実行状況  
- 同期したファイルのファイルサイズ、ファイルの更新日時  
- エラー情報  

### 1.3.SMBSync3の活動記録  

ログを有効にするとアプリの実行結果の検証と技術サポートのために活動データをアプリ内の記憶領域に保存します。ログを無効にした場合はデータの記録は停止します、ただし、既に記録したデータは削除されません。  

- デバイス情報(メーカー名、モデル名、OSのバージョン, マウントポイント, アプリ固有ディレクトリー, StorageAccessFramework, Storage manager, IPアドレス, WiFi有効・無効, WiFiリンクスピード)  
- SMBSync3のバージョン、SMBSync3の実行オプション  
- ディレクトリー名、ファイル名、実行状況  
- 同期したファイルのファイルサイズ、ファイルの更新日時  
- デバッグ情報  
- エラー情報  

### 1.4.SMBSync3外へのデータの送信または書出し  

<span style="color: red;"><u>ユーザーが操作しない限りSMBSync3が保有するデータは外部に送信または書出しは行いません。</u></span>  

- 履歴タブから「共有ボタン」を押す  
- システム情報から「開発者に送る」ボタンを押す  
- ログ管理から「共有」ボタンを押す  
- ログ管理から「開発者に送る」ボタンを押す  
パスワードを指定すると添付ファイルをパスワード保護されます。パスワードは画面を閉じると廃棄され保存されません。  
- ログ管理から「ログファイルの書出」ボタンを押すことにより外部ストレージに書き出します  
- メニューから「タスクリストの保存」を実行する事により「 1.1.ユーザーからSMBSync3に提供されるデータ」を書き出します。  
書き出す際にパスワードを指定することにより情報は暗号化されます。パスワードは画面を閉じると廃棄され保存されません。  

### 1.5.SMBSync3内に保存されたデータの削除  

SMBSync3をアンインストールする事により保存したデータ("1.1.ユーザーからSMBSync3に提供されるデータ", "1.2.SMBSync3の実行結果", "1.3.SMBSync3の活動記録")はデバイスから削除されます。  
<span style="color: red; "><u>ただし、ユーザーの操作により外部ストレージに保存されたデータは削除されません。</u></span>  

## 2.アプリ実行に必要な権限  

### 2.1.ストレージ  

### 2.1.1.Android11以降  
**<u>All file access</u>**  
ファイル同期と管理ファイルの読み書きで使用します。  

### 2.1.2.Android10以前  

#### 2.1.2.1.写真、メディア、ファイル  
**<u>read the contents of your USB storage</u>**  
**<u>modify or delete the contents of your USB storage</u>**  
ファイル同期と管理ファイルの読み書きで使用します。  

### 2.2.Wi-Fi 接続情報  
**view Wi-Fi connections**  
同期開始時にネットワークに接続されていることを確認するために使用します。  

### 2.3.その他  
### 2.3.1.View network connections  
同期開始時にネットワークに接続されていることを確認するために使用します。  
### 2.3.2.Connect and disconnect from Wi-Fi  
Andoid 8/9でスケジュール同期でWi-Fiのオン・オフを行うために使用します。  
### 2.3.3.Full network access  
ネットワークを通じてSMBプロトコルで同期を行うために使用します。  
### 2.3.4.Run at startup  
デバイス再起動時にスケジュール同期の初期化を行うために使用します。  
### 2.3.5.Control vibration  
同期終了時にユーザーに通知を行うために使用します。  
### 2.3.6.Prevent device from sleeping  
同期中にデバイスがスリープしないようにするために使用します。  
### 2.3.7.Install shortcuts  
デスクトップに同期開始ショートカットを追加するために使用します。  