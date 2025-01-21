package beans;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import beans.entities.Transaction;
import beans.entities.User;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
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
        return currentUser != null && currentUser.getLockoutEndTime() != null &&
                LocalDateTime.now().isBefore(currentUser.getLockoutEndTime());
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

    public double getPiggyBankGoal() {
        return userBean.getCurrentUser().getPiggyBankGoal();
    }

    public double getSavingGoalAmount() {
        return userBean.getCurrentUser().getPiggyBankGoal();
    }

    @Transactional
    public void setSavingGoalAmount(double savingGoal) {
        if (savingGoal <= 0) {
            throw new IllegalArgumentException("Saving goal should be positive.");
        }
        User currentUser = userBean.getCurrentUser();
        if (currentUser != null) {
            currentUser.setPiggyBankGoal(savingGoal);
            saveUser(currentUser);

            userBean.refreshBalance();
        }
    }


    public double getRemainingGoal() {
        return Math.max(userBean.getCurrentUser().getPiggyBankGoal() - userBean.getCurrentUser().getPiggyBank(), 0);
    }


    public LocalDateTime getLockEndDate() {
        return lockEndDate;
    }

    public void setLockEndDate(LocalDateTime lockEndDate) {
        this.lockEndDate = lockEndDate;
    }

    public long getLockDurationInDays() {
        User currentUser = userBean.getCurrentUser();
        if (currentUser != null && currentUser.getLockoutEndTime() != null) {
            return ChronoUnit.DAYS.between(LocalDateTime.now(), currentUser.getLockoutEndTime());
        }
        return 0;
    }

    @Transactional
    public void activateLock() {
        if (lockEndDate == null) {
            throw new IllegalArgumentException("You must select a lock end date.");
        }
        if (lockEndDate.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Lock end date must be in the future.");
        }

        User currentUser = userBean.getCurrentUser();
        if (currentUser != null) {
            currentUser.setLockoutEndTime(lockEndDate);
            saveUser(currentUser);
        }

        // Reset lockEndDate for next use
        lockEndDate = null;
    }


    @Transactional
    public void withdrawFromPiggyBank() {
        if (withdrawAmount <= 0) {
            throw new IllegalArgumentException("Amount should be positive");
        }
        User currentUser = userBean.getCurrentUser();
        if (currentUser != null) {
            if (isFundsLocked()) {
                throw new IllegalStateException("Piggy bank is locked");
            }
            if (currentUser.getPiggyBank() < withdrawAmount) {
                throw new IllegalStateException("Insufficient funds");
            }
            double currentPiggyBank = currentUser.getPiggyBank();
            currentUser.setPiggyBank(currentPiggyBank - withdrawAmount);
            double currentBalance = currentUser.getBalance();
            currentUser.setBalance(currentBalance + withdrawAmount);

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

    @Transactional
    public void unlockPiggyBank() {
        User currentUser = userBean.getCurrentUser();
        if (currentUser != null) {
            // Clear the lockoutEndTime to unlock the piggy bank
            currentUser.setLockoutEndTime(null);
            saveUser(currentUser);
        }
    }

    @Transactional
    public void breakPiggyBank() {
        User currentUser = userBean.getCurrentUser();

        if (currentUser != null) {
            if (isFundsLocked()) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Piggy Bank Locked",
                                "The piggy bank is locked until " + getFormattedLockEndDate()));
                return;
            }

            double piggyBankAmount = currentUser.getPiggyBank();

            if (piggyBankAmount > 0) {
                currentUser.setBalance(currentUser.getBalance() + piggyBankAmount);

                currentUser.setPiggyBank(0.0);
                currentUser.setPiggyBankGoal(0.0);

                Transaction transaction = new Transaction();
                transaction.setSender(currentUser);
                transaction.setReceiver(currentUser);
                transaction.setType("Break Piggy Bank");
                transaction.setCategory("Emergency");
                transaction.setValue(piggyBankAmount);
                transaction.setTransactionDate(LocalDateTime.now());

                em.persist(transaction);

                saveUser(currentUser);

                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO,
                                "Piggy Bank Broken",
                                "Funds transferred to your balance. Piggy bank and savings goal reset."));
            } else {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN,
                                "Empty Piggy Bank",
                                "The piggy bank is already empty."));
            }
        }
    }

    public String getFormattedLockEndDate() {
        User currentUser = userBean.getCurrentUser();
        if (currentUser != null && currentUser.getLockoutEndTime() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
            return currentUser.getLockoutEndTime().format(formatter);
        }
        return "N/A"; // Ensure it's always a string
    }


    public String getLockStatusTitle() {
        if (isFundsLocked()) {
            return "Piggy bank is locked until " + getFormattedLockEndDate();
        }
        return "";
    }

    private void saveUser(User user) {
        em.merge(user);
        em.flush();
    }
}
