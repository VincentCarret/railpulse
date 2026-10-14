# Rail Pulse

Application Spring Boot 4 / Java 21 avec Thymeleaf, Spring Data JPA et MySQL. Le code utilise toujours la base MySQL locale ; aucune connexion Supabase n'est activée.

## Développement

Avec un JDK 21, ouvrir le projet Maven dans IntelliJ IDEA et lancer `RailPulseApplication`. Sous Windows, l'application essaie de démarrer le service `MySQL95` si la base locale ne répond pas. Par défaut, elle utilise `localhost:3305`, la base `Base_ALTN72`, l'utilisateur `root` et le mot de passe défini par `DB_PASSWORD` (ou `root`).

```powershell
.\mvnw.cmd verify
```

Les tests utilisent H2 en mémoire localement. Dans GitHub Actions, ils s'exécutent sur un MySQL temporaire grâce aux variables `SPRING_DATASOURCE_*` du workflow.

## CI / CD vers un VPS

Le workflow [`.github/workflows/ci-cd.yml`](.github/workflows/ci-cd.yml) compile, teste et publie le JAR comme artefact à chaque push et pull request. Si la variable de dépôt GitHub `DEPLOY_ENABLED` vaut `true`, un push sur `master` copie le JAR sur le VPS et redémarre le service systemd.

Préparer le VPS Linux avec Java 21 et systemd :

1. Créer un utilisateur `railpulse` avec accès SSH par clé et un répertoire `/opt/railpulse` dont il est propriétaire.
2. Créer `/etc/railpulse/railpulse.env` (propriétaire `root`, permissions `600`) avec les paramètres de la base accessible depuis le VPS. Pour la configuration MySQL actuelle :

```text
SPRING_DATASOURCE_URL=jdbc:mysql://<hote-mysql>:3306/Base_ALTN72
SPRING_DATASOURCE_USERNAME=<utilisateur-mysql>
DB_PASSWORD=<mot-de-passe-mysql>
SERVER_PORT=8080
SERVER_ADDRESS=127.0.0.1
```

3. Copier [`deploy/railpulse.service`](deploy/railpulse.service) dans `/etc/systemd/system/railpulse.service`, puis lancer `sudo systemctl daemon-reload` et `sudo systemctl enable railpulse`. Vérifier le chemin de Java (`/usr/bin/java`) dans le service. Configurer un proxy HTTP vers le port 8080 si l'application doit être accessible depuis Internet.
4. Autoriser l'utilisateur SSH `railpulse` à exécuter uniquement `sudo /usr/bin/systemctl restart railpulse` sans mot de passe, avec `sudo visudo -f /etc/sudoers.d/railpulse` :

```text
railpulse ALL=(root) NOPASSWD: /usr/bin/systemctl restart railpulse
```

5. Ajouter les secrets GitHub `VPS_HOST`, `VPS_USER`, `VPS_SSH_KEY`, `VPS_KNOWN_HOSTS` et éventuellement `VPS_PORT`. Vérifier l'empreinte de la clé SSH du VPS avant de remplir `VPS_KNOWN_HOSTS`.
6. Quand le VPS est prêt, définir `DEPLOY_ENABLED=true` dans les variables GitHub et pousser sur `master`.

Le service lit ses variables dans `/etc/railpulse/railpulse.env`. Ce mécanisme pourra aussi recevoir plus tard l'URL, l'utilisateur et le mot de passe PostgreSQL de Supabase, sans placer de secrets dans GitHub Actions. Pour utiliser réellement Supabase, il faudra d'abord ajouter le pilote PostgreSQL et adapter la configuration de la base de l'application. Le pipeline actuel ne fait aucune opération sur Supabase.

L'application ne possède pas encore d'authentification ; protéger son accès avant de l'ouvrir publiquement.
