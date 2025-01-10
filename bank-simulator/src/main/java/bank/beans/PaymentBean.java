package bank.beans;

import java.io.Serializable;

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
    public String sendPayment() {
        try {
            // Add validation for zero or negative amount
            if (amount == 0) {
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                        "Invalid Amount", 
                        "Payment amount cannot be zero"));
                return null;
            }
            
            if (amount < 0) {
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                        "Invalid Amount", 
                        "Payment amount cannot be negative"));
                return null;
            }

            // Rest of the existing sendPayment logic
            if (amount > budget) {
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                        "Insufficient Funds", 
                        "You don't have enough funds for this payment"));
                return null;
            }

            // Create payment object
            Payment payment = new Payment();
            payment.setSenderAccount(MY_BANK_IBAN);
            payment.setReceiverAccount(receiverAccount);
            payment.setAmount(amount);
            payment.setVariableSymbol(variableSymbol);
            payment.setRemainingBudget(budget - amount);

            // Update budget
            budget -= amount;

            // Send payment to e-wallet
            Client client = ClientBuilder.newClient();
            Response response = client.target(E_WALLET_API)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(payment, MediaType.APPLICATION_JSON));

            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_INFO, 
                        "Success", 
                        "Payment sent successfully"));
            } else {
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                        "Error", 
                        "Failed to send payment: " + response.readEntity(String.class)));
            }

            // Reset form
            amount = 0;
            receiverAccount = "";
            variableSymbol = "";

            return null;
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Error", 
                    "An error occurred while processing the payment: " + e.getMessage()));
            return null;
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