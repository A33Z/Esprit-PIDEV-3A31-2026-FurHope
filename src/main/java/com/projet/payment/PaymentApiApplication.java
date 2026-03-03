package com.projet.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PaymentApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentApiApplication.class, args);
        System.out.println("ENV USER = " + System.getenv("MAILTRAP_USERNAME"));
        System.out.println("ENV PASS = " + System.getenv("MAILTRAP_PASSWORD"));
    }

}