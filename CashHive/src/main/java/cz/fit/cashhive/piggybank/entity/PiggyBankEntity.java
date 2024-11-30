package cz.fit.cashhive.piggybank.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "piggybank")
public class PiggyBankEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(nullable = false)
    private Long id;

    @NotNull
    @NotBlank
    private String owner;

    @Positive
    private double targetAmount;

    @PositiveOrZero
    private double currentAmount;

    public PiggyBankEntity() {
        // For Hibernate
    }

    public PiggyBankEntity(String owner, double targetAmount, double currentAmount) {
        this.owner = owner;
        this.targetAmount = targetAmount;
        this.currentAmount = currentAmount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(Double targetAmount) {
        this.targetAmount = targetAmount;
    }

    public Double getCurrentAmount() {
        return currentAmount;
    }

    public void setCurrentAmount(Double currentAmount) {
        this.currentAmount = currentAmount;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }
}
