package beans.transaction.services;

import beans.transaction.api.SimulatedTransaction;
import beans.entities.Transaction;
import beans.entities.User;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;

/**
 * Service class to handle transaction logic.
 * Handles validation, database updates, and transaction processing.
 *
 * @author Arthur PHOMMACHANH - xphomma00
 */
@ApplicationScoped
public class TransactionHandler {

    @PersistenceContext
    private EntityManager em;

    /**
     * Processes a transaction received from the frontend.
     */
    @Transactional
    public void handleTransaction(SimulatedTransaction transaction) {
        validateTransaction(transaction);

        double totalCost = transaction.getItemPrice() * transaction.getQuantity();

        User user = findUserByVariableSymbol(transaction.getUserVariableSymbol());

        checkUserBalance(user, totalCost);

        Transaction newTransaction = createAndPersistTransaction(transaction, user, totalCost);

        updateUserBalance(user, totalCost);
    }

    /**
     * Validates the transaction data.
     */
    private void validateTransaction(SimulatedTransaction transaction) {
        if (transaction.getItemName() == null || transaction.getItemName().isEmpty()) {
            throw new IllegalArgumentException("Item name is required.");
        }
        if (transaction.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }
        if (transaction.getItemPrice() <= 0) {
            throw new IllegalArgumentException("Item price must be greater than zero.");
        }
    }

    /**
     * Fetches the user from the database using their variable symbol
     */
    private User findUserByVariableSymbol(String variableSymbol) {
        User user = em.createQuery("SELECT u FROM User u WHERE u.variableSymbol = :vs", User.class)
                .setParameter("vs", variableSymbol)
                .getSingleResult();

        if (user == null) {
            throw new IllegalArgumentException("User not found with the provided variable symbol.");
        }

        return user;
    }

    /**
     * Checks if the user has sufficient balance for the transaction.
     */
    private void checkUserBalance(User user, double totalCost) {
        if (user.getBalance() < totalCost) {
            throw new IllegalArgumentException("Insufficient balance to complete the transaction.");
        }
    }

    /**
     * Creates and persists a new Transaction record in the database.
     */
    private Transaction createAndPersistTransaction(SimulatedTransaction transaction, User user, double totalCost) {
        Transaction newTransaction = new Transaction();
        newTransaction.setReceiver(user);
        newTransaction.setNameOfSender("Store Simulator");
        newTransaction.setValue(totalCost);
        newTransaction.setType("Purchase");
        newTransaction.setCategory(transaction.getItemName());
        newTransaction.setTransactionDate(LocalDateTime.now());

        em.persist(newTransaction);
        return newTransaction;
    }

    /**
     * Updates the user's balance and persists the changes to the database.
     */
    private void updateUserBalance(User user, double totalCost) {
        user.setBalance(user.getBalance() - totalCost);
        em.merge(user);
    }
}
