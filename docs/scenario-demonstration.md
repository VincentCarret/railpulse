# Scénario de démonstration RailPulse

Préparer l'application suivant le [README](../README.md), avec une base de démonstration neuve et les deux mots de passe définis localement. Utiliser `demo.agent` et `demo.responsable`. Le scénario suit un incident depuis son signalement jusqu'à l'évaluation de l'intervention.

1. **Constater et signaler.** Se connecter comme agent. Dans les équipements de la station République, ouvrir la fiche de l'escalier `REP-ESC-01`. Montrer ses incidents actifs et résolus, puis déclarer un nouvel incident avec une description courte. La fiche confirme son état non affecté et l'heure d'ouverture issue du serveur.
2. **Superviser.** Se connecter comme responsable et ouvrir le tableau de bord. Montrer les incidents de démonstration ouverts depuis 47, 48 et 72 heures : les seuils changent respectivement l'indicateur de vert à orange puis rouge. Filtrer par station et par prise en charge, puis ouvrir le nouvel incident.
3. **Prendre en charge et résoudre.** Corriger sa description si nécessaire, affecter le prestataire de démonstration, puis confirmer la résolution. Le responsable peut montrer que l'incident n'apparaît plus parmi les actifs et figure dans l'historique. L'agent conserve la lecture mais ne peut effectuer ces opérations.
4. **Évaluer.** Sur le détail de l'incident résolu et affecté, saisir une note et un commentaire. Vérifier que la note est ensuite lisible et qu'une seconde évaluation est refusée.
5. **Montrer les extensions.** Exporter le catalogue CSV depuis la liste des stations. Ouvrir `/api/openapi.yaml`, puis consulter `/api/incidents` et le détail de l'incident. La résolution par API réutilise la session, le jeton CSRF et les droits du responsable.
6. **Terminer.** Se déconnecter et ouvrir une page privée pour vérifier le retour vers `/login`.

Le service en ligne doit être testé séparément depuis un autre ordinateur avant le dépôt. Garder les mots de passe de démonstration hors du dépôt et les transmettre à l'enseignant par un canal approprié.
