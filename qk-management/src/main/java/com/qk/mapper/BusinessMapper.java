package com.qk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qk.entity.Business;
import com.qk.entity.dto.BusinessPoolDto;
import com.qk.entity.dto.BusinessQueryDto;
import com.qk.entity.vo.BusinessVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 商机数据访问接口
 * <p>
 * 列表与详情都需要 join 出归属人姓名、意向课程名称，SQL 见同包路径下的 BusinessMapper.xml。
 */
@Mapper
public interface BusinessMapper extends BaseMapper<Business> {

    /**
     * 商机列表（含归属人姓名），只查询跟进中的商机
     */
    IPage<BusinessVO> listBusinesses(Page<BusinessVO> page, @Param("businessQueryDto") BusinessQueryDto businessQueryDto);

    /**
     * 根据ID查询商机基本信息（不含跟进记录）
     */
    BusinessVO getBusinessById(@Param("id") Integer id);

    /**
     * 公海池列表（回收的商机）
     */
    IPage<BusinessVO> getPoolBusinesses(Page<BusinessVO> page, @Param("businessPoolDto") BusinessPoolDto businessPoolDto);
}
