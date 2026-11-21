package com.example;

import com.example.model.Reservation;
import com.example.model.Salle;
import com.example.repository.SalleRepositoryImpl;
import com.example.service.SalleService;
import com.example.service.SalleServiceImpl;
import com.example.util.PaginationResult;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 主程序（Java SE）：初始化 JPA、插入测试数据，并测试可用会议室查询、多条件搜索与分页。
 */
public class App {

    public static void main(String[] args) {
        // 根据 persistence.xml 中的持久化单元创建 EntityManagerFactory
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("SallePU");
        EntityManager em = emf.createEntityManager();

        try {
            // 插入测试数据
            initialiserDonnees(em);
            // 清空持久化上下文，确保后续查询真正访问数据库
            em.clear();

            // 手动注入依赖（在真实的 Java EE 环境中由 CDI/EJB 自动完成）
            SalleService salleService = new SalleServiceImpl(new SalleRepositoryImpl(em));

            LocalDateTime debut = LocalDateTime.of(2024, 6, 1, 9, 0);
            LocalDateTime fin = LocalDateTime.of(2024, 6, 1, 11, 0);

            // ===== 测试 1：JPQL 查询可用会议室 =====
            List<Salle> sallesDisponibles = salleService.getAvailableRooms(debut, fin);
            titre("1. Salles disponibles entre " + debut + " et " + fin + " (JPQL)");
            afficher(sallesDisponibles);

            // ===== 测试 2：Criteria API 多条件搜索 =====
            titre("2. Recherche multi-critères : nom contient 'salle', capacité >= 20, disponible sur le créneau");
            afficher(salleService.searchRooms("salle", 20, debut, fin, 1, 100));

            titre("3. Recherche par capacité seule (>= 50), sans créneau");
            afficher(salleService.searchRooms(null, 50, null, null, 1, 100));

            // ===== 测试 3：分页 =====
            int pageSize = 4;
            titre("4. Pagination de toutes les salles (taille de page = " + pageSize + ")");
            PaginationResult<Salle> page = salleService.searchRoomsPaginated(null, null, null, null, 1, pageSize);
            // 逐页遍历，直到没有下一页
            while (true) {
                System.out.println(page);
                afficher(page.getItems());
                if (!page.hasNext()) {
                    break;
                }
                page = salleService.searchRoomsPaginated(null, null, null, null, page.getCurrentPage() + 1, pageSize);
            }

            titre("5. Pagination des salles disponibles sur le créneau (page 2, taille 3)");
            PaginationResult<Salle> page2 = salleService.searchRoomsPaginated(null, null, debut, fin, 2, 3);
            System.out.println(page2);
            afficher(page2.getItems());
        } finally {
            // 关闭资源
            em.close();
            emf.close();
        }
    }

    /**
     * 在一个事务中插入 10 个会议室及若干预订，用于测试。
     */
    private static void initialiserDonnees(EntityManager em) {
        em.getTransaction().begin();

        String[] noms = {"Salle Alpha", "Salle Beta", "Salle Gamma", "Salle Delta", "Salle Epsilon",
                         "Amphi Zeta", "Salle Eta", "Salle Theta", "Salle Iota", "Amphi Kappa"};
        int[] capacites = {10, 20, 30, 15, 25, 120, 40, 8, 60, 200};

        Salle[] salles = new Salle[noms.length];
        for (int i = 0; i < noms.length; i++) {
            salles[i] = new Salle(noms[i], capacites[i]);
        }

        // 与查询时间段 09:00-11:00 重叠的预订 → 这些会议室不可用
        salles[0].addReservation(new Reservation(LocalDateTime.of(2024, 6, 1, 8, 0),
                                                 LocalDateTime.of(2024, 6, 1, 10, 0)));   // 部分重叠（开始前）
        salles[2].addReservation(new Reservation(LocalDateTime.of(2024, 6, 1, 10, 30),
                                                 LocalDateTime.of(2024, 6, 1, 12, 0)));   // 部分重叠（结束前）
        salles[5].addReservation(new Reservation(LocalDateTime.of(2024, 6, 1, 9, 30),
                                                 LocalDateTime.of(2024, 6, 1, 10, 30)));  // 完全包含在内

        // 不与查询时间段重叠的预订 → 这些会议室仍然可用
        salles[1].addReservation(new Reservation(LocalDateTime.of(2024, 6, 1, 11, 0),
                                                 LocalDateTime.of(2024, 6, 1, 12, 0)));   // 紧接着结束时间开始
        salles[4].addReservation(new Reservation(LocalDateTime.of(2024, 6, 1, 7, 0),
                                                 LocalDateTime.of(2024, 6, 1, 9, 0)));    // 恰好在开始时间结束
        salles[8].addReservation(new Reservation(LocalDateTime.of(2024, 6, 2, 9, 0),
                                                 LocalDateTime.of(2024, 6, 2, 11, 0)));   // 另一天

        // 由于 cascade = ALL，持久化会议室时会同时持久化其预订
        for (Salle salle : salles) {
            em.persist(salle);
        }

        em.getTransaction().commit();
    }

    // 打印分节标题
    private static void titre(String texte) {
        System.out.println();
        System.out.println("==== " + texte + " ====");
    }

    // 打印会议室列表
    private static void afficher(List<Salle> salles) {
        if (salles.isEmpty()) {
            System.out.println("  (aucune salle)");
        }
        for (Salle salle : salles) {
            System.out.println("  - " + salle);
        }
    }
}
