package beans.transaction.api;

/**
 * DTO for mapping transaction data received from the frontend.
 *
 * @author Arthur PHOMMACHANH - xphomma00
 */
public class SimulatedTransaction {
    private String itemName;
    private double itemPrice;
    private String userVariableSymbol;
    private int quantity;

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public double getItemPrice() { return itemPrice; }
    public void setItemPrice(double itemPrice) { this.itemPrice = itemPrice; }

    public String getUserVariableSymbol() { return userVariableSymbol; }
    public void setUserVariableSymbol(String userVariableSymbol) { this.userVariableSymbol = userVariableSymbol; }

    public int getQuantity() { // Updated to return 'int' (correct type)
        return quantity;
    }
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
