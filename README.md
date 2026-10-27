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
Les fonctionnalités F5 à F12 restent à réaliser.

Le développement F2 est isolé dans `feature/f2-equipements-par-station`. Les fonctionnalités suivantes pourront suivre le même nommage `feature/fN-description`, en partant de `master` après intégration de leurs dépendances.

## F3 — Fiche d'un équipement

Cliquer sur la référence d'un équipement dans la liste F2 ouvre `/equipements/{id}`.
La fiche présente sa référence, son libellé, sa station, son type, son état de fonctionnement et tous ses incidents, en cours comme résolus.
Les incidents sont triés par date d'ouverture décroissante, puis par identifiant décroissant en cas d'égalité (RG7).
Chaque ligne affiche la description, l'ouverture, le statut, le prestataire éventuel et la résolution éventuelle. Les horaires sont affichés en UTC.
Les deux profils ont accès en lecture ; une fiche absente renvoie 404 et une fiche sans incident affiche un message explicite.

Sur un catalogue vide, le profil `demo` ajoute aussi un prestataire et cinq incidents non affectés, affectés ou résolus, avec des dates relatives à une horloge UTC : notamment 47 h, 48 h et 72 h.
Un catalogue déjà renseigné est conservé ; activer `demo` ne réinjecte pas ces données sur une base F2 existante.
Le modèle contient les transitions nécessaires à ces données cohérentes ; les écrans d'affectation/résolution (F7/F8) et le détail d'incident (F5) restent à réaliser.

Branche F3 : `feature/f3-fiche-equipement`.

## F4 — Déclarer un incident

Depuis la fiche d'un équipement, cliquer sur **Déclarer un incident**, saisir une description puis valider.
Les deux profils disposent de cette action. La description est obligatoire, non blanche et limitée à 2000 caractères côté navigateur et côté serveur.
Une saisie invalide conserve le formulaire et les valeurs, affiche un message français et n'écrit rien en base.

Le POST `/equipements/{id}/incidents` utilise uniquement la description : l'équipement vient de l'URL, l'ouverture est calculée par l'horloge UTC du serveur, le statut initial est **Non affecté**, sans prestataire ni date de résolution (RG1/RG2).
Une réussite redirige vers la fiche avec un message de confirmation et le nouvel incident dans la liste ; rafraîchir cette fiche ne répète pas la déclaration.
La connexion et un jeton CSRF sont requis. Les identifiants, dates, statuts et prestataires ajoutés artificiellement au formulaire ne sont pas liés au modèle.

La déclaration ne change pas automatiquement l'état de fonctionnement du catalogue : un signalement peut décrire une anomalie sans arrêt complet. Plusieurs incidents d'un même équipement restent possibles ; le sujet ne prescrit pas de restriction supplémentaire.
Branche F4 : `feature/f4-declaration-incident`, basée sur F3. F3 est intégrée en premier ; la PR F4 vise `master` et contient uniquement la déclaration d'incident.

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

## Déploiement Render

Créer un **Web Service** avec **Language = Docker** et suivre le [guide Render](docs/render.md) pour les champs et les variables Supabase.
Le `Dockerfile` exécute `mvn verify` sous Java 21 avant de construire l'image d'exécution. Le profil `render` configure le port fourni par l'hébergeur et les sessions HTTPS.
La branche `feature/deploiement-render` contient cette configuration et F2. Les identifiants de base se renseignent dans les variables d'environnement de Render.
