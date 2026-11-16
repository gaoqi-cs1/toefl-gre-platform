package com.demo.toefl.config;

import com.demo.toefl.interceptor.JwtInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Resource
    private JwtInterceptor jwtInterceptor;

    // 拦截器
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                // 放行的接口：登录注册、只读题库（篇章/题目/套题）、作答与错题本
                .excludePathPatterns(
                        "/api/user/login",
                        "/api/user/register",
                        "/api/section/**",
                        "/api/tpo-section/**",
                        "/api/question/**",
                        "/api/suite/**",
                        "/api/tposuite/**",
                        "/api/wrong/**",
                        "/api/answer/**",
                        // 模考全过程接口：开始/提交/成绩查询都放行，
                        // 用户id由 AuthHelper 从 token 里解析，拿不到再兜底
                        "/api/examRecord/**",
                        // 真题套题（独立数据）：目录与整卷提交都放行，
                        // 用户id同样由 AuthHelper 从 token 解析
                        "/api/fullSuite/**",
                        "/api/fullExam/**",
                        // GRE 模块（独立数据）：分块练习、套题目录、模考记录各一套接口，
                        // 与 TOEFL 的 /api/fullSuite、/api/fullExam 完全无关，互不调用
                        "/api/gre/practice/**",
                        "/api/greSuite/**",
                        "/api/greExam/**",
                        // GRE 错题本（独立表 gre_wrong，不并入托福 wrong_book）
                        "/api/greWrong/**",
                        "/api/essay/**",
                        "/api/file/**"
                );
    }

    // 全局跨域配置，解决CORS
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET","POST","PUT","DELETE","OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }

    /**
     * 口语录音静态资源映射
     * 录音保存在项目根目录 upload/audio/ 下，URL 是 /upload/audio/xxx.webm，
     * 不注册映射的话前端播放录音会 404
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 用绝对路径，避免工作目录不同（IDE 运行 / 打包运行）时映射不到文件
        String audioDir = new java.io.File("upload/audio/").getAbsolutePath().replace("\\", "/");
        registry.addResourceHandler("/upload/audio/**")
                .addResourceLocations("file:" + audioDir + "/");
    }
}
