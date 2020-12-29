### Trouver et configurer le serveur SMB

Scannez le réseau et sélectionnez dans la liste des serveurs SMB à configurer. 

### Modifier les paramètres du serveur SMB

Définissez manuellement les paramètres du serveur SMB. 

### Nom de l'hôte du serveur/adresse IP

Pour spécifier le nom du serveur SMB ou l'adresse IP 

### Protocole SMB

Vous pouvez spécifier le protocole SMB.

- Utilisez SMB V1

- Utiliser SMB V2/3

### Utiliser le numéro de port

Précisez si le numéro de port de la norme n'est pas disponible. Le numéro de port de la norme est 139/tcp et 445/tcp. 

### Utiliser le nom de compte et le mot de passe

Il précise s'il faut utiliser le nom de compte et le mot de passe du serveur SMB

Le nom du compte est un compte local sur le serveur SMB. <span style="color : red ;"><u>Le compte Microsoft n'est pas disponible.</u></span> 

### Nom du compte

Pour spécifier le nom de compte pour le serveur SMB. 

### Mot de passe

Pour spécifier le mot de passe du serveur SMB. 

### List share

Afficher le nom de partage pour le serveur SMB.  

### Liste des répertoires

Afficher la liste du répertoire des serveurs SMB.  

### Modifier les paramètres du nom du répertoire

La date et l'heure **<u> peuvent être incluses dans le répertoire cible</u>**. Les variables sont converties en date lorsque la synchronisation commence. Veuillez confirmer les détails de la variable en appuyant sur "Modifier le paramètre du nom du répertoire". 

### Répertoire

Si le répertoire cible n'existe pas, il sera créé au moment de la synchronisation.

Note : Dans les conditions suivantes, il devient une référence circulaire et des boucles. Spécifier un filtre de répertoire ou spécifier un répertoire du côté maître qui est différent de la cible.

- Aucun répertoire n'est spécifié lorsque le même serveur SMB est spécifié pour le maître et la cible

- Aucun filtre de répertoire spécifié

### <u> Ce qui suit n'est affiché que lorsque le type de synchronisation est Archive.</u>

### Enregistrer  tous les fichiers dans le répertoire de destination sans créer de  sous-répertoires

Si cette case est cochée, le répertoire de destination ne créera pas de sous-répertoire dans le répertoire source.

### Pour archiver le

Sélectionnez un fichier dont la date et l'heure de tournage sont antérieures à la date et à l'heure d'exécution de l'archive. (Indépendamment de la date et de l'heure de tournage, la date de tournage est de 7 jours ou plus, la date de tournage est de 30 jours ou plus, la date de tournage est de 60 jours ou plus, la date de tournage est de 90 jours ou plus, la date de tournage est de 180 jours ou plus, la date de tournage est de Vous pouvez choisir parmi des fichiers datant de plus d'un an) 

### Incrémenter  les noms de fichiers

Vous pouvez ajouter un numéro de séquence au nom du fichier. 

### Modifier le paramètre du nom de fichier

Pour inclure la date et l'heure dans le nom du fichier, appuyez sur le bouton et modifiez.