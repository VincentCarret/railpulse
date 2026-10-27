# PostgreSQL / Supabase

Rail Pulse utilise JDBC et JPA avec PostgreSQL, sans appeler la Data API Supabase. Il ne requiert aucune cl? `sb_secret_*` ou cl? publique de cette API.

## Connexion

Dans **Connect ? Session pooler**, relever l'h?te, le port, le nom de base et l'utilisateur. La connexion directe utilise g?n?ralement IPv6 ; le Session pooler permet une connexion IPv4. Ne pas deviner l'h?te du pooler ? partir de la r?gion.

Variables, fournies dans l'environnement ou le gestionnaire de secrets de Render :

```dotenv
SPRING_PROFILES_ACTIVE=supabase,render
SPRING_DATASOURCE_URL=jdbc:postgresql://HOTE_SESSION_POOLER:5432/postgres?sslmode=require&connectTimeout=10&socketTimeout=30
SPRING_DATASOURCE_USERNAME=UTILISATEUR_SESSION_POOLER
SPRING_DATASOURCE_PASSWORD=
```

Remplir le mot de passe PostgreSQL, s?par?ment de l'URL JDBC. Le `.env` local n'est pas charg? automatiquement par Spring : Render peut l'importer, et un terminal/IDE peut d?finir les variables explicitement. Le script de d?monstration autonome n'importe pas les param?tres de base distante.

Le profil `supabase` reste le profil par d?faut historique ; les variables standard remplacent ses param?tres. Pour une d?monstration sans ce service, utiliser le script H2 d?crit dans le README.

## Sch?ma et comptes

`db/supabase-schema.sql` cr?e le sch?ma `railpulse` ; Hibernate initialise les tables et contraintes. Le compte PostgreSQL doit disposer des droits correspondants. Les utilisateurs applicatifs sont dans `railpulse.users` ; il ne s'agit pas de Supabase Auth.

Au d?marrage, les anciennes valeurs de mot de passe sont migr?es en BCrypt transactionnellement. Les nouvelles ?critures imposent BCrypt. Le profil `demo`, avec ses deux variables de secret, cr?e `demo.agent` et `demo.responsable` sans remplacer les comptes existants. Voir [Render](render.md) pour la liste compl?te des variables.

Le sch?ma `railpulse` n'a pas ? ?tre expos? par la Data API pour les usages JDBC. Les donn?es MySQL ne sont pas automatiquement transf?r?es. La g?n?ration JPA `ddl-auto=update` est adapt?e au projet p?dagogique ; des migrations de sch?ma versionn?es sont ? privil?gier pour une exploitation durable.

## Tests isol?s

La CI PostgreSQL utilise une base temporaire d?di?e et les variables `TEST_DB_*`, dans quatre sch?mas s?par?s. Les tests emploient `create-drop` : ne jamais fournir la base Supabase ou une base contenant des donn?es ? conserver dans `TEST_DB_URL`.

R?f?rence : [connexions PostgreSQL Supabase](https://supabase.com/docs/guides/database/connecting-to-postgres).
