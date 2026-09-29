package com.qk.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 公共字段自动填充
 * <p>
 * 插入时填充 createTime / updateTime，更新时只填充 updateTime。
 * 使用 setFieldValByName：先判断实体上是否存在该属性，存在才填充，避免对其他对象产生影响。
 */
@Slf4j
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        log.debug("开始插入填充...");
        // 不管前端是否传入，都以服务器时间为准，避免前端伪造时间
        LocalDateTime now = LocalDateTime.now();
        this.setFieldValByName("createTime", now, metaObject);
        this.setFieldValByName("updateTime", now, metaObject);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        log.debug("开始更新填充...");
        // 注意：更新时绝不能回填 createTime，否则创建时间会被改掉
        this.setFieldValByName("updateTime", LocalDateTime.now(), metaObject);
    }
}
