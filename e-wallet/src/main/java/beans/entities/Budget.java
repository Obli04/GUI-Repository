package beans.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Budget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_user", nullable = false)
    private User user;

    @Column(name = "budget", nullable = false)
    private Double budget = 0.0;

    @Column(name = "budget_spent", nullable = false)
    private Double budgetSpent = 0.0;

    @Column(name = "budget_category", nullable = false)
    private String budgetCategory;

    // Getters and setters
    public Long getId() {
        return id;
    }

    public Double getBudget() {
        return budget;
    }

    public void setBudget(Double budget) {
        this.budget = budget;
    }

    public Double getBudgetSpent() {
        return budgetSpent;
    }

    public void setBudgetSpent(Double budgetSpent) {
        this.budgetSpent = budgetSpent;
    }

    public String getBudgetCategory() {
        return budgetCategory;
    }

    public void setBudgetCategory(String budgetCategory) {
        this.budgetCategory = budgetCategory;
    }
}
