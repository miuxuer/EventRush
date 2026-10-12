package com.miuxuer.eventrush.controller;

import com.miuxuer.eventrush.dto.Result;
import com.miuxuer.eventrush.entity.EventCategory;
import com.miuxuer.eventrush.service.IEventCategoryService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 演出分类接口。
 *
 * @author miuxuer
 * @since 2026-10-12
 */
@RestController
@RequestMapping("/category")
public class EventCategoryController {

    @Resource
    private IEventCategoryService categoryService;

    /**
     * 查询全部演出分类，用于首页顶部的分类导航。
     *
     * @return 分类列表，按 sort 升序
     */
    @GetMapping("list")
    public Result queryCategoryList() {
        List<EventCategory> categoryList = categoryService.queryCategoryList();
        return Result.ok(categoryList);
    }
}
