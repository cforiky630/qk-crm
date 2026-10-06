package com.qk.entity.dto;

import lombok.Data;
import jakarta.validation.constraints.Size;

/**
 * 标记伪线索的请求参数
 * 对应 PUT /clues/false/{id}
 */
@Data
public class MarkFalseClueDto {

    /** 伪线索原因，1:空号, 2:停机, 3:竞品, 4:无法联系, 5:其他 */
    private Integer reason;

    /** 备注信息，对应跟进记录的 record */
    @Size(max = 100, message = "备注长度不能超过 100 个字符")
    private String remark;
}
