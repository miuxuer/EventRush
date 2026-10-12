package com.miuxuer.eventrush.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.miuxuer.eventrush.dto.Result;
import com.miuxuer.eventrush.entity.Event;

/**
 * 演出服务。
 *
 * @author miuxuer
 * @since 2026-10-12
 */
public interface IEventService extends IService<Event> {

    /**
     * 查询演出详情，走 Redis 缓存。
     *
     * @param id 演出id
     * @return 演出详情
     */
    Result queryById(Long id);

    /**
     * 修改演出信息，同时删除对应的缓存。
     *
     * @param event 演出信息，id 必填
     * @return 无
     */
    Result update(Event event);

    /**
     * 按分类分页查询演出，可选按距离排序。
     *
     * @param categoryId 分类id
     * @param current    页码
     * @param x          经度，可为 null
     * @param y          纬度，可为 null
     * @return 演出列表
     */
    Result queryEventByCategory(Integer categoryId, Integer current, Double x, Double y);
}
