package com.example.util;

import java.util.List;

/**
 * 通用分页结果类：包含当前页的数据以及分页元数据。
 *
 * @param <T> 列表元素类型
 */
public class PaginationResult<T> {
    private final List<T> items;      // 当前页的数据
    private final int currentPage;    // 当前页码（从 1 开始）
    private final int pageSize;       // 每页大小
    private final int totalPages;     // 总页数
    private final long totalItems;    // 总记录数

    public PaginationResult(List<T> items, int currentPage, int pageSize, long totalItems) {
        this.items = items;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.totalItems = totalItems;
        // 总页数 = 总记录数 / 每页大小，向上取整
        this.totalPages = (int) Math.ceil((double) totalItems / pageSize);
    }

    // Getter 方法

    public List<T> getItems() { return items; }
    public int getCurrentPage() { return currentPage; }
    public int getPageSize() { return pageSize; }
    public int getTotalPages() { return totalPages; }
    public long getTotalItems() { return totalItems; }

    // 是否存在下一页
    public boolean hasNext() { return currentPage < totalPages; }

    // 是否存在上一页
    public boolean hasPrevious() { return currentPage > 1; }

    @Override
    public String toString() {
        return "Page " + currentPage + "/" + totalPages + " (" + totalItems + " éléments au total)";
    }
}
