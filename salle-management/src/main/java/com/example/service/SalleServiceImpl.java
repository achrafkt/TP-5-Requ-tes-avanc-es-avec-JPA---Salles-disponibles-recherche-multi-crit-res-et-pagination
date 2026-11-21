package com.example.service;

import com.example.model.Salle;
import com.example.repository.SalleRepository;
import com.example.util.PaginationResult;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.List;

/**
 * SalleService 的实现类：调用 Repository 并应用业务规则（参数校验等）。
 * 在 Java EE 中 Repository 由 CDI 自动注入；在 Java SE 中通过构造函数手动注入。
 */
@Stateless
public class SalleServiceImpl implements SalleService {

    // 由容器（CDI）自动注入
    @Inject
    private SalleRepository salleRepository;

    // EJB/CDI 要求的无参构造函数
    public SalleServiceImpl() {
    }

    // Java SE 环境下手动注入 Repository
    public SalleServiceImpl(SalleRepository salleRepository) {
        this.salleRepository = salleRepository;
    }

    public void setSalleRepository(SalleRepository salleRepository) {
        this.salleRepository = salleRepository;
    }

    @Override
    public List<Salle> getAvailableRooms(LocalDateTime debut, LocalDateTime fin) {
        // 业务规则：时间段必须完整且有效
        if (debut == null || fin == null) {
            throw new IllegalArgumentException("Les dates de début et de fin sont obligatoires");
        }
        validerCreneau(debut, fin);
        return salleRepository.findAvailableRooms(debut, fin);
    }

    @Override
    public List<Salle> searchRooms(String nom, Integer capaciteMin, LocalDateTime debut, LocalDateTime fin,
                                   int page, int size) {
        validerCreneau(debut, fin);
        validerPagination(page, size);
        return salleRepository.searchRooms(nom, capaciteMin, debut, fin, page, size);
    }

    @Override
    public long countRooms(String nom, Integer capaciteMin, LocalDateTime debut, LocalDateTime fin) {
        validerCreneau(debut, fin);
        return salleRepository.countRooms(nom, capaciteMin, debut, fin);
    }

    @Override
    public PaginationResult<Salle> searchRoomsPaginated(String nom, Integer capaciteMin,
                                                        LocalDateTime debut, LocalDateTime fin,
                                                        int page, int size) {
        // 先统计总数，再查询当前页数据，最后组装分页结果
        long total = countRooms(nom, capaciteMin, debut, fin);
        List<Salle> items = searchRooms(nom, capaciteMin, debut, fin, page, size);
        return new PaginationResult<>(items, page, size, total);
    }

    // 校验时间段：若提供了两个日期，开始时间必须早于结束时间
    private void validerCreneau(LocalDateTime debut, LocalDateTime fin) {
        if (debut != null && fin != null && !debut.isBefore(fin)) {
            throw new IllegalArgumentException("La date de début doit être antérieure à la date de fin");
        }
    }

    // 校验分页参数：页码从 1 开始，每页大小必须为正数
    private void validerPagination(int page, int size) {
        if (page < 1) {
            throw new IllegalArgumentException("Le numéro de page doit être >= 1");
        }
        if (size < 1) {
            throw new IllegalArgumentException("La taille de page doit être >= 1");
        }
    }
}
