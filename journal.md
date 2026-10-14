# Journal de développement — Rail Pulse

## Périmètre et règles retenues

Ce journal décrit le code effectivement réalisé, ses vérifications et ses limites.
Le projet utilise Java 21, Spring Boot, Spring MVC, Thymeleaf, Spring Data JPA et PostgreSQL/Supabase. Le profil MySQL local reste disponible pour compatibilité.
Les explications du corrigé initial sont conservées dans [les notes du TP 5](docs/journal-tp5.md).

Les exigences détaillées disponibles portent sur la connexion/déconnexion et la grille d'évaluation.
Le CRUD de programmeurs provient du TP et sert de support aux formulaires, transactions et tests.
Aucune règle supplémentaire sur des équipements ou incidents ferroviaires n'a été fournie : ces fonctionnalités ne sont pas déclarées réalisées.

| Règle | Mise en œuvre et justification |
|---|---|
| Login existant et mot de passe valide | Spring Security vérifie le compte de la table `users`. |
| Erreur d'identification | Retour au même formulaire, message générique ne révélant pas si le compte existe. |
| Utilisateur connecté | Prénom, profil et formulaire de déconnexion dans le bandeau commun. |
| Déconnexion | POST protégé par CSRF, invalidation de la session, suppression du cookie, retour à `/login`. |
| Profils | `AGENT` et `RESPONSABLE_EXPLOITATION`. Aucune différence de droits inventée en l'absence d'exigence. |
| Nom et prénom des programmeurs | Obligatoires, non blancs, 100 caractères maximum, en cohérence avec les colonnes existantes. |
| Autres champs textuels | Facultatifs ; adresse limitée à 255 caractères, langage à 100, livre à 150, conformément au schéma. |
| Salaire | Facultatif, fini et positif ou nul. Le refus des valeurs négatives est une hypothèse explicite du projet, à confirmer si le sujet évolue. Aucune devise ou périodicité n'est supposée. |
| Création | Génère un nouvel identifiant ; un identifiant envoyé par le client ne peut pas écraser une ligne. |
| Modification | Conserve l'identifiant de l'URL et n'autorise que les champs du formulaire. |
| Ressource inexistante | Réponse 404 pour la lecture, la modification et la suppression. Une modification ne crée pas une ligne manquante. |
| Saisie invalide | Le formulaire conserve les valeurs saisies et affiche les erreurs ; la base reste inchangée. |
| Liste vide | État vide explicite et lien pour créer le premier programmeur. |

## Architecture et points du cours

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

## SOLID : principes appliqués et limites justifiées

### SRP — responsabilité unique : appliqué

`ProgrammeurControleur` peut changer si le parcours HTTP évolue ; `ProgrammeurService` si les opérations applicatives évoluent ; `ProgrammeurRepository` si les besoins d'accès aux données évoluent.
`GlobalExceptionHandler` centralise la traduction des exceptions en pages et statuts HTTP.
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

## Fonctionnalités transverses réalisées

### 1. Sécurité et contexte de session

- Contrôle d'accès commun aux routes privées, indépendamment du contrôleur.
- Connexion par formulaire, message d'échec, session HTTP et déconnexion.
- Protection CSRF des formulaires d'écriture et de déconnexion.
- Rotation de l'identifiant de session après connexion et invalidation après déconnexion.
- Bandeau partagé via un fragment et un `ControllerAdvice`.
- BCrypt pour les mots de passe ; migration des anciens mots de passe en clair seulement après une identification réussie.

Cette fonctionnalité traverse les contrôleurs et les pages. La protection CSRF n'est pas comptée comme une deuxième fonctionnalité séparée de la sécurité.

### 2. Validation et gestion homogène des erreurs

- Contraintes Jakarta Validation sur les données ; validation au niveau des formulaires et du service.
- Messages français par champ, réaffichage de la saisie et absence d'écriture en cas d'erreur.
- Gestion centralisée des ressources inexistantes, paramètres invalides, conflits de données et erreurs inattendues.
- Conservation des statuts HTTP 400, 403, 404, 405, 409 et 500 selon le cas.
- Page d'erreur commune, lien de retour et bandeau utilisateur si une session est ouverte.
- Masquage des détails SQL, messages internes et traces techniques dans les réponses.

La validation ne dépend pas du seul JavaScript du navigateur ; elle reste active pour une requête HTTP fabriquée manuellement.
Le regroupement validation/erreurs constitue ici la deuxième fonctionnalité transverse retenue, en l'absence d'une liste plus précise fournie par le sujet.

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

## Vérification de la solution

Commande complète : `./mvnw.cmd verify` sous Windows, avec Java 21.
Les tests HTTP démarrent un vrai serveur sur un port aléatoire et utilisent un cookie de session par client.
Ils s'exécutent sur une base H2 en mémoire, sans toucher à MySQL.
Depuis la préparation Supabase, H2 utilise le mode PostgreSQL. Des variables `TEST_DB_*` permettent également de lancer les mêmes tests sur un PostgreSQL temporaire, comme décrit dans le guide Supabase.

| Suite | Vérifications principales |
|---|---|
| `RailPulseApplicationTests` | Page publique, routes privées, erreurs de connexion, deux profils, formats de mots de passe, migration BCrypt, rotation de session, déconnexion, ancien cookie refusé, CSRF, bandeau et déconnexion depuis chaque page. |
| `ProgrammeurIntegrationTests` | Création/modification/suppression effectivement persistées, identifiants falsifiés, messages de succès, champs facultatifs, zéro accepté, champs manquants/trop longs, salaire négatif/invalide/non fini, conservation de la saisie et des données, ressource absente, paramètres et méthodes invalides, liste vide, validation du service et CSRF. |
| `GlobalExceptionHandlerTest` | Messages d'erreur sûrs, conflit de données, statut HTTP conservé et détails internes masqués. |

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

Les commits attestent les étapes enregistrées, pas un temps de travail ou une répartition entre membres.
L'assistance Codex a été utilisée pour la mise en œuvre et la vérification. L'autonomie du groupe reste à démontrer par la compréhension du code, la reproduction des tests et l'explication des choix lors de la présentation.

## Contrôles manuels avant présentation

1. Démarrer sur la machine prévue et vérifier la connexion à MySQL.
2. Se connecter avec un compte de chacun des deux profils.
3. Vérifier le bandeau, une erreur de saisie, une création, une modification et une suppression.
4. Vérifier l'annulation de la confirmation de suppression.
5. Se déconnecter et tenter de rouvrir une page privée.
6. Examiner les écrans sur mobile et au clavier, ainsi que le chargement du CSS Bootstrap.
7. Présenter les résultats Maven, le rapport de couverture et les commits.

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
