<h1 align="center">Bienvenue sur HAI913I_AST</h1>
<p>
</p>

> HAI913I_AST est une application d’analyse statique permettant de visualiser et d’explorer la structure interne de vos projets Java à travers un modèle Abstract Syntax Tree (AST) basé sur Eclipse JDT.

🏠 Page d'accueil: https://github.com/FeyzaKdnz/HAI913I_AST

✨ Démonstration: https://drive.google.com/file/d/154OMfOpobjVjwUedxiKdC0xOF471ZbGJ/view?usp=sharing

## Usage

Prérequis:

- Java 17 ou supérieur (recommandé pour compatibilité JavaFX et JDT).
Pour vérifier la version:
```sh
java --version
```
- Maven (pour la compilation et la gestion des dépendances). Pour vérifier la version:
```sh
mvn -v
```
- Un IDE compatible: Eclipse, IntelliJ IDEA, ..

## Installation

1. Cloner le dépôt:
```sh
https://github.com/FeyzaKdnz/HAI913I_AST.git
```
2. Déplacez vous sur le répertoire qui contient le projet
```sh
cd HAI913I_AST
```
3. Compiler et lancer l'application:
```sh
mvn clean install
mvn javafx:run
```

## Utilisation

Depuis l'interface , cliquer sur "Choisir projet" pour sélectionner un dossier source que vous souhaitez analyser. 

Le chemin devrait s'afficher après avoir sélectionné votre dossier.

Cliquer sur "Analyser" pour lancer l'analyse.

Vous pouvez visualiser le graphe en cliquant sur "Afficher le graphe".

## Author
Feyza Karadeniz