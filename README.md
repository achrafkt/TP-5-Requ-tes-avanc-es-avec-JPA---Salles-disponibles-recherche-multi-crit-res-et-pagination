# TP 5 : Requêtes avancées avec JPA

**Salles disponibles, recherche multi-critères et pagination**

Application Java de gestion de salles construite avec **JPA (Hibernate)** et une base **H2 en mémoire**. Elle montre trois techniques de requêtage avancé :

1. une **requête JPQL** qui trouve les salles libres sur un créneau horaire ;
2. une **recherche multi-critères dynamique** avec l'**API Criteria** ;
3. une **pagination côté serveur** avec `setFirstResult` et `setMaxResults`.

---

## Technologies

| Outil | Version |
|---|---|
| Java | 8 ou plus (testé avec JDK 21) |
| Maven | 3.x |
| JPA (`javax.persistence-api`) | 2.2 |
| Hibernate (`hibernate-core`) | 5.4.30.Final |
| H2 Database | 1.4.200 |
| Java EE API (`javaee-api`, scope `provided`) | 8.0 |

---

## Structure du projet

```
salle-management/
├── pom.xml
└── src/main/
    ├── java/com/example/
    │   ├── App.java                         # Classe principale : données de test et démonstration
    │   ├── model/
    │   │   ├── Salle.java                   # Entité Salle (OneToMany → Reservation)
    │   │   └── Reservation.java             # Entité Reservation (ManyToOne → Salle)
    │   ├── repository/
    │   │   ├── SalleRepository.java         # Interface d'accès aux données
    │   │   └── SalleRepositoryImpl.java     # Requêtes JPQL et Criteria, pagination
    │   ├── service/
    │   │   ├── SalleService.java            # Interface métier
    │   │   └── SalleServiceImpl.java        # Règles métier et validation des paramètres
    │   └── util/
    │       └── PaginationResult.java        # Résultat paginé générique
    └── resources/META-INF/
        └── persistence.xml                  # Unité de persistance "SallePU"
```

### Architecture en couches

```
App  ──►  SalleService  ──►  SalleRepository  ──►  EntityManager (JPA / Hibernate)  ──►  H2
```

- **Modèle** : les entités JPA `Salle` et `Reservation`, liées par une relation bidirectionnelle.
- **Repository** : il contient toute la logique de requêtage (JPQL, Criteria, comptage, pagination).
- **Service** : il applique les règles métier. Le début doit précéder la fin, la page doit valoir au moins 1 et la taille au moins 1. Il assemble aussi les `PaginationResult`.
- **Annotations JEE** : `@Stateless`, `@Inject` et `@PersistenceContext` sont présentes pour un déploiement sur un serveur d'applications. En Java SE (classe `App`), les dépendances sont injectées à la main par constructeur.

---

## Modèle de données

```
┌──────────────┐ 1        * ┌──────────────────┐
│    salles    │────────────│   reservations   │
├──────────────┤            ├──────────────────┤
│ id (PK)      │            │ id (PK)          │
│ nom (unique) │            │ salle_id (FK)    │
│ capacite     │            │ debut            │
└──────────────┘            │ fin              │
                            └──────────────────┘
```

---

## Fonctionnalités

### 1. Salles disponibles (JPQL)

Une salle est **disponible** si aucune de ses réservations ne **chevauche** le créneau demandé. Deux intervalles se chevauchent si `debutRecherche < r.fin` **et** `finRecherche > r.debut`.

```java
SELECT s FROM Salle s WHERE s.id NOT IN (
    SELECT r.salle.id FROM Reservation r
    WHERE (:debut < r.fin AND :fin > r.debut)
) ORDER BY s.nom
```

> Une réservation qui se termine exactement au début du créneau (ou qui commence exactement à sa fin) **ne bloque pas** la salle.

### 2. Recherche multi-critères (API Criteria)

Les filtres sont construits dynamiquement, seulement pour les critères renseignés (non `null`) :

| Critère | Prédicat |
|---|---|
| `nom` | `LIKE '%nom%'`, insensible à la casse |
| `capaciteMin` | `capacite >= capaciteMin` |
| `debut` et `fin` | sous-requête `NOT IN` qui exclut les salles réservées sur le créneau |

Une seule méthode privée, `buildPredicates()`, construit les filtres de la requête de recherche **et** de la requête de comptage. Le nombre total d'éléments correspond donc toujours aux résultats affichés.

### 3. Pagination côté serveur

```java
query.setFirstResult((page - 1) * size);   // page commence à 1
query.setMaxResults(size);
```

Les résultats sont triés par nom pour que l'ordre des pages reste stable. La classe `PaginationResult<T>` contient :
`items`, `currentPage`, `pageSize`, `totalItems`, `totalPages`, ainsi que `hasNext()` et `hasPrevious()`.

---

## Installation et exécution

### Prérequis
- JDK 8 ou plus
- Maven 3.x

### Commandes

```bash
git clone https://github.com/achrafkt/TP-5-Requ-tes-avanc-es-avec-JPA---Salles-disponibles-recherche-multi-crit-res-et-pagination.git
cd TP-5-Requ-tes-avanc-es-avec-JPA---Salles-disponibles-recherche-multi-crit-res-et-pagination/salle-management

mvn clean compile
mvn exec:java -Dexec.mainClass="com.example.App"
```

> La classe principale est déjà configurée dans le `pom.xml`, donc `mvn exec:java` suffit.

Les requêtes SQL générées par Hibernate s'affichent dans la console (`hibernate.show_sql=true`), ce qui aide au débogage.

---

## Données de test et résultats attendus

`App` insère **10 salles** et **6 réservations**. Le créneau testé est le **01/06/2024 de 09:00 à 11:00**.

| Salle | Capacité | Réservation | Disponible ? |
|---|---|---|---|
| Salle Alpha | 10 | 08:00 à 10:00 (chevauchement au début) | ❌ |
| Salle Beta | 20 | 11:00 à 12:00 (commence à la fin du créneau) | ✅ |
| Salle Gamma | 30 | 10:30 à 12:00 (chevauchement à la fin) | ❌ |
| Salle Delta | 15 | aucune | ✅ |
| Salle Epsilon | 25 | 07:00 à 09:00 (se termine au début du créneau) | ✅ |
| Amphi Zeta | 120 | 09:30 à 10:30 (incluse dans le créneau) | ❌ |
| Salle Eta | 40 | aucune | ✅ |
| Salle Theta | 8 | aucune | ✅ |
| Salle Iota | 60 | le 02/06 (un autre jour) | ✅ |
| Amphi Kappa | 200 | aucune | ✅ |

### Sortie obtenue

```
==== 1. Salles disponibles entre 2024-06-01T09:00 et 2024-06-01T11:00 (JPQL) ====
  - Amphi Kappa (Capacité: 200)
  - Salle Beta (Capacité: 20)
  - Salle Delta (Capacité: 15)
  - Salle Epsilon (Capacité: 25)
  - Salle Eta (Capacité: 40)
  - Salle Iota (Capacité: 60)
  - Salle Theta (Capacité: 8)

==== 2. Recherche multi-critères : nom contient 'salle', capacité >= 20, disponible sur le créneau ====
  - Salle Beta (Capacité: 20)
  - Salle Epsilon (Capacité: 25)
  - Salle Eta (Capacité: 40)
  - Salle Iota (Capacité: 60)

==== 3. Recherche par capacité seule (>= 50), sans créneau ====
  - Amphi Kappa (Capacité: 200)
  - Amphi Zeta (Capacité: 120)
  - Salle Iota (Capacité: 60)

==== 4. Pagination de toutes les salles (taille de page = 4) ====
Page 1/3 (10 éléments au total)   → Amphi Kappa, Amphi Zeta, Salle Alpha, Salle Beta
Page 2/3 (10 éléments au total)   → Salle Delta, Salle Epsilon, Salle Eta, Salle Gamma
Page 3/3 (10 éléments au total)   → Salle Iota, Salle Theta

==== 5. Pagination des salles disponibles sur le créneau (page 2, taille 3) ====
Page 2/3 (7 éléments au total)
  - Salle Epsilon (Capacité: 25)
  - Salle Eta (Capacité: 40)
  - Salle Iota (Capacité: 60)
```

> Sous Windows, il arrive que les accents s'affichent mal dans la console (`Capacit�`). Les sources sont bien en UTF-8 : le problème vient seulement de l'encodage du terminal. Pour le corriger, lance `chcp 65001` avant l'exécution.

---

## Commentaires du code

Le code source est commenté **en chinois (中文注释)**.

---

## Pistes d'amélioration

- Gérer les **équipements** des salles et pouvoir les rechercher.
- Ajouter un **tri dynamique** (par capacité, par nom, etc.).
- Créer une **interface web** (JSF ou Servlet/JSP).
- Détecter les **conflits de réservation** dans le service au moment de créer une réservation.

---

## Auteur

**Achraf**, TP 5 : Requêtes avancées avec JPA (Java EE)
