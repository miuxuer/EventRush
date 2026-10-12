package com.miuxuer.eventrush.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.miuxuer.eventrush.entity.EventCategory;

import java.util.List;

/**
 * 演出分类服务。
 *
 * @author miuxuer
 * @since 2026-10-12
 */
public interface IEventCategoryService extends IService<EventCategory> {

    /**
     * 查询全部演出分类，按展示顺序升序。
     *
     * <p>分类数据变动极少但访问频繁，所以整份列表缓存成一个 key。</p>
     *
     * @return 分类列表
     */
    List<EventCategory> queryCategoryList();
}
