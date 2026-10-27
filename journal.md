# Journal ? Rail Pulse (ALTN72, 2026-2027)

**Groupe : Amir Nassiri et Vincent Carret.** ?tat au 27 octobre 2026, p?rim?tre F1 ? F6.

## README

Le [README](README.md) contient les pr?requis Java 21, les commandes Maven et le lancement autonome sur H2 avec `scripts/demarrer-demo.ps1`. Une base neuve initialise le catalogue et les deux profils ; une base existante est conserv?e.

- Application : https://railpulse-23pc.onrender.com ; formulaire de connexion v?rifi? HTTP 200 lors de l'audit. La pr?sence en ligne ne prouve pas ? elle seule la version du code ni tous les parcours authentifi?s.
- Comptes : `demo.agent` / `RAILPULSE_DEMO_AGENT_PASSWORD` et `demo.responsable` / `RAILPULSE_DEMO_RESPONSABLE_PASSWORD`. L'?valuateur d?finit ses propres mots de passe au lancement ; ils sont hach?s avant insertion et ne sont pas versionn?s.
- Base locale : H2 relationnelle dans `.data/`, ignor?e par Git. PostgreSQL/Supabase pour Render, MySQL disponible en profil explicite. Le script local force H2 pour prot?ger les bases distantes d'une configuration h?rit?e.
- Initialisation : JPA pour le sch?ma, `ComptesDemonstrationAuDemarrage` et `DonneesDemonstration` pour les donn?es. Migration BCrypt pr?alable. Les initialisations sont transactionnelles et idempotentes ; aucun ?crasement de compte/catalogue existant.
- Limites : F7 ? F12 restent ? r?aliser. Pas d'interface de saisie d'?valuation F10, pas encore d'indicateur d'urgence F11 ni d'API REST F12. `ddl-auto=update` est un choix p?dagogique, pas une strat?gie de migration de sch?ma de production.

## ARCHITECTURE

D?p?t : https://github.com/VincentCarret/railpulse

```text
Navigateur ?? HTTP/session/CSRF ??? Contr?leurs MVC ??? Services ??? Repositories JPA
                                         ?               ?              ?
                                         ?               ?              ?
                                 Vues Thymeleaf      Mod?le m?tier   Base relationnelle
                                 et fragments       et contraintes  PostgreSQL / H2
```

- Les contr?leurs adaptent le protocole HTTP, valident la saisie et choisissent les vues/redirections.
- Les services orchestrent les cas d'usage, les horloges, les droits de m?thode et les transactions.
- Le mod?le porte les invariants d'?tat ; les repositories portent les requ?tes.
- `SecurityConfig` prot?ge les routes et active les autorisations sur les m?thodes sensibles. `GlobalExceptionHandler` traduit les erreurs attendues vers les bons statuts et masque les d?tails internes.
- F2/F3 chargent station et incidents avec les relations utiles au rendu ; F5 charge ?quipement/station/prestataire et l'?valuation ?ventuelle. Les vues n'interrogent pas la base.
- `ExportCatalogueService` transforme une projection SQL en CSV ; son contr?leur ne contient ni SQL ni logique d'?chappement.
- Le CRUD de programmeurs est un exemple h?rit? du TP, conserv? pour compatibilit? et s?par? du socle Rail Pulse. Il ne compte ni comme F1?F12 ni comme lot alpha.

## TESTS

Commande : `./mvnw.cmd -B -ntp verify` sous Windows, `bash ./mvnw -B -ntp verify` sous Linux.

Les tests HTTP lancent un vrai serveur sur port al?atoire et manipulent cookies, session et CSRF. Ils v?rifient les ?critures r?elles, les refus et les valeurs conserv?es, et ne se limitent pas ? appeler des accesseurs.

| Domaine | Comportements couverts |
|---|---|
| F1 | Deux profils BCrypt, formats avec/sans pr?fixe, ?chec g?n?rique, renouvellement de session, bandeau, d?connexion, cookie ancien refus? et CSRF. |
| F2/F3 | Filtrage par station/?quipement, donn?es affich?es, tri RG7, listes vides, 400/404, XSS et r?f?rence unique RG9. |
| F4 | Description obligatoire/longueur, horloge fixe, ?tat initial non affect?, absence de prestataire/r?solution, champs falsifi?s, CSRF et refus sans ?criture. |
| F5 | Dur?e courante ou fig?e ? la r?solution, relations facultatives, RG8, note et unicit? d'?valuation, 400/404 et HTML ?chapp?. |
| F6 | Responsable autoris?, agent refus? par HTTP et service, ?tat r?solu interdit, formulaire devenu obsol?te apr?s r?solution, validation, champs falsifi?s, CSRF et absence de mutation sur refus. |
| S?curit? des comptes | Clair et `{noop}` refus?s ? la connexion ; conversion pr?alable en BCrypt, idempotence, rollback d'une migration invalide et refus de nouvelles ?critures non hach?es. |
| D?monstration | Cr?ation transactionnelle des deux comptes, secrets absents refus?s, profils pr?existants prot?g?s, connexion r?elle, catalogue vari? et relancement sans duplication. |
| Export CSV | Droits des deux profils, format/encodage/en-t?tes, catalogue vide, guillemets/s?parateurs/retours ligne et neutralisation des formules. |

V?rification locale apr?s corrections : **75 tests r?ussis, aucun ?chec ni erreur**, JAR et rapport JaCoCo g?n?r?s (JDK install? 25.0.3, cible Java 21).
Le JAR a aussi ?t? d?marr? sans configuration de test sur une base H2 fichier neuve : connexion des deux comptes, F2/F3, d?claration F4, d?tail F5, refus d'?dition agent, correction responsable, CSV et d?connexion v?rifi?s par HTTP. Cette instance temporaire a ?t? arr?t?e.

La CI ex?cute deux jobs Java 21 : H2, puis PostgreSQL 17 temporaire. Les propri?t?s `TEST_DB_*` sont utilis?es par tous les contextes, y compris F4, suivi et demo, avec des sch?mas isol?s (`railpulse`, `railpulse_f4`, `railpulse_suivi`, `railpulse_demo`). Les rapports et le JAR sont conserv?s comme artifacts. R?sultat du dernier passage ? v?rifier dans l'onglet Actions de la PR.

La couverture JaCoCo est une mesure d'ex?cution, pas une preuve compl?te de conformit?. Le rendu visuel, l'accessibilit? compl?te, la charge, l'environnement Supabase r?el et la compr?hension orale restent des contr?les distincts. Les tests F11 (47/48/72 h) seront ajout?s avec le calcul d'urgence ; les dates de d?monstration seules ne prouvent pas ce calcul.

## CHOIX TECHNIQUES

### Droits par profil (?5.6)

| N? | Fonctionnalit? | AGENT | RESPONSABLE_EXPLOITATION |
|---|---|---|---|
| F2?F3 | ?quipements et fiches | Lecture seule | Lecture seule |
| F4 | D?clarer un incident | Acc?s complet | Acc?s complet |
| F5?F6 | Consulter / modifier | Lecture F5 ; F6 interdit | Lecture F5 ; correction de description des actifs F6 |
| F7 | Affecter / r?affecter | Interdit | Acc?s complet pr?vu |
| F8 | R?soudre | Interdit | Acc?s complet pr?vu |
| F9 | Historique | Lecture pr?vue | Lecture pr?vue |
| F10 | ?valuer | Interdit | Acc?s complet pr?vu |
| F11 | Tableau de bord | Interdit | Lecture pr?vue |

F7?F11 d?crivent les choix pour la suite ; leurs ?crans/op?rations ne sont pas r?alis?s. L'export CSV reprend les droits de lecture du catalogue.

F6 ne modifie que la description d'un incident actif : les identifiants, dates, ?quipement, statut et affectation restent inchang?s. Les r?solus sont immuables pour pr?server l'historique. Les routes et `@PreAuthorize` v?rifient le r?le ; `Incident` v?rifie l'?tat. La pr?sence/absence du bouton n'est pas la protection serveur.

### R?gles et exceptions

- RG1/RG2 : incident li? ? l'?quipement de l'URL, ouverture calcul?e par `Clock`, statut initial non affect? sans prestataire.
- RG5 : le mod?le interdit r?solution ant?rieure ? l'ouverture et r?affectation d'un r?solu ; F7/F8 ne sont pas encore expos?es.
- RG7 : incidents d'un ?quipement tri?s par ouverture d?croissante, puis identifiant d?croissant. L'historique global F9 reste ? r?aliser.
- RG8 : ?valuation d'un r?solu avec prestataire, note 1?5, date coh?rente et unicit? en base ; F10 n'a pas encore d'interface.
- RG9 : r?f?rence d'?quipement unique en base, test?e entre stations.
- RG10/RG11 : transactions, contr?les d'?tat et droits c?t? serveur. Les seuils/calculs et listes F11 li?s ? RG3/RG4/RG6 restent ? r?aliser.

`@ControllerAdvice` est conserv? pour uniformiser les pages d'erreur. Les exceptions personnalis?es utilisent des messages fran?ais : 400 pour une saisie/identifiant invalide, 403 pour un droit insuffisant, 404 pour une ressource absente, 409 pour un ?tat interdit ou un conflit. Une erreur inattendue renvoie 500 avec un message neutre ; aucune trace SQL n'est envoy?e dans la page. `ResponseEntity` n'est pas n?cessaire aux vues actuelles ; le CSV ?crit des en-t?tes HTTP explicites.

### SOLID

- **SRP appliqu?** : contr?leurs HTTP, orchestration dans les services, invariants dans `Incident`/`Evaluation`, acc?s aux donn?es dans les repositories, format CSV dans son service et erreurs dans l'advice. Chaque responsabilit? a une raison de changement diff?rente.
- **DIP appliqu? au d?coupage utilis?** : injection par constructeur d'interfaces de repository et de `PasswordEncoder`, au lieu d'impl?mentations JDBC construites par les services.
- **OCP** : aucune hi?rarchie de strat?gies artificielle ; les points d'extension Spring suffisent aux besoins actuels. Une strat?gie sera extraite si un v?ritable point de variation appara?t.
- **LSP** : pas de hi?rarchie m?tier de sous-types interchangeables ? introduire ; les contrats Spring sont utilis?s sans sous-types artificiels.
- **ISP** : repositories s?par?s par entit? et services cibl?s. Ajouter un wrapper par m?thode JPA ne r?pondrait ? aucun consommateur distinct.

### Persistance et unique requ?te SQL native (?7.6)

Tous les repositories actifs d?clarent une requ?te utile. Les requ?tes d?riv?es (utilisateurs, stations, ?valuations, tri du TP) compl?tent les JPQL (catalogue, d?tails, historique d'?quipement et verrouillage).

L'unique requ?te native de `src/main/java` se trouve dans `EquipementRepository.exporterCatalogue`. Elle joint ?quipements et stations et renvoie uniquement les cinq colonnes du CSV, d?j? tri?es, au lieu d'hydrater des entit?s et d'acc?der ? des associations par ligne.

**Analyse critique :** la requ?te contr?le directement colonnes et jointure, mais d?pend des noms physiques de tables/colonnes et d'une projection dont les alias doivent rester coh?rents. JPQL aurait offert une abstraction plus forte ; le choix natif est limit? ? cet export et satisfait l'exercice, sans disperser du SQL dans les contr?leurs. Le SQL produit utilise une jointure et un tri standards ; `{h-schema}` est un jeton sp?cifique ? Hibernate pour qualifier correctement les tables selon le sch?ma configur?. Il apporte donc une d?pendance au fournisseur JPA, explicitement assum?e. Le double passage H2/PostgreSQL v?rifie aussi ce point.

R?f?rence : [Hibernate ? sch?mas des requ?tes natives](https://docs.hibernate.org/stable/orm/userguide/html_single/#sql-global-catalog-schema).

### Transactions

- Lectures `EquipementService` et `IncidentService` : `readOnly=true` pour regrouper les requ?tes et charger les relations n?cessaires aux vues.
- D?claration F4 : transaction d'?criture regroupant lecture de l'?quipement et insertion d'incident.
- Correction F6 : verrou `PESSIMISTIC_WRITE` sur la ligne incident avant le contr?le d'?tat ; le dirty checking persiste la correction sans recopier les champs client. La derni?re correction valide est conserv?e, sans fusion de deux descriptions concurrentes.
- Initialisations : catalogue complet et deux comptes cr??s chacun dans leur transaction, avec rollback si une condition ?choue. Un compte existant n'est ni r?affect? ni ?cras?.
- Migration BCrypt : une transaction pour les comptes ? migrer ; format historique inconnu/vide = rollback complet et d?marrage refus?. Aucun secret n'est journalis?.
- Export : transaction de lecture pour extraire une vue coh?rente du catalogue.
- Le CRUD TP conserve ses transactions existantes ; les modifications d'entit?s g?r?es utilisent le dirty checking.

### S?curit? et configuration

Le `PasswordEncoder` n'accepte que BCrypt. `User` impose un format BCrypt sur les nouvelles ?critures JPA. `AuthService` n'accepte plus le clair ni `{noop}` : leur conversion est une migration de donn?es au d?marrage, pas une m?thode d'authentification.

Les deux secrets demo sont fournis par environnement ou saisie locale ; aucun mot de passe par d?faut n'est inscrit dans le code. Le fallback MySQL a ?t? supprim? et sa configuration locale pr?serv?e hors Git. `.env`, `.env.*` et les bases locales sont ignor?s ; `.env.example` ne contient que des valeurs vides ou des param?tres de connexion factices.

La suppression d'une valeur du code courant ne r??crit pas l'historique Git. Une ancienne valeur de base encore utilis?e doit ?tre renouvel?e par son propri?taire. Les tests utilisent uniquement des secrets temporaires sans lien avec la production.

## LOT ALPHA

### 1. Qualit? renforc?e ? piste 7

Besoin : d?tecter les r?gressions du socle et d?montrer le comportement r?el des droits, de la persistance et des exceptions.

R?alis? : tests HTTP/JPA et m?tier, horloge fixe, sc?narios invalides et champs falsifi?s, migration/rollback, JaCoCo, artifacts et deux jobs CI sur Java 21 avec H2 et PostgreSQL. Cela d?passe la seule pr?sence de validation : les transactions, droits et formats produits sont exerc?s.

Limites : couverture d'ex?cution distincte de l'ergonomie et des performances. MySQL n'est pas inclus dans la nouvelle matrice, car le d?ploiement cible PostgreSQL. F7?F12 n?cessiteront leurs propres tests.

### 2. Export de donn?es ? piste 9

Besoin : obtenir un inventaire exploitable du r?seau dans un tableur sans acc?s direct ? la base.

R?alis? : export CSV du catalogue, accessible aux deux profils depuis F2. UTF-8 avec BOM, s?parateur point-virgule, guillemets et retours ligne ?chapp?s, formules neutralis?es, nom de fichier et absence de cache explicites. Une seule requ?te native produit une projection plate tri?e. Catalogue vide et droits test?s.

Limites : catalogue export? en m?moire, adapt? au volume de d?monstration ; un export pagin?/stream? serait pr?f?rable pour un r?seau tr?s volumineux. L'export de l'historique r?solu pourra ?tre ajout? apr?s F9, sans ?tre compt? comme d?j? r?alis?.

Ces deux choix appartiennent aux pistes propos?es au ?6. La s?curit? F1 et CSRF ne sont pas compt?s comme des alpha suppl?mentaires.

## BILAN

### Groupe et tra?abilit?

| Membre | Traces techniques observables dans Git |
|---|---|
| Amir Nassiri | Contribution initiale CI/CD et d?ploiement VPS, commit `58646e2` sign? `amirnssr`. |
| Vincent Carret | Configuration, int?gration des branches, puis commits F2?F6, tests et documentation sous `vincent` / `vincent.carret`. |

Les noms du groupe ont ?t? fournis par les membres. Cette table d?crit l'historique, pas une mesure de temps ni une r?partition compl?te du travail hors Git. Chaque membre doit compl?ter/valider sa contribution r?elle et savoir expliquer la solution. L'assistance Codex pour le code, les tests et les revues est d?clar?e ; elle ne prouve pas la ma?trise orale du groupe.

PR d?j? int?gr?es : F1/CI (#1?#2), F2 (#3), Render (#4), F3 (#5), F4 (#6), F5 (#7), F6 (#8). Les corrections de conformit? sont isol?es sur `fix/conformite-mi-parcours`, avec PR de revue.

### Difficult?s techniques observ?es

- L'acceptation du clair ? la connexion masquait un ?cart de s?curit? : migration transactionnelle pr?alable, BCrypt seul ? la connexion et validation de persistance.
- Une valeur de secours MySQL et l'absence de comptes reproductibles emp?chaient une configuration s?re : secrets hors Git, comptes param?tr?s et script autonome H2.
- Une CI annon?ait MySQL alors que les tests for?aient H2 : correction des propri?t?s de tous les contextes et ajout d'un vrai job PostgreSQL avec sch?mas isol?s.

Ces difficult?s sont attest?es par les corrections ; le groupe doit pr?ciser laquelle il consid?re comme sa difficult? principale.

### Reste ? faire

F7?F12 : affectation, r?solution, historique global, saisie d'?valuation, tableau de bord/urgence (48/72 h) et API REST document?e OpenAPI. Les d?monstrations navigateur sur Render, la rotation ?ventuelle d'un ancien secret et la validation humaine du bilan restent ? effectuer. La conformit? finale n'est pas d?clar?e acquise ? mi-parcours.

Trois acquis : s?parer HTTP, m?tier et donn?es ; faire respecter invariants/droits c?t? serveur ; v?rifier les succ?s et les refus par des tests reproductibles sur des bases isol?es.
