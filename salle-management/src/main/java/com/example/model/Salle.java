package com.example.model;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * 会议室实体：表示一个具有名称和容量的会议室。
 * 映射到数据库表 "salles"。
 */
@Entity
@Table(name = "salles")
public class Salle {

    // 主键，由数据库自增生成
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 会议室名称，不可为空且唯一
    @Column(nullable = false, unique = true)
    private String nom;

    // 会议室容量（可容纳人数）
    @Column(nullable = false)
    private int capacite;

    // 一对多关系：一个会议室可以有多个预订（延迟加载，级联所有操作）
    @OneToMany(mappedBy = "salle", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Reservation> reservations = new HashSet<>();

    // JPA 要求的无参构造函数
    public Salle() {
    }

    // 便捷构造函数
    public Salle(String nom, int capacite) {
        this.nom = nom;
        this.capacite = capacite;
    }

    /**
     * 辅助方法：添加预订并同步维护双向关系的两端。
     */
    public void addReservation(Reservation reservation) {
        reservations.add(reservation);
        reservation.setSalle(this);
    }

    // Getter 和 Setter 方法

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public int getCapacite() { return capacite; }
    public void setCapacite(int capacite) { this.capacite = capacite; }

    public Set<Reservation> getReservations() { return reservations; }
    public void setReservations(Set<Reservation> reservations) { this.reservations = reservations; }

    @Override
    public String toString() {
        return nom + " (Capacité: " + capacite + ")";
    }
}
