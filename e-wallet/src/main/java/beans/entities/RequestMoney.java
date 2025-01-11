package beans.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * Represents a request for money between users.
 * Contains information about the sender, receiver, value, and description.
 * 
 * @author Davide Scaccia
 */
@Entity
public class RequestMoney {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_sender", nullable = false)
    private User sender;

    @ManyToOne
    @JoinColumn(name = "id_receiver", nullable = false)
    private User receiver;

    @Column(name = "value", nullable = false)
    private Double value;

    @Column(name = "description", nullable = true)
    private String description;

    // Getters and setters
    public Long getId() {
        return id;
    }

    public User getSender() {
        return sender;
    }

    public User getReceiver() {
        return receiver;
    }

    public Double getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public void setReceiver(User receiver) {
        this.receiver = receiver;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }
}
