package cz.fit.cashhive.transaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record TransactionDTO(
        @NotBlank String sender,
        @NotBlank String receiver,
        @Positive double amount,
        @NotBlank String reason
) {}
