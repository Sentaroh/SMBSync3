### Mode test

Il n'effectue pas l'écrasement et la suppression du fichier si vous vérifiez. Veuillez être testé lorsque vous créez une tâche de synchronisation, les fichiers qui sont copiés ou supprimés se trouvent dans l'onglet message.

### Synchronisation  Auto

Si la tâche a été vérifiée, elle est passée à l'automatique. Les tâches qui sont réglées sur la synchronisation automatique commenceront lorsque vous appuierez sur le bouton de synchronisation.

### Nom de la tâche

Précisez le nom de la tâche.

### Mode de sync
Choisissez une méthode parmi les suivantes : miroir, copie, déplacement et archivage. <span style="color : red ;"><u>La synchronisation se fait dans un sens, du dossier source au dossier de destination.</u></span>   
- Miroir  
  Faites une copie différentielle (**<u>*1</u>**) des répertoires et des fichiers du côté source vers le côté destination, et supprimez les fichiers et les répertoires du côté destination qui n'existent pas du côté source une fois la copie terminée.

- Déplacer  

  Faites une copie différentielle du répertoire et du fichier côté source vers le côté destination, et supprimez le fichier côté source copié vers le côté destination. Toutefois, le fichier portant le même nom que la source et la destination mais ayant la même taille et la même date de modification n'est pas copié et le fichier du côté source est supprimé.

- Copie  
  Faites une copie différentielle des fichiers contenus dans le répertoire source vers le côté destination.

- Archive  
  Déplacer les photos et vidéos contenues dans le répertoire source vers la destination à condition que la date et l'heure de tournage soient antérieures à 7 jours ou 30 jours à la date et l'heure d'exécution de l'archive. (Toutefois, le ZIP ne peut pas être utilisé comme destination).

**<u>*1</u>** Si l'une des trois conditions suivantes est remplie, le fichier est considéré comme un fichier de différence et est copié ou déplacé. Cependant, la taille du fichier et le moment de la dernière modification peuvent être ignorés dans les options de la tâche de synchronisation.  

1. le fichier n'existe pas
2. la taille du fichier est différente
3. la date et l'heure de la dernière modification sont différentes de plus de 3 secondes (le nombre de secondes peut être modifié par l'option de la tâche de synchronisation)

### Inverser  les dossiers source et destination
Échanger le contenu du dossier source et du dossier de destination 

### Dossier source

Appuyez sur le bouton pour modifier le dossier source

### Dossier de destination

Appuyez sur le bouton pour modifier le dossier de destination

### Sélectionner les fichiers à synchroniser
Si elle n'est pas cochée, tous les fichiers seront synchronisés. Si coché, vous pouvez sélectionner les fichiers à synchroniser.
##### 		<u>Filtre sur les noms de fichiers</u>

- Synchro des fichiers audio

  Une fois vérifiée, la synchronisation inclura les fichiers avec les extensions suivantes :  
  aac, aif, aifc, aiff, flac, kar, m3u, m4a, mid, midi, mp2, mp3, mpga, ogg, ra, ram, wav

- Synchro des fichiers images

  Une fois vérifiée, la synchronisation inclura les fichiers avec les extensions suivantes :  
  bmp, cgm, djv, djvu, gif, ico, ief, jpe, jpeg, jpg, pbm, pgm, png, pnm, ppm, ras, rgb, svg, tif, tiff, wbmp, xbm, xpm, xwd

- Synchro des fichiers vidéo

  Une fois vérifiée, la synchronisation inclura les fichiers avec les extensions suivantes :  
  avi, m4u, mov, mp4, movie, mpe, mpeg, mpg, mxu, qt, wmv 

- Filtre de fichiers

  Il s'agit d'un filtre personnalisé d'inclusion/exclusion de fichiers. Vous pouvez sélectionner le nom et l'extension des fichiers que vous voulez exclure ou inclure du processus de synchronisation. 

##### 		<u>Filtre sur la taille des fichiers</u>

Vous pouvez choisir les fichiers à synchroniser en fonction de leur taille.  

- Moins de / Plus de  
  Vous pouvez spécifier n'importe quelle taille de fichier.  
  
##### <u>Filtre de la date de dernière modification du fichier</u>

Vous pouvez sélectionner des fichiers dont la date de dernière modification est antérieure ou inférieure à xx jours à la date de début de la synchronisation.
- Si vous indiquez 0 jours avant, la dernière date de mise à jour sera sélectionnée uniquement pour la date de début de la synchronisation.
- Si vous spécifiez 1 jour avant, vous sélectionnez les fichiers dont la date de dernière modification se situe entre la date de début et le jour précédant la date de début.
- Si vous spécifiez Plus de 1 jour, les fichiers dont la date de dernière modification est antérieure au jour précédant le début de la synchronisation seront sélectionnés.  

### Sélectionner les sous-dossiers

Si vous ne vérifiez pas et synchronisez ensuite tous les sous-répertoires. Si vous vérifiez que le bouton de filtrage des répertoires s'affiche.

- Filtre de répertoire
  Vous pouvez sélectionner le nom du répertoire que vous souhaitez synchroniser.
### Démarrer la synchronisation uniquement pendant la charge

Si la case est cochée, vous ne pouvez démarrer la synchronisation que pendant la charge. Si la synchronisation est lancée alors que vous n'êtes pas en train de charger, cela entraînera une erreur.

### Synchroniser  les fichiers situés dans la racine du dossier source 

si elle n'est pas cochée, seuls les dossiers et leurs fichiers/sous-dossiers sont synchronisés

### Confirmer  avant de remplacer/supprimer

Il affichera un dialogue de confirmation lorsque vous voudrez écraser et supprimer le fichier si vous avez coché.

### Options d'erreur  

Vous pouvez spécifier le comportement à adopter lorsqu'une erreur se produit.
- Arrêter la synchronisation
- Ignorez  toutes les erreurs et commencez les tâches suivantes  
  Utilisez cette option si vous voulez vous assurer que les tâches suivantes sont exécutées. 
- Commencer  les tâches suivantes même si elles ne correspondent pas aux options du réseau  
  Utilisez cette fonction si vous souhaitez exécuter des tâches ultérieures lorsque l'adresse n'est pas privée ou lorsqu'elle n'est pas l'adresse IP spécifiée.  

### Réseau

- Même si  Wifi éteint  
Vous pouvez toujours commencer à synchroniser
- Lorsqu'il est connecté à l'AP  
La synchronisation peut démarrer si le réseau local sans fil est connecté à un point d'accès quelconque.
- Adresse IP privée uniquement  
La synchronisation peut être lancée lorsque l'adresse IP est une adresse privée
- Enregistré dans la liste des adresses IP  
Vous pouvez lancer la synchronisation lorsque l'adresse IP est enregistrée dans la liste des adresses IP.  
  Vous pouvez utiliser des jokers pour le filtre. (par exemple : 192.168.100.\*, 192.168.\*.\*.)

### Autoriser  la connexion aux addresses IP publiques(inclure le public)  

Permet la synchronisation sur toutes les adresses IP. Cependant, le balayage du serveur SMB ne peut pas être effectué.
### Affiche les options avancées

**Veuillez l'utiliser lors de la définition des options détaillées.**
### Inclure les sous-répertoires
Il inclura récursivement les sous-répertoires sous le dossier source spécifié. 

### Inclure les répertoires vides
Synchronise les répertoires vides (même si un répertoire est vide sur la source, il sera créé sur la destination). Si elle n'est pas cochée, les répertoires vides sur la source sont ignorés. 

### Inclure les répertoires cachés
Lorsqu'elle est cochée, Sync inclura les dossiers linux cachés (ceux dont le nom commence par un point). Notez que dans Windows et Samba, l'attribut caché n'est pas défini par le nom du dossier. Ainsi, le dossier synchronisé sur la destination SMB/Windows n'aura pas l'attribut caché host. 

### Inclure les fichiers cachés
Lorsqu'il est vérifié, Sync inclura les fichiers linux cachés (ceux dont le nom commence par un point). Notez que dans Windows et Samba, l'attribut caché n'est pas défini par le nom du fichier. Ainsi, le fichier synchronisé sur la destination SMB/Windows n'aura pas l'attribut caché de l'hôte.

### Écraser les fichiers de destination
S'ils ne sont pas cochés, les fichiers sur la destination ne seront jamais écrasés, même si les critères de comparaison par taille et par heure sont différents. 

### Réessayer en cas d'erreur de réseau (uniquement pour les partages SMB)
En cas d'erreur de connexion côté serveur, SMBSync3 réessaiera la synchronisation au maximum 3 fois à un intervalle de 30 secondes. 

### Limiter le tampon d'écriture des E/S SMB à 16KB (uniquement pour les partages SMB)
Veuillez essayer si vous obtenez une erreur "Access is denied" en écrivant dans le dossier PC/NAS. Lorsqu'il est vérifié, il limite le tampon d'entrée/sortie à 16KB pour les opérations d'écriture sur l'hôte SMB. 

### Effacer les fichiers avant la synchronisation (mode miroir uniquement)

Lorsqu'elle est cochée, les répertoires et les fichiers qui sont présents sur le dossier de destination mais qui n'existent pas sur la source, seront d'abord supprimés. Ensuite, les fichiers et les dossiers qui sont différents seront copiés dans le dossier de destination.
Si le dossier source est SMB, le temps de traitement sera plus long car la structure des répertoires et leur contenu sont scannés par le réseau. Il est fortement recommandé d'activer l'option "Utiliser la négociation SMB2" car SMB1 sera très lent.

### Supprimer les répertoires et les fichiers exclus par les filtres

Si elle est activée, **elle supprime les répertoires/fichiers qui sont exclus du filtre.** 

### Ne pas régler l'heure de la dernière modification du fichier de destination pour qu'elle corresponde à celle du fichier source

Veuillez l'activer si vous obtenez une erreur du type SmbFile#setLastModified()/File#setLastModified() fails. Cela signifie que l'hôte distant n'autorise pas le paramétrage du fichier de la dernière modification. Si cette case n'est pas cochée, la dernière heure de modification du fichier copié sur la destination sera fixée à l'heure à laquelle il a été copié / synchronisé. Cela signifie que le fichier de destination apparaîtra plus récent que la source. 

### Utiliser la taille du fichier pour déterminer si les fichiers sont différents

Lors de la vérification, les fichiers sont considérés comme différents s'ils diffèrent par leur taille. 
### Comparaison des tailles uniquement
Les fichiers ne sont considérés comme différents que si la taille de la source est supérieure à celle de la destination. Cela désactivera la comparaison par temps de fichier. 

### Utiliser l'heure de la dernière modification pour déterminer si les fichiers sont différents 
Lors de la vérification, les fichiers sont considérés comme différents en fonction de leur dernière date de modification 

### Différence de temps minimale autorisée (en secondes) entre les fichiers source et destination
Les fichiers sont considérés comme identiques si la différence entre leurs derniers temps modifiés est inférieure ou égale au temps sélectionné en secondes. Ils sont considérés comme différents si la différence de temps entre les fichiers est supérieure à l'heure sélectionnée. Les fichiers FAT et ExFAT ont besoin d'une tolérance minimale de 2 secondes. Si 0 seconde est sélectionnée, les fichiers doivent avoir exactement le même temps pour être considérés comme similaires.

### N'écrasez pas le fichier de destination s'il est plus récent que le fichier source
Si la case est cochée, le fichier ne sera écrasé que si le fichier source est plus récent que le fichier de destination, même si la taille du fichier et les heures de la dernière mise à jour sont différentes. Gardez à l'esprit que si vous changez de fuseau horaire ou si les fichiers sont modifiés pendant la période d'intervalle du changement d'heure d'été, le dernier fichier modifié pourrait apparaître plus ancien que le fichier non mis à jour. Ceci est lié aux différences de système de fichiers et seule une vérification manuelle avant d'écraser le fichier évitera la perte de données. Il est généralement recommandé de ne pas modifier les fichiers pendant l'intervalle de changement d'heure d'été s'ils sont destinés à être auto-synchronisés. 

### Ignorer l'heure d'été Différence de temps entre les fichiers
Permet de sélectionner le décalage horaire en minutes entre l'heure d'été et l'heure d'hiver. Les fichiers sont considérés comme différents si le décalage horaire n'est pas exactement égal à l'intervalle spécifié (+/- le "décalage horaire minimum autorisé (en secondes)" spécifié dans l'option précédente)

### Sauter les noms de répertoires et de fichiers qui contiennent des caractères non valides (", :, \, *, <, >, |)
Si elle est cochée, elle affichera un message d'avertissement et la synchronisation se poursuivra sans traiter les répertoires/fichiers contenant des caractères non valides. 

### Supprimez le répertoire source lorsqu'il est vide (uniquement lorsque l'option de synchronisation est Déplacer)
Lorsque le mode de synchronisation est "Déplacement", après que les fichiers aient été déplacés vers la destination, le dossier Source est également supprimé.  

### Si la date et l'heure ne peuvent pas être déterminées par les données EXIF, un message de confirmation s'affiche
Afficher un message de confirmation lorsque la date et l'heure de la prise ne peuvent être obtenues auprès d'Exif.

### Ignorer les fichiers sources de plus de 4 Go lors de la synchronisation avec le stockage externe.
Si cette case est cochée, vous pouvez éviter les erreurs d'entrée/sortie lors de la synchronisation avec une carte MicroSD en ignorant les fichiers sources de plus de 4 Go à synchroniser avec le stockage local.

### Ignorer les fichiers dont le nom dépasse 255 octets
Si cette option est cochée, ignorez les fichiers dont le nom dépasse 255 octets.

### Ignorer les fichiers d'une taille de 0 octet  
Si cette option est cochée, les fichiers d'une taille de 0 octet seront ignorés.  
