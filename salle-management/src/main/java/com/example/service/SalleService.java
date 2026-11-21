package com.example.service;

import com.example.model.Salle;
import com.example.util.PaginationResult;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会议室业务服务接口：在数据访问层之上封装业务规则。
 */
public interface SalleService {

    // 获取指定时间段内可用的会议室
    List<Salle> getAvailableRooms(LocalDateTime debut, LocalDateTime fin);

    // 多条件搜索（分页）
    List<Salle> searchRooms(String nom, Integer capaciteMin, LocalDateTime debut, LocalDateTime fin, int page, int size);

    // 统计满足条件的会议室数量
    long countRooms(String nom, Integer capaciteMin, LocalDateTime debut, LocalDateTime fin);

    // 多条件搜索，返回包含分页元数据（总页数、总条数等）的完整结果
    PaginationResult<Salle> searchRoomsPaginated(String nom, Integer capaciteMin,
                                                 LocalDateTime debut, LocalDateTime fin, int page, int size);
}
