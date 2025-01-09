package beans.services;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;

import javax.imageio.ImageIO;

import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;

import beans.entities.Transaction;
import beans.entities.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class PaymentService {
    
    private static final String BANK_IBAN = "CZ6550400000001234567890";
    
    @PersistenceContext
    private EntityManager em;
    
    public boolean isValidPayment(PaymentInfo payment) throws IllegalArgumentException {
        // First check if the payment is sent to our bank account
        if (!BANK_IBAN.equals(payment.getReceiverAccount())) {
            throw new IllegalArgumentException("Invalid receiver IBAN: " + payment.getReceiverAccount());
        }
        
        // Then check if the variable symbol exists
        User user = em.createQuery("SELECT u FROM User u WHERE u.variableSymbol = :vs", User.class)
                     .setParameter("vs", payment.getVariableSymbol())
                     .getResultList()
                     .stream()
                     .findFirst()
                     .orElse(null);
                     
        if (user == null) {
            throw new IllegalArgumentException("Variable symbol not found: " + payment.getVariableSymbol());
        }
        
        return true;
    }
    
    @Transactional
    public Transaction processPayment(PaymentInfo payment) {
        try {
            // Find user by variable symbol
            User user = em.createQuery("SELECT u FROM User u WHERE u.variableSymbol = :vs", User.class)
                         .setParameter("vs", payment.getVariableSymbol())
                         .getSingleResult();
            
            // Create and save transaction
            Transaction transaction = new Transaction();
            transaction.setReceiver(user);
            //transaction.setNameOfSender(payment.getSenderAccount());
            transaction.setNameOfSender("Test Name of sender");
            transaction.setValue(payment.getAmount());
            transaction.setType("Deposit");
            transaction.setTransactionDate(LocalDateTime.now());
            
            // Update user balance
            user.setBalance(user.getBalance() + payment.getAmount());
            
            // Persist changes
            em.persist(transaction);
            em.merge(user);
            
            return transaction;
        } catch (Exception e) {
            System.err.println("Error processing payment: " + e.getMessage());
            return null;
        }
    }
    
    public StreamedContent generateQRCode(String spaydString) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BufferedImage qrCode = MatrixToImageWriter.toBufferedImage(
                qrCodeWriter.encode(spaydString, BarcodeFormat.QR_CODE, 200, 200)
            );
            
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(qrCode, "png", os);
            return DefaultStreamedContent.builder()
                                      .contentType("image/png")
                                      .stream(() -> new ByteArrayInputStream(os.toByteArray()))
                                      .build();
        } catch (Exception e) {
            System.err.println("Error generating QR code: " + e.getMessage());
            return null;
        }
    }
} 