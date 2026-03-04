package com.projet.payment;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public final class SpringPaymentServer {

    private static ConfigurableApplicationContext context;
    private static int port = 8080;

    private SpringPaymentServer() {
    }

    public static synchronized void start() {
        if (context != null) {
            return;
        }

        context = new SpringApplicationBuilder(PaymentApiApplication.class)
                .properties(
                        "server.port=0",
                        "spring.config.name=payment-api"
                )
                .run();

        Integer detectedPort = context.getEnvironment().getProperty("local.server.port", Integer.class);
        if (detectedPort != null) {
            port = detectedPort;
        }
    }

    public static synchronized void stop() {
        if (context != null) {
            context.close();
            context = null;
        }
    }

    public static String getPaymentProcessUrl() {
        return "http://localhost:" + port + "/api/payment/process";
    }
}
