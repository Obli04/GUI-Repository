package beans.deposit.services;

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

/**
 * Service class handling payment processing and QR code generation for the e-wallet application.
 * Manages payment validation, transaction processing, and QR code generation for bank transfers.
 *
 * @author Danilo Spera
 */
@ApplicationScoped // This class is a singleton and can be injected into other beans
public class PaymentService {
    
    /** The bank account IBAN used for receiving deposits */
    private static final String BANK_IBAN = "CZ6550400000001234567890";
    
    /** Entity manager for database operations */
    @PersistenceContext
    private EntityManager em;
    
    /**
     * Validates incoming payment information.
     * Checks if the receiver account matches our bank account and if the variable symbol exists.
     *
     * @param payment The payment information to validate
     * @return true if payment is valid
     * @throws IllegalArgumentException if payment validation fails
     */
    public boolean isValidPayment(PaymentInfo payment) throws IllegalArgumentException {
        // First check if the payment is sent to our bank account
        if (!BANK_IBAN.equals(payment.getReceiverAccount())) {
            throw new IllegalArgumentException("Invalid receiver IBAN: " + payment.getReceiverAccount());
        }
        
        // Then check if the variable symbol exists in our database
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
    
    /**
     * Processes a validated payment by creating a transaction record and updating user balance.
     * This method is transactional to ensure database consistency.
     *
     * @param payment The validated payment information to process
     * @return The created transaction record, or null if processing fails
     */
    @Transactional
    public Transaction processPayment(PaymentInfo payment) {
        try {
            // Find user by their variable symbol
            User user = em.createQuery("SELECT u FROM User u WHERE u.variableSymbol = :vs", User.class)
                         .setParameter("vs", payment.getVariableSymbol())
                         .getSingleResult();
            
            // Create new transaction record
            Transaction transaction = new Transaction();
            transaction.setReceiver(user);
            transaction.setNameOfSender("Bank Simulator, IBAN:" + payment.getSenderAccount());
            transaction.setValue(payment.getAmount());
            transaction.setType("Deposit");
            transaction.setCategory("Bank Transfer");
            transaction.setTransactionDate(LocalDateTime.now());
            
            // Update user's balance with the deposited amount
            user.setBalance(user.getBalance() + payment.getAmount());
            
            // Persist changes to database
            em.persist(transaction);
            em.merge(user);
            
            return transaction;
        } catch (Exception e) {
            System.err.println("Error processing payment: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Generates a QR code image from a SPAYD (Short Payment Descriptor) string.
     * The QR code can be scanned by banking apps to facilitate easy payment.
     *
     * @param spaydString The SPAYD format string containing payment information
     * @return StreamedContent containing the QR code image, or null if generation fails
     */
    public StreamedContent generateQRCode(String spaydString) {
        try {
            // Create QR code writer and generate QR code image
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BufferedImage qrCode = MatrixToImageWriter.toBufferedImage(
                qrCodeWriter.encode(spaydString, BarcodeFormat.QR_CODE, 200, 200)
            );
            
            // Convert BufferedImage to StreamedContent for web display
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