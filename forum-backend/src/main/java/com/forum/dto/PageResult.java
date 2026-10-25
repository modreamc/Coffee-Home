package com.forum.dto;
import lombok.Data;
import java.util.List;
@Data
public class PageResult<T> {
    private List<T> list;
    private long total;
    private int page;
    private int size;
    public static <T> PageResult<T> of(List<T> list, long total, int page, int size) {
        PageResult<T> result = new PageResult<>();
        result.list = list;
        result.total = total;
        result.page = page;
        result.size = size;
        return result;
    }
}
