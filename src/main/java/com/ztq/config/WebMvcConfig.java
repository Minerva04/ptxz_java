package com.ztq.config;

import com.ztq.interceptor.LoginCheckInterceptor;
import com.ztq.comment.JacksonObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import java.util.List;

@Configuration
public class WebMvcConfig extends WebMvcConfigurationSupport {


    @Autowired
    LoginCheckInterceptor loginCheckInterceptor;

    @Override
    protected void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginCheckInterceptor).addPathPatterns("/**");
    }

    //序列化配置
    @Override
    protected void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        //创建消息转换器对象
        MappingJackson2HttpMessageConverter httpMessageConverter=new MappingJackson2HttpMessageConverter();
        //设置对象转换器
        httpMessageConverter.setObjectMapper(new JacksonObjectMapper());
        //将消息转换器追加到mvc框架消息转换器集合中
        converters.add(0,httpMessageConverter);
        super.extendMessageConverters(converters);
    }


}
