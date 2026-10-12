# Journal – Rail Pulse 5 (ALTN72) – Couche "V" : Thymeleaf

Réponses aux questions du TP 5. Code de référence : ce projet.

## 5.2 Configuration
- Dépendance spring-boot-starter-thymeleaf déjà dans le pom.xml. À vérifier, rien à ajouter.
- Apporte : moteur Thymeleaf + intégration Spring MVC.
- Auto-configuration : templates cherchés dans `src/main/resources/templates`, suffixe `.html`.

## 5.3 Dossiers
- Templates : `src/main/resources/templates`.
- Statiques (non transformés) : `src/main/resources/static`.

## 5.4 Changements dans le contrôleur
- `@RestController` = `@Controller` + `@ResponseBody`.
- `@RestController` : la valeur retournée est écrite dans le corps HTTP (JSON).
- `@Controller` : la valeur retournée (String) est le NOM d'un template.
- Spring passe ce nom au moteur Thymeleaf, qui produit la page HTML.
- Données vers la vue : objet `Model`.
- Conséquence : ResponseEntity (4.9) et @RequestBody (JSON) ne servent plus ici.

## 5.5 – 5.6 Syntaxe, attributs
- Template = HTML + attributs `th:*`, interprétés côté serveur.
- `th:*` ou `data-th-*` : équivalents. La 2e forme est du HTML5 valide.
- `${...}` : expression évaluée sur le Model.
- `xmlns:th` : facultatif, utile à l'IDE.

## 5.7 Templates à créer
- listeProgrammeurs.html, detailsProgrammeur.html, nouveauProgrammeur.html.

## 5.8 Affichage d'un seul programmeur
- Comment `Model` arrive dans la méthode : il suffit de le déclarer en paramètre. Spring le fournit.
- Ajouter une donnée : `model.addAttribute("nom", valeur)`.
- Lien avec `${...}` : le 1er paramètre d'addAttribute = le nom retrouvé dans l'expression. `addAttribute("programmeurs", liste)` → `${programmeurs}`.
- `${programmeur.nom}` appelle `getNom()`.
- Prénom du dernier programmeur : `liste.get(liste.size() - 1).getPrenom()`. Attention : liste vide → exception.
- Template : `<span th:text="${prenomDernierProgrammeur}"></span>`.
- Étape intermédiaire : laissée en commentaire dans le code.

## 5.9 Affichage des programmeurs
- Trois éléments :
  - contrôleur : récupère la liste (service) et la met dans le Model ;
  - contrôleur : renvoie le nom du template (`"listeProgrammeurs"`, sans `.html`) ;
  - template : parcourt la liste avec `th:each`.
- `th:each="programmeur : ${programmeurs}"` : répète la balise (`<tr>`) pour chaque élément. `programmeur` = élément courant.
- `th:text` : affiche la valeur, HTML échappé.

### 5.9.1 Squelette HTML
- Fait : une ligne `<tr>` par programmeur, une colonne par attribut.
- Noms utilisés : attributs de la classe Java (`langageExpertise`, `livrePrefere`), pas les colonnes SQL.

### 5.9.2 Bootstrap
- Fait : `<link>` Bootstrap dans le `<head>`. Classes `table`, `table-striped`, `btn`...

## 5.10 Suppression

### 5.10.1 Bouton "Supprimez"
- Bouton dans un `<form>` par ligne du tableau. URL construite avec `@{/programmeurs/{id}(id=...)}`.

### 5.10.2 Que se passe-t-il au clic ? Cause ?
- Erreur 405 Method Not Allowed.
- Cause : le formulaire HTML n'envoie que GET ou POST. La route du contrôleur n'accepte que DELETE.
- Un client HTTP (IntelliJ) peut envoyer DELETE. Pas un formulaire de navigateur.

### Note technique 3 – HiddenHttpMethodFilter
- Filtre de Spring MVC.
- Sur une requête POST, lit le paramètre `_method`.
- Si la valeur est PUT, PATCH ou DELETE : la requête est présentée à Spring avec cette méthode.
- Le contrôleur voit donc un vrai DELETE ou PUT.
- Formulaire : `method="POST"` + `<input type="hidden" name="_method" value="DELETE">`.
- Raccourci Thymeleaf : `th:method="DELETE"` génère les deux.
- Activation (désactivé par défaut) : `spring.mvc.hiddenmethod.filter.enabled=true` dans application.properties.

### 5.10.3 Solution + redirection
- Filtre activé + `th:method="DELETE"`.
- Contrôleur : `return "redirect:/programmeurs"`.
- Redirection : réponse 302 + en-tête `Location`. Le navigateur refait un GET /programmeurs.
- Pourquoi pas `return "listeProgrammeurs"` directement :
  - la liste n'est pas dans le Model → page vide ou erreur ;
  - l'URL affichée resterait celle de l'action (/programmeurs/12) ;
  - F5 rejouerait la suppression.
- Pattern : Post / Redirect / Get.
- Id inexistant : `ResponseStatusException` 404.

## 5.11 Ajout

### 5.11.1 Bouton "Ajoutez"
- Liste : formulaire GET vers `/programmeurs/nouveau`.
- Contrôleur : met un `Programmeur` vide dans le Model (`nouveauProgrammeur`) → template `nouveauProgrammeur`.
- Template : `th:object="${nouveauProgrammeur}"` + `th:field="*{nom}"`, etc.
- Bouton "Enregistrez" : POST vers `/programmeurs`.

### 5.11.2 Succès → liste actualisée
- `@ModelAttribute` (et non `@RequestBody`) : le formulaire envoie `nom=valeur&...`, pas du JSON.
- Noms des champs = noms des attributs de `Programmeur`.
- Service : `ajouterProgrammeur`. Puis `redirect:/programmeurs`.
- Le service force l'id à null : POST crée toujours.

## 5.12 Modification

### 5.12.1 Bouton "Modifiez"
- Liste : formulaire GET vers `/programmeurs/{id}`.
- Contrôleur : charge le programmeur → Model (`programmeurToUpdate`) → template `detailsProgrammeur`.
- Formulaire prérempli : `th:object` + `th:field` (génère `value`).
- Validation : POST + `_method=PUT` (`th:method="PUT"`), vers `/programmeurs/{id}`.

### 5.12.2 Succès → liste actualisée
- `@PutMapping("/{id}")` avec `@ModelAttribute`.
- Service : `modifierProgrammeur` (@Transactional).
- Puis `redirect:/programmeurs`.
- Id inexistant : 404.

## Remarques
- Salaire : `<input type="number" step="any">`. Le navigateur envoie un point décimal. Avec `type="text"`, "45000,5" → erreur de conversion (400).
- `server.error.include-message=always` : affiche le message du 404 dans la page d'erreur.
- Les routes restent celles de 4.8 (REST) : seules les routes de formulaires s'ajoutent (`/nouveau`).
- Le service n'a pas changé : seuls les commentaires ont été enrichis.

## Routes finales
- GET /programmeurs → listeProgrammeurs
- GET /programmeurs/nouveau → nouveauProgrammeur
- GET /programmeurs/{id} → detailsProgrammeur
- POST /programmeurs → ajout, redirection
- PUT /programmeurs/{id} → modification, redirection (via `_method`)
- DELETE /programmeurs/{id} → suppression, redirection (via `_method`)
