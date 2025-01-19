package beans.deposit.api;

/**
 * Data Transfer Object (DTO) for receiving payment information from bank simulator.
 * Maps JSON payment data to Java object for processing.
 *
 * @author Danilo Spera
 */
public class SimulatedPayment {
    /** The bank account IBAN sending the payment */
    private String senderAccount;
    
    /** The target bank account IBAN */
    private String receiverAccount;
    
    /** The payment amount in CZK */
    private double amount;
    
    /** The variable symbol for identifying the receiving user */
    private String variableSymbol;
    
    // Getters and setters
    public String getSenderAccount() { return senderAccount; }
    public void setSenderAccount(String senderAccount) { this.senderAccount = senderAccount; }
    
    public String getReceiverAccount() { return receiverAccount; }
    public void setReceiverAccount(String receiverAccount) { this.receiverAccount = receiverAccount; }
    
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    
    public String getVariableSymbol() { return variableSymbol; }
    public void setVariableSymbol(String variableSymbol) { this.variableSymbol = variableSymbol; }
} 