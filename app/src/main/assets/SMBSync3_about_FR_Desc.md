## Fonction  
SMBSync3 est un outil permettant de synchroniser des fichiers entre le stockage interne de l'appareil Android, la carte MicroSD, la clé USB et le PC/NAS via un réseau local sans fil en utilisant le protocole SMB1, SMB2 ou SMB3. <span style="color : red ;"><u>La synchronisation est unidirectionnelle</u></span> du dossier source au dossier de destination et peut être mise en miroir, déplacée, copiée ou archivée.  
La synchronisation périodique peut être initiée par la fonction de planification de SMBSync3 ou par des applications externes (Tasker, AutoMagic, etc.).  
- Miroir  
Le répertoire et les fichiers sources sont copiés de manière différentielle (<u>***1**</u>) vers la destination, et une fois la copie terminée, les fichiers et répertoires qui n'existent pas du côté source sont supprimés.  
- Déplacement  
Le répertoire et les fichiers sources sont copiés de manière différentielle (<u>***1**</u>) vers le côté destination, et les fichiers du côté source sont supprimés lorsque la copie est terminée. (Toutefois, le fichier ayant le même nom, la même taille et la même date de modification sont les mêmes du côté source et du côté destination, et le fichier n'est pas copié, et le répertoire source et le fichier sont supprimés une fois la copie terminée. côté du fichier).  
- Copie  
Delta-copie(<u>***1**</u>) les fichiers contenus dans le répertoire source vers la destination.  
- Archive  
Photos et vidéos contenues dans le répertoire source avec une date et une heure à 7 jours de la date d'exécution de l'archive Aller à la destination avant ou avant 30 jours, etc. (Mais vous ne pouvez pas utiliser ZIP pour la destination).  

<u>***1**</u> Le fichier de différence est l'une des trois conditions suivantes.  

1. Le fichier n'existe pas  
2. Différentes tailles de fichiers  
3. Différent de la dernière mise à jour 3 secondes  

### Manuels  
[FAQs](https://sentaroh.github.io/Documents/SMBSync3/SMBSync3_FAQ_EN.htm)  
[Description](https://sentaroh.github.io/Documents/SMBSync3/SMBSync3_Desc_EN.htm)  
