# D?ployer Rail Pulse sur Render

## Service

- Type : **Web Service** ; d?p?t `VincentCarret/railpulse`.
- Language : **Docker** ; branche : `master` apr?s revue et int?gration de la PR.
- Root Directory : vide ; Dockerfile Path : `./Dockerfile` ; Docker Command : vide.
- Choisir une r?gion proche de Supabase et le forfait souhait? (Free possible pour la d?monstration).
- Health Check Path : `/login` ; **Auto-Deploy ? After CI Checks Pass** pour attendre les deux jobs Java 21.

Le Dockerfile compile et teste, puis ex?cute le JAR avec Java 21 et un utilisateur sans privil?ges. Render fournit `PORT` ; le profil `render` configure le port, les en-t?tes du proxy HTTPS et un cookie de session s?curis?.

## Variables ? importer avant le d?ploiement des correctifs

Importer le `.env` local via **Environment ? Add from .env**, ou renseigner :

| Cl? | Valeur |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `supabase,render,demo` pour la d?monstration ; `supabase,render` sans initialisation demo |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://HOTE_SESSION_POOLER:5432/postgres?sslmode=require&connectTimeout=10&socketTimeout=30` |
| `SPRING_DATASOURCE_USERNAME` | Utilisateur exact de **Supabase ? Connect ? Session pooler** |
| `SPRING_DATASOURCE_PASSWORD` | Mot de passe PostgreSQL Supabase |
| `RAILPULSE_DEMO_AGENT_PASSWORD` | Secret local d'au moins 12 caract?res pour `demo.agent` |
| `RAILPULSE_DEMO_RESPONSABLE_PASSWORD` | Secret local d'au moins 12 caract?res pour `demo.responsable` |

Reprendre h?te, port et base exacts de Supabase. Une cl? API Supabase ne remplace pas le mot de passe PostgreSQL.
Les secrets ne doivent ?tre plac?s ni dans Git ni dans le Dockerfile. `.env.example` ne contient qu'un mod?le vide.

**Les deux secrets demo doivent ?tre d?finis avant d'activer le profil `demo`.** Les comptes absents sont cr??s en BCrypt ; les comptes existants ne sont pas ?cras?s. Changer une variable ne r?initialise pas un compte d?j? cr??. Le catalogue n'est rempli que s'il est vide.

La migration de s?curit? convertit les anciennes valeurs en clair et `{noop}` en BCrypt dans une transaction au d?marrage. Elle conserve les identifiants et les mots de passe utilisables. Un format inconnu ou vide arr?te le d?marrage et demande une r?initialisation ; la connexion elle-m?me n'accepte plus le clair.

## Contr?le apr?s mise en ligne

1. V?rifier le dernier commit d?ploy? et les logs de d?marrage, sans recopier de secrets.
2. Ouvrir https://railpulse-23pc.onrender.com/login.
3. Tester `demo.agent` et `demo.responsable` avec leurs secrets fournis dans Render.
4. V?rifier station, ?quipement, d?claration, d?tail, modification responsable et refus agent.
5. T?l?charger le CSV ; se d?connecter puis tenter une page priv?e.

Le forfait Free se met en veille apr?s 15 minutes sans requ?te ; le r?veil peut prendre environ une minute. Supabase conserve les donn?es hors du conteneur.

La CI v?rifie H2 et PostgreSQL temporaires, sans manipuler la base distante. Le d?ploiement de cette PR et ses contr?les authentifi?s sur Render restent une ?tape distincte.

R?f?rences : [Docker](https://render.com/docs/docker), [d?ploiement apr?s CI](https://render.com/docs/deploys#automatic-deploys), [offre Free](https://render.com/docs/free).
