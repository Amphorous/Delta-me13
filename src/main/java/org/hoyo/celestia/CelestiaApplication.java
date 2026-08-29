package org.hoyo.celestia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class CelestiaApplication {

    public static void main(String[] args) {
        SpringApplication.run(CelestiaApplication.class, args);
    }

}
