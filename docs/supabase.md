# Connexion JPA à Supabase

L'application utilise PostgreSQL via JDBC et Spring Data JPA. Elle ne passe pas par l'API REST Supabase : une clé `sb_secret_*` n'est donc pas utilisée pour cette connexion.

## Paramètres

Le profil `supabase` est activé par défaut. Les mêmes variables peuvent être renseignées localement, dans IntelliJ ou avec les secrets du futur déploiement CI/CD.

| Variable | Valeur |
|---|---|
| `SUPABASE_DB_PASSWORD` | Mot de passe de la base PostgreSQL, obligatoire pour une connexion réelle. |
| `SUPABASE_DB_URL` | URL JDBC avec SSL. Par défaut : `jdbc:postgresql://db.aehaxzwjhtkhqttvvqrd.supabase.co:5432/postgres?sslmode=require&connectTimeout=10&socketTimeout=30`. |
| `SUPABASE_DB_USERNAME` | `postgres` en connexion directe ; reprendre la valeur exacte du tableau de bord pour le pooler. |

Le pool Hikari est limité à 5 connexions et garde au minimum une connexion disponible.
Le dialecte SQL est déduit du pilote PostgreSQL. Aucun mot de passe n'est inclus dans l'URL ou les fichiers versionnés.

### Connexion directe ou Session pooler

Ouvrir le projet Supabase, puis **Connect**. La connexion directe nécessite normalement IPv6, sauf option IPv4 du projet.
Si le réseau ou le DNS ne permet pas la connexion directe, sélectionner **Session pooler** et reprendre son hôte et son utilisateur exacts.
Le nom d'hôte du pooler ne doit pas être deviné à partir de la région.
Le mode transaction n'est pas retenu ici : l'application Spring est un serveur persistant et utilise des transactions JPA et des requêtes préparées.

Transformer l'URI affichée en URL JDBC, sans y mettre les identifiants :

```text
jdbc:postgresql://HOTE_REPRIS_DEPUIS_CONNECT:5432/postgres?sslmode=require&connectTimeout=10&socketTimeout=30
```

L'hôte direct fourni n'était pas résolu depuis le PC lors de la préparation du 14/10/2026. La connexion au projet distant reste à confirmer après renseignement des identifiants et d'un point d'accès joignable.

Référence : [documentation officielle des connexions Supabase](https://supabase.com/docs/guides/database/connecting-to-postgres).

## Développement local

Copier `config/supabase-local.properties.example` vers `config/supabase-local.properties`, puis remplir le mot de passe et les éventuels paramètres de pooler.
Le second fichier est ignoré par Git et chargé seulement par le profil Supabase.
Les variables d'environnement priment sur les valeurs de ce fichier.

Dans IntelliJ, lancer le projet avec son dossier racine comme répertoire de travail. Les variables peuvent aussi être renseignées dans **Run > Edit Configurations > Environment variables**.
Recharger Maven pour récupérer le pilote PostgreSQL.

Pour fournir un mot de passe ponctuel dans PowerShell sans le saisir dans une commande enregistrée dans l'historique :

```powershell
$supabasePassword = Read-Host 'Mot de passe PostgreSQL Supabase' -AsSecureString
$env:SUPABASE_DB_PASSWORD = [System.Net.NetworkCredential]::new('', $supabasePassword).Password
.\mvnw.cmd spring-boot:run
```

Au déploiement, injecter les trois variables depuis le gestionnaire de secrets du CI/CD. Aucun pipeline n'est créé par cette configuration.

## Schéma et comptes de l'application

Le script `db/supabase-schema.sql` crée le schéma `railpulse` si nécessaire. Hibernate met à jour ses tables dans ce schéma.
Les entités utilisent les identifiants identity et les types PostgreSQL déduits par Hibernate.
Le compte PostgreSQL doit disposer des droits de création du schéma et des tables lors de l'initialisation.

Les comptes de connexion de Rail Pulse restent dans `railpulse.users`, avec le mécanisme Spring Security existant. Cela n'active pas le produit Supabase Auth.
Le schéma `railpulse` n'est pas à ajouter aux schémas exposés par la Data API pour cette application JDBC.
Les données existantes de MySQL ne sont pas transférées automatiquement. Une base vide nécessite de créer ou migrer des comptes applicatifs avant de pouvoir s'y connecter.
Pour une mise en production durable, remplacer le mode de développement `ddl-auto=update` par des migrations de schéma versionnées.

Dans l'onglet Database d'IntelliJ, utiliser PostgreSQL, l'URL JDBC, l'utilisateur PostgreSQL et le mot de passe de la base, puis sélectionner le schéma `railpulse` après initialisation.

## Vérifications reproductibles

```powershell
.\mvnw.cmd verify
```

Les tests utilisent par défaut H2 en mémoire, en mode PostgreSQL.
Le 14/10/2026, les 20 tests ont réussi sur H2 et sur une instance PostgreSQL 18 temporaire liée uniquement à `127.0.0.1:55432`, en utilisant le schéma `railpulse`. Le serveur de test a ensuite été arrêté.
Pour les exécuter sur une **base PostgreSQL temporaire dédiée aux tests**, définir :

```powershell
$env:TEST_DB_URL = 'jdbc:postgresql://127.0.0.1:55432/postgres'
$env:TEST_DB_DRIVER = 'org.postgresql.Driver'
$env:TEST_DB_USERNAME = 'railpulse_test'
$env:TEST_DB_SCHEMA = 'railpulse'
$env:TEST_DB_INIT = 'always'
.\mvnw.cmd verify
```

`TEST_DB_PASSWORD` peut être défini si le serveur temporaire en demande un.
Les tests utilisent `create-drop` : ne jamais renseigner la base Supabase ou une base contenant des données à conserver dans `TEST_DB_URL`.

Une clé secrète d'API déjà partagée doit être révoquée dans Supabase. Elle n'est ni nécessaire à JDBC, ni conservée dans ce dépôt.
