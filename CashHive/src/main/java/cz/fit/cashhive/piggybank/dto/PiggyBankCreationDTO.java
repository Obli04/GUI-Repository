package cz.fit.cashhive.piggybank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PiggyBankCreationDTO(
        @NotNull @NotBlank String name,
        @Positive double targetAmount
) {}