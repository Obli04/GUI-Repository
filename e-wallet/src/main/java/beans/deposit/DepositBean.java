/**
 * Bean handling deposit functionality.
 * Manages payment information display and processing of incoming payments.
 * 
 * @author Danilo Spera
 */
package beans.deposit;

import java.io.Serializable;

import org.primefaces.model.StreamedContent;

import beans.UserBean;
import beans.deposit.services.PaymentInfo;
import beans.deposit.services.PaymentService;
import beans.entities.Transaction;
import beans.entities.User;
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
    
    // Central bank account used for all deposits
    private static final String BANK_IBAN = "CZ6550400000001234567890";
    
    @Inject
    private UserBean userBean;
    
    @Inject
    private PaymentService paymentService;
    
    // WebSocket channel for real-time payment notifications
    @Inject @Push
    private PushContext paymentChannel;
    
    private StreamedContent qrCode;
    private String spaydString;
    private double amount = 0.0;
    
    /**
     * Initializes or reinitializes deposit information.
     * Called after successful login to prepare deposit details.
     */
    public void initializeDeposit() {
        generateSpayd();
        generateQRCode();
    }
    
    /**
     * Generates SPD (Short Payment Descriptor) string containing payment information.
     * This string includes bank account, amount, currency, and variable symbol.
     */
    private void generateSpayd() {
        User currentUser = userBean.getCurrentUser();
        spaydString = String.format("SPD*1.0*ACC:%s*AM:%.2f*CC:CZK*MSG:Deposit to CashHive*X-VS:%s",
            BANK_IBAN,
            amount,
            currentUser.getVariableSymbol());
    }
    
    /**
     * Generates QR code from SPD string.
     */
    private void generateQRCode() {
        qrCode = paymentService.generateQRCode(spaydString);
    }
    
    /**
     * Handles incoming payment notification from bank API.
     * Validates and processes the payment, updating user balance and creating transaction record.
     * 
     * @param payment The payment information received from bank
     * @throws IllegalArgumentException if payment validation fails
     */
    public void handlePaymentNotification(PaymentInfo payment) throws IllegalArgumentException {
        // Validate payment first
        if (paymentService.isValidPayment(payment)) {
            // Process payment if validation passes
            Transaction transaction = paymentService.processPayment(payment);
            if (transaction != null) {
                // Notify client through WebSocket
                paymentChannel.send("Payment received: " + payment.getAmount() + " CZK");
                // Refresh user data to show new balance
                userBean.refreshUserData();
            } else {
                throw new IllegalArgumentException("Failed to process payment");
            }
        }
    }
    
    /**
     * Updates QR code when amount changes.
     * Called by AJAX when user modifies the amount input.
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