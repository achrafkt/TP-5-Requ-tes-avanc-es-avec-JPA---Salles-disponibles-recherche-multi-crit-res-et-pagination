package com.example.model;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 预订实体：表示某个会议室在某个时间段内被预订。
 * 映射到数据库表 "reservations"。
 */
@Entity
@Table(name = "reservations")
public class Reservation {

    // 主键，由数据库自增生成
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 多对一关系：多个预订属于同一个会议室（外键列 salle_id）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salle_id", nullable = false)
    private Salle salle;

    // 预订开始时间
    @Column(nullable = false)
    private LocalDateTime debut;

    // 预订结束时间
    @Column(nullable = false)
    private LocalDateTime fin;

    // JPA 要求的无参构造函数
    public Reservation() {
    }

    // 便捷构造函数
    public Reservation(LocalDateTime debut, LocalDateTime fin) {
        this.debut = debut;
        this.fin = fin;
    }

    // Getter 和 Setter 方法

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Salle getSalle() { return salle; }
    public void setSalle(Salle salle) { this.salle = salle; }

    public LocalDateTime getDebut() { return debut; }
    public void setDebut(LocalDateTime debut) { this.debut = debut; }

    public LocalDateTime getFin() { return fin; }
    public void setFin(LocalDateTime fin) { this.fin = fin; }
}
