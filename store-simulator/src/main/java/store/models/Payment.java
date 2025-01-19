package beans.store.models;

/**
 * Model representing a payment in the store simulator.
 * @author - Arthur PHOMMACHANH xphomma00
 */
public class Payment {
    private String senderAccount;
    private String receiverAccount;
    private double amount;
    private String description;

    public Payment(String senderAccount, String receiverAccount, double amount, String description) {
        this.senderAccount = senderAccount;
        this.receiverAccount = receiverAccount;
        this.amount = amount;
        this.description = description;
    }

    public String getSenderAccount() {
        return senderAccount;
    }

    public void setSenderAccount(String senderAccount) {
        this.senderAccount = senderAccount;
    }

    public String getReceiverAccount() {
        return receiverAccount;
    }

    public void setReceiverAccount(String receiverAccount) {
        this.receiverAccount = receiverAccount;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
