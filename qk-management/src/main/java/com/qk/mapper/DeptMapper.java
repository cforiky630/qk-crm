package com.qk.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qk.entity.po.Dept;
import com.qk.entity.enums.EnableStatus;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 部门数据访问接口
 * <p>
 * 单表查询统一写在本接口的 default 方法里，Service 只做业务判断，
 * 不感知 MyBatis-Plus 的查询 DSL。
 */
@Mapper
public interface DeptMapper extends BaseMapper<Dept> {

    /**
     * 部门分页查询：name 模糊匹配、status 等值匹配
     * <p>
     * 排序与页面原型一致：按最后修改时间倒序，刚改过的部门排在最前面。
     * 末尾固定补一个 id，避免同一秒内多条记录的相对顺序在翻页时漂移
     * （排序键不唯一会导致记录重复或丢失）。
     */
    default IPage<Dept> pageDepts(Page<Dept> page, String name, Integer status) {
        LambdaQueryWrapper<Dept> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(name), Dept::getName, name)
                .eq(status != null, Dept::getStatus, status)
                .orderByDesc(Dept::getUpdateTime)
                .orderByDesc(Dept::getId);
        return selectPage(page, wrapper);
    }

    /**
     * 全部正常状态的部门，用于下拉框
     */
    default List<Dept> listEnabled() {
        LambdaQueryWrapper<Dept> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dept::getStatus, EnableStatus.ENABLED.getCode()).orderByAsc(Dept::getId);
        return selectList(wrapper);
    }
}
