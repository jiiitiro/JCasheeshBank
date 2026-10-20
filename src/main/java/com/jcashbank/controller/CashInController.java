package com.jcashbank.controller;

import com.jcashbank.model.Transaction;
import com.jcashbank.model.TransactionType;
import com.jcashbank.model.User;
import com.jcashbank.service.TransactionService;
import com.jcashbank.service.TransactionValidationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@Controller
public class CashInController {
    private final TransactionService transactionService;
    private final TransactionValidationService transactionValidation;
    private final SessionUserProvider sessionUser;

    public CashInController(TransactionService transactionService, TransactionValidationService transactionValidation,
                            SessionUserProvider sessionUser) {
        this.transactionService = transactionService;
        this.transactionValidation = transactionValidation;
        this.sessionUser = sessionUser;
    }

    @GetMapping("/cash-in")
    public String cashInPage(HttpSession session, Model model) {
        model.addAttribute("user", sessionUser.getLoggedInUser(session));
        return "cash-in";
    }

    @PostMapping("/cash-in/confirm")
    public String confirmCashIn(
            @RequestParam BigDecimal amount,
            HttpSession session,
            Model model) {

        User user = sessionUser.getLoggedInUser(session);

        try {
            transactionValidation.validateCashInAmount(amount);

            model.addAttribute("user", user);
            model.addAttribute("amount", amount);

            return "confirm-cash-in";

        } catch (Exception ex) {
            model.addAttribute("user", user);
            model.addAttribute("error", ex.getMessage() != null ? ex.getMessage() : "Invalid cash in amount.");
            return "cash-in";
        }
    }

    @PostMapping("/cash-in/execute")
    public String executeCashIn(
            @RequestParam BigDecimal amount,
            HttpSession session,
            Model model) {

        User user = sessionUser.getLoggedInUser(session);

        try {
            Transaction transaction = transactionService.cashIn(user.getId(), amount);

            // Redirect so refreshing the receipt doesn't resubmit the cash-in
            return "redirect:/cash-in/receipt/" + transaction.getId();

        } catch (Exception ex) {
            model.addAttribute("user", user);
            model.addAttribute("amount", amount);
            model.addAttribute("error", ex.getMessage() != null ? ex.getMessage() : "Cash in failed.");
            return "confirm-cash-in";
        }
    }

    @GetMapping("/cash-in/receipt/{id}")
    public String cashInReceipt(@PathVariable Long id, HttpSession session, Model model) {
        User user = sessionUser.getLoggedInUser(session);
        Transaction transaction = transactionService.getUserTransaction(user.getId(), id, TransactionType.CASH_IN);

        model.addAttribute("user", user);
        model.addAttribute("transaction", transaction);
        model.addAttribute("amount", transaction.getAmount());

        return "cash-in-receipt";
    }
}
