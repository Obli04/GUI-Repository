package cz.fit.cashhive.piggybank.repository;

import cz.fit.cashhive.piggybank.entity.PiggyBankEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PiggyBankRepository extends JpaRepository<PiggyBankEntity, Long> {

    Optional<PiggyBankEntity> findByOwner(@NotNull @NotBlank String owner);

}
