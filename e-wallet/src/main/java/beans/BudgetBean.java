package beans;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import beans.entities.Budget;
import beans.entities.User;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;

/** 
 * BudgetBean is a managed bean that handles budget-related operations 
 * for a user, including setting total budgets, category budgets, 
 * and retrieving spent amounts.
 * 
 * @author xromang00
 */
@Named
@SessionScoped
public class BudgetBean implements Serializable {
    
    @PersistenceContext
    private EntityManager em;
    
    @Inject
    private UserBean userBean;
    
    private double totalBudget;
    final private Map<String, Double> categoryBudgets = new HashMap<>();
    private String selectedCategory;
    private double categoryAmount;
    
    private static final String[] CATEGORIES = {
        "Food", "Transportation", "Entertainment", "Shopping", 
        "Bills", "Healthcare", "Education", "Other"
    };
    
    /**
     * Sets the total budget for the current user.
     * 
     * @return null if successful, or an error message if the budget is invalid.
     */
    @Transactional
    public String setBudget() {
        User currentUser = userBean.getCurrentUser();
        
        try {
            if (totalBudget <= 0) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Invalid amount", 
                    "Please enter a positive amount for the budget");
                return null;
            }
            
            currentUser.setBudget(totalBudget);
            em.merge(currentUser);
            
            addMessage(FacesMessage.SEVERITY_INFO, "Success", 
                "Budget set successfully to " + totalBudget + " Kč");
            
            return null;
            
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", 
                "An error occurred while setting the budget: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Sets the budget for a specific category for the current user.
     * 
     * @return null if successful, or an error message if the category amount is invalid.
     */
    @Transactional
    public String setCategoryBudget() {
        User currentUser = userBean.getCurrentUser();
        try {
            if (categoryAmount <= 0) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Invalid amount", 
                    "Please enter a positive amount for the category budget");
                return null;
            }
            
            // Find existing budget for this category
            TypedQuery<Budget> query = em.createQuery(
                "SELECT b FROM Budget b WHERE b.user = :user AND b.budgetCategory = :category",
                Budget.class);
            query.setParameter("user", currentUser);
            query.setParameter("category", selectedCategory);
            
            Budget categoryBudget;
            try {
                categoryBudget = query.getSingleResult();
            } catch (Exception e) {
                // Create new budget if none exists
                categoryBudget = new Budget();
                categoryBudget.setBudgetCategory(selectedCategory);
                categoryBudget.setBudgetSpent(0.0);
                categoryBudget.setUser(currentUser);
            }
            
            // Update or set the budget
            categoryBudget.setBudget(categoryAmount);
            
            // Persist changes
            if (categoryBudget.getId() == null) {
                em.persist(categoryBudget);
            } else {
                em.merge(categoryBudget);
            }
            
            // Update the local map for display
            categoryBudgets.put(selectedCategory, categoryAmount);
            
            addMessage(FacesMessage.SEVERITY_INFO, "Success", 
                "Budget for " + selectedCategory + " set to " + categoryAmount + " Kč");
            
            // Reset fields
            selectedCategory = null;
            categoryAmount = 0;
            
            return null;
            
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", 
                "An error occurred while setting the category budget: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Calculates the remaining budget for the current user.
     * 
     * @return the remaining budget amount.
     */
    public double getRemainingBudget(Long userId) {
        User currentUser = userBean.getCurrentUser();
        if (currentUser != null) {
            Double userBudget = currentUser.getBudget();
            if (userBudget != 0) {
                return userBudget - getTotalSpent(userId);
            }
        }
        return 0.0;
    }

    /**
     * Retrieves the total amount spent by the current user for the current month.
     * 
     * @return the total spent amount.
     */
    public double getTotalSpent(Long userId) {
        try {
            User currentUser = userBean.getCurrentUser();
            if (currentUser == null) return 2.0;

            // Get total spent amount for withdrawals and sending money for current month
            TypedQuery<Double> query = em.createQuery(
                "SELECT COALESCE(SUM(CASE " +
                "    WHEN t.type = 'Withdraw' THEN t.value " +
                "    WHEN t.type = 'Transfer' AND t.sender.id = :userId THEN t.value " +
                "    ELSE 0 END), 0) " +
                "FROM Transaction t " +
                "WHERE EXTRACT(MONTH FROM t.transactionDate) = EXTRACT(MONTH FROM CURRENT_DATE) " +
                "AND EXTRACT(YEAR FROM t.transactionDate) = EXTRACT(YEAR FROM CURRENT_DATE) " +
                "AND t.sender.id = :userId", Double.class);
            query.setParameter("userId", currentUser.getId());
            
            return query.getSingleResult();
        } catch (Exception e) {
            return 1.0;
        }
    }
    
    /**
     * Adds a message to the FacesContext for user feedback.
     * 
     * @param severity the severity level of the message.
     * @param summary a brief summary of the message.
     * @param detail detailed information about the message.
     */
    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, 
            new FacesMessage(severity, summary, detail));
    }
    

    /**
     * Gets the total budget.
     * 
     * @return the total budget.
     */
    public double getTotalBudget() {
        return totalBudget;
    }
    
    /**
     * Sets the total budget.
     * 
     * @param totalBudget the total budget to set.
     */
    public void setTotalBudget(double totalBudget) {
        this.totalBudget = totalBudget;
    }
    
    /**
     * Gets the selected category.
     * 
     * @return the selected category.
     */
    public String getSelectedCategory() {
        return selectedCategory;
    }
    
    /**
     * Sets the selected category.
     * 
     * @param selectedCategory the category to set.
     */
    public void setSelectedCategory(String selectedCategory) {
        this.selectedCategory = selectedCategory;
    }
    
    /**
     * Gets the category amount.
     * 
     * @return the category amount.
     */
    public double getCategoryAmount() {
        return categoryAmount;
    }
    
    /**
     * Sets the category amount.
     * 
     * @param categoryAmount the category amount to set.
     */
    public void setCategoryAmount(double categoryAmount) {
        this.categoryAmount = categoryAmount;
    }
    
    /**
     * Gets the available categories.
     * 
     * @return an array of category names.
     */
    public String[] getCategories() {
        return CATEGORIES;
    }
    
    /**
     * Gets the budgets for each category.
     * 
     * @return a map of category names to budget amounts.
     */
    public Map<String, Double> getCategoryBudgets() {
        return categoryBudgets;
    }
    
    /**
     * Initializes the bean by loading the user's existing budgets.
     */
    @PostConstruct
    public void init() {
        User currentUser = userBean.getCurrentUser();
        if (currentUser != null) {
            TypedQuery<Budget> query = em.createQuery(
                "SELECT b FROM Budget b WHERE b.user = :user", Budget.class);
            query.setParameter("user", currentUser);
            
            query.getResultList().forEach(budget -> 
                categoryBudgets.put(budget.getBudgetCategory(), budget.getBudget()));
        }
    }
    
    /**
     * Retrieves the amount spent in a specific category for the current month.
     * 
     * @param category the category to check.
     * @return the amount spent in the specified category.
     */
    @Transactional
    public double getCategorySpent(String category) {
        User currentUser = userBean.getCurrentUser();
        if (currentUser == null) return 0.0;
        
        try {
            // Get spent amount for specific category for current month
            TypedQuery<Double> query = em.createQuery(
                "SELECT COALESCE(SUM(t.value), 0) FROM Transaction t " +
                "WHERE t.sender.id = :userId " +
                "AND (t.type = 'WITHDRAW' OR t.type = 'SEND') " +
                "AND t.category = :category " +
                "AND FUNCTION('YEAR', t.timestamp) = FUNCTION('YEAR', CURRENT_DATE) " +
                "AND FUNCTION('MONTH', t.timestamp) = FUNCTION('MONTH', CURRENT_DATE)", Double.class);
            query.setParameter("userId", currentUser.getId());
            query.setParameter("category", category);
            
            double spentAmount = query.getSingleResult();
            
            // Update the Budget entity with the latest spent amount
            TypedQuery<Budget> budgetQuery = em.createQuery(
                "SELECT b FROM Budget b WHERE b.user = :user AND b.budgetCategory = :category",
                Budget.class);
            budgetQuery.setParameter("user", currentUser);
            budgetQuery.setParameter("category", category);
            
            Budget budget;
            try {
                budget = budgetQuery.getSingleResult();
            } catch (Exception e) {
                budget = new Budget();
                budget.setUser(currentUser);
                budget.setBudgetCategory(category);
                budget.setBudget(0.0);
            }
            
            budget.setBudgetSpent(spentAmount);
            
            if (budget.getId() == null) {
                em.persist(budget);
            } else {
                em.merge(budget);
            }
            
            return spentAmount;
        } catch (Exception e) {
            return 0.0;
        }
    }
}