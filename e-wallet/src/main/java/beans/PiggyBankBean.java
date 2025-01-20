package beans;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import beans.entities.User;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Named
@SessionScoped
public class PiggyBankBean implements Serializable {

    @PersistenceContext
    private EntityManager em;

    private double addAmount = 0.0;
    private double withdrawAmount = 0.0;
    private double savingGoalAmount = 0.0;
    private LocalDateTime lockEndDate;
    private long lockDurationInDays;
    private double piggyBank = 0.0;


    @Inject
    private UserBean userBean;

    public double getAddAmount() {
        return addAmount;
    }

    public void setAddAmount(double addAmount) {
        this.addAmount = addAmount;
    }

    public double getWithdrawAmount() {
        return withdrawAmount;
    }

    public void setWithdrawAmount(double withdrawAmount) {
        this.withdrawAmount = withdrawAmount;
    }

    public boolean isFundsLocked() {
        User currentUser = userBean.getCurrentUser();
        return currentUser != null && lockEndDate != null && LocalDateTime.now().isBefore(lockEndDate);
    }

    public double getPiggyBank() {
        User currentUser = userBean.getCurrentUser();
        return currentUser != null ? currentUser.getPiggyBank() : 0.0;
    }

    @Transactional
    public void setPiggyBank(double piggyBank) {
        User currentUser = userBean.getCurrentUser();
        if (currentUser != null) {
            currentUser.setPiggyBank(piggyBank);
            saveUser(currentUser);
        }
    }

    @Transactional
    public void addToBalance() {
        if (addAmount <= 0) {
            throw new IllegalArgumentException("Amount should be positive");
        }
        User currentUser = userBean.getCurrentUser();
        if (currentUser != null) {
            double currentPiggyBank = currentUser.getPiggyBank();
            double currentBalance = currentUser.getBalance();
            if(currentBalance < addAmount) {
                throw new IllegalStateException("Insufficient funds");
            }   
            currentUser.setPiggyBank(currentPiggyBank + addAmount);
            currentUser.setBalance(currentBalance - addAmount);
            saveUser(currentUser);
        }
    }

    public double getSavingGoalAmount() {
        User currentUser = userBean.getCurrentUser();
        return currentUser != null ? currentUser.getBudget() : 0.0;
    }

    public void setSavingGoalAmount(double savingGoal) {
        if (savingGoal <= 0) {
            throw new IllegalArgumentException("Saving goal should be positive");
        }
        User currentUser = userBean.getCurrentUser();
        if (currentUser != null) {
            currentUser.setPiggyBank(savingGoal);
            saveUser(currentUser);
        }
    }

    public double getRemainingGoal() {
        User currentUser = userBean.getCurrentUser();
        if (currentUser != null) {
            return Math.max(currentUser.getBudget() - currentUser.getPiggyBank(), 0);
        }
        return 0.0;
    }

    public LocalDateTime getLockEndDate() {
        return lockEndDate;
    }

    public void setLockEndDate(LocalDateTime lockEndDate) {
        Objects.requireNonNull(lockEndDate);
        if (lockEndDate.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Lock date cannot be in the past");
        }
        this.lockEndDate = lockEndDate;
        User currentUser = userBean.getCurrentUser();
        saveUser(currentUser);
    }

    public long getLockDurationInDays() {
        if (lockEndDate != null) {
            return ChronoUnit.DAYS.between(LocalDateTime.now(), lockEndDate);
        }
        return 0;
    }

    @Transactional
    public void withdrawFromPiggyBank() {
        if (withdrawAmount <= 0) {
            throw new IllegalArgumentException("Amount should be positive");
        }
        User currentUser = userBean.getCurrentUser();
        if (currentUser != null) {
            if (lockEndDate != null && LocalDateTime.now().isBefore(lockEndDate)) {
                throw new IllegalStateException("Piggy bank is locked");
            }
            if (currentUser.getPiggyBank() < withdrawAmount) {
                throw new IllegalStateException("Insufficient funds");
            }
            double currentPiggyBank = currentUser.getPiggyBank();
            currentUser.setPiggyBank(currentPiggyBank - withdrawAmount);
            double currentBalance = currentUser.getBalance();
            currentUser.setBalance(currentBalance + withdrawAmount);
            saveUser(currentUser);
        }
    }

    private void reloadPage() {
    }

    private void saveUser(User user) {
        em.merge(user); // Merge the changes with the user
        em.flush(); // Execute changes
    }
}
