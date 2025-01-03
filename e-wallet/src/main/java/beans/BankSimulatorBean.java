package beans;

import java.io.Serializable;
import java.time.LocalDateTime;

import beans.services.PaymentInfo;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
@SessionScoped
public class BankSimulatorBean implements Serializable {
    
    @Inject
    private DepositBean depositBean;
    
    private String senderAccount = "CZ6550000000001234567890"; // Default test account
    private String receiverAccount;
    private double amount;
    private String variableSymbol;
    
    public String simulatePayment() {
        try {
            PaymentInfo payment = new PaymentInfo(
                senderAccount,
                receiverAccount,
                amount,
                LocalDateTime.now(),
                variableSymbol
            );
            
            depositBean.handlePaymentNotification(payment);
            
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", 
                    "Payment of " + amount + " CZK has been sent"));
                    
            // Reset form
            amount = 0.0;
            
            return "deposit?faces-redirect=true";
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", 
                    "Failed to process payment: " + e.getMessage()));
            return null;
        }
    }
    
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