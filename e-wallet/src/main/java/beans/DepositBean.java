package beans;

import java.io.Serializable;
import java.util.Random;

import org.primefaces.model.StreamedContent;

import beans.entities.Transaction;
import beans.entities.User;
import beans.services.PaymentInfo;
import beans.services.PaymentService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.push.Push;
import jakarta.faces.push.PushContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Bean handling deposit functionality for the e-wallet application.
 * Manages payment information display and processing of incoming payments.
 */
@Named
@SessionScoped
public class DepositBean implements Serializable {
    
    @Inject
    private UserBean userBean;
    
    @Inject
    private PaymentService paymentService;
    
    @Inject @Push
    private PushContext paymentChannel;
    
    private String variableSymbol;
    private StreamedContent qrCode;
    private String spaydString;
    
    @PostConstruct
    public void init() {
        // Generate a unique variable symbol for this deposit session
        generateVariableSymbol();
        // Generate SPD (Short Payment Descriptor)
        generateSpayd();
        // Generate QR code based on SPD
        generateQRCode();
    }
    
    /**
     * Generates a random 10-digit variable symbol
     */
    private void generateVariableSymbol() {
        Random random = new Random();
        variableSymbol = String.format("%010d", random.nextInt(1000000000));
    }
    
    /**
     * Generates SPD string containing payment information
     */
    private void generateSpayd() {
        User currentUser = userBean.getCurrentUser();
        spaydString = String.format("SPD*1.0*ACC:%s*AM:0.00*CC:CZK*MSG:Deposit to CashHive*X-VS:%s",
            currentUser.getIban(),
            variableSymbol);
    }
    
    /**
     * Generates QR code from SPD string
     */
    private void generateQRCode() {
        qrCode = paymentService.generateQRCode(spaydString);
    }
    
    /**
     * Handles incoming payment notification from bank API
     * @param payment The payment information received
     */
    public void handlePaymentNotification(PaymentInfo payment) {
        if (paymentService.isValidPayment(payment)) {
            // Process the payment and update user's balance
            Transaction transaction = paymentService.processPayment(payment);
            if (transaction != null) {
                // Notify the UI about the new payment
                paymentChannel.send("Payment received: " + payment.getAmount() + " CZK");
                // Refresh user data
                userBean.refreshUserData();
            }
        }
    }
    
    // Getters and setters
    public String getVariableSymbol() {
        return variableSymbol;
    }
    
    public StreamedContent getQrCode() {
        return qrCode;
    }
    
    public String getSpaydString() {
        return spaydString;
    }
    
    public String getBankAccount() {
        return userBean.getCurrentUser().getIban();
    }
} 