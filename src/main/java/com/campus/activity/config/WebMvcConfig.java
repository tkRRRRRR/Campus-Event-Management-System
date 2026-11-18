package com.campus.activity.config;

import com.campus.activity.interceptor.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Web 层配置：跨域、登录拦截、上传文件静态映射。
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    @Value("${file.upload-path:./upload}")
    private String uploadPath;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/ping",
                        "/error"
                );
    }

    /**
     * 前端部署在 Vercel（不同域名）时必须放行跨域。
     * JWT 放在请求头里，不依赖 Cookie，所以 allowCredentials 保持 false 即可。
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization")
                .maxAge(3600);
    }

    /**
     * 上传的海报图片通过 /upload/** 访问。
     * 路径转成绝对路径，避免在不同工作目录启动时找不到文件。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        File dir = new File(uploadPath);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("无法创建上传目录: " + dir.getAbsolutePath());
        }
        String absolute = dir.getAbsolutePath().replace("\\", "/");
        registry.addResourceHandler("/upload/**")
                .addResourceLocations("file:" + absolute + "/");
    }
}
