package com.demo.toefl.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.toefl.entity.User;
import com.demo.toefl.service.UserService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 当前登录用户解析工具
 *
 * 背景：前端页面里 userId 之前是写死 1 的，一旦登录别的账号，
 * 作答记录、错题本都会记到 1 号用户名下。
 * 这里统一改成"优先从 token 解析，解析不到再兜底 1"：
 *  - 被 JwtInterceptor 拦截的接口，拦截器已把用户名放进 request 的 loginUser 属性；
 *  - 被放行的接口（/api/answer/**、/api/wrong/add 等）不走拦截器，
 *    就直接读 Authorization 头自己解析一次。
 */
@Component
public class AuthHelper {

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private UserService userService;

    /**
     * 拿到当前登录用户的 id，拿不到时返回 1（单机练习模式兜底）
     */
    public Long currentUserId(HttpServletRequest request) {
        String username = resolveUsername(request);
        if (username != null) {
            User user = userService.getOne(
                    new LambdaQueryWrapper<User>().eq(User::getUsername, username), false);
            if (user != null && user.getId() != null) {
                return user.getId();
            }
        }
        return 1L;
    }

    /**
     * 从 request 中解析用户名，解析不出来返回 null
     */
    private String resolveUsername(HttpServletRequest request) {
        Object attr = request.getAttribute("loginUser");
        if (attr instanceof String && !((String) attr).isEmpty()) {
            return (String) attr;
        }
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            try {
                return jwtUtil.parseToken(auth.substring(7));
            } catch (Exception ignore) {
                return null;
            }
        }
        return null;
    }
}
