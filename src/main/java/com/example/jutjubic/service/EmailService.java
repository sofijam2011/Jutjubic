package com.example.jutjubic.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.url}")
    private String appUrl;

    public void sendVerificationEmail(String toEmail, String token) {
        String subject = "Verifikacija naloga";
        String verificationUrl = "http://localhost:3000/verify-email?token=" + token;
        String message = "Poštovani,\n\n" +
                "Hvala što ste se registrovali. Molimo vas da kliknete na sledeći link " +
                "da biste aktivirali svoj nalog:\n\n" +
                verificationUrl + "\n\n" +
                "Link je validan 24 sata.\n\n" +
                "Ako niste kreirali nalog, molimo vas ignorišite ovaj email.\n\n" +
                "Srdačan pozdrav";

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom(fromEmail);
        mailMessage.setTo(toEmail);
        mailMessage.setSubject(subject);
        mailMessage.setText(message);

        mailSender.send(mailMessage);
    }
}
