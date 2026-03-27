package org.example.ilink;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@EnableRabbit
@SpringBootApplication
public class IlinkApplication {
    public static void main(String[] args) {
        SpringApplication.run(IlinkApplication.class, args);
    }

}
