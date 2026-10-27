# PostgreSQL / Supabase

Rail Pulse utilise JDBC et JPA avec PostgreSQL, sans appeler la Data API Supabase. Il ne requiert aucune clé `sb_secret_*` ou clé publique de cette API.

## Connexion

Dans **Connect à Session pooler**, relever l'hôte, le port, le nom de base et l'utilisateur. La connexion directe utilise généralement IPv6 ; le Session pooler permet une connexion IPv4. Ne pas deviner l'hôte du pooler à partir de la région.

Variables, fournies dans l'environnement ou le gestionnaire de secrets de Render :

```dotenv
SPRING_PROFILES_ACTIVE=supabase,render
SPRING_DATASOURCE_URL=jdbc:postgresql://HOTE_SESSION_POOLER:5432/postgres?sslmode=require&connectTimeout=10&socketTimeout=30
SPRING_DATASOURCE_USERNAME=UTILISATEUR_SESSION_POOLER
SPRING_DATASOURCE_PASSWORD=
```

Remplir le mot de passe PostgreSQL, séparément de l'URL JDBC. Le `.env` local n'est pas chargé automatiquement par Spring : Render peut l'importer, et un terminal/IDE peut définir les variables explicitement. Le script de démonstration autonome n'importe pas les paramètres de base distante.

Le profil `supabase` reste le profil par défaut historique ; les variables standard remplacent ses paramètres. Pour une démonstration sans ce service, utiliser le script H2 décrit dans le README.

## Schéma et comptes

`db/supabase-schema.sql` crée le schéma `railpulse` ; Hibernate initialise les tables et contraintes. Le compte PostgreSQL doit disposer des droits correspondants. Les utilisateurs applicatifs sont dans `railpulse.users` ; il ne s'agit pas de Supabase Auth.

Au démarrage, les anciennes valeurs de mot de passe sont migrées en BCrypt transactionnellement. Les nouvelles écritures imposent BCrypt. Le profil `demo`, avec ses deux variables de secret, crée `demo.agent` et `demo.responsable` sans remplacer les comptes existants. Voir [Render](render.md) pour la liste complète des variables.

Le schéma `railpulse` n'a pas à être exposé par la Data API pour les usages JDBC. Les données MySQL ne sont pas automatiquement transférées. La génération JPA `ddl-auto=update` est adaptée au projet pédagogique ; des migrations de schéma versionnées sont à privilégier pour une exploitation durable.

## Tests isolés

La CI PostgreSQL utilise une base temporaire dédiée et les variables `TEST_DB_*`, dans quatre schémas séparés. Les tests emploient `create-drop` : ne jamais fournir la base Supabase ou une base contenant des données à conserver dans `TEST_DB_URL`.

Référence : [connexions PostgreSQL Supabase](https://supabase.com/docs/guides/database/connecting-to-postgres).
