package beans;

import java.io.Serializable;

import org.primefaces.model.StreamedContent;

import beans.entities.Transaction;
import beans.entities.User;
import beans.services.PaymentInfo;
import beans.services.PaymentService;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.push.Push;
import jakarta.faces.push.PushContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Bean handling deposit functionality
 * Manages payment information display and processing of incoming payments.
 */
@Named
@SessionScoped
public class DepositBean implements Serializable {
    
    private static final String BANK_IBAN = "CZ6550400000001234567890"; // Central bank account for all deposits
    
    @Inject
    private UserBean userBean;
    
    @Inject
    private PaymentService paymentService;
    
    @Inject @Push
    private PushContext paymentChannel;
    
    private StreamedContent qrCode;
    private String spaydString;
    private double amount = 0.0;
    
    /**
     * Initializes or reinitializes deposit information
     * Called after successful login
     */
    public void initializeDeposit() {
        generateSpayd();
        generateQRCode();
    }
    
    /**
     * Generates SPD string containing payment information
     */
    private void generateSpayd() {
        User currentUser = userBean.getCurrentUser();
        spaydString = String.format("SPD*1.0*ACC:%s*AM:%.2f*CC:CZK*MSG:Deposit to CashHive*X-VS:%s",
            BANK_IBAN,
            amount,
            currentUser.getVariableSymbol());
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
    public void handlePaymentNotification(PaymentInfo payment) throws IllegalArgumentException {
        // First validate the payment
        if (paymentService.isValidPayment(payment)) {
            // Only process if validation passes
            Transaction transaction = paymentService.processPayment(payment);
            if (transaction != null) {
                paymentChannel.send("Payment received: " + payment.getAmount() + " CZK");
                userBean.refreshUserData();
            } else {
                throw new IllegalArgumentException("Failed to process payment");
            }
        }
    }
    
    /**
     * Updates QR code when amount changes
     */
    public void onAmountChange() {
        generateSpayd();
        generateQRCode();
    }
    
    // Getters and setters
    public String getVariableSymbol() {
        return userBean.getCurrentUser().getVariableSymbol();
    }
    
    public StreamedContent getQrCode() {
        return qrCode;
    }
    
    public String getSpaydString() {
        return spaydString;
    }
    
    public String getBankAccount() {
        return BANK_IBAN;
    }
    
    public double getAmount() {
        return amount;
    }
    
    public void setAmount(double amount) {
        this.amount = amount;
    }
} 