package com.jcashbank.controller;

import com.jcashbank.model.User;
import com.jcashbank.service.TransactionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {
    private final TransactionService transactionService;
    private final SessionUserProvider sessionUser;

    public DashboardController(TransactionService transactionService, SessionUserProvider sessionUser) {
        this.transactionService = transactionService;
        this.sessionUser = sessionUser;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User user = sessionUser.getLoggedInUser(session);
        model.addAttribute("user", user);
        model.addAttribute("transactions", transactionService.getTransactions(user.getId()));
        return "dashboard";
    }

    @GetMapping("/transactions")
    public String transactions(HttpSession session, Model model) {
        User user = sessionUser.getLoggedInUser(session);
        model.addAttribute("user", user);
        model.addAttribute("transactions", transactionService.getTransactions(user.getId()));
        return "transactions";
    }
}
