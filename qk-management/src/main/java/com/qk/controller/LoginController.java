package com.qk.controller;

import com.qk.Result;
import com.qk.User;
import com.qk.service.UserService;
import com.qk.vo.LoginResultVo;
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
     * @param user 账号和密码
     * @return 登录结果
     */
    @PostMapping("/login")
    public Result login(@RequestBody User user) {
        // 只打印用户名，禁止把整个 User 对象（含密码）写进日志
        log.info("用户登录请求: {}", user.getUsername());
        LoginResultVo loginResult = userService.login(user.getUsername(), user.getPassword());
        return loginResult != null ? Result.success(loginResult) : Result.error("用户名或密码错误");
    }
}
