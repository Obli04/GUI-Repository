package cz.fit.cashhive.transaction.controller;

import cz.fit.cashhive.transaction.dto.TransactionDTO;
import cz.fit.cashhive.transaction.entity.TransactionEntity;
import cz.fit.cashhive.transaction.service.TransactionService;
import cz.fit.cashhive.account.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final AccountService accountService;

    public TransactionController(TransactionService transactionService, AccountService accountService) {
        this.transactionService = transactionService;
        this.accountService = accountService;
    }

    @GetMapping("/send")
    public String sendMoneyPage() {
        return "send-transaction";
    }

    @PostMapping("/send")
    public String sendMoney(@Valid @ModelAttribute TransactionDTO transactionDTO, Model model) {
        try {
            transactionService.sendMoney(transactionDTO);
            model.addAttribute("message", "Transaction completed successfully!");
        } catch (Exception e) {
            model.addAttribute("error", "Failed to send money: " + e.getMessage());
        }
        return "send-transaction";
    }

    @GetMapping("/sent/{sender}")
    public String getSentTransactions(@PathVariable String sender, Model model) {
        List<TransactionEntity> sentTransactions = transactionService.getTransactionsBySender(sender);
        model.addAttribute("transactions", sentTransactions);
        model.addAttribute("title", "Sent Transactions");
        return "transaction-list";
    }

    @GetMapping("/received/{receiver}")
    public String getReceivedTransactions(@PathVariable String receiver, Model model) {
        List<TransactionEntity> receivedTransactions = transactionService.getTransactionsByReceiver(receiver);
        model.addAttribute("transactions", receivedTransactions);
        model.addAttribute("title", "Received Transactions");
        return "transaction-list";
    }

    @GetMapping("/transaction")
    public String transactionPage(Model model) {
        double balance = accountService.getCurrentBalance();
        model.addAttribute("balance", balance);
        return "transaction";
    }
}