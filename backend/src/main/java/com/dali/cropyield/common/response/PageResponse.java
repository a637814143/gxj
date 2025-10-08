package com.dali.cropyield.common.response;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PageResponse<T> extends ApiResponse<List<T>> {

    private long total;
    private int page;
    private int size;

    private PageResponse(List<T> data, long total, int page, int size) {
        super(true, "OK", data);
        this.total = total;
        this.page = page;
        this.size = size;
    }

    public static <T> PageResponse<T> of(List<T> data, long total, int page, int size) {
        return new PageResponse<>(data, total, page, size);
    }
}
