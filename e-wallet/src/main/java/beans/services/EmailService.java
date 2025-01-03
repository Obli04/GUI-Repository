package beans.services;

import java.util.Properties;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

@ApplicationScoped
public class EmailService {
    private static final String FROM_EMAIL = "cashhivegja@gmail.com";
    private static final String EMAIL_PASSWORD = "vxdl euux tbon zecu";
    private static final int SMTP_PORT = 587;
    private static final String SMTP_HOST = "smtp.gmail.com";


    public void sendVerificationEmail(String toEmail, String token) throws Exception {
        System.out.println("\n=== Email Service: Sending Verification Email ===");
        System.out.println("Recipient: " + toEmail);
        System.out.println("SMTP Host: " + SMTP_HOST);
        System.out.println("SMTP Port: " + SMTP_PORT);
        System.out.println("From Email: " + FROM_EMAIL);
        
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        props.put("mail.smtp.ssl.trust", SMTP_HOST);
        props.put("mail.debug", "true");
        props.put("mail.debug.auth", "true");
        
        try {
            System.out.println("Creating mail session...");
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    System.out.println("Attempting authentication with username: " + FROM_EMAIL);
                    return new PasswordAuthentication(FROM_EMAIL, EMAIL_PASSWORD);
                }
            });
            
            // Enable session debugging
            session.setDebug(true);
            
            System.out.println("Creating email message...");
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Verify your CashHive account");

            String verificationLink = "http://localhost:8080/e-wallet/verify.xhtml?token=" + token;
            String htmlContent = String.format(
                "<html>" +
                "<body style='font-family: Arial, sans-serif;'>" +
                    "<h2>Welcome to CashHive!</h2>" +
                    "<p>Thank you for registering. Please click the link below to verify your email address:</p>" +
                    "<p><a href='%s' style='background-color: #4CAF50; color: white; padding: 14px 25px; text-align: center; text-decoration: none; display: inline-block;'>Verify Email</a></p>" +
                    "<p>If the button doesn't work, copy and paste this link into your browser:</p>" +
                    "<p>%s</p>" +
                    "<p>This link will expire in 24 hours.</p>" +
                    "<p>Best regards,<br>The CashHive Team</p>" +
                "</body>" +
                "</html>",
                verificationLink, verificationLink);

            message.setContent(htmlContent, "text/html; charset=utf-8");

            System.out.println("Attempting to send email...");
            Transport transport = session.getTransport("smtp");
            try {
                System.out.println("Connecting to SMTP server...");
                transport.connect(SMTP_HOST, FROM_EMAIL, EMAIL_PASSWORD);
                System.out.println("Connected successfully");
                
                System.out.println("Sending message...");
                transport.sendMessage(message, message.getAllRecipients());
                System.out.println("Message sent successfully");
            } finally {
                transport.close();
            }
            
            System.out.println("=== End Email Service ===\n");
            System.out.flush();
            
        } catch (Exception e) {
            System.err.println("\n=== Email Service Error ===");
            System.err.println("Failed to send email to: " + toEmail);
            System.err.println("Error type: " + e.getClass().getName());
            System.err.println("Error message: " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("Caused by: " + e.getCause().getMessage());
            }
            e.printStackTrace(System.err);
            System.err.println("=== End Email Service Error ===\n");
            System.err.flush();
            throw new Exception("Failed to send verification email: " + e.getMessage(), e);
        }
    }

    public void sendPasswordResetEmail(String toEmail, String token) throws Exception {
        System.out.println("Attempting to send password reset email to: " + toEmail);
        
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, EMAIL_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Reset your CashHive password");

            String resetLink = "http://localhost:8080/e-wallet/reset-password.xhtml?token=" + token;
            String htmlContent = String.format(
                "<html>" +
                "<body style='font-family: Arial, sans-serif;'>" +
                    "<h2>Password Reset Request</h2>" +
                    "<p>We received a request to reset your CashHive password. Click the link below to set a new password:</p>" +
                    "<p><a href='%s' style='background-color: #4CAF50; color: white; padding: 14px 25px; text-align: center; text-decoration: none; display: inline-block;'>Reset Password</a></p>" +
                    "<p>If the button doesn't work, copy and paste this link into your browser:</p>" +
                    "<p>%s</p>" +
                    "<p>This link will expire in 1 hour.</p>" +
                    "<p>If you didn't request this password reset, please ignore this email.</p>" +
                    "<p>Best regards,<br>The CashHive Team</p>" +
                "</body>" +
                "</html>",
                resetLink, resetLink);

            message.setContent(htmlContent, "text/html; charset=utf-8");

            Transport.send(message);
            System.out.println("Password reset email sent successfully to: " + toEmail);
        } catch (Exception e) {
            System.err.println("Failed to send password reset email: " + e.getMessage());
            e.printStackTrace();
            throw new Exception("Failed to send password reset email: " + e.getMessage());
        }
    }
}
   
