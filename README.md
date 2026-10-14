# Rail Pulse

Application Spring Boot avec Java 21, Maven, Thymeleaf et MySQL.

## IntelliJ IDEA

Ouvrir ce dossier comme projet et recharger Maven depuis `pom.xml` (Reload All Maven Projects).
Le SDK du projet est Java 21, installé ici dans `C:\Users\vincc\.jdks\ms-21.0.12.1`.
Lancer `RailPulseApplication` dans le package `altn72.railpulse`.
Sous Windows, l'application démarre automatiquement le service `MySQL95` si la base locale ne répond pas encore. Windows peut demander une confirmation administrateur pour démarrer le service. Si MySQL est déjà disponible, l'application se connecte directement.

## Compilation sous PowerShell

```powershell
$env:JAVA_HOME = 'C:\Users\vincc\.jdks\ms-21.0.12.1'
.\mvnw.cmd -DskipTests package
```

Pour lancer l'application, MySQL doit être disponible sur `localhost:3305`, avec la base `Base_ALTN72` et les identifiants configurés dans `application.properties`. La variable `DB_PASSWORD` permet de définir le mot de passe.
Les tests utilisent une base H2 temporaire et ne modifient pas MySQL : `.\mvnw.cmd test`.

## Connexion et déconnexion

Ouvrir `/login` et saisir le login et le mot de passe d'un compte de la table `users`.
Les profils possibles sont `AGENT` et `RESPONSABLE_EXPLOITATION`.
Une erreur d'identification réaffiche le formulaire avec un message ; une connexion réussie ouvre l'accueil.
Le bouton « Déconnectez-vous » ferme la session et revient à la page de connexion.

Les comptes existants avec un mot de passe en clair sont reconnus et leur mot de passe est remplacé par un hash BCrypt après une connexion réussie.
Les mots de passe BCrypt, avec ou sans préfixe `{bcrypt}`, sont aussi reconnus.
Le mécanisme utilise le [PasswordEncoder de Spring Security](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html).
