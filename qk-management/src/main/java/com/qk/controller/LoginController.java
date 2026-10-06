package com.qk.controller;

import com.qk.common.Result;
import com.qk.entity.dto.LoginDto;
import com.qk.service.UserService;
import com.qk.entity.vo.LoginResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class LoginController {

    private final UserService userService;

    @Autowired
    public LoginController(UserService userService) {
        this.userService = userService;
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
        LoginResultVO loginResult = userService.login(loginDto.getUsername(), loginDto.getPassword());
        return loginResult != null ? Result.success(loginResult) : Result.error("用户名或密码错误");
    }
}
