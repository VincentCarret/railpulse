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
Les tests de contexte nécessitent aussi cette base : `.\mvnw.cmd test`.
