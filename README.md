# Rail Pulse

Application Spring Boot avec Java 21, Maven, Thymeleaf et PostgreSQL/Supabase.

## IntelliJ IDEA

Ouvrir ce dossier comme projet et recharger Maven depuis `pom.xml` (Reload All Maven Projects).
Le SDK cible du projet est Java 21 ; configurer `JAVA_HOME` avec le chemin du JDK installé sur votre machine.
Lancer `RailPulseApplication` dans le package `altn72.railpulse`.
Le profil par défaut est `supabase`. Pour MySQL local, définir `SPRING_PROFILES_ACTIVE=mysql`. Pour Supabase, sélectionner **Rail Pulse - Supabase** ou définir `SPRING_PROFILES_ACTIVE=supabase`, puis configurer les identifiants PostgreSQL selon le [guide Supabase](docs/supabase.md).

## Compilation sous PowerShell

```powershell
# Adapter ce chemin à votre installation de Java 21.
$env:JAVA_HOME = 'C:\chemin\vers\jdk-21'
.\mvnw.cmd -DskipTests package
```

Pour Supabase, les valeurs sont renseignées directement dans `src/main/resources/application-supabase.properties`. Le mot de passe PostgreSQL reste à remplir. Pour le futur CI/CD, les variables standard `SPRING_DATASOURCE_PASSWORD`, `SPRING_DATASOURCE_URL` et `SPRING_DATASOURCE_USERNAME` peuvent remplacer ces valeurs.
Une clé API `sb_secret_*` ne remplace pas le mot de passe PostgreSQL.
Les tables JPA sont créées dans le schéma dédié `railpulse` ; les comptes et données de l'ancienne base MySQL ne sont pas automatiquement transférés.

La configuration locale avec le profil `mysql` utilise `localhost:3305/Base_ALTN72` et peut démarrer le service Windows `MySQL95` si nécessaire.
Les tests utilisent par défaut une base H2 temporaire en mode PostgreSQL et ne modifient ni Supabase ni MySQL : `.\mvnw.cmd test`.

## Connexion et déconnexion

Ouvrir `/login` et saisir le login et le mot de passe d'un compte de la table `users`.
Les profils possibles sont `AGENT` et `RESPONSABLE_EXPLOITATION`.
Une erreur d'identification réaffiche le formulaire avec un message ; une connexion réussie ouvre l'accueil.
Le bouton « Déconnectez-vous » ferme la session et revient à la page de connexion.

Les comptes existants avec un mot de passe en clair sont reconnus et leur mot de passe est remplacé par un hash BCrypt après une connexion réussie.
Les mots de passe BCrypt, avec ou sans préfixe `{bcrypt}`, sont aussi reconnus.
Le mécanisme utilise le [PasswordEncoder de Spring Security](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html).

## F2 — Équipements par station

Après connexion, cliquer sur **Consulter les équipements par station** depuis l'accueil, ou ouvrir `/stations`.
Choisir une station affiche `/stations/{id}/equipements` avec la référence, le libellé, le type et l'état de fonctionnement de ses seuls équipements.
Les états sont **En service**, **En panne** et **En maintenance**, affichés en toutes lettres et distingués par couleur.
Les profils `AGENT` et `RESPONSABLE_EXPLOITATION` ont accès en lecture, avec contrôle des rôles côté serveur.
Une station vide affiche un message explicite ; une station inexistante renvoie une page 404.

JPA crée les tables `station` et `equipement` au démarrage (`ddl-auto=update`).
La référence d'un équipement est unique dans tout le catalogue (RG9) et chaque équipement est lié à une station obligatoire.

Pour initialiser un catalogue de démonstration vide, ajouter le profil `demo` au profil de base :

```powershell
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=mysql,demo'
# Ou avec la connexion Supabase configurée :
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=supabase,demo'
```

Le profil `demo` crée trois stations et cinq équipements couvrant cinq types et les trois états ; une station reste sans équipement pour montrer ce cas.
L'initialisation est atomique et ne s'exécute que si les deux tables sont vides. Les relancements ne dupliquent ni ne remplacent le catalogue existant.
Sans le profil `demo`, aucune donnée métier n'est ajoutée automatiquement. Les utilisateurs de F1 sont conservés ; aucun compte ni mot de passe n'est créé par F2.
La fiche équipement et les incidents seront réalisés dans les fonctionnalités suivantes (F3 à F12).

Le développement F2 est isolé dans `feature/f2-equipements-par-station`. Les fonctionnalités suivantes pourront suivre le même nommage `feature/fN-description`, en partant de `master` après intégration de leurs dépendances.

## Qualité et tests

```powershell
$env:JAVA_HOME = 'C:\chemin\vers\jdk-21'
.\mvnw.cmd verify
```

Cette commande compile, exécute les tests isolés sur H2, construit le JAR et produit le rapport de couverture dans `target/site/jacoco/index.html`.
Les formulaires de programmeurs sont validés côté serveur : noms obligatoires, longueurs limitées et salaire facultatif non négatif.
Les erreurs de saisie conservent les valeurs du formulaire ; les erreurs HTTP utilisent une page commune sans détails techniques.

Le [journal du projet](journal.md) explique SOLID, les droits par profil, l'état du lot alpha, les règles, les tests et les limites de la vérification.
Les [notes du TP initial](docs/journal-tp5.md) sont archivées séparément.
