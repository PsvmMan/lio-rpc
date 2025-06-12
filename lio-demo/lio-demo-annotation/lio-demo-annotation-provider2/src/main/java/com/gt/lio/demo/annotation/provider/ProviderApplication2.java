package com.gt.lio.demo.annotation.provider;

import com.gt.lio.config.spring.annotation.EnableLio;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

public class ProviderApplication2 {

    public static void main(String[] args) throws Exception{
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ProviderConfiguration.class);
        context.start();
        System.out.println("provider started");
        System.in.read();
    }

    @Configuration
    @EnableLio(scanBasePackages = {"com.gt.lio.demo.annotation.provider"})
    @PropertySource("classpath:/spring/lio-provider2.properties")
    static class ProviderConfiguration {

    }
}
