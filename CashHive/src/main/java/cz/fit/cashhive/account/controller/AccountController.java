package cz.fit.cashhive.account.controller;

import cz.fit.cashhive.account.service.AccountService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/account")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/balance")
    public String getBalance(Model model) {
        double balance = accountService.getCurrentBalance();
        model.addAttribute("balance", balance);
        return "account-balance";
    }
}
