-- ============================================================================
-- SCRIPT SQL SCHEMA - BASE DE DONNÉES LIAS (MySQL / phpMyAdmin)
-- Université Hassan II de Casablanca - Faculté des Sciences Ben M'Sik
-- ============================================================================

CREATE DATABASE IF NOT EXISTS lias_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE lias_db;

SET FOREIGN_KEY_CHECKS = 0;

-- Drop table order sorted strictly by dependency (child tables first, parent tables last)
DROP TABLE IF EXISTS event_organizers;
DROP TABLE IF EXISTS equipment_distribution;
DROP TABLE IF EXISTS equipment_requests;
DROP TABLE IF EXISTS equipment_assignment_history;
DROP TABLE IF EXISTS document_history;
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS mandates;
DROP TABLE IF EXISTS role_histories;
DROP TABLE IF EXISTS role_history;
DROP TABLE IF EXISTS profile_history;
DROP TABLE IF EXISTS affiliations_history;
DROP TABLE IF EXISTS adhesion_requests;
DROP TABLE IF EXISTS messages;
DROP TABLE IF EXISTS members;
DROP TABLE IF EXISTS events;
DROP TABLE IF EXISTS equipment;
DROP TABLE IF EXISTS publications;
DROP TABLE IF EXISTS documents;
DROP TABLE IF EXISTS conventions;
DROP TABLE IF EXISTS pv_meetings;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS roles;
DROP TABLE IF EXISTS teams;

CREATE TABLE users (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       username VARCHAR(50) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       email VARCHAR(150) NOT NULL UNIQUE,
                       enabled BOOLEAN DEFAULT TRUE,
                       created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE roles (
                       id INT AUTO_INCREMENT PRIMARY KEY,
                       name VARCHAR(50) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_roles (
                            user_id BIGINT NOT NULL,
                            role_id INT NOT NULL,
                            PRIMARY KEY (user_id, role_id),
                            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                            FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE teams (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       name VARCHAR(150) NOT NULL UNIQUE,
                       description TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE members (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         first_name VARCHAR(100) NOT NULL,
                         last_name VARCHAR(100) NOT NULL,
                         email VARCHAR(150) NOT NULL UNIQUE,
                         photo VARCHAR(255),
                         academic_year INT,
                         status VARCHAR(50) NOT NULL, -- PERMANENT, ASSOCIATE, DOCTORANT, RETIRED, FORMER_MEMBER
                         institution VARCHAR(150),
                         origin_lab VARCHAR(150),
                         biography TEXT,
                         interests TEXT,
                         birth_date DATE,
                         hire_date DATE,
                         affiliation_date DATE,
                         team_id BIGINT,
                         FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE affiliations_history (
                                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                      member_id BIGINT NOT NULL,
                                      start_date DATE NOT NULL,
                                      end_date DATE,
                                      team_name VARCHAR(150),
                                      FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE role_history (
                              id BIGINT AUTO_INCREMENT PRIMARY KEY,
                              member_id BIGINT NOT NULL,
                              old_role VARCHAR(100) NOT NULL,
                              new_role VARCHAR(100) NOT NULL,
                              date_change DATE NOT NULL,
                              FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE adhesion_requests (
                                   id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                   name VARCHAR(150) NOT NULL,
                                   email VARCHAR(150) NOT NULL,
                                   cv VARCHAR(255) NOT NULL,
                                   motivation TEXT NOT NULL,
                                   status VARCHAR(50) DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED
                                   created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE events (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        title VARCHAR(255) NOT NULL,
                        type VARCHAR(50) NOT NULL, -- CONFERENCE, SEMINAR, WORKSHOP
                        description TEXT,
                        event_date DATE NOT NULL,
                        location VARCHAR(200) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE publications (
                              id BIGINT AUTO_INCREMENT PRIMARY KEY,
                              title VARCHAR(255) NOT NULL,
                              author VARCHAR(255) NOT NULL,
                              publication_year INT NOT NULL,
                              team VARCHAR(150)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE documents (
                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                           name VARCHAR(255) NOT NULL,
                           type VARCHAR(50) NOT NULL,
                           path VARCHAR(255) NOT NULL,
                           date_upload TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE equipment (
                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                           name VARCHAR(150) NOT NULL,
                           description VARCHAR(255),
                           quantity INT NOT NULL DEFAULT 1,
                           status VARCHAR(50) DEFAULT 'AVAILABLE'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE equipment_distribution (
                                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        equipment_id BIGINT NOT NULL,
                                        member_id BIGINT NOT NULL,
                                        distribution_date DATE NOT NULL,
                                        FOREIGN KEY (equipment_id) REFERENCES equipment(id) ON DELETE CASCADE,
                                        FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE conventions (
                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             partner VARCHAR(150) NOT NULL,
                             description TEXT,
                             convention_date DATE NOT NULL,
                             status VARCHAR(50) DEFAULT 'PENDING'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pv_meetings (
                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             title VARCHAR(255) NOT NULL,
                             writer VARCHAR(255) NOT NULL,
                             type VARCHAR(100) NOT NULL,
                             agenda TEXT NOT NULL,
                             decisions TEXT,
                             meeting_date DATE NOT NULL,
                             file_path VARCHAR(255),
                             status VARCHAR(50) DEFAULT 'PENDING'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE notifications (
                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                               message TEXT NOT NULL,
                               created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               is_read BOOLEAN DEFAULT FALSE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE mandates (
                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          member_id BIGINT NOT NULL,
                          role_title VARCHAR(255) NOT NULL,
                          team_assigned VARCHAR(255),
                          start_date DATE NOT NULL,
                          end_date DATE,
                          is_active BOOLEAN NOT NULL DEFAULT TRUE,
                          FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE role_histories (
                                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                member_id BIGINT NOT NULL,
                                old_role VARCHAR(150) NOT NULL,
                                new_role VARCHAR(150) NOT NULL,
                                change_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE equipment_assignment_history (
                                              id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                              equipment_id BIGINT,
                                              member_id BIGINT,
                                              action_type VARCHAR(50) NOT NULL,
                                              action_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                              FOREIGN KEY (equipment_id) REFERENCES equipment(id) ON DELETE SET NULL,
                                              FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE document_history (
                                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                  document_name VARCHAR(255) NOT NULL,
                                  action_type VARCHAR(50) NOT NULL,
                                  action_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE profile_history (
                                 id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 member_id BIGINT NOT NULL,
                                 first_name VARCHAR(100),
                                 last_name VARCHAR(100),
                                 email VARCHAR(150),
                                 status VARCHAR(50),
                                 institution VARCHAR(150),
                                 origin_lab VARCHAR(150),
                                 biography TEXT,
                                 interests TEXT,
                                 field_name VARCHAR(100),
                                 old_value TEXT,
                                 new_value TEXT,
                                 change_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                 FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE equipment_requests (
                                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                    equipment_name VARCHAR(150) NOT NULL,
                                    quantity INT NOT NULL DEFAULT 1,
                                    member_id BIGINT NOT NULL,
                                    status VARCHAR(50) DEFAULT 'PENDING',
                                    request_date DATE NOT NULL,
                                    decision_date DATE,
                                    FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE messages (
                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          sender VARCHAR(150) NOT NULL,
                          content TEXT NOT NULL,
                          message_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          channel_type VARCHAR(50) NOT NULL,
                          channel_name VARCHAR(150)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;

