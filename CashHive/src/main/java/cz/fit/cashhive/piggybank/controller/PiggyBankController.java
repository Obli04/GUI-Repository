package cz.fit.cashhive.piggybank.controller;

import cz.fit.cashhive.piggybank.dto.PiggyBankCreationDTO;
import cz.fit.cashhive.piggybank.service.PiggyBankService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/piggybank")
public class PiggyBankController {

    private final PiggyBankService piggyBankService;

    public PiggyBankController(PiggyBankService piggyBankService) {
        this.piggyBankService = piggyBankService;
    }

    @GetMapping("/target-amount/{owner}")
    public ResponseEntity<Double> getPiggyBankAmount(@PathVariable @NotNull @NotBlank String owner) {
        var amount = piggyBankService.targetAmountFor(owner);
        return amount.map(ResponseEntity::ok)
                .orElseThrow(() -> new IllegalArgumentException("Piggy bank not found for " + owner));
    }

    @PostMapping("/create")
    public ResponseEntity<String> createPiggyBank(@Valid @RequestBody PiggyBankCreationDTO piggyBankCreationDTO) {
        piggyBankService.createPiggyBank(piggyBankCreationDTO.name(), piggyBankCreationDTO.targetAmount());
        return ResponseEntity.ok("Piggy bank created");
    }

}