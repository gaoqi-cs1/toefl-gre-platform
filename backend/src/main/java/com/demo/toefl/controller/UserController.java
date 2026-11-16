package com.demo.toefl.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.toefl.dto.LoginDTO;
import com.demo.toefl.entity.User;
import com.demo.toefl.service.UserService;
import com.demo.toefl.util.AuthHelper;
import com.demo.toefl.util.JwtUtil;
import com.demo.toefl.util.Result;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户接口控制器：登录、注册、当前用户信息、用户列表
 * @RestController = @Controller + @ResponseBody，返回json
 */
@RestController
@RequestMapping("/api/user") // 统一前缀 /api/user
public class UserController {
    @Resource
    private UserService userService;
    @Resource
    private JwtUtil jwtUtil;
    @Resource
    private AuthHelper authHelper;

    /**
     * 登录接口 POST /api/user/login
     * @RequestBody 接收前端提交的json
     */
    @PostMapping("/login")
    public Result<Map<String,Object>> login(@RequestBody LoginDTO dto){
        // 构造查询条件：根据用户名查询
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, dto.getUsername());
        User user = userService.getOne(wrapper, false);

        // 用户不存在
        if(user == null){
            return Result.fail("用户不存在");
        }
        // 密码比对（当前版本明文，调试用）
        if(!user.getPassword().equals(dto.getPassword())){
            return Result.fail("密码错误");
        }
        // 登录成功，生成token
        String token = jwtUtil.generateToken(user.getUsername());

        Map<String,Object> map = new HashMap<>();
        map.put("token",token);
        map.put("username",user.getUsername());
        map.put("userId",user.getId());
        return Result.success(map);
    }

    /**
     * 注册接口 POST /api/user/register
     */
    @PostMapping("/register")
    public Result<String> register(@RequestBody LoginDTO dto){
        // 判断用户名是否已经存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, dto.getUsername());
        long count = userService.count(wrapper);
        if(count > 0){
            return Result.fail("用户名已存在");
        }
        // 新建用户存入数据库
        User u = new User();
        u.setUsername(dto.getUsername());
        u.setPassword(dto.getPassword());
        userService.save(u);
        return Result.success("注册成功");
    }

    /**
     * 当前登录用户信息 GET /api/user/info
     * 前端 stores/user.js、api/user.js 里有封装但后端一直没有实现
     */
    @GetMapping("/info")
    public Result<User> info(HttpServletRequest request) {
        Long uid = authHelper.currentUserId(request);
        User user = userService.getById(uid);
        if (user == null) {
            return Result.fail("当前用户不存在，请重新登录");
        }
        return Result.success(user);
    }

    /**
     * 用户列表 GET /api/user/list
     * UserList.vue 用这个接口渲染表格
     */
    @GetMapping("/list")
    public Result<List<User>> list() {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(User::getId);
        return Result.success(userService.list(wrapper));
    }
}
