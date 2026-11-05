# Jeu de données fictives pour Supabase

Le script [dataset-demo-supabase.sql](../scripts/dataset-demo-supabase.sql) complète la base de l'application Render. Il ajoute des données fictives sans modifier les comptes, les secrets ou les données déjà présentes. Il se lance manuellement ; il ne dépend pas du profil `demo`, qui initialise uniquement un catalogue vide.

## Importer

1. Ouvrir le projet Supabase utilisé par Rail Pulse, puis **SQL Editor → New query**.
2. Copier **tout** le contenu de `scripts/dataset-demo-supabase.sql` et cliquer sur **Run**, avec le rôle d'administration PostgreSQL habituel de l'éditeur SQL. Les tables du schéma `railpulse` doivent déjà exister.
3. Vérifier le bilan retourné : **8 stations, 40 équipements, 4 prestataires, 48 incidents et 8 évaluations** pour ce jeu.
4. Se reconnecter à [Rail Pulse](https://railpulse-23pc.onrender.com/login), puis consulter les stations préfixées **[Démo 2026]**, le tableau de bord responsable et l'historique.

L'import s'exécute dans une transaction et verrouille brièvement les écritures sur les cinq tables concernées. Les consultations restent possibles. En cas d'erreur, ne pas exécuter la suite séparément : faire `ROLLBACK;` si la session conserve la transaction en erreur, corriger la cause, puis relancer le fichier entier.

## Contenu

Chaque station possède les cinq types d'équipement du projet et six incidents :

| Cas | Ancienneté à l'import | Statut | Parcours illustré |
|---|---|---|---|
| Ascenseur bloqué | 12 h | Non affecté | Attention verte, déclaration et correction |
| Escalier intermittent | 60 h | Non affecté | Attention orange, affectation |
| Écran figé | 90 h | Non affecté | Attention rouge, urgence |
| Portique en intervention | 96 h | Affecté | Prise en charge et résolution |
| Distributeur réparé | Ouvert il y a 240 h, résolu il y a 192 h | Résolu avec prestataire et évaluation | Historique et notes de 1 à 5 |
| Ancien incident d'ascenseur | Ouvert il y a 360 h, résolu il y a 300 h | Résolu sans prestataire | Historique sans évaluation |

Au total : **24 non affectés, 8 affectés et 16 résolus**. Les dates sont en UTC, relatives au premier import ; les niveaux d'attention évoluent ensuite avec le temps. Les contacts utilisent le domaine fictif `example.invalid`.

Le script peut être relancé sans dupliquer ces données tant que les noms de stations/prestataires, les références `DEMO26-...` et les marqueurs `[DEMO26-...]` au début des descriptions sont conservés. Les statuts, dates et évaluations déjà enregistrés sont préservés. Si une description est corrigée dans l'application, conserver son marqueur pour permettre cette reconnaissance. Aucun secret n'est nécessaire dans le fichier SQL.

## Vérification locale

Le 5 novembre 2026, le script a été exécuté sur un PostgreSQL 18 temporaire avec le schéma créé par l'application : quantités attendues, relations, chronologie, trois niveaux d'urgence, quatre prestataires et notes de 1 à 5 vérifiés. Un second import n'a ajouté aucune ligne. Un troisième, après correction d'une description, résolution d'un incident et modification d'une évaluation, a conservé ces changements. Des données et un compte préexistants sont restés inchangés. Cette vérification locale ne signifie pas que le script a déjà été exécuté dans Supabase.
