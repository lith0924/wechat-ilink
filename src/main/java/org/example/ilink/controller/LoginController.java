package org.example.ilink.controller;

import org.example.ilink.service.LoginService;
import org.example.ilink.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
@RequestMapping(value ="/api/auth")
public class LoginController {
    @Autowired
    private LoginService loginService;

    @GetMapping("/login/wechat")
    public Result loginWeChat(){
        String loginUrl = loginService.loginWeChat();
        return Result.success(loginUrl);
    }
    @GetMapping("/login/status")
    public Result loginStatus(){
        String status = loginService.loginStatus();
        return Result.success(status);
    }
}
