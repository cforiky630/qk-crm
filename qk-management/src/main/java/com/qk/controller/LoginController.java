package com.qk.controller;

import com.qk.common.Result;
import com.qk.common.exception.BusinessException;
import com.qk.common.exception.ErrorCode;
import com.qk.entity.dto.LoginDto;
import com.qk.service.AuthService;
import com.qk.entity.vo.LoginResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class LoginController {

    private final AuthService authService;

    @Autowired
    public LoginController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 用户登录
     *
     * @param loginDto 账号和密码
     * @return 登录结果
     */
    @PostMapping("/login")
    public Result<LoginResultVO> login(@RequestBody LoginDto loginDto) {
        // 只打印用户名，禁止把整个入参对象（含密码）写进日志
        log.info("用户登录请求: {}", loginDto.getUsername());
        LoginResultVO loginResult = authService.login(loginDto.getUsername(), loginDto.getPassword());
        if (loginResult == null) {
            // 文案集中在 ErrorCode 里维护，对外仍是 code = 0 + 「用户名或密码错误」
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        return Result.success(loginResult);
    }
}
