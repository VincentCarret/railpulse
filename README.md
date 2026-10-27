# Rail Pulse

Projet ALTN72 2026-2027 ? **Amir Nassiri et Vincent Carret**.
Application Java 21 / Spring Boot 4.1.1, Maven, Thymeleaf et JPA. PostgreSQL/Supabase pour le d?ploiement ; H2 pour la d?monstration autonome et les tests rapides.

F1 ? F6 sont r?alis?es. Le [journal](journal.md) justifie les choix et le [bilan de conformit?](docs/etat-des-lieux.md) distingue le r?alis? du reste ? faire.

## D?marrer depuis un clone propre sous Windows

Pr?requis : JDK 21 (ou version compatible sup?rieure) accessible par `java` et `JAVA_HOME`, acc?s Internet pour t?l?charger les d?pendances Maven. Aucun serveur de base de donn?es n'est requis pour cette d?monstration.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/demarrer-demo.ps1
```

Le script lit uniquement les deux secrets de d?monstration du `.env` local s'il existe ; sinon il les demande sans affichage. Choisir au moins 12 caract?res. Il construit le JAR, lance une base relationnelle H2 locale dans `.data/` et initialise stations, ?quipements, incidents, prestataire, ?valuation et comptes.

Ouvrir **http://localhost:8080/login** :

| Profil | Login | Mot de passe |
|---|---|---|
| Agent | `demo.agent` | Valeur locale `RAILPULSE_DEMO_AGENT_PASSWORD` |
| Responsable d'exploitation | `demo.responsable` | Valeur locale `RAILPULSE_DEMO_RESPONSABLE_PASSWORD` |

Les comptes sont cr??s une seule fois en BCrypt ; relancer ne remplace aucun compte, secret ou catalogue existant. Les mots de passe saisis au premier lancement sont ceux des comptes persist?s. Un changement ult?rieur des variables ne r?initialise pas ces comptes.

Le script force les param?tres H2 pour ?viter qu'une ancienne variable Supabase/MySQL ne cible une base distante. `.data/`, `.env` et ses variantes sont exclus de Git et du contexte Docker. Un autre port est possible : `scripts/demarrer-demo.ps1 -Port 8081`.

## Lancement manuel

D?finir les deux variables `RAILPULSE_DEMO_AGENT_PASSWORD` et `RAILPULSE_DEMO_RESPONSABLE_PASSWORD` dans l'environnement, puis :

```powershell
.\mvnw.cmd -B -ntp package
java -jar target/railpulse-0.0.1-SNAPSHOT.jar --spring.profiles.active=demo-local,demo
```

Pour le lancement manuel, v?rifier qu'aucune variable `SPRING_DATASOURCE_*` h?rit?e ne remplace le profil local. Dans IntelliJ : importer le projet Maven, s?lectionner Java 21 et lancer `RailPulseApplication` avec `demo-local,demo` et les deux secrets de d?monstration.

Pour Supabase : profil `supabase` (ou `supabase,render` sur Render), param?tres d?crits dans [docs/supabase.md](docs/supabase.md). Ajouter `demo` et ses deux secrets si l'on souhaite cr?er les comptes de d?monstration et initialiser un catalogue vide. Le profil `mysql` reste disponible ; son mot de passe doit ?tre fourni par `MYSQL_DB_PASSWORD` ou `SPRING_DATASOURCE_PASSWORD`, sans valeur de secours versionn?e.

## Parcours F1 ? F6

1. **F1** : connexion des deux profils, erreur g?n?rique en cas d'?chec, bandeau utilisateur et d?connexion POST avec fermeture de session.
2. **F2** : `/stations`, choix de station, type et ?tat de ses seuls ?quipements ; r?f?rence unique en base (RG9).
3. **F3** : cliquer sur une r?f?rence pour ouvrir `/equipements/{id}` et consulter ses incidents en cours/pass?s, tri?s par ouverture d?croissante (RG7).
4. **F4** : d?clarer un incident depuis la fiche ; description obligatoire, 2000 caract?res maximum, ouverture UTC c?t? serveur, statut non affect? sans prestataire (RG1/RG2).
5. **F5** : cliquer sur le num?ro d'incident ; description, ?quipement, station, dur?e, dates, prestataire et ?valuation ?ventuels. La dur?e s'arr?te ? la r?solution.
6. **F6** : le responsable corrige uniquement la description d'un incident actif ; l'agent consulte. Un r?solu n'est plus modifiable (409). Droits HTTP et service, CSRF et contr?le des champs envoy?s.

Les erreurs conservent la saisie sans ?criture ; identifiant mal form? 400, ressource absente 404, acc?s interdit 403. Les horaires sont en UTC. Le profil `demo` cr?e des incidents aux dates relatives, dont 47/48/72 h, ainsi que des cas affect?s/r?solus. Les couleurs actuelles repr?sentent des statuts, pas encore les indicateurs d'urgence F11.

## Deux fonctionnalit?s alpha

- **Qualit? renforc?e (piste 7)** : tests HTTP/JPA et m?tier, H2 et PostgreSQL isol?s dans la CI, horloge inject?e, JaCoCo et v?rification des refus sans mutation.
- **Export de donn?es (piste 9)** : bouton **Exporter le catalogue (CSV)** sur F2, ou `/stations/export.csv`. Export UTF-8 compatible tableur, tri par station/r?f?rence, champs ?chapp?s et formules neutralis?es. Les deux profils peuvent exporter ; une connexion est requise. L'export utilise l'unique requ?te SQL native de l'application, analys?e dans le journal.

## S?curit? des comptes existants

La connexion accepte uniquement BCrypt, avec ou sans pr?fixe `{bcrypt}`. Toute nouvelle ?criture JPA d'un compte impose un mot de passe d?j? hach?.
Une migration transactionnelle au d?marrage transforme les anciennes valeurs en clair ou `{noop}` en BCrypt, avant l'?tat pr?t de l'application. Elle conserve les mots de passe utilisables, identifiants et profils ; elle est idempotente et ne journalise pas les valeurs. Un format historique inconnu ou vide arr?te le d?marrage avec un message de r?initialisation, au lieu d'?tre interpr?t? comme du clair.

Le `.env` n'est pas charg? automatiquement par Spring. Le script local ne lit que les secrets de d?monstration ; Render importe les valeurs via **Environment ? Add from .env**. Le mod?le versionn? [.env.example](.env.example) ne contient aucun secret.

## Tests et CI

```powershell
.\mvnw.cmd -B -ntp verify
```

Les tests locaux utilisent des bases H2 temporaires. `verify` produit le JAR, `target/site/jacoco/index.html` et les rapports dans `target/surefire-reports/`.
La CI ex?cute les m?mes tests sous Java 21 avec H2 et un vrai PostgreSQL temporaire, dans des sch?mas s?par?s par contexte de test. Aucune connexion Supabase n'est requise. Ne pas renseigner une base contenant des donn?es ? conserver dans `TEST_DB_URL` : les tests utilisent `create-drop`.

Le CRUD de programmeurs reste un exemple isol? du TP ; il n'est pas compt? comme fonctionnalit? Rail Pulse. Son repository contient une requ?te d?riv?e r?ellement utilis?e pour le tri de la liste.

## D?ploiement et limites

Application : https://railpulse-23pc.onrender.com ? [proc?dure Render](docs/render.md).
Le Dockerfile construit et teste avec Java 21. Les variables de base et de d?monstration doivent ?tre configur?es sur Render avant d'y d?ployer les correctifs. Le d?ploiement peut attendre les deux jobs CI via **Auto-Deploy ? After CI Checks Pass**.

F7 ? F12 restent ? r?aliser : affectation, r?solution, historique global, saisie d'?valuation, supervision/urgence et API REST OpenAPI. Le d?tail peut d?j? afficher une ?valuation initialis?e, sans interface F10. `ddl-auto=update` et l'initialisation applicative conviennent au projet p?dagogique ; des migrations de sch?ma versionn?es restent une am?lioration pour une exploitation durable.
