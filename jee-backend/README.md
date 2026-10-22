# 🏢 LIAS Laboratory Manager Backend

Ce répertoire contient le backend d'entreprise pour le laboratoire d'Informatique, d'Énergies et d'Aide à la Décision (LIAS) de la Faculté des Sciences Ben M'Sik - Casablanca.

## 🧱 Architecture du Projet

Le code respecte l'architecture multi-couche standard recommandée JEE / Spring Boot :

```
/jee-backend
 ├── pom.xml                               # Fichier de dépendances Maven
 ├── schema.sql                             # Script MySQL complet (Mots clefs, Foreign Keys et Cascades)
 └── src/main/java/com/lias/manager
      ├── config/
      │    └── SecurityConfig.java         # Filtre de sécurité Spring Security (Rôles & Directives)
      ├── controller/
      │    └── MemberController.java       # Contrôleur REST d'accès aux chercheurs et listings
      ├── entity/
      │    ├── User.java                   # Entité chercheur (avec attributs d'historisation)
      │    ├── Role.java                   # Entité Rôle autoritaire
      │    └── Mandate.java                # Contrat/Mandat des directeurs et chefs d'équipes
      ├── repository/
      │    └── UserRepository.java         # Interface de communication MySQL (Spring Data JPA)
      └── service/
           └── MemberService.java          # Service métier encapsulant le Soft-Delete
```

## 🚀 Fonctionnalités Implémentées

1. **Gouvernance & Mandats**: Historisation automatique des nominations directrices.
2. **Soft-Delete**: Les comptes des chercheurs ou anciens doctorants ne perdent jamais leurs données d'archives (utilisation de l'attribut logique `deleted`).
3. **Sécurité avancée**: Spring Security 6 ségréguant les routes d'examen des CVs/Demandes d'adhésion au rôle `ROLE_DIRECTOR` (Pr. Ennassiri) et l'accès général aux publications aux rôles `ROLE_PERMANENT_MEMBER`.

## ⚙️ Comment Lancer le projet

1. **Base de données MySQL (phpMyAdmin)**:
   - Importez le fichier `./src/main/resources/schema.sql` dans phpMyAdmin ou votre console MySQL. Une base `lias_db` sera provisionnée avec les relations de cascades et de fausses données de démonstration préinstallées.
   - Configurez vos identifiants dans votre fichier `src/main/resources/application.properties` (non inclus) :
     ```properties
     spring.datasource.url=jdbc:mysql://localhost:3306/lias_db?useSSL=false&serverTimezone=UTC
     spring.datasource.username=votre_utilisateur
     spring.datasource.password=votre_mot_de_passe
     spring.jpa.hibernate.ddl-auto=none
     ```

2. **Démarrer le serveur Spring Boot**:
   - Compilez le projet via Maven :
     ```bash
     mvn clean install
     ```
   - Démarrez l'application :
     ```bash
     mvn spring-boot:run
     ```
