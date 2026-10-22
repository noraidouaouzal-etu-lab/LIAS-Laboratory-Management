-- ============================================================================
-- SCRIPT SQL DATA - INITIAL VALUES FOR LIAS LABORATORY MANAGEMENT SYSTEM
-- ============================================================================

USE lias_db;

-- Roles
INSERT IGNORE INTO roles (id, name) VALUES 
(1, 'ROLE_VISITOR'),
(2, 'ROLE_DOCTORANT'),
(3, 'ROLE_MEMBER'),
(4, 'ROLE_DIRECTOR'),
(5, 'ROLE_ADMIN'),
(6, 'ROLE_ASSOCIATE');

-- Teams
INSERT IGNORE INTO teams (id, name, description) VALUES 
(1, 'Aide à la Décision', 'Systèmes d\'aide à la décision complexes et optimization multicible'),
(2, 'Réseaux Sans Fil', 'Protocoles de routage IoT légers et sécurisation intégrée'),
(3, 'Systèmes Embarqués', 'Modélisation, FPGA, calcul Edge AI et efficacité énergétique');

-- Members
INSERT IGNORE INTO members (id, first_name, last_name, email, status, institution, origin_lab, biography, interests, team_id, birth_date, hire_date, affiliation_date) VALUES 
(1, 'Abdelouahad', 'ENNASSIRI', 'director@lias-lab.ma', 'PERMANENT', 'FSBM', 'LIAS', 'Directeur fondateur du LIAS', 'IA, Aide à la Décision', 1, '1975-04-12', '2010-09-01', '2015-01-15'),
(2, 'Salma', 'ALAMI', 'doctorant@lias-lab.ma', 'DOCTORANT', 'FSBM', 'LIAS', 'Doctorante de 3ème année', 'Deep Learning, Urban Intelligent Transport', 1, '1999-08-15', NULL, '2023-11-01'),
(3, 'Khadija', 'BENJELLOUN', 'member@lias-lab.ma', 'PERMANENT', 'FSBM', 'LIAS', 'Professeure habilitée', 'IoT, Securité', 2, '1980-01-22', '2015-03-01', '2016-01-15'),
(4, 'Karim', 'SADIKI', 'associate@lias-lab.ma', 'ASSOCIATE', 'U Hassan II', 'Partner Lab', 'Expert et chercheur associé au projet Edge-AI.', 'Embedded systems, IoT Security', 3, '1985-06-18', NULL, '2024-03-10');

-- Events
INSERT IGNORE INTO events (id, title, type, description, event_date, location) VALUES 
(1, 'Journées Scientifiques sur la Sécurité des Systèmes Communicants', 'CONFERENCE', 'Conférence nationale sur la sécurité IoT réunissant experts et industriels.', '2026-10-18', 'Grand Amphithéâtre, FSBM, Casablanca'),
(2, 'Atelier Deep Learning et Edge AI', 'WORKSHOP', 'Atelier d\'évaluation pratique de modèles compressés sur micro-cibles.', '2026-06-25', 'Salle des Séminaires, LIAS, FSBM');

-- Publications
INSERT IGNORE INTO publications (id, title, author, publication_year, team) VALUES 
(1, 'Deep Reinforcement Learning for Dynamic Traffic Sign Control', 'Salma ALAMI, Abdelouahad ENNASSIRI', 2026, 'Aide à la Décision'),
(2, 'A Lightweight Cryptographic Frame Scheme for Secure Industrial IoT', 'Khadija BENJELLOUN', 2025, 'Réseaux Sans Fil');

-- Equipment
INSERT IGNORE INTO equipment (id, name, description, quantity, status) VALUES
(1, 'Sondeur RFID Industriel UHF', 'Sondeur d''analyse RFID pour IoT', 2, 'AVAILABLE'),
(2, 'Nvidia Jetson Agx Orin (64GB)', 'Kit de développement IA embarqué', 1, 'ASSIGNED');

-- Adhesion Requests
INSERT IGNORE INTO adhesion_requests (id, name, email, cv, motivation, status) VALUES 
(1, 'Anass OUAHABI', 'anass.ouahabi@gmail.com', 'CV_Anass_Ouahabi.pdf', 'Je postule pour un doctorat en Edge AI appliquée à l\'agri-tech.', 'PENDING');

-- Conventions
INSERT IGNORE INTO conventions (id, partner, description, convention_date) VALUES 
(1, 'Masen (Moroccan Agency for Sustainable Energy)', 'Intégration énergétique intelligente.', '2024-05-01');

-- Mandates
INSERT IGNORE INTO mandates (id, member_id, role_title, team_assigned, start_date, is_active) VALUES
(1, 1, 'Director', 'LIAS Laboratory', '2015-01-15', 1),
(2, 3, 'Vice Director', 'LIAS Laboratory', '2016-01-15', 1);

-- Notifications
INSERT IGNORE INTO notifications (id, message, is_read) VALUES
(1, 'Acquisition de matériel validée par le conseil.', 0),
(2, 'Appel à soumission d\'articles pour JSSSC 2026 ouvert.', 0);

-- Seeding Plain Text Users (password is '1234')
INSERT IGNORE INTO users (id, username, email, password, enabled) VALUES
(1, 'admin', 'admin@lias-lab.ma', '1234', 1),
(2, 'director', 'director@lias-lab.ma', '1234', 1),
(3, 'member', 'member@lias-lab.ma', '1234', 1),
(4, 'doctorant', 'doctorant@lias-lab.ma', '1234', 1),
(5, 'associate', 'associate@lias-lab.ma', '1234', 1),
(6, 'test', 'test@gmail.com', '1234', 1);

-- Seeding User Role Associations
INSERT IGNORE INTO user_roles (user_id, role_id) VALUES
(1, 5), -- ROLE_ADMIN
(2, 4), -- ROLE_DIRECTOR
(3, 3), -- ROLE_MEMBER
(4, 2), -- ROLE_DOCTORANT
(5, 6), -- ROLE_ASSOCIATE
(6, 3); -- ROLE_MEMBER

-- Seeding member profile for test_user
INSERT IGNORE INTO members (id, first_name, last_name, email, status, institution, origin_lab, biography, interests, team_id, birth_date, hire_date, affiliation_date) VALUES 
(5, 'Test', 'USER', 'test@gmail.com', 'PERMANENT', 'FSBM', 'LIAS', 'Chercheur de test', 'AI, Cloud computing', 1, '1990-01-01', '2020-01-01', '2020-01-01');

