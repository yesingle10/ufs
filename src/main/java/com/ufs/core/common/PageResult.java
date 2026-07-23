package com.ufs.core.common;

import java.util.List;

/**
 * 分页结果，对齐契约 §4.2 的 offset / cursor 双分页模式。
 * offset 模式用 {@link #total}；cursor 模式用 {@link #nextCursor} + {@link #hasMore}。
 */
public final class PageResult<T> {

    private final List<T> list;
    private final long total;
    private final int pageSize;
    private final String nextCursor;
    private final boolean hasMore;

    private PageResult(Builder<T> b) {
        this.list = b.list;
        this.total = b.total;
        this.pageSize = b.pageSize;
        this.nextCursor = b.nextCursor;
        this.hasMore = b.hasMore;
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static <T> PageResult<T> of(List<T> list, long total, int pageSize) {
        return new Builder<T>().list(list).total(total).pageSize(pageSize).build();
    }

    public static <T> PageResult<T> of(List<T> list, String nextCursor, boolean hasMore, int pageSize) {
        return new Builder<T>().list(list).nextCursor(nextCursor).hasMore(hasMore).pageSize(pageSize).build();
    }

    public List<T> getList() {
        return list;
    }

    public long getTotal() {
        return total;
    }

    public int getPageSize() {
        return pageSize;
    }

    public String getNextCursor() {
        return nextCursor;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public static final class Builder<T> {
        private List<T> list = List.of();
        private long total = 0;
        private int pageSize = 20;
        private String nextCursor;
        private boolean hasMore = false;

        public Builder<T> list(List<T> list) {
            this.list = list;
            return this;
        }

        public Builder<T> total(long total) {
            this.total = total;
            return this;
        }

        public Builder<T> pageSize(int pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        public Builder<T> nextCursor(String nextCursor) {
            this.nextCursor = nextCursor;
            return this;
        }

        public Builder<T> hasMore(boolean hasMore) {
            this.hasMore = hasMore;
            return this;
        }

        public PageResult<T> build() {
            return new PageResult<>(this);
        }
    }
}
