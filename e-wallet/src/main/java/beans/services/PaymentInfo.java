package beans.services;

import java.time.LocalDateTime;

/**
 * Data class representing payment information from bank API
 */
public class PaymentInfo {
    private String senderAccount;
    private String receiverAccount;
    private double amount;
    private LocalDateTime date;
    private String variableSymbol;
    
    // Constructor
    public PaymentInfo(String senderAccount, String receiverAccount, double amount, LocalDateTime date, String variableSymbol) {
        this.senderAccount = senderAccount;
        this.receiverAccount = receiverAccount;
        this.amount = amount;
        this.date = date;
        this.variableSymbol = variableSymbol;
    }
    
    // Getters
    public String getSenderAccount() { return senderAccount; }
    public String getReceiverAccount() { return receiverAccount; }
    public double getAmount() { return amount; }
    public LocalDateTime getDate() { return date; }
    public String getVariableSymbol() { return variableSymbol; }
} 