package com.qk.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.qk.entity.dto.PageQuery;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.qk.mapper")
public class MybatisPlusConfig {

    /**
     * 添加分页插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        // 限制单页最大条数：避免前端传 pageSize=100000 之类把数据库和内存拖垮。
        // 上限取自 PageQuery.MAX_PAGE_SIZE，保证「DTO 校验」与「插件兜底」是同一个口径，
        // 不会出现插件允许 200、参数校验允许 1000 这种两处不一致。
        pagination.setMaxLimit((long) PageQuery.MAX_PAGE_SIZE);
        interceptor.addInnerInterceptor(pagination); // 如果配置多个插件, 切记分页最后添加
        // 如果有多数据源可以不配具体类型, 否则都建议配上具体的 DbType
        return interceptor;
    }

}
