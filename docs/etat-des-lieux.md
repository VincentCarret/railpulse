# État des lieux — conformité à mi-parcours

**Rail Pulse — Amir Nassiri et Vincent Carret — 27 octobre 2026.**
Ce document décrit les correctifs proposés sur `fix/conformite-mi-parcours`. Il ne déclare pas le projet final terminé ni les correctifs déjà déployés.

## Socle fonctionnel

| Exigence | État | Preuve |
|---|---|---|
| F1 | Réalisée et durcie | Connexion BCrypt, sessions, bandeau et déconnexion ; migration préalable et refus du clair à la connexion. |
| F2 | Réalisée | Station → équipements, type et état, référence unique, cas vides et 400/404. |
| F3 | Réalisée | Fiche, incidents actifs/résolus et tri par ouverture décroissante RG7. |
| F4 | Réalisée | Validation, date serveur, non affecté sans prestataire, champs falsifiés ignorés. |
| F5 | Réalisée | Détail, durée courante ou arrêtée à la résolution, prestataire et évaluation éventuels. |
| F6 | Réalisée | Description active corrigée par le responsable, refus agent et résolu côté serveur. |
| F7 | Réalisée | Affectation et réaffectation des incidents actifs par le responsable ; refus des résolus et droits côté serveur. |
| F8 | Réalisée | Résolution d'un incident actif avec horodatage serveur, confirmation, contrôle du rôle et refus des transitions invalides. |
| F9 | Réalisée | Historique global des seuls incidents résolus, tri par résolution décroissante et liens vers les détails. |
| F10 | Réalisée | Saisie d'une note de 1 à 5 et d'un commentaire facultatif après résolution avec prestataire ; unicité et date serveur. |
| F11–F12 | à réaliser | Supervision et API OpenAPI. |

## écarts de l'audit corrigés

| Point du sujet | Correction |
|---|---|
| Secret MySQL dans le code | Fallback supprimé ; variable d'environnement requise. La configuration locale est préservée uniquement hors Git. |
| Acceptation de mots de passe en clair | Encoder limité à BCrypt, validation du modèle User, migration transactionnelle préalable ; plus de `{noop}` dans la connexion. |
| Repository TP vide | Requête dérivée de tri réellement utilisée, avec test d'ordre. |
| Initialisation incomplète | Deux comptes demo créés avec secrets d'environnement, sans écrasement ; script autonome et catalogue complet sur base neuve. |
| Une seule piste alpha | Qualité renforcée (piste 7) et export CSV du catalogue (piste 9). |
| SQL natif absent | Une seule requête native de l'application pour la projection CSV, avec analyse critique dans le journal. |
| CI trompeuse sur le moteur utilisé | Job H2 et vrai job PostgreSQL ; tous les contextes suivent `TEST_DB_URL`, avec schémas séparés. Ancienne étape VPS supprimée. |
| Documentation incomplète/obsolète | README reproductible, groupe renseigné, URL Render et comptes paramétrés documentés, journal réécrit avec choix et limites. |

## Preuves de vérification

- `mvnw verify` local avec F10 : **97 tests, 0 échec, 0 erreur**, JAR et JaCoCo générés.
- JAR autonome lancé avec une base H2 fichier neuve, sans configuration de test : connexion des deux comptes, F2/F3, F4, détail F5, refus d'édition agent, correction responsable, CSV et déconnexion vérifiés par HTTP. Instance de vérification arrêtée.
- CI Java 21 : **75 tests réussis sur H2 et 75 sur PostgreSQL 17**, aucun échec ni erreur. Les quatre contextes PostgreSQL utilisent ce moteur sans repli H2. [Exécution de référence](https://github.com/VincentCarret/railpulse/actions/runs/40544261917), résultats et rapports attachés à la PR. Aucun secret de production ni accès Supabase nécessaire.
- `.env` et ses variantes sont ignorés. `.env.example` contient seulement des champs vides ou factices. Les bases locales et secrets sont aussi exclus du contexte Docker.
- La page publique Render répond HTTP 200 ; cela ne certifie pas à elle seule le déploiement des présents correctifs ou les parcours authentifiés de cette version.

## Exigences transverses

Architecture MVC/services/repositories/modèle, exceptions personnalisées et statuts cohérents, SRP/DIP et principes écartés justifiés, requêtes dérivées/JPQL/native, transactions motivées, horloge injectée et droits serveur sont documentés dans [journal.md](../journal.md).
Le schéma et les données peuvent être reconstruits à partir des entités et initialisations, sur une base relationnelle vide. Les tests ne remplacent pas la vérification ergonomique ni l'explication orale.

## Actions restantes

1. Relire et intégrer la PR, importer le `.env` mis à jour dans Render avant d'activer `demo`, puis déployer et contrôler les deux profils.
2. Si l'ancien secret présent dans l'historique Git est encore utilisé, le renouveler ; l'historique n'a pas été réécrit.
3. Faire valider par les deux membres leurs contributions réelles et leur difficulté principale ; seules les traces Git sont attestées dans le bilan.
4. Développer F11–F12 et leurs tests, notamment les seuils 48/72 h et l'API documentée.
5. Réaliser les contrôles de copie propre, rendu navigateur et maîtrise orale prévus par le sujet avant le rendu final.

Six fonctionnalités sur douze sont réalisées. Les deux alpha et les fondations techniques sont vérifiables ; cela n'équivaut pas à une conformité finale de F1 à F12 ni à une estimation de note.
