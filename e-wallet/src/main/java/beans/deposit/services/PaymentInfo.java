package beans.deposit.services;

import java.time.LocalDateTime;

/**
 * Data class representing payment information received from bank API.
 * This class encapsulates all necessary information for processing a bank transfer,
 * including sender and receiver details, amount, and transaction metadata.
 *
 * @author Danilo Spera
 */
public class PaymentInfo {
    /** The IBAN of the account sending the payment */
    private final String senderAccount;
    
    /** The IBAN of the account receiving the payment */
    private final String receiverAccount;
    
    /** The amount of money being transferred in CZK */
    private final double amount;
    
    /** The timestamp when the payment was initiated */
    private final LocalDateTime date;
    
    /** The variable symbol used to identify the receiving user */
    private final String variableSymbol;
    
    /**
     * Constructs a new PaymentInfo object with all necessary payment details.
     *
     * @param senderAccount   The IBAN of the sending account
     * @param receiverAccount The IBAN of the receiving account (must match e-wallet's bank account)
     * @param amount         The amount to transfer in CZK
     * @param date          The timestamp of the payment
     * @param variableSymbol The unique identifier for the receiving user
     */
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