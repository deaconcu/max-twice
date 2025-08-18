package com.prosper.learn.front.web;

import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.api.client.UserClient;
import com.prosper.learn.dto.Response;
import com.prosper.learn.dto.UserDTO;
import com.prosper.learn.dto.UserDTOV2;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@Slf4j
public class UserController {

    private UserClient userClient;

    public UserController(UserClient userClient) {
        this.userClient = userClient;
    }

    @GetMapping("/login")
    public String Getlogin(HttpServletRequest request) {
        if (StpUtil.isLogin()) return "redirect:/course/list";
        return "login";
    }

    @PostMapping("/login")
    @ResponseBody
    public Response<Object> Postlogin(String email, String password, Model model) {
        Response<UserDTOV2> response = userClient.login(email, password);
        if (response.getCode() == Response.success.getCode()) {
            // 登录成功，生成 Token 并写入 Cookie
            StpUtil.login(response.getData().getId());
            //return "redirect:/targetPage";
            return Response.success;
        }
        return Response.failed;
    }

    @GetMapping("logout")
    public String logout() {
        StpUtil.logout();
        return "redirect:/login";
    }

    @PostMapping("/register")
    @ResponseBody
    public Response<Object> register(String userName, String email, String password, Model model) {
        userClient.register(userName, email, password);
        return Response.success;
    }

    @PostMapping("/validate")
    @ResponseBody
    public Response<Object> validate(String email, String code, Model model) {
        Response<UserDTO> response = userClient.validateMail(email, code);
        StpUtil.login(response.getData().getId());
        return Response.success;
    }
}
