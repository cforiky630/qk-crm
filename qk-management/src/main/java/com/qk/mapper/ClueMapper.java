package com.qk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qk.entity.Clue;
import com.qk.entity.dto.CluePoolDto;
import com.qk.entity.dto.ClueQueryDto;
import com.qk.entity.vo.ClueVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 线索数据访问接口
 * <p>
 * 列表与详情都需要 join 出归属人姓名、活动名称，SQL 见同包路径下的 ClueMapper.xml。
 */
@Mapper
public interface ClueMapper extends BaseMapper<Clue> {

    /**
     * 线索列表（含归属人姓名），只查询未关闭的线索
     */
    IPage<ClueVO> listClues(Page<ClueVO> page, @Param("clueQueryDto") ClueQueryDto clueQueryDto);

    /**
     * 根据ID查询线索基本信息（不含跟进记录）
     */
    ClueVO getClueById(@Param("id") Integer id);

    /**
     * 线索池列表（含活动名称）
     */
    IPage<ClueVO> getPoolClues(Page<ClueVO> page, @Param("cluePoolDto") CluePoolDto cluePoolDto);
}
