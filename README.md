# Rail Pulse

Application Spring Boot avec Java 21, Maven, Thymeleaf et PostgreSQL/Supabase.

## IntelliJ IDEA

Ouvrir ce dossier comme projet et recharger Maven depuis `pom.xml` (Reload All Maven Projects).
Le SDK du projet est Java 21, installé ici dans `C:\Users\vincc\.jdks\ms-21.0.12.1`.
Lancer `RailPulseApplication` dans le package `altn72.railpulse`.
Le profil par défaut est `mysql` pour permettre le lancement local habituel. Pour Supabase, sélectionner **Rail Pulse - Supabase** ou définir `SPRING_PROFILES_ACTIVE=supabase`, puis configurer les identifiants PostgreSQL selon le [guide Supabase](docs/supabase.md).

## Compilation sous PowerShell

```powershell
$env:JAVA_HOME = 'C:\Users\vincc\.jdks\ms-21.0.12.1'
.\mvnw.cmd -DskipTests package
```

Pour Supabase, renseigner `SUPABASE_DB_PASSWORD` et éventuellement `SUPABASE_DB_URL` et `SUPABASE_DB_USERNAME` via des variables d'environnement ou `config/supabase-local.properties` (ignoré par Git).
Une clé API `sb_secret_*` ne remplace pas le mot de passe PostgreSQL.
Les tables JPA sont créées dans le schéma dédié `railpulse` ; les comptes et données de l'ancienne base MySQL ne sont pas automatiquement transférés.

La configuration locale par défaut utilise le profil `mysql`, `localhost:3305/Base_ALTN72` et peut démarrer le service Windows `MySQL95` si nécessaire.
Les tests utilisent par défaut une base H2 temporaire en mode PostgreSQL et ne modifient ni Supabase ni MySQL : `.\mvnw.cmd test`.

## Connexion et déconnexion

Ouvrir `/login` et saisir le login et le mot de passe d'un compte de la table `users`.
Les profils possibles sont `AGENT` et `RESPONSABLE_EXPLOITATION`.
Une erreur d'identification réaffiche le formulaire avec un message ; une connexion réussie ouvre l'accueil.
Le bouton « Déconnectez-vous » ferme la session et revient à la page de connexion.

Les comptes existants avec un mot de passe en clair sont reconnus et leur mot de passe est remplacé par un hash BCrypt après une connexion réussie.
Les mots de passe BCrypt, avec ou sans préfixe `{bcrypt}`, sont aussi reconnus.
Le mécanisme utilise le [PasswordEncoder de Spring Security](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html).

## Qualité et tests

```powershell
$env:JAVA_HOME = 'C:\Users\vincc\.jdks\ms-21.0.12.1'
.\mvnw.cmd verify
```

Cette commande compile, exécute les tests isolés sur H2, construit le JAR et produit le rapport de couverture dans `target/site/jacoco/index.html`.
Les formulaires de programmeurs sont validés côté serveur : noms obligatoires, longueurs limitées et salaire facultatif non négatif.
Les erreurs de saisie conservent les valeurs du formulaire ; les erreurs HTTP utilisent une page commune sans détails techniques.

Le [journal du projet](journal.md) explique SOLID, les deux fonctionnalités transverses retenues, les règles, les tests et les limites de la vérification.
Les [notes du TP initial](docs/journal-tp5.md) sont archivées séparément.
