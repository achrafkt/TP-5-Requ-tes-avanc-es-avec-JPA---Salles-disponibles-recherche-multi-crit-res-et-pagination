package com.example.repository;

import com.example.model.Salle;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会议室数据访问接口（Repository）：封装所有与会议室相关的查询。
 */
public interface SalleRepository {

    /**
     * 使用 JPQL 查询在给定时间段内没有被预订的会议室。
     */
    List<Salle> findAvailableRooms(LocalDateTime debut, LocalDateTime fin);

    /**
     * 使用 Criteria API 进行多条件动态搜索，并分页返回结果。
     * 所有条件均可为 null（表示不按该条件过滤）；page 从 1 开始。
     */
    List<Salle> searchRooms(String nom, Integer capaciteMin, LocalDateTime debut, LocalDateTime fin, int page, int size);

    /**
     * 统计满足相同搜索条件的会议室总数（用于计算总页数）。
     */
    long countRooms(String nom, Integer capaciteMin, LocalDateTime debut, LocalDateTime fin);
}
