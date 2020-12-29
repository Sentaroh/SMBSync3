## Fonction

SMBSync3 est le stockage interne d'un appareil Android, MicroSD, USB Flash et PC/NAS via un réseau local sans fil utilisant les protocoles SMBv1, SMBv2 ou SMBv3. C'est un outil de synchronisation de fichiers. 

<u>**La synchronisation est unidirectionnelle de la source à la destination**</u> et peut être mise en miroir, déplacée, copiée ou archivée. Une combinaison de stockage local(<u>***1**</u>), SMB et ZIP est possible).  

La synchronisation périodique peut être initiée par la fonction de planification de SMBSync3 ou par des applications externes (Tasker, AutoMagic, etc.).

- Miroir

  Le répertoire et les fichiers sources sont copiés en delta (<u>***2**</u>) vers la destination, et une fois la copie terminée, les fichiers et répertoires qui n'existent pas du côté source sont supprimés.

- Déplacement

  Le répertoire source et les fichiers sont copiés en delta(<u>***2**</u>) vers le côté destination, et les fichiers du côté source sont supprimés lorsque la copie est terminée (cependant, le fichier avec le même nom, la taille du fichier et la date de modification sont les mêmes dans le côté source et le côté destination, et le fichier n'est pas copié, et le répertoire source et le fichier sont supprimés lorsque la copie est terminée. du fichier).

- Copie

  Delta-copie(<u>***2**</u>) les fichiers contenus dans le répertoire source vers la destination.

- Archive

  Photos et vidéos contenues dans le répertoire source avec une date et une heure à 7 jours de la date d'exécution de l'archive Aller à la destination avant ou avant 30 jours, etc. (Mais vous ne pouvez pas utiliser ZIP pour la destination).

<u>***1**</u> Le stockage local peut être soit un stockage interne, soit une carte MicroSD ou une clé USB. 

<u>***2**</u> Le fichier de différence est l'une des trois conditions suivantes.  

1. Le fichier n'existe pas  
2. Différentes tailles de fichiers  
3. Différent de la dernière mise à jour 3 secondes

S'il n'est pas permis de modifier l'heure de la dernière mise à jour du fichier par la demande, l'heure de la dernière mise à jour du fichier est enregistrée dans le fichier de gestion et elle est utilisée pour juger le fichier de différence. Par conséquent, si vous copiez un fichier autre que SMBSync3 ou s'il n'y a pas de fichier de gestion, le fichier sera copié.

## FAQs

[Veuillez vous référer au PDF](https://drive.google.com/file/d/1v4-EIWuucUErSg9uYZtycsGGn9o-T_2t/view?usp=sharing)

## Document

[Veuillez vous référer au PDF](https://drive.google.com/file/d/1gIsulxyGBY-Fl0Ki7BJ50gPFWx0iQ9Tm/view?usp=sharing)

## Bibliothèque

- [jcifs-ng ClientLibrary](https://github.com/AgNO3/jcifs-ng)
- [jcifs-1.3.17](https://jcifs.samba.org/)
- [Zip4J 2.2.3](http://www.lingala.net/zip4j.html)
- [juniversalchardet-1.0.3](https://code.google.com/archive/p/juniversalchardet/)
- [Metadata-extractor](https://github.com/drewnoakes/metadata-extractor)
