package beans;

import java.io.Serializable;

import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Named
@SessionScoped
public class BankSimulatorBean implements Serializable {
    
    private String senderAccount = "CZ6550000000001234567890"; // Default test account
    private String receiverAccount;
    private double amount;
    private String variableSymbol;
    
    public String simulatePayment() {
        try {
            // Create REST client
            Client client = ClientBuilder.newClient();
            
            // Create payment request object
            PaymentRequest payment = new PaymentRequest(
                senderAccount,
                receiverAccount,
                amount,
                variableSymbol
            );
            
            // Make API call to bank-api endpoint
            Response response = client.target("http://localhost:8080/e-wallet/api/bank-api/simulate-payment")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(payment, MediaType.APPLICATION_JSON));
            
            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", 
                        "Payment of " + amount + " CZK has been sent"));
                        
                // Reset form
                amount = 0.0;
                
                return "deposit?faces-redirect=true";
            } else {
                String error = response.readEntity(String.class);
                throw new Exception("API call failed: " + error);
            }
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
    
    // Make PaymentRequest public and static
    public static class PaymentRequest {
        private String senderAccount;
        private String receiverAccount;
        private double amount;
        private String variableSymbol;
        
        // Add no-args constructor for JSON deserialization
        public PaymentRequest() {}
        
        public PaymentRequest(String senderAccount, String receiverAccount, 
                            double amount, String variableSymbol) {
            this.senderAccount = senderAccount;
            this.receiverAccount = receiverAccount;
            this.amount = amount;
            this.variableSymbol = variableSymbol;
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
} 