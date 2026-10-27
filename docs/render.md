# Déployer Rail Pulse sur Render

## Service

- Type : **Web Service** ; dépôt `VincentCarret/railpulse`.
- Language : **Docker** ; branche : `master` après revue et intégration de la PR.
- Root Directory : vide ; Dockerfile Path : `./Dockerfile` ; Docker Command : vide.
- Choisir une région proche de Supabase et le forfait souhaité (Free possible pour la démonstration).
- Health Check Path : `/login` ; **Auto-Deploy à After CI Checks Pass** pour attendre les deux jobs Java 21.

Le Dockerfile compile et teste, puis exécute le JAR avec Java 21 et un utilisateur sans privilèges. Render fournit `PORT` ; le profil `render` configure le port, les en-têtes du proxy HTTPS et un cookie de session sécurisé.

## Variables à importer avant le déploiement des correctifs

Importer le `.env` local via **Environment à Add from .env**, ou renseigner :

| Clé | Valeur |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `supabase,render,demo` pour la démonstration ; `supabase,render` sans initialisation demo |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://HOTE_SESSION_POOLER:5432/postgres?sslmode=require&connectTimeout=10&socketTimeout=30` |
| `SPRING_DATASOURCE_USERNAME` | Utilisateur exact de **Supabase à Connect à Session pooler** |
| `SPRING_DATASOURCE_PASSWORD` | Mot de passe PostgreSQL Supabase |
| `RAILPULSE_DEMO_AGENT_PASSWORD` | Secret local d'au moins 12 caractères pour `demo.agent` |
| `RAILPULSE_DEMO_RESPONSABLE_PASSWORD` | Secret local d'au moins 12 caractères pour `demo.responsable` |

Reprendre hôte, port et base exacts de Supabase. Une clé API Supabase ne remplace pas le mot de passe PostgreSQL.
Les secrets ne doivent être placés ni dans Git ni dans le Dockerfile. `.env.example` ne contient qu'un modèle vide.

**Les deux secrets demo doivent être définis avant d'activer le profil `demo`.** Les comptes absents sont créés en BCrypt ; les comptes existants ne sont pas écrasés. Changer une variable ne réinitialise pas un compte déjà créé. Le catalogue n'est rempli que s'il est vide.

La migration de sécurité convertit les anciennes valeurs en clair et `{noop}` en BCrypt dans une transaction au démarrage. Elle conserve les identifiants et les mots de passe utilisables. Un format inconnu ou vide arrête le démarrage et demande une réinitialisation ; la connexion elle-même n'accepte plus le clair.

## Contrôle après mise en ligne

1. Vérifier le dernier commit déployé et les logs de démarrage, sans recopier de secrets.
2. Ouvrir https://railpulse-23pc.onrender.com/login.
3. Tester `demo.agent` et `demo.responsable` avec leurs secrets fournis dans Render.
4. Vérifier station, équipement, déclaration, détail, modification responsable et refus agent.
5. Télécharger le CSV ; se déconnecter puis tenter une page privée.

Le forfait Free se met en veille après 15 minutes sans requête ; le réveil peut prendre environ une minute. Supabase conserve les données hors du conteneur.

La CI vérifie H2 et PostgreSQL temporaires, sans manipuler la base distante. Le déploiement de cette PR et ses contrôles authentifiés sur Render restent une étape distincte.

Références : [Docker](https://render.com/docs/docker), [déploiement après CI](https://render.com/docs/deploys#automatic-deploys), [offre Free](https://render.com/docs/free).
