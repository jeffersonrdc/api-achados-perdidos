package br.com.achadosperdidos.pagination;

public final class PaginationParams {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private PaginationParams() {}

    public static int resolvePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    public static int resolveLimit(Integer limit) {
        if (limit == null || limit < 1) return DEFAULT_LIMIT;
        return Math.min(limit, MAX_LIMIT);
    }
}
