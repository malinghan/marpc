package com.malinghan.marpc;

import com.malinghan.marpc.annotation.EnableMarpc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableMarpc
public class MarpcApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarpcApplication.class, args);
    }

}
