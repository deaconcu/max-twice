package com.prosper.learn.user;

import com.prosper.learn.common.JwtUtil;
import com.prosper.learn.common.ResponseResult;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping()
    @PassLogin
    public ResponseResult register(@RequestBody User user) {
        try {
            userMapper.insert(user);
            log.info("user created, id: " + user.getId());
            return ResponseResult.ok();
        } catch (DuplicateKeyException e) {
            return ResponseResult.invalid_request();
        }
    }

    @PostMapping(path = "login")
    @PassLogin
    public ResponseResult login(@RequestBody User user, HttpServletResponse response) {
        if (isUserExist(user)) {
            String token = jwtUtil.createToken("1");
            log.info("登录成功, token: " + token);
            response.setHeader(jwtUtil.getHeader(), "Bearer " + token);
            return ResponseResult.ok();
        } else {
            log.info("登录失败");
            return ResponseResult.invalid_request();
        }
    }

    @PostMapping(path = "logout")
    public String logout() {
        // todo
        return "ok";
    }

    private boolean isUserExist(User userForChk) {
        if (userForChk == null || !StringUtils.hasText(userForChk.getPasswordMD5())) return false;

        User user = userMapper.getUserByEmail(userForChk.getEmail());
        String passwordMD5 = DigestUtils.md5DigestAsHex(userForChk.getPassword().getBytes(StandardCharsets.UTF_8));
        if (user != null && passwordMD5.equals(user.getPasswordMD5())) return true;
        return false;
    }
}
















