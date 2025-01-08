package bank.beans;

import java.io.Serializable;

import org.primefaces.PrimeFaces;

import bank.models.Payment;
import jakarta.annotation.PostConstruct;
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
public class PaymentBean implements Serializable {
    
    private static final String E_WALLET_API = "http://localhost:8080/e-wallet/api/bank-api/simulate-payment";
    private static final String MY_BANK_IBAN = "CZ1234567890"; // Fixed sender IBAN
    
    private double amount;
    private String variableSymbol;
    private double budget = 1000.0;
    private String receiverAccount;
    
    @PostConstruct
    public void init() {
        budget = 1000.0;
        receiverAccount = "";
    }
    
    public void sendPayment() {
        try {
            // Validate input
            if (amount > budget) {
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", 
                        "Insufficient funds. Your budget is " + String.format("%.2f", budget) + " CZK"));
                return;
            }
            
            if (receiverAccount == null || receiverAccount.trim().isEmpty()) {
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", 
                        "Please enter receiver's IBAN"));
                return;
            }
            
            // Create payment object
            Payment payment = new Payment();
            payment.setSenderAccount(MY_BANK_IBAN);
            payment.setReceiverAccount(receiverAccount);
            payment.setAmount(amount);
            payment.setVariableSymbol(variableSymbol);
            payment.setRemainingBudget(budget - amount);
            
            // Send request
            Client client = ClientBuilder.newClient();
            Response response = client.target(E_WALLET_API)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(payment, MediaType.APPLICATION_JSON));
            
            String responseBody = response.readEntity(String.class);
            
            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                // Only update budget and reset amount on successful payment
                budget -= amount;
                amount = 0.0;
                
                // Show success message
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", 
                        String.format("Payment of %.2f CZK has been sent. Remaining budget: %.2f CZK", 
                            payment.getAmount(), budget)));
                
                // Update UI components
                PrimeFaces.current().ajax().update("form:budget-display", "form:amount");
            } else {
                // Parse and display the error message from the response
                String errorMessage;
                try {
                    // Try to extract the error message from JSON response
                    errorMessage = responseBody.contains("message") ? 
                        responseBody.split("message\":\"")[1].split("\"")[0] : 
                        "Payment failed";
                } catch (Exception e) {
                    errorMessage = "Payment failed: " + responseBody;
                }
                
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Payment Failed", errorMessage));
            }
            
        } catch (Exception e) {
            // Show error message for unexpected errors
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", 
                    "Failed to send payment: " + e.getMessage()));
        }
    }
    
    // Getters and setters
    public double getAmount() { return amount; }
    public void setAmount(double amount) { 
        if (amount > budget) {
            this.amount = 0.0;
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", 
                    "Amount exceeds available budget of " + String.format("%.2f", budget) + " CZK"));
        } else {
            this.amount = amount;
        }
    }
    
    public String getVariableSymbol() { return variableSymbol; }
    public void setVariableSymbol(String variableSymbol) { this.variableSymbol = variableSymbol; }
    
    public double getBudget() { return budget; }
    public void setBudget(double budget) { this.budget = budget; }
    
    public String getReceiverAccount() { return receiverAccount; }
    public void setReceiverAccount(String receiverAccount) { this.receiverAccount = receiverAccount; }
    
    public String getMyBankIban() { return MY_BANK_IBAN; }
} 