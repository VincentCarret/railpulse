# Journal de développement — Rail Pulse

## README — Périmètre et règles retenues

Ce journal décrit le code effectivement réalisé, ses vérifications et ses limites.
Le projet utilise Java 21, Spring Boot, Spring MVC, Thymeleaf, Spring Data JPA et PostgreSQL/Supabase. Le profil MySQL local reste disponible pour compatibilité.
Les explications du corrigé initial sont conservées dans [les notes du TP 5](docs/journal-tp5.md).

Le sujet de référence est **ALTN72 — Projet final 2026-27**, notamment §5.2 (F2), §5.6 (droits), RG9, RG11 et §7 (architecture, persistance, tests).
F1 à F5 sont réalisées : connexion, équipements par station, fiche équipement, déclaration et détail d'un incident.
Le CRUD de programmeurs provient du TP et sert de support aux formulaires, transactions et tests.
Les fonctionnalités F6 à F12 ne sont pas encore réalisées.
Les instructions de configuration, de lancement et d'initialisation F2 sont dans le [README](README.md#f2--équipements-par-station).
L'URL déployée, les comptes de démonstration et les noms du groupe restent à renseigner avant le rendu.

| Règle | Mise en œuvre et justification |
|---|---|
| Login existant et mot de passe valide | Spring Security vérifie le compte de la table `users`. |
| Erreur d'identification | Retour au même formulaire, message générique ne révélant pas si le compte existe. |
| Utilisateur connecté | Prénom, profil et formulaire de déconnexion dans le bandeau commun. |
| Déconnexion | POST protégé par CSRF, invalidation de la session, suppression du cookie, retour à `/login`. |
| Profils | `AGENT` et `RESPONSABLE_EXPLOITATION`. F2 autorisée en lecture pour les deux, avec contrôle des rôles côté serveur. |
| Catalogue F2 | Choisir une station et retrouver uniquement ses équipements avec référence, libellé, type et état. |
| RG9 | Contrainte unique en base sur la référence d'équipement, commune à toutes les stations. |
| Station absente / vide | Page 404 avec message français / page 200 avec état vide explicite. |
| Nom et prénom des programmeurs | Obligatoires, non blancs, 100 caractères maximum, en cohérence avec les colonnes existantes. |
| Autres champs textuels | Facultatifs ; adresse limitée à 255 caractères, langage à 100, livre à 150, conformément au schéma. |
| Salaire | Facultatif, fini et positif ou nul. Le refus des valeurs négatives est une hypothèse explicite du projet, à confirmer si le sujet évolue. Aucune devise ou périodicité n'est supposée. |
| Création | Génère un nouvel identifiant ; un identifiant envoyé par le client ne peut pas écraser une ligne. |
| Modification | Conserve l'identifiant de l'URL et n'autorise que les champs du formulaire. |
| Ressource inexistante | Réponse 404 pour la lecture, la modification et la suppression. Une modification ne crée pas une ligne manquante. |
| Saisie invalide | Le formulaire conserve les valeurs saisies et affiche les erreurs ; la base reste inchangée. |
| Liste vide | État vide explicite et lien pour créer le premier programmeur. |

## ARCHITECTURE

### Vue d'ensemble

Rail Pulse suit une architecture en couches de type MVC enrichie par une couche de service et une couche de persistance. Une requête traverse les composants dans cet ordre : navigateur, contrôleur Spring MVC, service métier, repository Spring Data JPA, base de données. La réponse revient ensuite vers le contrôleur, qui fournit un modèle à Thymeleaf pour produire la page HTML.

```text
Navigateur
    │ HTTP / formulaire / session
    ▼
Contrôleurs MVC ──► Services applicatifs ──► Repositories JPA ──► PostgreSQL Supabase
    │                      │                       │
    └──────────────► Modèles Thymeleaf       Entités JPA
```

Le contrôleur ne contient ni requête SQL ni règle de validation métier complexe. Le service orchestre l'opération et définit la transaction. Le repository fournit l'accès aux données. Cette séparation permet de tester chaque responsabilité et de changer le moteur de base sans réécrire les écrans.

### Responsabilités des couches

- Présentation : `EquipementControleur` expose `/stations` et `/stations/{stationId}/equipements` ; `ProgrammeurControleur` conserve le CRUD du TP. Les templates Thymeleaf affichent les données ; les fragments partagent le bandeau et les champs communs.
- Sécurité : `SecurityConfig` protège les routes privées, gère `/login`, la déconnexion POST et CSRF. `AuthService` adapte les utilisateurs au contrat Spring Security et compare les mots de passe BCrypt.
- Application : `EquipementService` vérifie l'existence d'une station et fournit ses équipements ; `ProgrammeurService` conserve les opérations du TP.
- Domaine : `Station`, `Equipement`, `TypeEquipement` et `EtatFonctionnement` décrivent le catalogue ; `Programmeur`, `User` et `Profil` restent disponibles.
- Persistance : `StationRepository` et `EquipementRepository` complètent les repositories existants. Le profil `supabase` utilise PostgreSQL dans le schéma `railpulse`, tandis que `mysql` conserve la base locale.

Dépôt : https://github.com/VincentCarret/railpulse

Flux F2 : navigateur → `EquipementControleur` → `EquipementService` → repositories → tables `station` / `equipement`, puis rendu `equipements.html`. La relation équipement/station est obligatoire et chargée à la demande ; la page n'accède pas à une relation non chargée.

### Flux de connexion

1. Le navigateur demande `/login`, seule page publique.
2. Spring Security délègue la recherche du compte à `AuthService`.
3. Le mot de passe est comparé au hash BCrypt stocké dans `railpulse.users`.
4. En cas d'échec, le formulaire est réaffiché avec un message générique.
5. En cas de succès, la session est renouvelée et le bandeau affiche le prénom et le profil.
6. La déconnexion invalide la session et redirige vers `/login`.

### Flux CRUD

Un formulaire POST passe par Jakarta Validation puis par `ProgrammeurService`. Le service enregistre l'entité dans une transaction via JPA ; le contrôleur redirige ensuite vers la liste selon le schéma Post/Redirect/Get. Les identifiants sont pris depuis l'URL et les ressources absentes renvoient 404. `GlobalExceptionHandler` transforme les erreurs attendues en pages et statuts HTTP cohérents.

- Les contrôleurs traitent les requêtes HTTP et préparent les modèles de vues.
- Les services portent les opérations applicatives et les limites transactionnelles.
- Les interfaces de repository délèguent la persistance à Spring Data JPA.
- Les entités utilisent les annotations Jakarta Persistence et Jakarta Validation.
- Thymeleaf utilise les modèles, le binding des formulaires et des fragments communs.
- `HiddenHttpMethodFilter` permet les modifications PUT et suppressions DELETE depuis des formulaires HTML.
- Le schéma Post/Redirect/Get évite de rejouer une écriture lors du rafraîchissement après succès.
- Le dirty checking JPA enregistre la modification de l'entité gérée dans la transaction.

## Clean Code

Les anciennes versions commentées des contrôleurs, services et écrans ont été retirées du code actif et conservées dans l'historique Git et les notes du TP.
Les dépendances applicatives sont injectées par constructeur et conservées dans des champs `final`.
Les modifications de propriétés sont explicites plutôt qu'effectuées par copie réflexive de toutes les propriétés.
Les champs HTML sont partagés dans un fragment unique, et le bandeau utilisateur dans un autre.
Les logs utilisent SLF4J ; les messages techniques ne sont pas montrés à l'utilisateur.
Les tests d'authentification et de CRUD sont séparés et partagent un support HTTP de test, sans héritage métier artificiel.

## CHOIX TECHNIQUES

### Droits par profil (§5.6)

Les droits des fonctionnalités futures sont la répartition retenue pour la suite ; F1 à F5 sont implémentées aujourd'hui.

| N° | Fonctionnalité | AGENT | RESPONSABLE_EXPLOITATION |
|---|---|---|---|
| F2–F3 | Consulter équipements et fiches | Lecture seule | Lecture seule |
| F4 | Déclarer un incident | Accès complet | Accès complet |
| F5–F6 | Consulter / modifier un incident | Lecture seule | Accès complet |
| F7 | Affecter ou réaffecter un prestataire | Interdit | Accès complet |
| F8 | Déclarer un incident résolu | Interdit | Accès complet |
| F9 | Consulter l'historique | Lecture seule | Lecture seule |
| F10 | Évaluer un prestataire | Interdit | Accès complet |
| F11 | Tableau de bord | Interdit | Lecture seule |

Le responsable pilote les affectations, la résolution et l'évaluation ; l'agent signale les pannes et consulte le suivi.
F6 sera limitée à la description d'un incident encore actif ; les transitions d'état auront leurs opérations dédiées (F7/F8).
`SecurityConfig` exige l'un des deux rôles pour les GET `/stations` et `/stations/**` (RG11). Aucun endpoint d'écriture du catalogue n'est exposé par F2.

### Exceptions, requêtes et transactions F2

- `StationIntrouvableException` porte un message français et `GlobalExceptionHandler` le traduit en page 404. Le même `@ControllerAdvice` conserve l'homogénéité des erreurs du projet et masque les détails techniques. Un identifiant mal formé renvoie 400.
- `StationRepository.findAllByOrderByNomAscIdAsc` est une requête dérivée ; `EquipementRepository.rechercherParStation` est une requête JPQL explicite. Le filtrage par station et le tri par référence sont effectués en base.
- Aucune requête native n'est ajoutée artificiellement pour F2. L'unique requête SQL native exigée au §7.6 reste à réaliser et à analyser dans une prochaine fonctionnalité adaptée.
- `EquipementService` utilise `@Transactional(readOnly = true)` pour regrouper les lectures de station et d'équipements dans une transaction sans écriture.
- `DonneesDemonstration.run` utilise une transaction d'écriture : stations et équipements de démonstration sont créés ensemble ou annulés ensemble (RG10). Le profil `demo` est explicite et ignore un catalogue déjà renseigné.
- Les enums sont persistées par leur nom, pour éviter qu'un réordonnancement change le sens des valeurs stockées. Les états de fonctionnement de F2 sont distincts des futurs indicateurs d'urgence des incidents (RG3/RG4).

### SOLID : principes appliqués et limites justifiées

### SRP — responsabilité unique : appliqué

`ProgrammeurControleur` peut changer si le parcours HTTP évolue ; `ProgrammeurService` si les opérations applicatives évoluent ; `ProgrammeurRepository` si les besoins d'accès aux données évoluent.
`GlobalExceptionHandler` centralise la traduction des exceptions en pages et statuts HTTP.
Le même découpage s'applique à F2 : contrôleur HTTP, service de consultation, repositories de catalogue et initialisation de démonstration séparés.
Les vues ne réalisent aucune requête SQL. Les services ne choisissent aucun template HTML.
Ces responsabilités ont des raisons de changement différentes et restent séparées.

### DIP — inversion des dépendances : appliqué

`ProgrammeurService` dépend de l'interface `ProgrammeurRepository`, pas d'une implémentation JDBC écrite dans le service.
Spring fournit l'implémentation au constructeur. Les contrôleurs ne construisent pas leurs propres services ou connexions SQL.
L'authentification s'appuie également sur les contrats `UserDetailsService`, `UserDetailsPasswordService` et `PasswordEncoder` de Spring Security.
Changer le mécanisme de hachage n'impose pas de modifier le contrôleur ou le formulaire de connexion.

### OCP — ouvert/fermé : pas de mécanisme métier supplémentaire

Le framework offre déjà des points d'extension (encodeurs de mots de passe, repositories, gestionnaires d'erreurs).
Le domaine actuel ne comporte pas plusieurs stratégies de calcul ou variantes de traitement.
Ajouter une hiérarchie de stratégies uniquement pour illustrer OCP compliquerait le code sans besoin identifié.
Si plusieurs variantes métier apparaissent, leur point de variation pourra être extrait dans une interface.

### LSP — substitution de Liskov : pas de hiérarchie métier à introduire

Les entités métier ne sont pas organisées en sous-classes interchangeables.
Créer des sous-types artificiels n'aiderait pas à satisfaire les règles actuellement connues.
Les contrats des interfaces du framework sont respectés, notamment les valeurs de retour et exceptions de `UserDetailsService`.
Cela ne constitue pas une preuve générale de tous les contrats LSP possibles du framework.

### ISP — ségrégation des interfaces : pas de découpage supplémentaire pertinent

Les dépôts sont déjà séparés par entité, et les services ne dépendent pas d'un service universel regroupant toutes les fonctionnalités.
Spring Security sépare notamment la lecture d'un compte et la mise à jour de son mot de passe en deux interfaces.
Ajouter des wrappers reproduisant chaque méthode de `JpaRepository` n'apporterait pas de valeur dans ce petit périmètre.
Un découpage supplémentaire deviendrait pertinent si des consommateurs avaient des besoins nettement différents.

## LOT ALPHA

### Socle F1 : sécurité et contexte de session

- Contrôle d'accès commun aux routes privées, indépendamment du contrôleur.
- Connexion par formulaire, message d'échec, session HTTP et déconnexion.
- Protection CSRF des formulaires d'écriture et de déconnexion.
- Rotation de l'identifiant de session après connexion et invalidation après déconnexion.
- Bandeau partagé via un fragment et un `ControllerAdvice`.
- BCrypt pour les mots de passe ; migration des anciens mots de passe en clair seulement après une identification réussie.

Ces mécanismes répondent au socle F1 et à la sécurité §7.7 ; ils ne comptent pas comme une fonctionnalité alpha supplémentaire.

### Piste 7 : qualité renforcée — travail engagé

- Contraintes Jakarta Validation sur les données ; validation au niveau des formulaires et du service.
- Messages français par champ, réaffichage de la saisie et absence d'écriture en cas d'erreur.
- Gestion centralisée des ressources inexistantes, paramètres invalides, conflits de données et erreurs inattendues.
- Conservation des statuts HTTP 400, 403, 404, 405, 409 et 500 selon le cas.
- Page d'erreur commune, lien de retour et bandeau utilisateur si une session est ouverte.
- Masquage des détails SQL, messages internes et traces techniques dans les réponses.

La validation ne dépend pas du seul JavaScript du navigateur ; elle reste active pour une requête HTTP fabriquée manuellement.
Les tests d'intégration HTTP/JPA, JaCoCo et l'intégration continue existante contribuent à la piste 7 du sujet. F2 ajoute les tests du catalogue, du filtrage, des cas vides, de RG9 et de l'initialisation de démonstration.
La validation et les exceptions font aussi partie du socle technique : leur seule présence ne suffit pas à démontrer deux fonctionnalités alpha.

### Deuxième fonctionnalité transverse — à choisir par le groupe

Le second choix alpha reste à définir parmi les pistes du §6 (ou à faire valider par l'enseignant s'il sort de la liste), puis à réaliser progressivement. Cette branche porte exclusivement sur F2 et ne prétend pas satisfaire tout le lot alpha.

### Diagnostic technique complémentaire

Les opérations du service sont journalisées avec leurs identifiants, sans enregistrer les mots de passe ou les formulaires complets dans ces messages.
Ces logs facilitent le diagnostic ; ce ne sont pas un journal d'audit métier persistant ni une preuve de commit transactionnel.

## Frontend et ergonomie

- Page de connexion Bootstrap, identité visuelle Rail Pulse, libellés et autocomplétion adaptés.
- Bouton d'affichage du mot de passe utilisable au clavier.
- Bandeau identique sur les pages privées et pages d'erreur.
- Formulaires sur une colonne mobile et deux colonnes lorsque la largeur le permet.
- Champs obligatoires identifiés, erreurs associées aux champs et messages de succès.
- Fil d'Ariane, annulation, liste vide explicite et tableau à défilement horizontal.
- Confirmation avant suppression ; le serveur conserve la protection CSRF même si JavaScript est désactivé.

## TESTS

Commande complète : `./mvnw.cmd verify` sous Windows, avec Java 21.
Les tests HTTP démarrent un vrai serveur sur un port aléatoire et utilisent un cookie de session par client.
Ils s'exécutent sur une base H2 en mémoire, sans toucher à MySQL.
Depuis la préparation Supabase, H2 utilise le mode PostgreSQL. Des variables `TEST_DB_*` permettent également de lancer les mêmes tests sur un PostgreSQL temporaire, comme décrit dans le guide Supabase.

| Suite | Vérifications principales |
|---|---|
| `RailPulseApplicationTests` | Page publique, routes privées, erreurs de connexion, deux profils, formats de mots de passe, migration BCrypt, rotation de session, déconnexion, ancien cookie refusé, CSRF, bandeau et déconnexion depuis chaque page. |
| `ProgrammeurIntegrationTests` | Création/modification/suppression effectivement persistées, identifiants falsifiés, messages de succès, champs facultatifs, zéro accepté, champs manquants/trop longs, salaire négatif/invalide/non fini, conservation de la saisie et des données, ressource absente, paramètres et méthodes invalides, liste vide, validation du service et CSRF. |
| `GlobalExceptionHandlerTest` | Messages d'erreur sûrs, conflit de données, statut HTTP conservé et détails internes masqués. |
| `EquipementIntegrationTests` | Connexion obligatoire, deux profils autorisés, navigation depuis l'accueil, isolation entre stations, tri, types et états, absence de modification lors des lectures, catalogue/station vides, 404/400, échappement HTML et référence unique interstations (RG9). |
| `FicheEquipementIntegrationTests` / `IncidentTest` | Fiche et incidents de l'équipement, tri RG7, droits, absence de ressource, listes vides, HTML échappé et invariants du modèle incident. |
| `DeclarationIncidentIntegrationTests` | Déclaration par les deux profils, horloge fixe, validation et limite de longueur, saisie conservée, données client falsifiées, erreurs 400/404, connexion, CSRF et absence de réécriture au rafraîchissement après redirection. |
| `DetailIncidentIntegrationTests` / `EvaluationTest` | Détail pour les deux profils, durée courante ou arrêtée à la résolution, prestataire, évaluation et commentaire facultatifs, RG8, unicité, HTML échappé, connexion et erreurs 400/404. |
| `DonneesDemonstrationIntegrationTests` | Initialisation effective avec le profil `demo`, diversité des données, station vide, absence de doublons au relancement et absence de création de comptes. |

Vérification F2 du **27/10/2026** : `./mvnw.cmd -B verify` réussie, **28 tests, 0 échec, 0 erreur**, JAR et rapport JaCoCo générés. Compilation ciblant Java 21, exécution sur le JDK 25.0.3 installé sur cette machine. Les huit nouveaux tests tournent sur H2 en mode PostgreSQL ; les bases réelles ne sont pas modifiées par cette vérification. Le rendu visuel et l'exécution de F2 sur MySQL/Supabase restent à contrôler sur l'environnement de démonstration.

Le rapport [JaCoCo](https://www.jacoco.org/jacoco/trunk/doc/maven.html) est généré dans `target/site/jacoco/index.html` par `verify`.
Vérification du 14/10/2026 : **20 tests réussis**, construction du JAR réussie, **80,8 % des lignes** et **44,2 % des branches** Java couvertes.
Les lignes des services d'authentification et de programmeurs et du contrôleur de programmeurs ont été exécutées intégralement, sans que cela constitue une preuve de tous leurs comportements.
La couverture restante concerne principalement le démarrage du service MySQL Windows et quelques accesseurs de l'entité utilisateur ; aucun test n'a été ajouté uniquement pour augmenter un pourcentage.
La couverture mesure du code Java exécuté : elle ne prouve ni la justesse de toutes les règles ni le rendu visuel des écrans.
H2 ne remplace pas une vérification sur MySQL. Le démarrage automatique du service Windows nécessite un contrôle sur la machine de démonstration et n'est pas lancé par les tests isolés.

## Traçabilité

| Date | Étape | Référence |
|---|---|---|
| 12/10/2026 | Renommage Rail Pulse, configuration IntelliJ et démarrage local MySQL | `02ff542` |
| 14/10/2026 | Connexion JPA au MySQL local | `1e35c1c` |
| 14/10/2026 | Modèle utilisateur et écran Bootstrap | `b3eae54` |
| 14/10/2026 | Authentification, fermeture de session et tests HTTP | `309716d` |
| 14/10/2026 | Bandeau commun et déconnexion depuis chaque page | `b416d3a` |
| 14/10/2026 | Validation, erreurs centralisées, nettoyage, formulaires et tests CRUD | `85c6c74` |
| 14/10/2026 | Documentation SOLID, bilan d'évaluation et rapport de couverture | Voir le commit correspondant dans `git log`. |
| 27/10/2026 | F2 : catalogue des équipements par station, RG9/RG11, profil demo et 8 nouveaux tests | Branche `feature/f2-equipements-par-station` |

Les commits attestent les étapes enregistrées, pas un temps de travail ou une répartition entre membres.
L'assistance Codex a été utilisée pour la mise en œuvre et la vérification. L'autonomie du groupe reste à démontrer par la compréhension du code, la reproduction des tests et l'explication des choix lors de la présentation.

## Contrôles manuels avant présentation

1. Démarrer sur la machine prévue avec le profil de base choisi et, si le catalogue est vide, le profil `demo`.
2. Se connecter avec un compte de chacun des deux profils.
3. Vérifier le bandeau, une erreur de saisie, une création, une modification et une suppression.
4. Vérifier l'annulation de la confirmation de suppression.
5. Se déconnecter et tenter de rouvrir une page privée.
6. Examiner les écrans sur mobile et au clavier, ainsi que le chargement du CSS Bootstrap.
7. Présenter les résultats Maven, le rapport de couverture et les commits.
8. Depuis l'accueil, ouvrir les équipements, passer d'une station à l'autre, vérifier types/états, station vide, puis accès direct à une station absente.

L'accessibilité complète, les différences H2/MySQL, la charge et la compatibilité de tous les navigateurs ne sont pas certifiées par les seuls tests présents.

## 14/10/2026 — Préparation de PostgreSQL/Supabase

- Ajout du pilote PostgreSQL et du profil Supabase par défaut ; l'ancien accès local passe dans le profil `mysql`.
- Configuration par `SUPABASE_DB_URL`, `SUPABASE_DB_USERNAME`, `SUPABASE_DB_PASSWORD`, utilisables comme secrets CI/CD.
- Fichier local de paramètres ignoré par Git et exemple sans secret versionné.
- SSL et pool de connexions limité ; schéma JPA dédié `railpulse` séparé de `public`.
- Conservation de l'authentification applicative existante, sans utilisation de la clé de la Data API.
- Documentation des connexions directe et Session pooler, sans déduire un hôte de pooler non fourni.
- Le mot de passe PostgreSQL n'a pas été fourni ; la connexion réelle à Supabase reste à confirmer. La clé d'API ne peut pas le remplacer.
- Aucune migration de données de l'ancienne base MySQL n'est effectuée automatiquement.
- Vérification : 20 tests réussis sur H2 en mode PostgreSQL, puis les mêmes 20 tests sur PostgreSQL 18 temporaire avec création du schéma `railpulse`. Le serveur de test a été arrêté après la vérification. Cela valide le dialecte PostgreSQL, pas la connectivité au projet Supabase distant.
- Diagnostic du lancement IntelliJ : `UnknownHostException` sur l'hôte Supabase direct. Ajout de deux configurations explicites MySQL local / Supabase pour éviter de confondre les environnements. Démarrage du JAR avec le profil MySQL vérifié sur un port temporaire : `/login` répond HTTP 200 ; cette instance de vérification a ensuite été arrêtée.
- Le lancement habituel sans profil continuait de choisir Supabase : le profil par défaut est finalement rétabli à `mysql`. L'utilisation distante reste explicite via `SPRING_PROFILES_ACTIVE=supabase` ou la configuration IntelliJ dédiée. La documentation CI/CD est alignée sur ce choix.

## BILAN

- État au 27/10/2026 : F1 à F5 réalisées ; le CRUD de programmeurs reste un support issu du TP. F6 à F12 restent à développer sur des branches dédiées.
- Point de conception F2 : distinguer une station absente (404) d'une station existante sans équipement (200), et effectuer le filtrage en base pour éviter de mélanger les stations.
- Trois points à retenir : séparer HTTP/métier/persistance ; garantir les invariants en base et les droits côté serveur ; tester les comportements attendus avec de vraies requêtes HTTP et une base isolée.
- Avant rendu : compléter la contribution de chaque membre et la difficulté principale du groupe, le second choix alpha, l'unique requête native et son analyse, les comptes de test et l'URL déployée. La migration des anciens mots de passe en clair de F1 reste une compatibilité historique ; les comptes de démonstration finaux doivent déjà être hachés.
- La configuration courante utilise `supabase` par défaut (les notes du 14/10 ci-dessus décrivent l'historique). Les secrets éventuels des profils existants doivent rester hors du dépôt conformément au §7.7 ; cette branche F2 n'en ajoute aucun.

## 27/10/2026 — Préparation du déploiement Render

- Branche `feature/deploiement-render`, basée sur F2 pour permettre sa démonstration en ligne avant intégration dans `master`.
- Dockerfile à deux étapes : compilation et tests Java 21, puis exécution du seul JAR avec un JRE Java 21 et un utilisateur sans privilèges. Le tas Java est limité à 60 % de la mémoire du conteneur.
- `.dockerignore` exclut Git, la configuration locale et les fichiers `.env` du contexte de construction.
- Profil `render` : port `PORT` fourni par Render, écoute sur toutes les interfaces, en-têtes du proxy HTTPS, cookie sécurisé et absence de SQL dans les logs. Les identifiants Supabase sont fournis par variables d'environnement.
- Paramètres de création du Web Service et procédure de contrôle : [docs/render.md](docs/render.md).
- Docker n'est pas installé sur cette machine : la construction de l'image et le démarrage sur Render restent à vérifier lors du déploiement. L'URL publique sera renseignée une fois le service démarré.
- Vérification locale après ajout de la configuration : `./mvnw.cmd -B verify`, 28 tests réussis et JAR généré (JDK local 25.0.3, cible Java 21). Le Dockerfile prévoit de réexécuter ces tests avec le JDK 21 pendant la construction sur Render.

## 27/10/2026 — F3 : fiche équipement et incidents

- Branche `feature/f3-fiche-equipement`. Depuis F2, la référence ouvre la fiche `/equipements/{id}` : informations de l'équipement, lien vers sa station et liste de tous ses incidents, y compris résolus.
- Ajout des entités `Incident`, `StatutIncident` et `Prestataire`, avec une relation obligatoire vers l'équipement, une description de 2000 caractères maximum, les dates d'ouverture/résolution et un prestataire facultatif. Ces données couvrent le minimum du §4 pour préparer F4.
- `EquipementService.consulterEquipement` regroupe les lectures dans sa transaction en lecture seule ; les `join fetch` chargent la station et les prestataires nécessaires à la vue sans dépendre d'Open Session in View ni provoquer de requête par ligne.
- `IncidentRepository.rechercherParEquipement` filtre en base et ordonne par ouverture décroissante puis identifiant décroissant (RG7). Un équipement absent produit une exception personnalisée et une page 404. Une liste vide produit une page 200 explicite.
- Les GET `/equipements/**` exigent un profil AGENT ou RESPONSABLE_EXPLOITATION côté serveur (RG11). Les données sont échappées par Thymeleaf ; le bandeau et la déconnexion F1 restent disponibles.
- Le modèle initialise l'état non affecté sans prestataire. Ses méthodes d'affectation/résolution garantissent des données de démonstration cohérentes : refus d'affecter un incident résolu, date de résolution au moins égale à l'ouverture (RG1/RG2/RG5). Aucun endpoint F7/F8 n'est exposé.
- Une horloge `Clock.systemUTC()` injectée rend les dates explicites et permettra les tests déterministes de F4/F11. Les vues indiquent UTC. Les couleurs des statuts ne représentent pas les seuils d'urgence F11.
- Le profil `demo` crée sur un catalogue neuf cinq incidents aux dates relatives (dont 47/48/72 h), un prestataire, des cas affectés/résolus et des équipements sans incident ; un catalogue existant reste intact.
- Tests ajoutés : `FicheEquipementIntegrationTests` (deux profils, navigation, isolement entre équipements, tri, état vide, 400/404, connexion obligatoire et échappement HTML), `IncidentTest` (invariants et refus sans mutation). Le test du profil demo vérifie aussi les incidents et l'absence de doublons.
- Vérification F3 : `./mvnw.cmd -B verify` réussie, **38 tests, aucun échec ni erreur**, JAR et rapport JaCoCo générés. Exécution locale sur JDK 25.0.3, cible Java 21 ; la CI de la PR exécute les mêmes vérifications sous Java 21. Les tests HTTP/JPA utilisent H2 et ne modifient pas Supabase.

## 27/10/2026 — F4 : déclarer un incident

- Branche `feature/f4-declaration-incident`, basée sur F3. F3 est intégrée en premier (PR #5) ; la PR F4 vise ensuite `master` pour présenter uniquement la déclaration.
- Parcours : fiche équipement → formulaire GET `/equipements/{id}/incidents/nouveau` → POST `/equipements/{id}/incidents` → redirection vers la fiche avec confirmation et liste actualisée.
- `DeclarationIncident` est un objet de saisie limité à la description (non blanche, 2000 caractères maximum). Le contrôleur lie explicitement ce seul champ. Les erreurs conservent les valeurs avec des messages français ; une ressource absente reste une 404, y compris si la description est invalide.
- `IncidentService.declarer` porte la transaction d'écriture et utilise l'équipement trouvé par l'URL et l'horloge injectée. Le modèle impose l'état initial non affecté sans prestataire ni résolution (RG1/RG2). La validation du service ne peut pas être contournée par un appel direct.
- L'identifiant de l'incident est généré par JPA ; les champs de statut/date/prestataire/équipement transmis artificiellement ne peuvent ni modifier un incident existant ni détourner la cible.
- Les deux profils peuvent déclarer un incident. La route POST vérifie le rôle côté serveur et conserve CSRF ; l'écran GET reste protégé (RG11). Les secrets Supabase et le fichier `.env` ne sont pas impliqués dans les tests.
- Le schéma Post/Redirect/Get évite de redéclarer l'incident quand on rafraîchit la page après succès. Les espaces de début et de fin sont retirés après validation ; le texte intérieur est conservé et affiché échappé.
- Aucun changement automatique de l'état de l'équipement : un incident peut être une anomalie sans arrêt de fonctionnement. Plusieurs incidents restent autorisés, car le sujet ne prescrit pas de limite à ce stade.
- Tests ajoutés : création par les deux profils avec horloge fixe, présence sur la fiche, statut et dates serveur, description vide/blanche/trop longue, limite exacte, conservation de la saisie, champs falsifiés, équipement absent, identifiant invalide, connexion et CSRF, échappement HTML et validation du service.
- Vérification F4 : `./mvnw.cmd -B verify` réussie, **45 tests, aucun échec ni erreur**, JAR et rapport JaCoCo générés. Exécution locale sur JDK 25.0.3, cible Java 21 ; les tests de déclaration utilisent une horloge fixe et une base H2 dédiée, sans toucher à Supabase.

## 27/10/2026 — F5 : détail d'un incident

- Branche `feature/f5-detail-incident`, avec PR vers `master`. F5 respecte le §5.3 : description, équipement, station, durée d'ouverture, prestataire éventuel et évaluation éventuelle.
- Le numéro d'incident dans la fiche équipement ouvre `/incidents/{id}`. Les deux profils sont autorisés en lecture, avec vérification du rôle côté serveur (RG11). Le bandeau utilisateur et la déconnexion restent présents.
- `IncidentService.consulter` calcule la durée via l'horloge UTC injectée : ouverture → heure courante pour les actifs, ouverture → résolution pour les résolus. La durée n'est pas stockée en base. Une ouverture future importée est présentée avec une durée nulle plutôt que négative.
- Le service reste indépendant des vues et le contrôleur prépare seulement le modèle HTTP (SRP). Les lectures sont regroupées dans `@Transactional(readOnly = true)` ; la requête JPQL charge l'équipement, sa station et le prestataire pour éviter des accès différés pendant le rendu.
- `IncidentIntrouvableException` et le `ControllerAdvice` existant produisent une page 404 en français ; les identifiants invalides produisent 400. Les textes sont échappés par Thymeleaf et les champs facultatifs ont des états vides lisibles.
- Le modèle `Evaluation` permet la lecture de l'évaluation demandée par F5. Il impose un incident résolu avec prestataire, une note entre 1 et 5 et une date au moins égale à la résolution ; une contrainte unique en base garantit une évaluation par incident (RG8). Le repository utilise une requête dérivée `findByIncidentId`. La saisie F10 reste hors périmètre.
- Le profil demo ajoute une évaluation à l'incident résolu sur un catalogue neuf, dans sa transaction existante. Les catalogues déjà renseignés ne sont pas remplacés.
- Vérification : `./mvnw.cmd -B verify`, **54 tests réussis**, aucun échec ni erreur, JAR et rapport JaCoCo générés. Horloge fixe et H2 dédiée pour les nouveaux tests ; JDK local 25.0.3, cible Java 21. Les vérifications de la PR sont exécutées sous Java 21 ; le rendu navigateur et la base distante restent à contrôler dans l'environnement de démonstration.
- Secrets : `.env` / `.env.*` sont ignorés par Git (tous dossiers), `.env.example` reste réservé à des exemples sans secret. Vérification `git check-ignore` réussie et aucun `.env` suivi par Git ; aucun secret n'a été ajouté à la branche.
