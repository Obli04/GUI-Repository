package beans.transaction.services;

/**
 * Service class for inventory management.
 * This could simulate stock tracking or interact with a real database.
 *
 * @author Arthur PHOMMACHANH - xphomma00
 */
public class InventoryService {

    public boolean isItemInStock(String itemName, int quantity) {
        // Simulate checking inventory
        System.out.println("Checking stock for item: " + itemName);
        return true; // Assume all items are in stock for this simulation
    }
}
