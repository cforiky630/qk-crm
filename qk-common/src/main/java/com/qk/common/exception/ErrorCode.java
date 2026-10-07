package com.qk.common.exception;

/**
 * 业务错误码
 * <p>
 * 一条业务规则一条错误码，枚举常量名本身就是稳定、可对外使用的 code，展示文案集中在这里维护：
 * 文案只有一处出处，调用方（前端、测试、日志）也可以按 code 而不是中文来判断失败类型。
 * <p>
 * <b>决定：code 不进入响应体。</b>前端只按 Result 的 0/1 判断成败、直接展示 msg，
 * 不按失败原因分支，所以对外契约保持 {@code {code, msg}} 不变，这里的 code 只用于结构化日志
 * 与文案集中。将来前端真需要按类型分支时再暴露，在此之前不必为它冻结对外命名。
 * <p>
 * 需要留意的是覆盖范围：目前只有「业务异常」与「唯一索引冲突」两条路径带 code，
 * 参数校验失败、坏 JSON、路径参数类型不匹配、上传超限、兜底 500 都还没有 ——
 * 真要对外暴露，得先把这几条补齐，否则会是一个时有时无的字段。
 * <p>
 * 文案里可以用 {@code %s} / {@code %d} 占位，由 {@link #format(Object...)} 填充。
 */
public enum ErrorCode {

    /** 通用：新增商机/客户时手机号为空：{@code 手机号不能为空} */
    PHONE_REQUIRED("手机号不能为空"),

    // ---------- 部门 ----------
    /** 部门：{@code 部门不存在} */
    DEPT_NOT_FOUND("部门不存在"),
    /** 部门：{@code 部门ID不能为空} */
    DEPT_ID_REQUIRED("部门ID不能为空"),
    /** 部门删除守卫：{@code 部门处于启用状态，请先停用后再删除} */
    DEPT_ENABLED_CANNOT_DELETE("部门处于启用状态，请先停用后再删除"),
    /** 部门删除守卫，参数为引用用户数：{@code 该部门下还有 %d 名用户，无法删除} */
    DEPT_HAS_USERS("该部门下还有 %d 名用户，无法删除"),

    // ---------- 角色 ----------
    /** 角色：{@code 角色不存在} */
    ROLE_NOT_FOUND("角色不存在"),
    /** 角色：{@code 角色ID不能为空} */
    ROLE_ID_REQUIRED("角色ID不能为空"),
    /** 角色删除守卫，参数为引用用户数：{@code 该角色下还有 %d 名用户，无法删除} */
    ROLE_HAS_USERS("该角色下还有 %d 名用户，无法删除"),

    // ---------- 用户 ----------
    /** 用户：{@code 用户不存在} */
    USER_NOT_FOUND("用户不存在"),
    /** 批量删除用户时未传 id：{@code 待删除的用户ID不能为空} */
    USER_IDS_REQUIRED("待删除的用户ID不能为空"),
    /** 新增用户必填字段：{@code 用户名、姓名、手机号、邮箱均不能为空} */
    USER_FIELDS_REQUIRED("用户名、姓名、手机号、邮箱均不能为空"),
    /** 防止把自己锁在系统外：{@code 不能停用当前登录用户} */
    USER_CANNOT_DISABLE_SELF("不能停用当前登录用户"),
    /** 防止把自己锁在系统外：{@code 不能删除当前登录用户} */
    USER_CANNOT_DELETE_SELF("不能删除当前登录用户"),
    /** 批量删除时列出不存在的 id，参数为 id 列表：{@code 用户不存在: %s} */
    USER_IDS_NOT_FOUND("用户不存在: %s"),
    /** 分配线索/商机时目标用户不可用：{@code 归属人不存在或已停用，请重新选择} */
    USER_NOT_ASSIGNABLE("归属人不存在或已停用，请重新选择"),
    /** 用户删除守卫，参数依次为用户ID、线索数、商机数、跟进记录数：{@code 用户 %d 仍被业务数据引用（线索 %d 条、商机 %d 条、跟进记录 %d 条），无法删除；如不再使用请改为停用} */
    USER_STILL_REFERENCED("用户 %d 仍被业务数据引用（线索 %d 条、商机 %d 条、跟进记录 %d 条），无法删除；如不再使用请改为停用"),

    // ---------- 线索 ----------
    /** 线索：{@code 线索不存在} */
    CLUE_NOT_FOUND("线索不存在"),
    /** 线索：{@code 线索ID不能为空} */
    CLUE_ID_REQUIRED("线索ID不能为空"),
    /** 新增线索必填字段：{@code 手机号与线索来源不能为空} */
    CLUE_PHONE_CHANNEL_REQUIRED("手机号与线索来源不能为空"),
    /** 线索状态守卫，参数为被拒绝的动作名：{@code 该线索当前状态不允许%s} */
    CLUE_STATUS_NOT_ALLOWED("该线索当前状态不允许%s"),

    // ---------- 商机 ----------
    /** 商机：{@code 商机不存在} */
    BUSINESS_NOT_FOUND("商机不存在"),
    /** 商机：{@code 商机ID不能为空} */
    BUSINESS_ID_REQUIRED("商机ID不能为空"),
    /** 商机状态守卫，参数为被拒绝的动作名：{@code 该商机当前状态不允许%s} */
    BUSINESS_STATUS_NOT_ALLOWED("该商机当前状态不允许%s"),
    /** 商机跟进记录「沟通重点」超出列宽，参数为列宽上限：{@code 沟通重点最多 %d 个字符，请精简后重试} */
    BUSINESS_KEY_ITEMS_TOO_LONG("沟通重点最多 %d 个字符，请精简后重试"),

    // ---------- 客户 ----------
    /** 客户：{@code 客户不存在} */
    CUSTOMER_NOT_FOUND("客户不存在"),

    // ---------- 课程 ----------
    /** 课程：{@code 课程不存在} */
    COURSE_NOT_FOUND("课程不存在"),
    /** 课程：{@code 课程ID不能为空} */
    COURSE_ID_REQUIRED("课程ID不能为空"),
    /** 新增课程必填字段：{@code 课程名称、学科、价格、适用人群均不能为空} */
    COURSE_FIELDS_REQUIRED("课程名称、学科、价格、适用人群均不能为空"),
    /** 修改课程时的名称校验：{@code 课程名称不能为空} */
    COURSE_NAME_REQUIRED("课程名称不能为空"),
    /** 课程价格取值范围：{@code 价格不能为负数} */
    COURSE_PRICE_NEGATIVE("价格不能为负数"),
    /** 课程学科取值范围，参数为下限、上限：{@code 学科取值必须在 %d~%d 之间} */
    COURSE_SUBJECT_RANGE("学科取值必须在 %d~%d 之间"),
    /** 课程适用人群取值范围，参数为下限、上限：{@code 适用人群取值必须在 %d~%d 之间} */
    COURSE_TARGET_RANGE("适用人群取值必须在 %d~%d 之间"),
    /** 课程删除守卫，参数为引用来源描述：{@code 该课程已被 %s引用，无法删除} */
    COURSE_STILL_REFERENCED("该课程已被 %s引用，无法删除"),

    // ---------- 活动 ----------
    /** 活动：{@code 活动不存在} */
    ACTIVITY_NOT_FOUND("活动不存在"),
    /** 活动：{@code 活动ID不能为空} */
    ACTIVITY_ID_REQUIRED("活动ID不能为空"),
    /** 新增活动必填字段：{@code 活动名称、渠道、类型、开始与结束时间均不能为空} */
    ACTIVITY_FIELDS_REQUIRED("活动名称、渠道、类型、开始与结束时间均不能为空"),
    /** 活动状态查询条件取值：{@code 活动状态取值为 1（未开始）、2（进行中）、3（已结束）} */
    ACTIVITY_STATUS_INVALID("活动状态取值为 1（未开始）、2（进行中）、3（已结束）"),
    /** 活动删除守卫，参数为引用线索数：{@code 该活动已关联 %d 条线索，无法删除} */
    ACTIVITY_HAS_CLUES("该活动已关联 %d 条线索，无法删除"),

    // ---------- 上传 ----------
    /** 上传时未选文件或文件为空：{@code 请选择要上传的图片} */
    UPLOAD_IMAGE_REQUIRED("请选择要上传的图片"),
    /** 上传扩展名校验：{@code 只支持 jpg、jpeg、png、gif、bmp、webp 格式的图片} */
    UPLOAD_IMAGE_TYPE_UNSUPPORTED("只支持 jpg、jpeg、png、gif、bmp、webp 格式的图片"),
    /** 上传内容与扩展名不符（文件头不是图片）：{@code 图片内容与文件类型不匹配，请上传真实图片} */
    UPLOAD_IMAGE_CONTENT_INVALID("图片内容与文件类型不匹配，请上传真实图片"),

    // ---------- 登录 ----------
    /** 登录失败：账号不存在、密码错误、账号已停用共用同一句提示，避免暴露账号是否存在 */
    LOGIN_FAILED("用户名或密码错误"),

    // ---------- 请求与系统（框架层）----------
    /** 请求体不是合法 JSON：{@code 请求参数格式不正确} */
    PARAM_FORMAT_INVALID("请求参数格式不正确"),
    /** 路径参数类型不匹配：{@code 请求参数类型不正确} */
    PARAM_TYPE_INVALID("请求参数类型不正确"),
    /** DTO 校验失败的兜底提示（通常用注解上的 message）：{@code 请求参数校验未通过} */
    PARAM_VALIDATION_FAILED("请求参数校验未通过"),
    /** 上传文件超出大小限制：{@code 上传文件过大} */
    UPLOAD_SIZE_EXCEEDED("上传文件过大"),
    /** 请求了不存在的路径：{@code 请求的资源不存在} */
    RESOURCE_NOT_FOUND("请求的资源不存在"),
    /** 请求方法不被支持：{@code 请求方法不被支持} */
    METHOD_NOT_ALLOWED("请求方法不被支持"),
    /** 请求头 Content-Type 不被支持：{@code 请求的 Content-Type 不被支持} */
    CONTENT_TYPE_UNSUPPORTED("请求的 Content-Type 不被支持"),
    /** 缺少必填的请求参数，参数为参数名：{@code 缺少请求参数：%s} */
    PARAM_MISSING("缺少请求参数：%s"),
    /** 数据库完整性约束不满足（列超长、非空、类型不匹配等）：{@code 提交的数据不符合约束，请检查后重试} */
    DATA_INTEGRITY_VIOLATION("提交的数据不符合约束，请检查后重试"),
    /** 未被识别的系统异常（代码或依赖缺陷）：{@code 系统繁忙,请稍后重试} */
    SYSTEM_ERROR("系统繁忙,请稍后重试"),

    // ---------- 唯一约束冲突（表.唯一索引名 → 提示）----------
    /** 部门名称唯一索引冲突：{@code 部门名称已存在} */
    DEPT_NAME_EXISTS("部门名称已存在"),
    /** 角色标识唯一索引冲突：{@code 角色标识已存在} */
    ROLE_LABEL_EXISTS("角色标识已存在"),
    /** 用户名唯一索引冲突：{@code 用户名已存在} */
    USER_USERNAME_EXISTS("用户名已存在"),
    /** 用户手机号唯一索引冲突：{@code 手机号已存在} */
    USER_PHONE_EXISTS("手机号已存在"),
    /** 用户邮箱唯一索引冲突：{@code 邮箱已存在} */
    USER_EMAIL_EXISTS("邮箱已存在"),
    /** 线索手机号唯一索引冲突：{@code 该手机号已录入线索} */
    CLUE_PHONE_EXISTS("该手机号已录入线索"),
    /** 商机手机号唯一索引冲突：{@code 该手机号已录入商机} */
    BUSINESS_PHONE_EXISTS("该手机号已录入商机"),
    /** 客户手机号唯一索引冲突：{@code 该手机号已录入客户} */
    CUSTOMER_PHONE_EXISTS("该手机号已录入客户"),

    // ---------- 兜底 ----------
    /** 无法归类的失败：{@code 操作失败,请联系管理员} */
    UNKNOWN_FAILURE("操作失败,请联系管理员"),

    // ---------- OSS ----------
    /** OSS 对象名需要扩展名：{@code 文件名缺少扩展名，无法识别图片格式} */
    OSS_FILENAME_NO_EXTENSION("文件名缺少扩展名，无法识别图片格式"),
    /** 读取上传流失败，根因随异常一起带出：{@code 读取上传文件失败} */
    OSS_READ_FAILED("读取上传文件失败");

    /** 展示文案模板，可能含 {@code %s} / {@code %d} 占位 */
    private final String message;

    ErrorCode(String message) {
        this.message = message;
    }

    /**
     * 用实参填充文案模板
     *
     * @param args 模板占位对应的实参；没有占位时不传
     * @return 可以直接展示给用户的提示文案
     */
    public String format(Object... args) {
        if (args == null || args.length == 0) {
            return message;
        }
        return String.format(message, args);
    }

    /** @return 文案模板原文 */
    public String getMessage() {
        return message;
    }
}
