## Fonction
SMBSync3 est le stockage interne d'un appareil Android, MicroSD, USB Flash et PC/NAS via un réseau local sans fil utilisant les protocoles SMBv1, SMBv2 ou SMBv3. C'est un outil de synchronisation de fichiers. <u>**La synchronisation est unidirectionnelle de la source à la destination**</u> et peut être mise en miroir, déplacée, copiée ou archivée. Une combinaison de stockage local(<u>***1**</u>), SMB et ZIP est possible).  
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

## FAQs
https://sentaroh.github.io/Documents/SMBSync3/SMBSync3_FAQ_EN.htm

## Document
https://sentaroh.github.io/Documents/SMBSync3/SMBSync3_Desc_EN.htm