-- Rail Pulse : jeu fictif additionnel pour PostgreSQL / Supabase.
-- Executer tout ce fichier dans SQL Editor, sur le projet de l'application.
-- Prerequis : les tables du schema railpulse sont deja initialisees par JPA.
-- Aucun compte, secret ou enregistrement existant n'est modifie ou supprime.
-- Relancable tant que les noms [Démo 2026], references et marqueurs [DEMO26-...]
-- sont conserves. Les dates existantes ne sont pas recalculees au relancement.

BEGIN;
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '30s';

-- Serialise les imports concurrents, sans bloquer les consultations.
LOCK TABLE railpulse.station, railpulse.equipement, railpulse.prestataire,
    railpulse.incident, railpulse.evaluation IN SHARE ROW EXCLUSIVE MODE;

CREATE TEMP TABLE rp_demo_stations ON COMMIT DROP AS
SELECT * FROM (VALUES
    (1, 'CEN', '[Démo 2026] Gare Centrale'),
    (2, 'REP', '[Démo 2026] République'),
    (3, 'UNI', '[Démo 2026] Université'),
    (4, 'HOP', '[Démo 2026] Hôpital'),
    (5, 'PAR', '[Démo 2026] Parc des Expositions'),
    (6, 'AER', '[Démo 2026] Aéroport'),
    (7, 'STA', '[Démo 2026] Stade'),
    (8, 'POR', '[Démo 2026] Port')
) AS stations(ordre, code, nom);

INSERT INTO railpulse.station (nom)
SELECT d.nom FROM rp_demo_stations d
WHERE NOT EXISTS (SELECT 1 FROM railpulse.station s WHERE s.nom = d.nom);

CREATE TEMP TABLE rp_demo_prestataires ON COMMIT DROP AS
SELECT * FROM (VALUES
    (1, '[Démo 2026] Maintenance Nord', 'demo26-nord@example.invalid'),
    (2, '[Démo 2026] Interventions Centre', 'demo26-centre@example.invalid'),
    (3, '[Démo 2026] Services Mobilité', 'demo26-mobilite@example.invalid'),
    (4, '[Démo 2026] Assistance Réseau', 'demo26-reseau@example.invalid')
) AS prestataires(ordre, nom, contact);

INSERT INTO railpulse.prestataire (nom, contact)
SELECT d.nom, d.contact FROM rp_demo_prestataires d
WHERE NOT EXISTS (
    SELECT 1 FROM railpulse.prestataire p WHERE p.nom = d.nom AND p.contact = d.contact
);

CREATE TEMP TABLE rp_demo_equipements ON COMMIT DROP AS
SELECT 'DEMO26-' || s.code || '-' || t.code || '-01' AS reference,
    t.libelle, t.type, s.nom AS station_nom, t.etat
FROM rp_demo_stations s
CROSS JOIN (VALUES
    ('ASC', 'Ascenseur du quai A', 'ASCENSEUR', 'EN_PANNE'),
    ('ESC', 'Escalier mécanique de la sortie nord', 'ESCALIER_MECANIQUE', 'EN_PANNE'),
    ('POR', 'Portique de l''entrée principale', 'PORTIQUE', 'EN_MAINTENANCE'),
    ('DIS', 'Distributeur de titres du hall', 'DISTRIBUTEUR', 'EN_SERVICE'),
    ('ECR', 'Écran d''information du quai B', 'ECRAN_INFORMATION', 'EN_PANNE')
) AS t(code, libelle, type, etat);

INSERT INTO railpulse.equipement (reference, libelle, type, station_id, etat_fonctionnement)
SELECT d.reference, d.libelle, d.type,
    (SELECT min(s.id) FROM railpulse.station s WHERE s.nom = d.station_nom), d.etat
FROM rp_demo_equipements d
WHERE NOT EXISTS (SELECT 1 FROM railpulse.equipement e WHERE e.reference = d.reference);

-- Une seule horloge UTC pour toutes les dates de cet import.
CREATE TEMP TABLE rp_demo_incidents ON COMMIT DROP AS
SELECT '[DEMO26-' || s.code || '-' || c.numero || ']' AS marqueur,
    '[DEMO26-' || s.code || '-' || c.numero || '] ' || c.description AS description,
    'DEMO26-' || s.code || '-' || c.equipement || '-01' AS reference,
    c.statut,
    horloge.maintenant - c.ouverture_heures * INTERVAL '1 hour' AS date_ouverture,
    CASE WHEN c.resolution_heures IS NOT NULL
        THEN horloge.maintenant - c.resolution_heures * INTERVAL '1 hour'
        ELSE NULL END AS date_resolution,
    CASE WHEN c.prestataire_ordre IS NOT NULL
        THEN 1 + ((s.ordre - 1) % 4) ELSE NULL END AS prestataire_ordre,
    CASE WHEN c.numero = '05' THEN 1 + ((s.ordre - 1) % 5) ELSE NULL END AS note
FROM rp_demo_stations s
CROSS JOIN (VALUES
    ('01', 'ASC', 'NON_AFFECTE', 12, NULL::integer, NULL::integer,
        'Porte bloquée : accès au quai temporairement indisponible.'),
    ('02', 'ESC', 'NON_AFFECTE', 60, NULL, NULL,
        'Arrêts intermittents signalés, diagnostic à planifier.'),
    ('03', 'ECR', 'NON_AFFECTE', 90, NULL, NULL,
        'Affichage voyageurs figé depuis plusieurs jours.'),
    ('04', 'POR', 'AFFECTE', 96, NULL, 3,
        'Lecteur de titres défaillant, intervention du prestataire en cours.'),
    ('05', 'DIS', 'RESOLU', 240, 192, 3,
        'Paiement indisponible : lecteur remplacé et remise en service confirmée.'),
    ('06', 'ASC', 'RESOLU', 360, 300, NULL,
        'Ancien défaut de fermeture corrigé par une intervention interne.')
) AS c(numero, equipement, statut, ouverture_heures, resolution_heures,
    prestataire_ordre, description)
CROSS JOIN (SELECT CURRENT_TIMESTAMP AT TIME ZONE 'UTC' AS maintenant) horloge;

INSERT INTO railpulse.incident
    (equipement_id, description, date_ouverture, statut, prestataire_id, date_resolution)
SELECT e.id, d.description, d.date_ouverture, d.statut,
    (SELECT min(p.id) FROM railpulse.prestataire p
        JOIN rp_demo_prestataires dp ON dp.nom = p.nom AND dp.contact = p.contact
        WHERE dp.ordre = d.prestataire_ordre),
    d.date_resolution
FROM rp_demo_incidents d
JOIN railpulse.equipement e ON e.reference = d.reference
WHERE NOT EXISTS (
    SELECT 1 FROM railpulse.incident i
    WHERE i.equipement_id = e.id AND starts_with(i.description, d.marqueur || ' ')
);

INSERT INTO railpulse.evaluation (incident_id, note, commentaire, date_evaluation)
SELECT i.id, d.note,
    CASE d.note
        WHEN 1 THEN 'Délai très long, suivi de l''intervention à améliorer.'
        WHEN 2 THEN 'Réparation correcte, communication insuffisante.'
        WHEN 3 THEN 'Intervention satisfaisante dans l''ensemble.'
        WHEN 4 THEN 'Réparation efficace et compte rendu clair.'
        WHEN 5 THEN 'Intervention rapide et excellente communication.'
    END,
    i.date_resolution + INTERVAL '2 hours'
FROM rp_demo_incidents d
JOIN railpulse.equipement e ON e.reference = d.reference
JOIN railpulse.incident i
    ON i.equipement_id = e.id AND starts_with(i.description, d.marqueur || ' ')
WHERE d.note IS NOT NULL AND i.statut = 'RESOLU' AND i.prestataire_id IS NOT NULL
    AND i.date_resolution + INTERVAL '2 hours' <= CURRENT_TIMESTAMP AT TIME ZONE 'UTC'
    AND NOT EXISTS (SELECT 1 FROM railpulse.evaluation v WHERE v.incident_id = i.id);

COMMIT;

-- Bilan du jeu fictif uniquement (hors donnees deja presentes).
-- Sur une premiere execution : 8 / 40 / 4 / 48 / 8.
SELECT
    (SELECT count(*) FROM railpulse.station WHERE starts_with(nom, '[Démo 2026] ')) AS stations_demo,
    (SELECT count(*) FROM railpulse.equipement WHERE starts_with(reference, 'DEMO26-')) AS equipements_demo,
    (SELECT count(*) FROM railpulse.prestataire WHERE starts_with(nom, '[Démo 2026] ')) AS prestataires_demo,
    (SELECT count(*) FROM railpulse.incident WHERE starts_with(description, '[DEMO26-')) AS incidents_demo,
    (SELECT count(*) FROM railpulse.evaluation v JOIN railpulse.incident i ON i.id = v.incident_id
        WHERE starts_with(i.description, '[DEMO26-')) AS evaluations_demo;
