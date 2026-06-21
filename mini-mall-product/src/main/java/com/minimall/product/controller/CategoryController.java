package com.minimall.product.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.minimall.common.core.domain.Result;
import com.minimall.product.entity.Category;
import com.minimall.product.service.ICategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品分类 Controller (从单体搬过来, 顺手清理了几个 bug)
 *
 * 端点:
 *   GET    /category/list  → 列分类
 *   POST   /category       → 新增分类
 *   GET    /category/{id}  → 查详情
 *   PUT    /category/{id}  → 改分类
 *   DELETE /category/{id}  → 逻辑删除
 *
 * 单体里的 bug 修复:
 *   ① 原 addCategory 方法签名写成 `public <category>Result<Category>`
 *      → 把 `category` 当成了泛型类型变量, 实际想说返 Result<Category>
 *      → 已清理
 *   ② 原 addCategory 返 Result.success() 没传 data
 *      → 改为返刚保存的 category (此时 id 已自增回填)
 *   ③ 路径: 单体是 /api/categories 复数风格,
 *      微服务统一改成 /category 单数, 跟其他模块一致
 */
@RestController
@RequestMapping("/category")
public class CategoryController {

    @Autowired
    private ICategoryService categoryService;

    /**
     * ① 分类列表
     *
     * 业务排序: status=1 启用的排前面, 再按 sort 升序 (越小越靠前)
     *
     * 注: 这里【没有】用 UserContext, 因为分类是【全平台共享】数据, 不绑用户
     */
    @GetMapping("/list")
    public Result<List<Category>> list() {
        QueryWrapper<Category> w = new QueryWrapper<>();
        w.orderByDesc("status")        // 启用的排前面 (1>0)
         .orderByAsc("sort");          // sort 越小越靠前
        // service.list() 是 IService 给的, 等价 selectList(w)
        return Result.success(categoryService.list(w));
    }

    /**
     * ② 详情
     */
    @GetMapping("/{id}")
    public Result<Category> getById(@PathVariable Long id) {
        Category c = categoryService.getById(id);
        return Result.success(c);  // 找不到也直接返 null, 前端 if (data) 处理
    }

    /**
     * ③ 新增
     *
     * 关键改动:
     *   - 单体没 @RequestBody, 走的 form 绑定; 微服务前端发 JSON, 必须加 @RequestBody
     *   - 返刚保存的 category, 让前端拿到自增 id
     */
    @PostMapping
    public Result<Category> create(@RequestBody Category category) {
        // save 是 IService 方法, 内部走 baseMapper.insert
        // 数据库 AUTO_INCREMENT 生成 id 后, MP 自动回填到 category.id
        categoryService.save(category);
        return Result.success(category);
    }

    /**
     * ④ 修改
     *
     * 路径里的 id 是【权威 id】, 防止前端 body 里塞别的 id 绕过
     */
    @PutMapping("/{id}")
    public Result<Category> update(@PathVariable Long id,
                                   @RequestBody Category category) {
        category.setId(id);                        // 强制覆盖, 防注入
        categoryService.updateById(category);      // 自动忽略 null 字段
        return Result.success(category);
    }

    /**
     * ⑤ 删除 (逻辑删除)
     *
     * 因为 Category.isDeleted 加了 @TableLogic,
     * 这里 removeById 实际执行: UPDATE category SET is_deleted=1 WHERE id=?
     * 不会真的 DELETE
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.removeById(id);
        return Result.success();
    }
}
