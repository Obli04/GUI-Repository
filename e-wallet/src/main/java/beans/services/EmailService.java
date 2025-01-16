package beans.services;

import java.util.Properties;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;


/**
 * Service for sending emails, including verification and password reset emails.
 * Utilizes Jakarta Mail API to send emails through an SMTP server.
 * 
 * @author Davide Scaccia - xscaccd00
 */

@ApplicationScoped
public class EmailService {
    private static final String FROM_EMAIL = "cashhivegja@gmail.com";
    private static final String EMAIL_PASSWORD = "vxdl euux tbon zecu";
    private static final int SMTP_PORT = 587;
    private static final String SMTP_HOST = "smtp.gmail.com";

    /**
     * Sends a verification email to the specified email address.
     *
     * @param toEmail the recipient's email address
     * @param token the verification token to be included in the email
     * @throws Exception if an error occurs while sending the email
     */
    public void sendVerificationEmail(String toEmail, String token) throws Exception {

        Properties props = new Properties(); //Properties for the SMTP server
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        props.put("mail.smtp.ssl.trust", SMTP_HOST);
        props.put("mail.debug", "true");
        props.put("mail.debug.auth", "true");
        
        try {
            Session session = Session.getInstance(props, new Authenticator() { //Session for the SMTP server
                @Override
                protected PasswordAuthentication getPasswordAuthentication() { //Authenticating the user
                    return new PasswordAuthentication(FROM_EMAIL, EMAIL_PASSWORD);
                }
            });
            
            Message message = new MimeMessage(session); //Message to be sent
            message.setFrom(new InternetAddress(FROM_EMAIL)); //Setting the sender
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail)); //Setting the recipient
            message.setSubject("Verify your CashHive account"); //Setting the subject

            String verificationLink = "http://localhost:8080/e-wallet/verify.xhtml?token=" + token; //Link for the verification
            String htmlContent = String.format( //HTML content for the email
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
                verificationLink, verificationLink); //Setting the content

            message.setContent(htmlContent, "text/html; charset=utf-8"); //Setting the content type
            Transport.send(message); //Sending the message
        } catch (MessagingException e) {
            e.printStackTrace(System.err); //Printing the stack trace
            throw new Exception("Failed to send verification email: " + e.getMessage(), e); //Throwing an exception
        }
    }

    /**
     * Sends a password reset email to the specified email address.
     *
     * @param toEmail the recipient's email address
     * @param token the password reset token to be included in the email
     * @throws Exception if an error occurs while sending the email
     */
    public void sendPasswordResetEmail(String toEmail, String token) throws Exception {
        
        Properties props = new Properties(); //Properties for the SMTP server
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        props.put("mail.smtp.ssl.trust", SMTP_HOST);
        props.put("mail.debug", "true");
        props.put("mail.debug.auth", "true");

        try { //Session for the SMTP server
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() { //Authenticating the user
                    return new PasswordAuthentication(FROM_EMAIL, EMAIL_PASSWORD);
                }
            });

            Message message = new MimeMessage(session); //Message to be sent
            message.setFrom(new InternetAddress(FROM_EMAIL)); //Setting the sender
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail)); //Setting the recipient
            message.setSubject("Reset your CashHive password"); //Setting the subject

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
                resetLink, resetLink); //Setting the content

            message.setContent(htmlContent, "text/html; charset=utf-8"); //Setting the content type
            Transport.send(message); //Sending the message
        } catch (MessagingException e) {
            e.printStackTrace(System.err);
            throw new Exception("Failed to send password reset email: " + e.getMessage());
        }
    }
}