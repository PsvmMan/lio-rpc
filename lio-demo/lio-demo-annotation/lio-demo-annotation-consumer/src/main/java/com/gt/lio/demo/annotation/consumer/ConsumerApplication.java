package com.gt.lio.demo.annotation.consumer;

import com.gt.lio.config.spring.annotation.EnableLio;
import com.gt.lio.demo.annotation.api.model.User;
import com.gt.lio.demo.annotation.consumer.impl.UserImpl;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.Scanner;

public class ConsumerApplication {

    public static void main(String[] args) throws Exception{
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ConsumerConfiguration.class);
        context.start();
        System.out.println("Consumer started");

        UserImpl userImpl = (UserImpl)context.getBean("userImpl");

        // 验证服务提供者动态加入和退出
        Scanner scanner = new Scanner(System.in);
        while(scanner.hasNextLine()){
            String s = scanner.nextLine();
            if("exit".equals(s)){
                break;
            }
            System.out.println("查询结果：" + userImpl.selectById(Long.parseLong(s)));
        }

        User messi = new User(100L, "messi", 38);
        System.out.println("--------------------------插入一个新用户:"+ messi +"--------------------------");
        System.out.println(userImpl.insert(messi));
        System.out.println(userImpl.insert(messi));

        System.out.println("--------------------------查询所有用户--------------------------");
        System.out.println(userImpl.selectAll());

        System.in.read();
    }

    @Configuration
    @EnableLio
    @PropertySource("classpath:/spring/lio-consumer.properties")
    @ComponentScan("com.gt.lio.demo.annotation.consumer")
    static class ConsumerConfiguration {

    }
}
