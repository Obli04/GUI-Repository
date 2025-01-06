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
    
    private String receiverAccount;
    private double amount;
    private String variableSymbol;
    private double budget = 1000.0; // Initial budget
    
    private static final String E_WALLET_API = "http://localhost:8080/e-wallet/api/bank-api/simulate-payment";
    
    @PostConstruct
    public void init() {
        budget = 1000.0;
    }
    
    public void sendPayment() {
        try {
            if (amount > budget) {
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", 
                        "Insufficient funds. Your budget is " + String.format("%.2f", budget) + " CZK"));
                return;
            }
            
            Client client = ClientBuilder.newClient();
            
            Payment payment = new Payment();
            payment.setReceiverAccount(receiverAccount);
            payment.setAmount(amount);
            payment.setVariableSymbol(variableSymbol);
            payment.setRemainingBudget(budget - amount);
            
            Response response = client.target(E_WALLET_API)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(payment, MediaType.APPLICATION_JSON));
            
            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                // Update budget after successful payment
                budget -= amount;
                
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", 
                        String.format("Payment of %.2f CZK has been sent. Remaining budget: %.2f CZK", 
                            amount, budget)));
                // Reset amount after successful payment
                amount = 0.0;
                
                // Update the UI components
                PrimeFaces.current().ajax().update("form:budget-display", "form:amount");
            } else {
                String error = response.readEntity(String.class);
                throw new Exception("Payment failed: " + error);
            }
            
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", 
                    "Failed to send payment: " + e.getMessage()));
        }
    }
    
    public void validateAmount() {
        if (amount > budget) {
            // Reset amount to 0
            amount = 0.0;
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", 
                    "Amount exceeds available budget of " + String.format("%.2f", budget) + " CZK"));
        }
    }
    
    // Getters and setters
    public String getReceiverAccount() { return receiverAccount; }
    public void setReceiverAccount(String receiverAccount) { this.receiverAccount = receiverAccount; }
    
    public double getAmount() { return amount; }
    public void setAmount(double amount) {
        if (amount > budget) {
            this.amount = 0.0; // Reset to zero instead of adjusting to budget
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
} 