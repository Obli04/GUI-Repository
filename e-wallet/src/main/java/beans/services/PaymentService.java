package beans.services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;

import beans.entities.Transaction;
import beans.entities.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

/**
 * Service handling payment processing and validation
 */
@Named
@ApplicationScoped
public class PaymentService {
    
    @PersistenceContext
    private EntityManager em;
    
    // Store processed payment IDs to prevent duplicates
    private Set<String> processedPayments = ConcurrentHashMap.newKeySet();
    
    /**
     * Generates QR code for payment
     * @param spayd SPD string containing payment information
     * @return StreamedContent containing QR code image
     */
    public StreamedContent generateQRCode(String spayd) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(
                qrCodeWriter.encode(spayd, BarcodeFormat.QR_CODE, 300, 300),
                "PNG",
                outputStream
            );
            
            return DefaultStreamedContent.builder()
                .contentType("image/png")
                .stream(() -> new ByteArrayInputStream(outputStream.toByteArray()))
                .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }
    
    /**
     * Validates incoming payment
     * @param payment Payment information from bank API
     * @return true if payment is valid and not processed before
     */
    public boolean isValidPayment(PaymentInfo payment) {
        String paymentId = generatePaymentId(payment);
        return !processedPayments.contains(paymentId);
    }
    
    /**
     * Processes incoming payment
     * @param payment Payment information
     * @return Created transaction or null if failed
     */
    @Transactional
    public Transaction processPayment(PaymentInfo payment) {
        String paymentId = generatePaymentId(payment);
        
        if (processedPayments.add(paymentId)) {
            User receiver = em.createQuery(
                "SELECT u FROM User u WHERE u.iban = :iban", User.class)
                .setParameter("iban", payment.getReceiverAccount())
                .getSingleResult();
            
            if (receiver != null) {
                Transaction transaction = new Transaction();
                transaction.setNameOfSender(payment.getSenderAccount());
                transaction.setReceiver(receiver);
                transaction.setValue(payment.getAmount());
                transaction.setType("Deposit");
                transaction.setTransactionDate(LocalDateTime.now());
                transaction.setCategory("Deposit");
                
                // Update user's balance
                receiver.setBalance(receiver.getBalance() + payment.getAmount());
                
                em.persist(transaction);
                em.merge(receiver);
                
                return transaction;
            }
        }
        return null;
    }
    
    private String generatePaymentId(PaymentInfo payment) {
        return payment.getSenderAccount() + 
               payment.getReceiverAccount() + 
               payment.getAmount() + 
               payment.getDate() + 
               payment.getVariableSymbol();
    }
} 