# Journal à Rail Pulse (ALTN72, 2026-2027)

**Groupe : Amir Nassiri et Vincent Carret.** état au 30 octobre 2026, périmètre F1 à F12 sur cette branche.

## README

Le [README](README.md) contient les prérequis Java 21, les commandes Maven et le lancement autonome sur H2 avec `scripts/demarrer-demo.ps1`. Une base neuve initialise le catalogue et les deux profils ; une base existante est conservée.

- Application prévue : https://railpulse-23pc.onrender.com. Le 30 octobre 2026, `/login` n'a renvoyé aucun octet pendant un contrôle de 90 secondes ; l'accessibilité publique et la version déployée doivent être rétablies et vérifiées.
- Comptes : `demo.agent` / `RAILPULSE_DEMO_AGENT_PASSWORD` et `demo.responsable` / `RAILPULSE_DEMO_RESPONSABLE_PASSWORD`. L'évaluateur définit ses propres mots de passe au lancement ; ils sont hachés avant insertion et ne sont pas versionnés.
- Base locale : H2 relationnelle dans `.data/`, ignorée par Git. PostgreSQL/Supabase pour Render, MySQL disponible en profil explicite. Le script local force H2 pour protéger les bases distantes d'une configuration héritée.
- Initialisation : JPA pour le schéma, `ComptesDemonstrationAuDemarrage` et `DonneesDemonstration` pour les données. Migration BCrypt préalable. Les initialisations sont transactionnelles et idempotentes ; aucun écrasement de compte/catalogue existant.
- Limites : le service Render n'était pas joignable lors du dernier contrôle. `ddl-auto=update` est un choix pédagogique, pas une stratégie de migration de schéma de production.

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
| F10 | Responsable autorisé, incident résolu avec prestataire, note de 1 à 5, commentaire facultatif, unicité, date serveur, validation, CSRF, champs falsifiés, droits HTTP et service, absence de mutation sur refus. |
| F11 | 48/72 h à la seconde près, recalcul avec l'horloge, affecté distinct, résolus exclus, tri par attention, filtres station et prise en charge, indicateurs de sélection, accès réservé au responsable et échappement HTML. |
| F12 | Liste et détail JSON pour les deux profils, résolution par le responsable avec date serveur, 400/404/409, CSRF et refus sans mutation, contrat OpenAPI servi après connexion. |
| Sécurité des comptes | Clair et `{noop}` refusés à la connexion ; conversion préalable en BCrypt, idempotence, rollback d'une migration invalide et refus de nouvelles écritures non hachées. |
| Démonstration | Création transactionnelle des deux comptes, secrets absents refusés, profils préexistants protégés, connexion réelle, catalogue varié et relancement sans duplication. |
| Export CSV | Droits des deux profils, format/encodage/en-têtes, catalogue vide, guillemets/séparateurs/retours ligne et neutralisation des formules. |

Vérification locale après retrait du module hors sujet : **98 tests réussis, aucun échec ni erreur** avec `clean verify`, JAR et rapport JaCoCo générés (JDK 21.0.2). Le JAR ne contient plus les classes ni les vues de ce module.
Une copie propre de `master` (`ba9ff69`) a été clonée dans un nouveau dossier le 30 octobre 2026 : `mvnw clean verify` y a réussi avec **98 tests, 0 échec, 0 erreur**. Le JAR issu de cette copie a démarré sur une base H2 fichier neuve avec les profils `demo-local,demo` et deux secrets temporaires ; `/login` a répondu 200 et `/api/incidents` a redirigé le visiteur vers `/login` (302). L'instance de contrôle a ensuite été arrêtée.
Le JAR a aussi été démarré sans configuration de test sur une base H2 fichier neuve : connexion des deux comptes, F2/F3, déclaration F4, détail F5, refus d'édition agent, correction responsable, CSV et déconnexion vérifiés par HTTP. Cette instance temporaire a été arrêtée.

La CI exécute deux jobs Java 21 : H2 et PostgreSQL 17 temporaire. Les deux jobs ont réussi sur le dernier commit de nettoyage dans l'[exécution du 30 octobre 2026](https://github.com/VincentCarret/railpulse/actions/runs/41032371010). Les contextes PostgreSQL utilisent ce moteur sans repli H2, avec des schémas isolés. Les rapports et le JAR sont conservés comme artifacts ; les nouveaux passages sont visibles dans Actions.

La couverture JaCoCo est une mesure d'exécution, pas une preuve complète de conformité. Le rendu visuel, l'accessibilité complète, la charge, l'environnement Supabase réel et la compréhension orale restent des contrôles distincts. Les tests F11 vérifient les frontières exactes de 48/72 h et le recalcul de l'attention ; les tests F12 exercent de vrais échanges HTTP et leurs refus.

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
| F10 | Évaluer / consulter la note | Lecture seule | Accès complet après résolution avec prestataire |
| F11 | Tableau de bord | Interdit | Lecture seule |
| F12 | API des incidents | Lecture seule | Lecture et résolution des actifs |

Le tableau de bord F11 est réservé au responsable côté serveur. L'export CSV reprend les droits de lecture du catalogue.

### API REST F12

`GET /api/incidents` liste tous les incidents par ouverture décroissante ; `GET /api/incidents/{id}` donne le détail. La réponse expose uniquement les champs utiles, sans sérialiser directement les entités JPA ni le contact du prestataire. Les dates sont transmises en UTC ISO 8601. `POST /api/incidents/{id}/resolution` reprend `IncidentService.resoudreIncident` : même verrou, mêmes contrôles d'état et même horloge que F8. Le responsable seul peut écrire ; une seconde résolution ou une date d'ouverture future est refusée sans changement. L'API renvoie un JSON d'erreur pour les identifiants mal formés (400), les incidents absents (404) et les transitions invalides (409).

La [spécification OpenAPI 3](src/main/resources/static/api/openapi.yaml) est servie à `/api/openapi.yaml` après connexion. L'authentification utilise la session existante ; `GET /api/csrf` fournit le jeton à transmettre dans `X-CSRF-TOKEN` pour le POST. Les utilisateurs non connectés sont redirigés vers `/login`, comme les autres pages protégées. Le contrat décrit les schémas, droits, codes et procédure CSRF ; il n'ajoute pas de secret ni de second mécanisme d'authentification.

F6 ne modifie que la description d'un incident actif : les identifiants, dates, équipement, statut et affectation restent inchangés. Les résolus sont immuables pour préserver l'historique. Les routes et `@PreAuthorize` vérifient le rôle ; `Incident` vérifie l'état. La présence/absence du bouton n'est pas la protection serveur.

### Règles et exceptions

- RG1/RG2 : incident lié à l'équipement de l'URL, ouverture calculée par `Clock`, statut initial non affecté sans prestataire.
- RG5 : le modèle interdit résolution antérieure à l'ouverture et réaffectation d'un résolu ; F7 et F8 exposent ces transitions avec verrouillage et transaction.
- RG7 : incidents d'un équipement triés par ouverture décroissante, puis identifiant décroissant. L'historique global F9 est trié par résolution décroissante, puis identifiant décroissant en cas d'égalité.
- RG8 : évaluation d'un résolu avec prestataire, note de 1 à 5, date serveur cohérente et unicité en base. F10 vérifie l'éligibilité sous verrou avant l'enregistrement.
- RG9 : référence d'équipement unique en base, testée entre stations.
- RG3/RG4/RG6 : seuils définis dans `NiveauAttention`, attention calculée avec l'horloge à la lecture, état distinct pour les affectés et seuls les incidents actifs sont chargés.
- RG10/RG11 : transactions, contrôles d'état et droits côté serveur.

`@ControllerAdvice` est conservé pour uniformiser les pages d'erreur. Les exceptions personnalisées utilisent des messages français : 400 pour une saisie/identifiant invalide, 403 pour un droit insuffisant, 404 pour une ressource absente, 409 pour un état interdit ou un conflit. Une erreur inattendue renvoie 500 avec un message neutre ; aucune trace SQL n'est envoyée dans la page. `ResponseEntity` n'est pas nécessaire aux vues actuelles ; le CSV écrit des en-têtes HTTP explicites.

### SOLID

- **SRP appliqué** : contrôleurs HTTP, orchestration dans les services, invariants dans `Incident`/`Evaluation`, accès aux données dans les repositories, format CSV dans son service et erreurs dans l'advice. Chaque responsabilité a une raison de changement différente.
- **DIP appliqué au découpage utilisé** : injection par constructeur d'interfaces de repository et de `PasswordEncoder`, au lieu d'implémentations JDBC construites par les services.
- **OCP** : aucune hiérarchie de stratégies artificielle ; les points d'extension Spring suffisent aux besoins actuels. Une stratégie sera extraite si un véritable point de variation apparaît.
- **LSP** : pas de hiérarchie métier de sous-types interchangeables à introduire ; les contrats Spring sont utilisés sans sous-types artificiels.
- **ISP** : repositories séparés par entité et services ciblés. Ajouter un wrapper par méthode JPA ne répondrait à aucun consommateur distinct.

### Persistance et unique requête SQL native (à7.6)

Tous les repositories actifs déclarent une requête utile. Les requêtes dérivées (utilisateurs, stations, prestataires, évaluations) complètent les JPQL (catalogue, détails, historique d'équipement et verrouillage).

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

### Sécurité et configuration

Le `PasswordEncoder` n'accepte que BCrypt. `User` impose un format BCrypt sur les nouvelles écritures JPA. `AuthService` n'accepte plus le clair ni `{noop}` : leur conversion est une migration de données au démarrage, pas une méthode d'authentification.

Les deux secrets demo sont fournis par environnement ou saisie locale ; aucun mot de passe par défaut n'est inscrit dans le code. Le fallback MySQL a été supprimé et sa configuration locale préservée hors Git. `.env`, `.env.*` et les bases locales sont ignorés ; `.env.example` ne contient que des valeurs vides ou des paramètres de connexion factices.

La suppression d'une valeur du code courant ne réécrit pas l'historique Git. Une ancienne valeur de base encore utilisée doit être renouvelée par son propriétaire. Les tests utilisent uniquement des secrets temporaires sans lien avec la production.

## LOT ALPHA

### 1. Qualité renforcée à piste 7

Besoin : détecter les régressions du socle et démontrer le comportement réel des droits, de la persistance et des exceptions.

Réalisé : tests HTTP/JPA et métier, horloge fixe, scénarios invalides et champs falsifiés, migration/rollback, JaCoCo, artifacts et deux jobs CI sur Java 21 avec H2 et PostgreSQL. Cela dépasse la seule présence de validation : les transactions, droits et formats produits sont exercés.

Limites : couverture d'exécution distincte de l'ergonomie et des performances. MySQL n'est pas inclus dans la nouvelle matrice, car le déploiement cible PostgreSQL. Les cinq tests HTTP de F12 couvrent le contrat et les refus principaux.

### 2. Export de données à piste 9

Besoin : obtenir un inventaire exploitable du réseau dans un tableur sans accès direct à la base.

Réalisé : export CSV du catalogue, accessible aux deux profils depuis F2. UTF-8 avec BOM, séparateur point-virgule, guillemets et retours ligne échappés, formules neutralisées, nom de fichier et absence de cache explicites. Une seule requête native produit une projection plate triée. Catalogue vide et droits testés.

Limites : catalogue exporté en mémoire, adapté au volume de démonstration ; un export paginé/streamé serait préférable pour un réseau très volumineux. L'export de l'historique résolu n'est pas réalisé.

Ces deux choix appartiennent aux pistes proposées au à6. La sécurité F1 et CSRF ne sont pas comptés comme des alpha supplémentaires.

## BILAN

### Groupe et traçabilité

| Membre | Traces techniques observables dans Git |
|---|---|
| Amir Nassiri | CI/CD initiale (`58646e2`), commits F7–F12 (`6a19d20` à `6208ce2`), retrait des restes hors sujet (`98ad467`) et intégration des PR #10 à #16 sous `amirnssr` / `Amir N`. |
| Vincent Carret | Authentification, configurations MySQL/Supabase, F2–F6 (`6f2c7c7` à `934b827`), déploiement Docker/Render, correctifs de conformité (`9f6c64b`), tests, documentation et intégration des premières PR sous `vincent` / `vincent.carret` / `Vins`. |

Les noms du groupe ont été fournis par les membres. Cette table décrit l'historique, pas une mesure de temps ni une répartition complète du travail hors Git. Chaque membre doit valider sa contribution réelle et savoir expliquer la solution. L'assistance Codex pour le code, les tests et les revues est déclarée ; elle ne prouve pas la maîtrise orale du groupe.

Les PR F7–F12 (#10 à #15) et le retrait des restes hors sujet (#16) sont intégrés à `master`. La présente mise à jour documentaire reste sur une branche distincte jusqu'à sa revue.

### Difficultés techniques observées

- L'acceptation du clair à la connexion masquait un écart de sécurité : migration transactionnelle préalable, BCrypt seul à la connexion et validation de persistance.
- Une valeur de secours MySQL et l'absence de comptes reproductibles empêchaient une configuration sûre : secrets hors Git, comptes paramétrés et script autonome H2.
- Une CI annonçait MySQL alors que les tests forçaient H2 : correction des propriétés de tous les contextes et ajout d'un vrai job PostgreSQL avec schémas isolés.

La difficulté technique principale documentée par l'historique est la reproductibilité des tests sur deux moteurs relationnels. Une CI annonçait MySQL alors que les tests utilisaient H2 ; le groupe a supprimé cette ambiguïté en paramétrant tous les contextes de test et en ajoutant un job PostgreSQL réel avec des schémas isolés. Les deux jobs réussissent désormais sur le dernier commit vérifié. Le groupe doit confirmer que ce choix reflète bien son expérience lors de la soutenance.

### Reste à faire

Les fonctionnalités F1 à F12 et les deux fonctionnalités du lot alpha sont présentes dans `master` ; aucune fonctionnalité obligatoire du socle n'est identifiée comme non réalisée. Les parcours authentifiés sur Render, l'accessibilité publique, la rotation éventuelle d'un ancien secret et la validation humaine du bilan restent à effectuer. La copie propre et la CI réussies ne prouvent pas à elles seules la conformité finale du déploiement ni du rendu.

Trois acquis : séparer HTTP, métier et données ; faire respecter invariants/droits côté serveur ; vérifier les succès et les refus par des tests reproductibles sur des bases isolées.
