## Envoyer des données enregistrées à partir de l'application
Les données enregistrées par l'application peuvent être envoyées à l'extérieur par courrier électronique et par des outils de partage grâce aux opérations suivantes de l'application. <span style="color : red ;"><u>L'application n'enverra pas les données enregistrées en externe à moins que l'utilisateur ne le fasse.</u></span>
- Appuyez sur le bouton "Partager" de l'onglet "Historique
- Appuyez sur le bouton "Envoyer au développeur" à partir des informations du système
- Appuyez sur le bouton "Partager" ou "Envoyer au développeur" depuis la gestion du journal

## 2.données enregistrées par l'application
### 2.1.Liste des tâches de synchronisation
L'application enregistre les données nécessaires pour effectuer la synchronisation.
- Nom de répertoire, nom de fichier, nom d'hôte du serveur SMB, adresse IP, numéro de port, nom de compte (***1**), mot de passe (***1**)
- Mot de passe de l'application (***1**) pour protéger le lancement de l'application et la modification des paramètres
- Valeur de réglage de l'application

***1** Chiffré avec une clé générée par le système et stockée dans AndroidKeystore.

### 2.2.Enregistrement de l'activité de l'application
L'application enregistrera les données suivantes lorsque vous activerez la journalisation pour vérifier et dépanner les résultats de la synchronisation.
- Version Android, fabricant du terminal, nom du terminal, modèle du terminal, version de l'application
- Nom du répertoire, nom du fichier, taille du fichier, date de la dernière modification du fichier
- Nom d'hôte du serveur SMB, adresse IP, numéro de port, nom de compte
- Nom de l'interface réseau, adresse IP
- Valeur de paramétrage du système (politique de veille WiFi, informations sur le stockage)
- Valeur de réglage de l'application
### 2.3.Synchroniser la liste des tâches exportées
L'application peut exporter "2.1 Liste de tâches de synchronisation" vers un fichier. Vous pouvez protéger l'exportation par un mot de passe.
- Nom du répertoire, nom du fichier
- Nom d'hôte du serveur SMB, adresse IP, numéro de port, nom de compte, mot de passe
- Valeur de réglage de l'application 
## 3.But de l'utilisation des données envoyées aux développeurs d'applications
Les données envoyées par les utilisateurs de l'application au développeur ne seront utilisées que pour résoudre les problèmes de l'application et ne seront pas divulguées à d'autres personnes que le développeur.

## 4.Autorisations
L'application nécessite les autorisations suivantes.
### 4.1.Photos/Media/Files
**read the contents of your USB storage  
modify or delete the contents of your USB storage**  
Utilisé pour la synchronisation des fichiers avec le stockage interne/externe et la lecture/écriture du fichier de gestion.

### 4.2.Storage

4.2.1. Android 11 or later
**All file access**

Utilisé pour synchroniser les fichiers vers et depuis le stockage interne et externe et pour lire et écrire les fichiers de gestion.

4.2.1. up to Android 10
**read the contents of your USB storage  
modify or delete the contents of your USB storage**  
Utilisé pour synchroniser les fichiers vers et depuis le stockage interne et externe et pour lire et écrire les fichiers de gestion.

### 4.3.Wi-Fi Connection infomation
**view Wi-Fi connections**  
Utilisé pour vérifier l'état du Wi-Fi au début de la synchronisation.

### 4.4.Autre
### 4.4.1.view network connections
Utilisé pour confirmer qu'il est connecté au réseau au début de la synchronisation.
### 4.4.2.connect and disconnect from Wi-Fi
Utilisé pour activer / désactiver le Wi-Fi dans la synchronisation des horaires dans Andoid 8/9.
### 4.4.3.full network access
Utilisé pour effectuer la synchronisation avec le protocole SMB à travers le réseau.
### 4.4.4.run at startup
Utilisé pour effectuer la synchronisation des horaires.
### 4.4.5.control vibration
Utilisé pour avertir l'utilisateur à la fin de la synchronisation.
### 4.4.6.prevent device from sleeping
Utilisez-le pour démarrer la synchronisation à partir d'un horaire ou d'une application externe.
### 4.4.7.install shortcuts
Utilisé pour ajouter un raccourci de démarrage de la synchronisation sur le bureau.
