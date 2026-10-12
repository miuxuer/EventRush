package com.miuxuer.eventrush.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 滚动分页（游标分页）结果，用于观演笔记 Feed 流这类基于 Redis ZSet 的滚动查询。
 * <p>
 * 与传统页码分页不同，它返回的是"下一次查询的起点"：minTime 是本次返回的最后一条记录的时间戳，
 * offset 是同一时间戳下需要跳过的条数，下次请求带上这两个值即可继续往下翻。
 * </p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
@Data
@Builder
public class ScrollResult {

    /**
     * 当前这一页的数据列表
     */
    private List<?> list;

    /**
     * 本次查询的最小时间戳，作为下一次滚动查询的游标起点
     */
    private Long minTime;

    /**
     * 下一次查询需要跳过的条数，用于处理同一时间戳下多条数据的情况
     */
    private Integer offset;
}
