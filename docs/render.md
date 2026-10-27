# Déployer Rail Pulse sur Render

Le `Dockerfile` compile et teste le projet avec Java 21, puis ne conserve que le JAR et un JRE Java 21 dans l'image finale. Aucune installation locale de Docker n'est nécessaire pour le déploiement depuis GitHub.

## Nouveau Web Service

| Champ Render | Valeur |
|---|---|
| Name | `railpulse` (ou un nom disponible) |
| Language | **Docker** |
| Branch | `feature/deploiement-render`, qui contient aussi F2 ; après intégration, utiliser `master` |
| Region | Choisir la région la plus proche de votre base Supabase ; Frankfurt est un choix européen possible |
| Root Directory | Laisser vide |
| Dockerfile Path | `./Dockerfile` |
| Docker Command | Laisser vide : le démarrage est défini dans le Dockerfile |
| Compute | **Free** pour la démonstration, si souhaité |
| Health Check Path (Advanced) | `/login` |

Avec Language = Docker, les commandes Node `yarn` et `yarn start` sont remplacées par le build et le démarrage du Dockerfile.

## Variables d'environnement

Configurer ces valeurs dans Render, sans placer le mot de passe dans Git ni dans l'URL JDBC :

| Clé | Valeur |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `supabase,render,demo` pour initialiser le catalogue F2 vide ; `supabase,render` ensuite |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://HOTE_SESSION_POOLER:5432/postgres?sslmode=require&connectTimeout=10&socketTimeout=30` |
| `SPRING_DATASOURCE_USERNAME` | L'utilisateur exact affiché par Supabase dans **Connect → Session pooler** |
| `SPRING_DATASOURCE_PASSWORD` | Le mot de passe PostgreSQL de la base Supabase |

Remplacer `HOTE_SESSION_POOLER` par l'hôte exact de **Connect → Session pooler**. Reprendre aussi le port et le nom de base affichés si différents de l'exemple. Ne pas utiliser une clé API Supabase comme mot de passe PostgreSQL.

Render fournit `PORT` automatiquement. Le profil `render` le transmet à Spring Boot, traite les en-têtes HTTPS du proxy, impose un cookie de session sécurisé et désactive les requêtes SQL dans les logs.
Le Dockerfile limite le tas Java à 60 % de la mémoire disponible pour laisser de la place aux autres besoins de la JVM ; l'utilisation réelle de la mémoire est à contrôler dans les métriques Render.

Le profil `demo` ne crée pas d'utilisateur. Utiliser les comptes F1 déjà présents dans `railpulse.users`, dont les mots de passe doivent être hachés. Le catalogue n'est initialisé que si stations et équipements sont tous deux vides.

## Vérification après déploiement

1. Vérifier dans les logs la réussite du build et du démarrage de Spring Boot.
2. Ouvrir `https://NOM_DU_SERVICE.onrender.com/login`.
3. Se connecter avec chacun des profils existants.
4. Ouvrir `/stations`, consulter les équipements et vérifier le changement de station.
5. Se déconnecter et vérifier le retour au formulaire de connexion.
6. Reporter l'URL réelle et les résultats de démonstration dans `journal.md`.

Le forfait Free met le service en veille après 15 minutes sans requête ; le premier chargement après veille peut prendre environ une minute. La base Supabase reste externe au conteneur.

Références : [Docker sur Render](https://render.com/docs/docker), [ports des Web Services](https://render.com/docs/web-services#port-binding), [limites du forfait Free](https://render.com/docs/free).
