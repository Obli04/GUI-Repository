package beans;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import beans.entities.Transaction;
import beans.entities.User;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

/**
 * PiggyBankBean is a managed bean that handles operations related to the piggy bank feature.
 * This includes adding funds to the piggy bank, withdrawing funds, setting and managing savings goals,
 * and enforcing lock periods for funds in the piggy bank.
 *
 * @author Arthur PHOMMACHANH - xphomma00
 */

@Named
@SessionScoped
public class PiggyBankBean implements Serializable {

    @PersistenceContext
    private EntityManager em;

    private double addAmount = 0.0;
    private double withdrawAmount = 0.0;
    private LocalDateTime lockEndDate;


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
            if (currentBalance < addAmount) {
                throw new IllegalStateException("Insufficient funds");
            }
            currentUser.setPiggyBank(currentPiggyBank + addAmount);
            currentUser.setBalance(currentBalance - addAmount);

            Transaction transaction = new Transaction();
            transaction.setReceiver(currentUser);
            transaction.setType("Deposit");
            transaction.setCategory("Piggy Bank Deposit");
            transaction.setValue(addAmount);
            transaction.setTransactionDate(LocalDateTime.now());

            em.persist(transaction);
            saveUser(currentUser);

        }
    }


    public double getSavingGoalAmount() {
        return userBean.getCurrentUser().getPiggyBankGoal();
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
        return Math.max(userBean.getCurrentUser().getPiggyBankGoal() - userBean.getCurrentUser().getPiggyBank(), 0);
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

            // Create a transaction for the withdrawal
            Transaction transaction = new Transaction();
            transaction.setSender(null);
            transaction.setReceiver(currentUser);
            transaction.setValue(withdrawAmount);
            transaction.setType("Withdraw");
            transaction.setCategory("Piggy Bank Withdraw");
            transaction.setTransactionDate(LocalDateTime.now());

            em.persist(transaction);
            saveUser(currentUser);
        }
    }

    private void saveUser(User user) {
        em.merge(user); // Merge the changes with the user
        em.flush(); // Execute changes
    }
}
