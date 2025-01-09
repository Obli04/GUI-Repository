package beans.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_sender", nullable = true)
    private User sender;

    @Column(name = "name_of_sender", nullable = true)
    private String nameOfSender;

    @ManyToOne
    @JoinColumn(name = "id_receiver", nullable = false)
    private User receiver;

    @Column(name="value", nullable = false)
    private double value;

    @Column(name="type", nullable = false)
    private String type; // e.g., Withdraw, Deposit, Send, Receive

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(name="category", nullable=true)
    private String category; // e.g., Payments, Utilities, Food, Travel, Shopping

    @Column(name="id_receiver", nullable=true)
    private Long id_receiver;
    
    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public String getNameOfSender() {
        return nameOfSender;
    }

    public void setNameOfSender(String nameOfSender) {
        this.nameOfSender = nameOfSender;
    }

    public User getReceiver() {
        return receiver;
    }

    public void setReceiver(User receiver) {
        this.receiver = receiver;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Long getId_receiver() {
        return id_receiver;
    }

    public void setId_receiver(Long id_receiver) {
        this.id_receiver = id_receiver;
    }
} 