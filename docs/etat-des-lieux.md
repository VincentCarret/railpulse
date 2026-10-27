# ?tat des lieux ? conformit? ? mi-parcours

**Rail Pulse ? Amir Nassiri et Vincent Carret ? 27 octobre 2026.**
Ce document d?crit les correctifs propos?s sur `fix/conformite-mi-parcours`. Il ne d?clare pas le projet final termin? ni les correctifs d?j? d?ploy?s.

## Socle fonctionnel

| Exigence | ?tat | Preuve |
|---|---|---|
| F1 | R?alis?e et durcie | Connexion BCrypt, sessions, bandeau et d?connexion ; migration pr?alable et refus du clair ? la connexion. |
| F2 | R?alis?e | Station ? ?quipements, type et ?tat, r?f?rence unique, cas vides et 400/404. |
| F3 | R?alis?e | Fiche, incidents actifs/r?solus et tri par ouverture d?croissante RG7. |
| F4 | R?alis?e | Validation, date serveur, non affect? sans prestataire, champs falsifi?s ignor?s. |
| F5 | R?alis?e | D?tail, dur?e courante ou arr?t?e ? la r?solution, prestataire et ?valuation ?ventuels. |
| F6 | R?alis?e | Description active corrig?e par le responsable, refus agent et r?solu c?t? serveur. |
| F7?F12 | ? r?aliser | Affectation, r?solution, historique global, saisie d'?valuation, supervision et API OpenAPI. |

## ?carts de l'audit corrig?s

| Point du sujet | Correction |
|---|---|
| Secret MySQL dans le code | Fallback supprim? ; variable d'environnement requise. La configuration locale est pr?serv?e uniquement hors Git. |
| Acceptation de mots de passe en clair | Encoder limit? ? BCrypt, validation du mod?le User, migration transactionnelle pr?alable ; plus de `{noop}` dans la connexion. |
| Repository TP vide | Requ?te d?riv?e de tri r?ellement utilis?e, avec test d'ordre. |
| Initialisation incompl?te | Deux comptes demo cr??s avec secrets d'environnement, sans ?crasement ; script autonome et catalogue complet sur base neuve. |
| Une seule piste alpha | Qualit? renforc?e (piste 7) et export CSV du catalogue (piste 9). |
| SQL natif absent | Une seule requ?te native de l'application pour la projection CSV, avec analyse critique dans le journal. |
| CI trompeuse sur le moteur utilis? | Job H2 et vrai job PostgreSQL ; tous les contextes suivent `TEST_DB_URL`, avec sch?mas s?par?s. Ancienne ?tape VPS supprim?e. |
| Documentation incompl?te/obsol?te | README reproductible, groupe renseign?, URL Render et comptes param?tr?s document?s, journal r??crit avec choix et limites. |

## Preuves de v?rification

- `mvnw verify` local : **75 tests, 0 ?chec, 0 erreur**, JAR et JaCoCo g?n?r?s.
- JAR autonome lanc? avec une base H2 fichier neuve, sans configuration de test : connexion des deux comptes, F2/F3, F4, d?tail F5, refus d'?dition agent, correction responsable, CSV et d?connexion v?rifi?s par HTTP. Instance de v?rification arr?t?e.
- CI Java 21 : deux jobs H2 et PostgreSQL 17 ; les r?sultats et rapports sont attach?s ? la PR. Aucun secret de production ni acc?s Supabase n?cessaire.
- `.env` et ses variantes sont ignor?s. `.env.example` contient seulement des champs vides ou factices. Les bases locales et secrets sont aussi exclus du contexte Docker.
- La page publique Render r?pond HTTP 200 ; cela ne certifie pas ? elle seule le d?ploiement des pr?sents correctifs ou les parcours authentifi?s de cette version.

## Exigences transverses

Architecture MVC/services/repositories/mod?le, exceptions personnalis?es et statuts coh?rents, SRP/DIP et principes ?cart?s justifi?s, requ?tes d?riv?es/JPQL/native, transactions motiv?es, horloge inject?e et droits serveur sont document?s dans [journal.md](../journal.md).
Le sch?ma et les donn?es peuvent ?tre reconstruits ? partir des entit?s et initialisations, sur une base relationnelle vide. Les tests ne remplacent pas la v?rification ergonomique ni l'explication orale.

## Actions restantes

1. Relire et int?grer la PR, importer le `.env` mis ? jour dans Render avant d'activer `demo`, puis d?ployer et contr?ler les deux profils.
2. Si l'ancien secret pr?sent dans l'historique Git est encore utilis?, le renouveler ; l'historique n'a pas ?t? r??crit.
3. Faire valider par les deux membres leurs contributions r?elles et leur difficult? principale ; seules les traces Git sont attest?es dans le bilan.
4. D?velopper F7?F12 et leurs tests, notamment les seuils 48/72 h, l'historique et l'API document?e.
5. R?aliser les contr?les de copie propre, rendu navigateur et ma?trise orale pr?vus par le sujet avant le rendu final.

Six fonctionnalit?s sur douze sont r?alis?es. Les deux alpha et les fondations techniques sont v?rifiables ; cela n'?quivaut pas ? une conformit? finale de F1 ? F12 ni ? une estimation de note.
