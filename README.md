# Rail Pulse

Projet ALTN72 2026-2027 — **Amir Nassiri et Vincent Carret**.
Application Java 21 / Spring Boot 4.1.1, Maven, Thymeleaf et JPA. PostgreSQL/Supabase pour le déploiement ; H2 pour la démonstration autonome et les tests rapides.

F1 à F12 sont réalisées sur `master`. Le [journal](journal.md) justifie les choix et le [bilan de conformité](docs/etat-des-lieux.md) distingue le réalisé du reste à faire.

## Démarrer depuis un clone propre sous Windows

Prérequis : JDK 21 (ou version compatible supérieure) accessible par `java` et `JAVA_HOME`, accès Internet pour télécharger les dépendances Maven. Aucun serveur de base de données n'est requis pour cette démonstration.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/demarrer-demo.ps1
```

Le script lit uniquement les deux secrets de démonstration du `.env` local s'il existe ; sinon il les demande sans affichage. Choisir au moins 12 caractères. Il construit le JAR, lance une base relationnelle H2 locale dans `.data/` et initialise stations, équipements, incidents, prestataire, évaluation et comptes.

Ouvrir **http://localhost:8080/login** :

| Profil | Login | Mot de passe |
|---|---|---|
| Agent | `demo.agent` | Valeur locale `RAILPULSE_DEMO_AGENT_PASSWORD` |
| Responsable d'exploitation | `demo.responsable` | Valeur locale `RAILPULSE_DEMO_RESPONSABLE_PASSWORD` |

Les comptes sont créés une seule fois en BCrypt ; relancer ne remplace aucun compte, secret ou catalogue existant. Les mots de passe saisis au premier lancement sont ceux des comptes persistés. Un changement ultérieur des variables ne réinitialise pas ces comptes.

Le script force les paramètres H2 pour éviter qu'une ancienne variable Supabase/MySQL ne cible une base distante. `.data/`, `.env` et ses variantes sont exclus de Git et du contexte Docker. Un autre port est possible : `scripts/demarrer-demo.ps1 -Port 8081`.

## Lancement manuel

Définir les deux variables `RAILPULSE_DEMO_AGENT_PASSWORD` et `RAILPULSE_DEMO_RESPONSABLE_PASSWORD` dans l'environnement, puis :

```powershell
.\mvnw.cmd -B -ntp package
java -jar target/railpulse-0.0.1-SNAPSHOT.jar --spring.profiles.active=demo-local,demo
```

Pour le lancement manuel, vérifier qu'aucune variable `SPRING_DATASOURCE_*` héritée ne remplace le profil local. Dans IntelliJ : importer le projet Maven, sélectionner Java 21 et lancer `RailPulseApplication` avec `demo-local,demo` et les deux secrets de démonstration.

Pour Supabase : profil `supabase` (ou `supabase,render` sur Render), paramètres décrits dans [docs/supabase.md](docs/supabase.md). Ajouter `demo` et ses deux secrets si l'on souhaite créer les comptes de démonstration et initialiser un catalogue vide. Le profil `mysql` reste disponible ; son mot de passe doit être fourni par `MYSQL_DB_PASSWORD` ou `SPRING_DATASOURCE_PASSWORD`, sans valeur de secours versionnée.

## Parcours F1 à F12

1. **F1** : connexion des deux profils, erreur générique en cas d'échec, bandeau utilisateur et déconnexion POST avec fermeture de session.
2. **F2** : `/stations`, choix de station, type et état de ses seuls équipements ; référence unique en base (RG9).
3. **F3** : cliquer sur une référence pour ouvrir `/equipements/{id}` et consulter ses incidents en cours/passés, triés par ouverture décroissante (RG7).
4. **F4** : déclarer un incident depuis la fiche ; description obligatoire, 2000 caractères maximum, ouverture UTC côté serveur, statut non affecté sans prestataire (RG1/RG2).
5. **F5** : cliquer sur le numéro d'incident ; description, équipement, station, durée, dates, prestataire et évaluation éventuels. La durée s'arrête à la résolution.
6. **F6** : le responsable corrige uniquement la description d'un incident actif ; l'agent consulte. Un résolu n'est plus modifiable (409). Droits HTTP et service, CSRF et contrôle des champs envoyés.
7. **F7** : sur le détail d'un incident actif, le responsable choisit un prestataire, puis peut le remplacer. Le prestataire courant est présélectionné. L'agent ne peut que consulter ; un incident résolu refuse toute affectation (409). Un prestataire inexistant renvoie 404 et aucune donnée n'est modifiée.
8. **F8** : le responsable confirme la résolution d'un incident actif depuis son détail. La date et l'heure de résolution viennent de l'horloge du serveur (UTC) ; une seconde résolution ou une date antérieure à l'ouverture est refusée (409). L'agent conserve un accès en lecture seule.
9. **F9** : depuis l'accueil, les deux profils consultent `/incidents/historique`, qui présente uniquement les incidents résolus, par date de résolution décroissante. Chaque ligne relie au détail de l'incident et à l'équipement.
10. **F10** : sur le détail d'un incident résolu et affecté, le responsable saisit une note de 1 à 5 et un commentaire facultatif. Une seule évaluation est autorisée par incident ; sa date vient du serveur (UTC). Les deux profils peuvent ensuite lire la note sur le détail.
11. **F11** : le responsable ouvre `/tableau-de-bord` depuis l'accueil. Les incidents non résolus y montrent leur station, équipement, durée et niveau d'attention calculé à l'instant courant : vert avant 48 h, orange de 48 h à moins de 72 h, rouge dès 72 h si aucun prestataire n'est affecté ; un incident pris en charge possède un état distinct. Les filtres station et prise en charge sont combinables ; quatre indicateurs résument la sélection et chaque ligne mène au détail.
12. **F12** : les deux profils consultent les incidents via `GET /api/incidents` et `GET /api/incidents/{id}`. Le responsable résout un incident actif avec `POST /api/incidents/{id}/resolution` ; la date UTC vient du serveur. L'API renvoie 400, 404 ou 409 selon les erreurs et conserve les règles métier et les droits de l'interface.

Les erreurs conservent la saisie sans écriture ; identifiant mal formé 400, ressource absente 404, accès interdit 403. Les horaires sont en UTC. Le profil `demo` crée des incidents aux dates relatives, dont 47/48/72 h, ainsi que des cas affectés/résolus. Le tableau de bord recalcule les indicateurs d'urgence à chaque consultation.

### API REST et OpenAPI

La [spécification OpenAPI 3](src/main/resources/static/api/openapi.yaml) est disponible à `/api/openapi.yaml` après connexion. Elle décrit les réponses JSON, les statuts HTTP et l'authentification par session. Une requête non connectée est redirigée vers `/login`. Pour l'écriture, récupérer le jeton avec `GET /api/csrf` puis le transmettre dans l'en-tête `X-CSRF-TOKEN` du `POST`. Aucun client ne fournit la date de résolution.

```text
GET  /api/incidents
GET  /api/incidents/1
GET  /api/csrf
POST /api/incidents/1/resolution  (en-tête X-CSRF-TOKEN)
```

## Deux fonctionnalités alpha

- **Qualité renforcée (piste 7)** : tests HTTP/JPA et métier, H2 et PostgreSQL isolés dans la CI, horloge injectée, JaCoCo et vérification des refus sans mutation.
- **Export de données (piste 9)** : bouton **Exporter le catalogue (CSV)** sur F2, ou `/stations/export.csv`. Export UTF-8 compatible tableur, tri par station/référence, champs échappés et formules neutralisées. Les deux profils peuvent exporter ; une connexion est requise. L'export utilise l'unique requête SQL native de l'application, analysée dans le journal.

## Sécurité des comptes existants

La connexion accepte uniquement BCrypt, avec ou sans préfixe `{bcrypt}`. Toute nouvelle écriture JPA d'un compte impose un mot de passe déjà haché.
Une migration transactionnelle au démarrage transforme les anciennes valeurs en clair ou `{noop}` en BCrypt, avant l'état prêt de l'application. Elle conserve les mots de passe utilisables, identifiants et profils ; elle est idempotente et ne journalise pas les valeurs. Un format historique inconnu ou vide arrête le démarrage avec un message de réinitialisation, au lieu d'être interprété comme du clair.

Le `.env` n'est pas chargé automatiquement par Spring. Le script local ne lit que les secrets de démonstration ; Render importe les valeurs via **Environment à Add from .env**. Le modèle versionné [.env.example](.env.example) ne contient aucun secret.

## Tests et CI

```powershell
.\mvnw.cmd -B -ntp verify
```

Les tests locaux utilisent des bases H2 temporaires. `verify` produit le JAR, `target/site/jacoco/index.html` et les rapports dans `target/surefire-reports/`.
La CI exécute les mêmes tests sous Java 21 avec H2 et un vrai PostgreSQL temporaire, dans des schémas séparés par contexte de test. Aucune connexion Supabase n'est requise. Ne pas renseigner une base contenant des données à conserver dans `TEST_DB_URL` : les tests utilisent `create-drop`.

## Déploiement et limites

URL de déploiement prévue : https://railpulse-23pc.onrender.com ; le 30 octobre 2026, Render répondait **502 Bad Gateway** avec `x-render-routing: no-deploy`. Voir la [procédure Render](docs/render.md) pour rétablir et vérifier le service.
Le Dockerfile construit et teste avec Java 21. Les variables de base et de démonstration doivent être configurées sur Render avant d'y déployer les correctifs. Le déploiement peut attendre les deux jobs CI via **Auto-Deploy à After CI Checks Pass**.

`ddl-auto=update` et l'initialisation applicative conviennent au projet pédagogique ; des migrations de schéma versionnées restent une amélioration pour une exploitation durable.
