package cz.fit.cashhive.piggybank.service;

import cz.fit.cashhive.piggybank.entity.PiggyBankEntity;
import cz.fit.cashhive.piggybank.repository.PiggyBankRepository;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
public final class PiggyBankService {

    private final PiggyBankRepository piggyBankRepository;

    public PiggyBankService(PiggyBankRepository piggyBankRepository) {
        this.piggyBankRepository = piggyBankRepository;
    }

    public void createPiggyBank(String owner, double targetAmount){
        Objects.requireNonNull(owner);
        if(targetAmount <= 0){
            throw new IllegalArgumentException("Target amount must be greater than 0");
        }
        if(piggyBankRepository.findByOwner(owner).isPresent()){
            throw new IllegalArgumentException("Piggy bank already exists");
        }
        var piggyBank = new PiggyBankEntity(owner, targetAmount, 0.0);
        piggyBankRepository.save(piggyBank);
    }

    public Optional<Double> targetAmountFor(String owner){
        Objects.requireNonNull(owner);
        return piggyBankRepository.findByOwner(owner)
                .map(PiggyBankEntity::getTargetAmount);
    }

}
