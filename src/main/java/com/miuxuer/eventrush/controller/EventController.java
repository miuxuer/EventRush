package com.miuxuer.eventrush.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.miuxuer.eventrush.dto.Result;
import com.miuxuer.eventrush.entity.Event;
import com.miuxuer.eventrush.service.IEventService;
import com.miuxuer.eventrush.utils.SystemConstants;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 演出（赛事/演唱会/话剧等）接口。
 *
 * <p>这个模块的接口都是公开的，不进登录拦截，游客也能浏览。</p>
 *
 * @author miuxuer
 * @since 2026-10-12
 */
@RestController
@RequestMapping("/event")
public class EventController {

    @Resource
    public IEventService eventService;

    /**
     * 查询演出详情。
     *
     * @param id 演出id
     * @return 演出详情，走 Redis 缓存
     */
    @GetMapping("/{id}")
    public Result queryEventById(@PathVariable("id") Long id) {
        return eventService.queryById(id);
    }

    /**
     * 新增演出。
     *
     * @param event 演出数据
     * @return 新演出的 id
     */
    @PostMapping
    public Result saveEvent(@RequestBody Event event) {
        eventService.save(event);
        return Result.ok(event.getId());
    }

    /**
     * 修改演出信息。
     *
     * <p>更新数据库的同时会把该演出的缓存删掉，下次查询重新加载。</p>
     *
     * @param event 演出数据，id 必填
     * @return 无
     */
    @PutMapping
    public Result updateEvent(@RequestBody Event event) {
        return eventService.update(event);
    }

    /**
     * 按分类分页查询演出；带上经纬度时会按距离由近到远排序。
     *
     * @param categoryId 演出分类id
     * @param current    页码，默认第一页
     * @param x          经度，不传则按普通分页查询
     * @param y          纬度，不传则按普通分页查询
     * @return 演出列表，按距离查询时每条数据带 distance 字段
     */
    @GetMapping("/of/category")
    public Result queryEventByCategory(
            @RequestParam("categoryId") Integer categoryId,
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "x", required = false) Double x,
            @RequestParam(value = "y", required = false) Double y
    ) {
        return eventService.queryEventByCategory(categoryId, current, x, y);
    }

    /**
     * 按名称关键字模糊搜索演出。
     *
     * @param name    演出名称关键字，可以不传（相当于查全部）
     * @param current 页码，默认第一页
     * @return 演出列表
     */
    @GetMapping("/of/name")
    public Result queryEventByName(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "current", defaultValue = "1") Integer current
    ) {
        Page<Event> page = eventService.query()
                .like(StrUtil.isNotBlank(name), "name", name)
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        return Result.ok(page.getRecords());
    }
}
