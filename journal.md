# Journal à Rail Pulse (ALTN72, 2026-2027)

**Groupe : Amir Nassiri et Vincent Carret.** état au 27 octobre 2026, périmètre F1 à F9.

## README

Le [README](README.md) contient les prérequis Java 21, les commandes Maven et le lancement autonome sur H2 avec `scripts/demarrer-demo.ps1`. Une base neuve initialise le catalogue et les deux profils ; une base existante est conservée.

- Application : https://railpulse-23pc.onrender.com ; formulaire de connexion vérifié HTTP 200 lors de l'audit. La présence en ligne ne prouve pas à elle seule la version du code ni tous les parcours authentifiés.
- Comptes : `demo.agent` / `RAILPULSE_DEMO_AGENT_PASSWORD` et `demo.responsable` / `RAILPULSE_DEMO_RESPONSABLE_PASSWORD`. L'évaluateur définit ses propres mots de passe au lancement ; ils sont hachés avant insertion et ne sont pas versionnés.
- Base locale : H2 relationnelle dans `.data/`, ignorée par Git. PostgreSQL/Supabase pour Render, MySQL disponible en profil explicite. Le script local force H2 pour protéger les bases distantes d'une configuration héritée.
- Initialisation : JPA pour le schéma, `ComptesDemonstrationAuDemarrage` et `DonneesDemonstration` pour les données. Migration BCrypt préalable. Les initialisations sont transactionnelles et idempotentes ; aucun écrasement de compte/catalogue existant.
- Limites : F10 à F12 restent à réaliser. Pas d'interface de saisie d'évaluation F10, pas encore d'indicateur d'urgence F11 ni d'API REST F12. `ddl-auto=update` est un choix pédagogique, pas une stratégie de migration de schéma de production.

## ARCHITECTURE

Dépôt : https://github.com/VincentCarret/railpulse

```text
Navigateur -> Controleurs MVC -> Services -> Repositories JPA -> Base relationnelle
                  |                |
                  v                v
             Vues Thymeleaf    Modele metier
```

- Les contrôleurs adaptent le protocole HTTP, valident la saisie et choisissent les vues/redirections.
- Les services orchestrent les cas d'usage, les horloges, les droits de méthode et les transactions.
- Le modèle porte les invariants d'état ; les repositories portent les requêtes.
- `SecurityConfig` protège les routes et active les autorisations sur les méthodes sensibles. `GlobalExceptionHandler` traduit les erreurs attendues vers les bons statuts et masque les détails internes.
- F2/F3 chargent station et incidents avec les relations utiles au rendu ; F5 charge équipement/station/prestataire et l'évaluation éventuelle. Les vues n'interrogent pas la base.
- `ExportCatalogueService` transforme une projection SQL en CSV ; son contrôleur ne contient ni SQL ni logique d'échappement.
- Le CRUD de programmeurs est un exemple hérité du TP, conservé pour compatibilité et séparé du socle Rail Pulse. Il ne compte ni comme F1–F12 ni comme lot alpha.

## TESTS

Commande : `./mvnw.cmd -B -ntp verify` sous Windows, `bash ./mvnw -B -ntp verify` sous Linux.

Les tests HTTP lancent un vrai serveur sur port aléatoire et manipulent cookies, session et CSRF. Ils vérifient les écritures réelles, les refus et les valeurs conservées, et ne se limitent pas à appeler des accesseurs.

| Domaine | Comportements couverts |
|---|---|
| F1 | Deux profils BCrypt, formats avec/sans préfixe, échec générique, renouvellement de session, bandeau, déconnexion, cookie ancien refusé et CSRF. |
| F2/F3 | Filtrage par station/équipement, données affichées, tri RG7, listes vides, 400/404, XSS et référence unique RG9. |
| F4 | Description obligatoire/longueur, horloge fixe, état initial non affecté, absence de prestataire/résolution, champs falsifiés, CSRF et refus sans écriture. |
| F5 | Durée courante ou figée à la résolution, relations facultatives, RG8, note et unicité d'évaluation, 400/404 et HTML échappé. |
| F6 | Responsable autorisé, agent refusé par HTTP et service, état résolu interdit, formulaire devenu obsolète après résolution, validation, champs falsifiés, CSRF et absence de mutation sur refus. |
| F7 | Affectation et réaffectation des actifs, formulaire présélectionné, refus des résolus, prestataire absent, saisie invalide, champs falsifiés, droits HTTP et service, CSRF et conservation de l'état après refus. |
| F8 | Résolution des incidents non affectés ou affectés, date serveur, refus des doublons et des ouvertures futures, champs falsifiés ignorés, droits HTTP et service, CSRF et absence de mutation sur refus. |
| F9 | Deux profils, résolus uniquement, tri par résolution décroissante puis identifiant, cas vide, liens de détail, accès authentifié et échappement HTML. |
| Sécurité des comptes | Clair et `{noop}` refusés à la connexion ; conversion préalable en BCrypt, idempotence, rollback d'une migration invalide et refus de nouvelles écritures non hachées. |
| Démonstration | Création transactionnelle des deux comptes, secrets absents refusés, profils préexistants protégés, connexion réelle, catalogue varié et relancement sans duplication. |
| Export CSV | Droits des deux profils, format/encodage/en-têtes, catalogue vide, guillemets/séparateurs/retours ligne et neutralisation des formules. |

Vérification locale avec F9 : **90 tests réussis, aucun échec ni erreur**, JAR et rapport JaCoCo générés (JDK 21.0.2).
Le JAR a aussi été démarré sans configuration de test sur une base H2 fichier neuve : connexion des deux comptes, F2/F3, déclaration F4, détail F5, refus d'édition agent, correction responsable, CSV et déconnexion vérifiés par HTTP. Cette instance temporaire a été arrêtée.

La CI exécute deux jobs Java 21 : H2 et PostgreSQL 17 temporaire. **75 tests réussis sur chacun des deux moteurs**, sans échec ni erreur, dans l'[exécution de référence](https://github.com/VincentCarret/railpulse/actions/runs/40544261917). Les quatre contextes utilisent PostgreSQL sans repli H2, avec des schémas isolés. Les rapports et le JAR sont conservés comme artifacts ; les nouveaux passages sont visibles dans Actions.

La couverture JaCoCo est une mesure d'exécution, pas une preuve complète de conformité. Le rendu visuel, l'accessibilité complète, la charge, l'environnement Supabase réel et la compréhension orale restent des contrôles distincts. Les tests F11 (47/48/72 h) seront ajoutés avec le calcul d'urgence ; les dates de démonstration seules ne prouvent pas ce calcul.

## CHOIX TECHNIQUES

### Droits par profil (à5.6)

| N° | Fonctionnalité | AGENT | RESPONSABLE_EXPLOITATION |
|---|---|---|---|
| F2–F3 | équipements et fiches | Lecture seule | Lecture seule |
| F4 | Déclarer un incident | Accès complet | Accès complet |
| F5–F6 | Consulter / modifier | Lecture F5 ; F6 interdit | Lecture F5 ; correction de description des actifs F6 |
| F7 | Affecter / réaffecter | Interdit | Accès complet sur les incidents actifs |
| F8 | Résoudre | Interdit | Accès complet sur les incidents actifs |
| F9 | Historique | Lecture seule | Lecture seule |
| F10 | évaluer | Interdit | Accès complet prévu |
| F11 | Tableau de bord | Interdit | Lecture prévue |

F10–F11 décrivent les choix pour la suite ; leurs écrans/opérations ne sont pas réalisés. L'export CSV reprend les droits de lecture du catalogue.

F6 ne modifie que la description d'un incident actif : les identifiants, dates, équipement, statut et affectation restent inchangés. Les résolus sont immuables pour préserver l'historique. Les routes et `@PreAuthorize` vérifient le rôle ; `Incident` vérifie l'état. La présence/absence du bouton n'est pas la protection serveur.

### Règles et exceptions

- RG1/RG2 : incident lié à l'équipement de l'URL, ouverture calculée par `Clock`, statut initial non affecté sans prestataire.
- RG5 : le modèle interdit résolution antérieure à l'ouverture et réaffectation d'un résolu ; F7 et F8 exposent ces transitions avec verrouillage et transaction.
- RG7 : incidents d'un équipement triés par ouverture décroissante, puis identifiant décroissant. L'historique global F9 est trié par résolution décroissante, puis identifiant décroissant en cas d'égalité.
- RG8 : évaluation d'un résolu avec prestataire, note 1à5, date cohérente et unicité en base ; F10 n'a pas encore d'interface.
- RG9 : référence d'équipement unique en base, testée entre stations.
- RG10/RG11 : transactions, contrôles d'état et droits côté serveur. Les seuils/calculs et listes F11 liés à RG3/RG4/RG6 restent à réaliser.

`@ControllerAdvice` est conservé pour uniformiser les pages d'erreur. Les exceptions personnalisées utilisent des messages français : 400 pour une saisie/identifiant invalide, 403 pour un droit insuffisant, 404 pour une ressource absente, 409 pour un état interdit ou un conflit. Une erreur inattendue renvoie 500 avec un message neutre ; aucune trace SQL n'est envoyée dans la page. `ResponseEntity` n'est pas nécessaire aux vues actuelles ; le CSV écrit des en-têtes HTTP explicites.

### SOLID

- **SRP appliqué** : contrôleurs HTTP, orchestration dans les services, invariants dans `Incident`/`Evaluation`, accès aux données dans les repositories, format CSV dans son service et erreurs dans l'advice. Chaque responsabilité a une raison de changement différente.
- **DIP appliqué au découpage utilisé** : injection par constructeur d'interfaces de repository et de `PasswordEncoder`, au lieu d'implémentations JDBC construites par les services.
- **OCP** : aucune hiérarchie de stratégies artificielle ; les points d'extension Spring suffisent aux besoins actuels. Une stratégie sera extraite si un véritable point de variation apparaît.
- **LSP** : pas de hiérarchie métier de sous-types interchangeables à introduire ; les contrats Spring sont utilisés sans sous-types artificiels.
- **ISP** : repositories séparés par entité et services ciblés. Ajouter un wrapper par méthode JPA ne répondrait à aucun consommateur distinct.

### Persistance et unique requête SQL native (à7.6)

Tous les repositories actifs déclarent une requête utile. Les requêtes dérivées (utilisateurs, stations, évaluations, tri du TP) complètent les JPQL (catalogue, détails, historique d'équipement et verrouillage).

L'unique requête native de `src/main/java` se trouve dans `EquipementRepository.exporterCatalogue`. Elle joint équipements et stations et renvoie uniquement les cinq colonnes du CSV, déjà triées, au lieu d'hydrater des entités et d'accéder à des associations par ligne.

**Analyse critique :** la requête contrôle directement colonnes et jointure, mais dépend des noms physiques de tables/colonnes et d'une projection dont les alias doivent rester cohérents. JPQL aurait offert une abstraction plus forte ; le choix natif est limité à cet export et satisfait l'exercice, sans disperser du SQL dans les contrôleurs. Le SQL produit utilise une jointure et un tri standards ; `{h-schema}` est un jeton spécifique à Hibernate pour qualifier correctement les tables selon le schéma configuré. Il apporte donc une dépendance au fournisseur JPA, explicitement assumée. Le double passage H2/PostgreSQL vérifie aussi ce point.

Référence : [Hibernate à schémas des requêtes natives](https://docs.hibernate.org/stable/orm/userguide/html_single/#sql-global-catalog-schema).

### Transactions

- Lectures `EquipementService` et `IncidentService` : `readOnly=true` pour regrouper les requêtes et charger les relations nécessaires aux vues.
- Déclaration F4 : transaction d'écriture regroupant lecture de l'équipement et insertion d'incident.
- Correction F6 : verrou `PESSIMISTIC_WRITE` sur la ligne incident avant le contrôle d'état ; le dirty checking persiste la correction sans recopier les champs client. La dernière correction valide est conservée, sans fusion de deux descriptions concurrentes.
- Initialisations : catalogue complet et deux comptes créés chacun dans leur transaction, avec rollback si une condition échoue. Un compte existant n'est ni réaffecté ni écrasé.
- Migration BCrypt : une transaction pour les comptes à migrer ; format historique inconnu/vide = rollback complet et démarrage refusé. Aucun secret n'est journalisé.
- Export : transaction de lecture pour extraire une vue cohérente du catalogue.
- Le CRUD TP conserve ses transactions existantes ; les modifications d'entités gérées utilisent le dirty checking.

### Sécurité et configuration

Le `PasswordEncoder` n'accepte que BCrypt. `User` impose un format BCrypt sur les nouvelles écritures JPA. `AuthService` n'accepte plus le clair ni `{noop}` : leur conversion est une migration de données au démarrage, pas une méthode d'authentification.

Les deux secrets demo sont fournis par environnement ou saisie locale ; aucun mot de passe par défaut n'est inscrit dans le code. Le fallback MySQL a été supprimé et sa configuration locale préservée hors Git. `.env`, `.env.*` et les bases locales sont ignorés ; `.env.example` ne contient que des valeurs vides ou des paramètres de connexion factices.

La suppression d'une valeur du code courant ne réécrit pas l'historique Git. Une ancienne valeur de base encore utilisée doit être renouvelée par son propriétaire. Les tests utilisent uniquement des secrets temporaires sans lien avec la production.

## LOT ALPHA

### 1. Qualité renforcée à piste 7

Besoin : détecter les régressions du socle et démontrer le comportement réel des droits, de la persistance et des exceptions.

Réalisé : tests HTTP/JPA et métier, horloge fixe, scénarios invalides et champs falsifiés, migration/rollback, JaCoCo, artifacts et deux jobs CI sur Java 21 avec H2 et PostgreSQL. Cela dépasse la seule présence de validation : les transactions, droits et formats produits sont exercés.

Limites : couverture d'exécution distincte de l'ergonomie et des performances. MySQL n'est pas inclus dans la nouvelle matrice, car le déploiement cible PostgreSQL. F10–F12 nécessiteront leurs propres tests.

### 2. Export de données à piste 9

Besoin : obtenir un inventaire exploitable du réseau dans un tableur sans accès direct à la base.

Réalisé : export CSV du catalogue, accessible aux deux profils depuis F2. UTF-8 avec BOM, séparateur point-virgule, guillemets et retours ligne échappés, formules neutralisées, nom de fichier et absence de cache explicites. Une seule requête native produit une projection plate triée. Catalogue vide et droits testés.

Limites : catalogue exporté en mémoire, adapté au volume de démonstration ; un export paginé/streamé serait préférable pour un réseau très volumineux. L'export de l'historique résolu n'est pas réalisé.

Ces deux choix appartiennent aux pistes proposées au à6. La sécurité F1 et CSRF ne sont pas comptés comme des alpha supplémentaires.

## BILAN

### Groupe et traçabilité

| Membre | Traces techniques observables dans Git |
|---|---|
| Amir Nassiri | Contribution initiale CI/CD et déploiement VPS, commit `58646e2` signé `amirnssr`. |
| Vincent Carret | Configuration, intégration des branches, puis commits F2–F6, tests et documentation sous `vincent` / `vincent.carret`. |

Les noms du groupe ont été fournis par les membres. Cette table décrit l'historique, pas une mesure de temps ni une répartition complète du travail hors Git. Chaque membre doit compléter/valider sa contribution réelle et savoir expliquer la solution. L'assistance Codex pour le code, les tests et les revues est déclarée ; elle ne prouve pas la maîtrise orale du groupe.

PR déjà intégrées : F1/CI (#1à#2), F2 (#3), Render (#4), F3 (#5), F4 (#6), F5 (#7), F6 (#8). Les corrections de conformité sont isolées sur `fix/conformite-mi-parcours`, avec PR de revue.

### Difficultés techniques observées

- L'acceptation du clair à la connexion masquait un écart de sécurité : migration transactionnelle préalable, BCrypt seul à la connexion et validation de persistance.
- Une valeur de secours MySQL et l'absence de comptes reproductibles empêchaient une configuration sûre : secrets hors Git, comptes paramétrés et script autonome H2.
- Une CI annonçait MySQL alors que les tests forçaient H2 : correction des propriétés de tous les contextes et ajout d'un vrai job PostgreSQL avec schémas isolés.

Ces difficultés sont attestées par les corrections ; le groupe doit préciser laquelle il considère comme sa difficulté principale.

### Reste à faire

F10–F12 : saisie d'évaluation, tableau de bord/urgence (48/72 h) et API REST documentée OpenAPI. Les démonstrations navigateur sur Render, la rotation éventuelle d'un ancien secret et la validation humaine du bilan restent à effectuer. La conformité finale n'est pas déclarée acquise à mi-parcours.

Trois acquis : séparer HTTP, métier et données ; faire respecter invariants/droits côté serveur ; vérifier les succès et les refus par des tests reproductibles sur des bases isolées.
