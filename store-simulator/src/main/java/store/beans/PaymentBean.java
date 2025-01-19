package beans.store.beans;

import java.io.Serializable;

import beans.store.models.Payment;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

/**
 * Bean for handling store payments. Manages user inputs and deducts budget.
 * @author - Arthur PHOMMACHANH xphomma00
 */
@Named
@SessionScoped
public class PaymentBean implements Serializable {

    private String senderAccount = "CZ1234567890"; // User IBAN
    private String storeAccount = "CZ6550400000001234567890"; // Store IBAN
    private double amount;
    private String description;
    private double budget = 1000.0;

    @PostConstruct
    public void init() {
        budget = 1000.0; // Reset budget when the application starts
    }

    public String processPayment() {
        if (amount <= 0) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Amount must be greater than zero."));
            return null;
        }
        if (amount > budget) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Insufficient funds."));
            return null;
        }

        budget -= amount;

        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Payment processed for: " + description));

        return null;
    }

    public String getSenderAccount() {
        return senderAccount;
    }

    public String getStoreAccount() {
        return storeAccount;
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

    public double getBudget() {
        return budget;
    }
}
