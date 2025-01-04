package bank.beans;

import java.io.Serializable;

import bank.models.Payment;
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
    
    private String senderAccount = "CZ6550000000001234567890";
    private String receiverAccount;
    private double amount;
    private String variableSymbol;
    
    // URL of your main e-wallet application
    private static final String E_WALLET_API = "http://localhost:8080/e-wallet/api/bank-api/simulate-payment";
    
    public void sendPayment() {
        try {
            Client client = ClientBuilder.newClient();
            
            Payment payment = new Payment();
            payment.setSenderAccount(senderAccount);
            payment.setReceiverAccount(receiverAccount);
            payment.setAmount(amount);
            payment.setVariableSymbol(variableSymbol);
            
            Response response = client.target(E_WALLET_API)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(payment, MediaType.APPLICATION_JSON));
            
            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", 
                        String.format("Payment of %.2f CZK has been sent", amount)));
                // Reset amount after successful payment
                amount = 0.0;
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