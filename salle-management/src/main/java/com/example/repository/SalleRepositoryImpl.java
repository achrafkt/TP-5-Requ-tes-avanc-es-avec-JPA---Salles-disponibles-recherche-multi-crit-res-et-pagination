package com.example.repository;

import com.example.model.Reservation;
import com.example.model.Salle;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.AbstractQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SalleRepository 的实现类：使用 EntityManager 执行 JPQL 和 Criteria 查询。
 * 在 Java EE 容器中作为无状态 EJB 运行；在 Java SE 中可通过构造函数手动注入 EntityManager。
 */
@Stateless
public class SalleRepositoryImpl implements SalleRepository {

    // 在 Java EE 容器中由容器自动注入
    @PersistenceContext(unitName = "SallePU")
    private EntityManager entityManager;

    // EJB 要求的无参构造函数
    public SalleRepositoryImpl() {
    }

    // Java SE 环境下手动注入 EntityManager
    public SalleRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * JPQL 查询：子查询找出在该时间段内存在重叠预订的会议室 ID，
     * 主查询返回 ID 不在该列表中的所有会议室。
     * 重叠条件：已有预订开始时间 < 查询结束时间 且 已有预订结束时间 > 查询开始时间。
     */
    @Override
    public List<Salle> findAvailableRooms(LocalDateTime debut, LocalDateTime fin) {
        String jpql = "SELECT s FROM Salle s WHERE s.id NOT IN (" +
                      "SELECT r.salle.id FROM Reservation r WHERE (:debut < r.fin AND :fin > r.debut)" +
                      ") ORDER BY s.nom";
        return entityManager.createQuery(jpql, Salle.class)
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .getResultList();
    }

    /**
     * Criteria API 多条件搜索 + 分页。
     */
    @Override
    public List<Salle> searchRooms(String nom, Integer capaciteMin, LocalDateTime debut, LocalDateTime fin,
                                   int page, int size) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Salle> cq = cb.createQuery(Salle.class);
        Root<Salle> salle = cq.from(Salle.class);

        // 根据传入的参数动态构建过滤条件
        List<Predicate> predicates = buildPredicates(cb, cq, salle, nom, capaciteMin, debut, fin);

        // 按名称排序，保证分页结果顺序稳定
        cq.select(salle)
          .where(cb.and(predicates.toArray(new Predicate[0])))
          .orderBy(cb.asc(salle.get("nom")));

        TypedQuery<Salle> query = entityManager.createQuery(cq);
        // 分页：第一条记录的偏移量 = (页码 - 1) * 每页大小
        query.setFirstResult((page - 1) * size);
        // 每页最多返回 size 条记录
        query.setMaxResults(size);

        return query.getResultList();
    }

    /**
     * 统计查询：必须使用与搜索查询完全相同的条件，否则总页数会不一致。
     */
    @Override
    public long countRooms(String nom, Integer capaciteMin, LocalDateTime debut, LocalDateTime fin) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Salle> countRoot = countQuery.from(Salle.class);

        // 复用同一个条件构建方法，确保与搜索查询一致
        List<Predicate> predicates = buildPredicates(cb, countQuery, countRoot, nom, capaciteMin, debut, fin);

        countQuery.select(cb.count(countRoot))
                  .where(cb.and(predicates.toArray(new Predicate[0])));

        return entityManager.createQuery(countQuery).getSingleResult();
    }

    /**
     * 构建动态过滤条件列表。只有非空的参数才会生成对应的条件。
     *
     * @param query 用于创建子查询的父查询（搜索查询或统计查询）
     * @param salle 查询的根实体
     */
    private List<Predicate> buildPredicates(CriteriaBuilder cb, AbstractQuery<?> query, Root<Salle> salle,
                                            String nom, Integer capaciteMin,
                                            LocalDateTime debut, LocalDateTime fin) {
        List<Predicate> predicates = new ArrayList<>();

        // 条件一：名称模糊匹配（忽略大小写）
        if (nom != null && !nom.trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(salle.get("nom")), "%" + nom.trim().toLowerCase() + "%"));
        }

        // 条件二：最小容量
        if (capaciteMin != null) {
            predicates.add(cb.ge(salle.get("capacite"), capaciteMin));
        }

        // 条件三：可用性 —— 排除在该时间段内已被预订的会议室
        if (debut != null && fin != null) {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<Reservation> reservation = subquery.from(Reservation.class);
            subquery.select(reservation.get("salle").get("id"));

            // 时间段重叠条件：预订开始 < 查询结束 且 预订结束 > 查询开始
            Predicate chevauchement = cb.and(
                    cb.lessThan(reservation.<LocalDateTime>get("debut"), fin),
                    cb.greaterThan(reservation.<LocalDateTime>get("fin"), debut)
            );
            subquery.where(chevauchement);

            // 会议室 ID 不在"已被占用"的子查询结果中
            predicates.add(cb.not(salle.get("id").in(subquery)));
        }

        return predicates;
    }
}
