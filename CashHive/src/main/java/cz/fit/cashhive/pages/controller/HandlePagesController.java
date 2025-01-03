package cz.fit.cashhive.pages.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HandlePagesController {

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalUsers", 123);
        model.addAttribute("activeProjects", 5);
        model.addAttribute("pendingTasks", 8);
        return "dashboard";
    }

    @GetMapping("/profile")
    public String profile() {
        return "profile";
    }

    @GetMapping("/settings")
    public String settings() {
        return "settings";
    }

    @GetMapping("/transaction")
    public String transactionPage(Model model) {
        return "transaction";
    }
}

