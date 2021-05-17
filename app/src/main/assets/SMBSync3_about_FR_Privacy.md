## 1.Données collectées  
### 1.1.Données fournies par l'utilisateur à SMBSync3  

Les données suivantes fournies par l'utilisateur pour utiliser SMBSync3 seront stockées dans la zone de mémoire de l'application.  

- Informations sur les fichiers (nom du répertoire, nom du fichier)  
- Informations sur le serveur SMB si vous utilisez un serveur SMB (nom d'hôte/adresse IP, numéro de port, nom de compte(**<u>\*1</u>**), mot de passe de compte(**<u>\*1</u>**))  
- Informations sur le fichier ZIP en cas d'utilisation d'un fichier ZIP (méthode de compression, méthode de cryptage, mot de passe de cryptage(**<u>\*1</u>**)).  
- Options de paramétrage de l'application (messages d'avertissement, langue et taille de la police, etc.)  
- Mot de passe de l'application(**<u>\*1</u>**)  

**<u>\*1</u>**Les données sont cryptées et stockées.  

### 1.2.Résultat de l'exécution de SMBSync3  

Sauvegarder les données dans la zone de stockage de l'application afin que l'utilisateur puisse vérifier le résultat de l'exécution de SMBSync3.  

- Nom du répertoire, nom du fichier, état d'exécution  
- Taille des fichiers synchronisés, date et heure de mise à jour des fichiers.  
- Informations sur les erreurs  

### 1.3.Fiche d'activité de SMBSync3  

L'activation de la journalisation permet de sauvegarder les données d'enregistrement de l'activité dans la zone de mémoire de l'application afin de vérifier les résultats d'exécution de l'application et d'obtenir une assistance technique. Lorsque la journalisation est désactivée, l'enregistrement des données s'arrête. Cependant, les données déjà enregistrées ne seront pas supprimées, vous pouvez donc les effacer si vous en avez besoin.  

- Informations sur le dispositif (nom du fabricant, nom du modèle, version du système d'exploitation, point de montage, répertoire spécifique à l'application, StorageAccessFramework, gestionnaire de stockage, adresse IP, activation/désactivation du WiFi, vitesse de la liaison WiFi)  
- Version de SMBSync3, options d'exécution de SMBSync3  
- Nom du répertoire, nom du fichier, état d'exécution  
- Taille des fichiers synchronisés, date et heure de mise à jour des fichiers.  
- Informations sur le débogage  
- Informations sur les erreurs  

### 1.4 Envoi ou écriture de données en dehors de SMBSync3  

<span style="color: red;"><u>Les données de SMBSync3 ne peuvent être envoyées ou écrites vers l'extérieur que si l'utilisateur l'actionne.</u></span>  

- Appuyez sur le [bouton Partager] dans l'onglet Historique.  
- Cliquez sur le bouton "Envoyer au développeur" à partir des informations système.  
- Cliquez sur le bouton "Envoyer au développeur" à partir de la gestion du journal.  
- Cliquez sur le bouton "Exporter le fichier journal" depuis la gestion du journal pour l'exporter vers un stockage externe.  
- En exécutant "Exporter la configuration" à partir du menu, "1.1.Données fournies par l'utilisateur à SMBSync3" sera exporté.  
Les informations seront cryptées en spécifiant un mot de passe lors de l'exportation.  

### 1.5.supprimer les données stockées dans SMBSync3  

En désinstallant SMBSync3, les données enregistrées ("1.1.Données fournies par l'utilisateur à SMBSync3", "1.2.Résultat de l'exécution de SMBSync3", "1.3.Fiche d'activité de SMBSync3") seront supprimées de l'appareil.  
<span style="color: red;"><u>Toutefois, les données stockées sur un support externe en raison de l'interaction de l'utilisateur ne seront pas supprimées.</u></span>  

### 2.les autorisations requises pour exécuter l'application.  

### 2.2.stockage  

### 2.2.1.Android11 ou plus récent.  
**Accès à tous les fichiers**.  

Accès à tous les fichiers** Utilisé pour la synchronisation et la gestion des fichiers en lecture/écriture.  

### 2.1.2.Android 10 ou avant  

#### 2.1.2.1.Photos, médias, fichiers  
**"read the contents of your USB storage"**  
**"modify or delete the contents of your USB storage"**  
Utilisé pour la synchronisation des fichiers et la lecture/écriture des fichiers de gestion.  

### 2.2.Informations sur la connexion Wi-Fi  
**voir les connexions Wi-Fi**.  
Utilisé pour vérifier l'état du Wi-Fi lorsque la synchronisation commence.  

### 2.3.Autres  
### 2.3.1.View network connections  
Utilisez cette option pour vérifier les connexions réseau lorsque la synchronisation est lancée.  
### 2.3.2.connect and disconnect from Wi-Fi  
Cette fonction est utilisée pour activer/désactiver le Wi-Fi pour la synchronisation programmée sur Andoid 8/9.  
### 2.3.3.Full network access  
Il est utilisé pour synchroniser via le protocole SMB à travers le réseau.  
### 2.3.4.Run at startup  
Utilisé pour effectuer une synchronisation programmée.  
### 2.3.5.Control vibration  
Ceci est utilisé pour notifier l'utilisateur lorsque la synchronisation est terminée.  
### 2.3.6.Prevent device from sleeping  
Utilisé pour démarrer la synchronisation à partir d'une planification ou d'une application externe.  
### 2.3.7.Install shortcuts  
Permet d'ajouter un raccourci de démarrage de la synchronisation sur le bureau.  
