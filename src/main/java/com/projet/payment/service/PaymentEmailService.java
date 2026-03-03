package com.projet.payment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class PaymentEmailService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentEmailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final boolean mockMode;
    private final String smtpUsername;
    private final String smtpPassword;

    public PaymentEmailService(
            JavaMailSender mailSender,
            @Value("${payment.mail.from:no-reply@petshop.local}") String fromAddress,
            @Value("${payment.mail.mock:true}") boolean mockMode,
            @Value("${spring.mail.username:}") String smtpUsername,
            @Value("${spring.mail.password:}") String smtpPassword
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.mockMode = mockMode;
        this.smtpUsername = smtpUsername;
        this.smtpPassword = smtpPassword;
    }

    public void sendConfirmationCode(String to, String transactionId, String code) {
        if (mockMode) {
            logger.info("MOCK MAIL to={} transactionId={} code={}", to, transactionId, code);
            return;
        }
        if (smtpUsername == null || smtpUsername.isBlank() || smtpPassword == null || smtpPassword.isBlank()) {
            throw new IllegalStateException("SMTP credentials are missing. Set MAILTRAP_USERNAME and MAILTRAP_PASSWORD.");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject("Pet Shop Payment Confirmation");
        message.setText(
                "Your payment verification code is: " + code
                        + "\nTransaction ID: " + transactionId
                        + "\nThis code expires in 10 minutes."
        );
        mailSender.send(message);
    }
}
