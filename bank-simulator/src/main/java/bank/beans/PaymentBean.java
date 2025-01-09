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

/**
 * Bean handling payment functionality for the bank simulator.
 * Manages payment processing and communication with the e-wallet API.
 *
 * @author Danilo Spera
 */
@Named
@SessionScoped
public class PaymentBean implements Serializable {
    
    /** API endpoint for e-wallet payment simulation */
    private static final String E_WALLET_API = "http://localhost:8080/e-wallet/api/bank-api/simulate-payment";
    
    /** Fixed sender IBAN */
    private static final String MY_BANK_IBAN = "CZ1234567890";
    
    /** Amount to be transferred */
    private double amount;
    
    /** Variable symbol */
    private String variableSymbol;
    
    /** Available budget for transfers , every time the application is deployed, the budget is reset to 1000.0*/
    private double budget = 1000.0;
    
    /** Receiver's bank account IBAN */
    private String receiverAccount;
    
    /**
     * Initializes the bean with default values.
     * Called after bean construction.
     */
    @PostConstruct
    public void init() {
        budget = 1000.0;
        receiverAccount = "";
    }
    
    /**
     * Processes and sends a payment to the e-wallet system.
     * Validates input, creates payment object, and handles the API response.
     */
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
            
            // Send request to API
            Client client = ClientBuilder.newClient();
            Response response = client.target(E_WALLET_API)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(payment, MediaType.APPLICATION_JSON));
            
            String responseBody = response.readEntity(String.class);
            
            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                // Update budget and reset amount on successful payment
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

    
    /**
     * Gets the payment amount.
     * @return The current payment amount
     */
    public double getAmount() { return amount; }
    
    /**
     * Sets the payment amount and validates against available budget.
     * @param amount The amount to set
     */
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
    
    /**
     * Gets the variable symbol.
     * @return The current variable symbol
     */
    public String getVariableSymbol() { return variableSymbol; }
    
    /**
     * Sets the variable symbol for payment identification.
     * @param variableSymbol The variable symbol to set
     */
    public void setVariableSymbol(String variableSymbol) { this.variableSymbol = variableSymbol; }
    
    /**
     * Gets the available budget.
     * @return The current budget
     */
    public double getBudget() { return budget; }
    
    /**
     * Sets the available budget.
     * @param budget The budget to set
     */
    public void setBudget(double budget) { this.budget = budget; }
    
    /**
     * Gets the receiver's account IBAN.
     * @return The receiver's IBAN
     */
    public String getReceiverAccount() { return receiverAccount; }
    
    /**
     * Sets the receiver's account IBAN.
     * @param receiverAccount The receiver's IBAN to set
     */
    public void setReceiverAccount(String receiverAccount) { this.receiverAccount = receiverAccount; }
    
    /**
     * Gets the bank's fixed IBAN.
     * @return The bank's IBAN
     */
    public String getMyBankIban() { return MY_BANK_IBAN; }
} 