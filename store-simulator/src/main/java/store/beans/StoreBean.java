package store.beans;

import java.io.Serializable;

import store.models.Transaction;
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
 * Bean handling transaction functionality for the store simulator.
 * Manages purchases and communication with the inventory API.
 *
 * @author: Arthur PHOMMACHANH - xphomma00
 */
@Named
@SessionScoped
public class StoreBean implements Serializable {

    private static final String INVENTORY_API = "http://localhost:4848/api/store-api/process-transaction";
    // http://localhost:8080/api/store-api/process-transaction
    // Bank api : "http://localhost:8080/e-wallet/api/bank-api/simulate-payment";
    private double budget = 1000.0;  // Store's available budget for purchases
    private double itemPrice;
    private String itemName;
    private int quantity;

    @PostConstruct
    public void init() {
        budget = 1000.0;
        itemName = "";
        quantity = 1;
    }

    public String makePurchase() {
        try {
            double totalCost = itemPrice * quantity;

            if (itemPrice <= 0) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Invalid Price",
                                "Item price must be greater than zero"));
                return null;
            }

            if (quantity <= 0) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Invalid Quantity",
                                "Quantity must be greater than zero"));
                return null;
            }

            if (totalCost > budget) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Insufficient Funds",
                                "Not enough budget for this purchase"));
                return null;
            }

            Transaction transaction = new Transaction();
            transaction.setItemName(itemName);
            transaction.setQuantity(quantity);
            transaction.setItemPrice(itemPrice);
            transaction.setTotalCost(totalCost);
            transaction.setRemainingBudget(budget - totalCost);

            budget -= totalCost;

            Client client = ClientBuilder.newClient();
            Response response = client.target(INVENTORY_API)
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(transaction, MediaType.APPLICATION_JSON));

            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO,
                                "Purchase Successful",
                                "Your purchase was completed successfully"));
            } else {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Error",
                                "Failed to process purchase: " + response.readEntity(String.class)));
            }

            itemPrice = 0;
            itemName = "";
            quantity = 1;

            return null;

        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Error",
                            "An error occurred while processing the purchase: " + e.getMessage()));
            return null;
        }
    }

    public double getBudget() { return budget; }
    public void setBudget(double budget) { this.budget = budget; }

    public double getItemPrice() { return itemPrice; }
    public void setItemPrice(double itemPrice) { this.itemPrice = itemPrice; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
