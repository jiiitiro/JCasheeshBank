package com.jcashbank.controller;

import com.jcashbank.exception.BankingException;
import com.jcashbank.model.Transaction;
import com.jcashbank.model.TransactionType;
import com.jcashbank.model.User;
import com.jcashbank.service.TransactionService;
import com.jcashbank.service.UserValidationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@Controller
public class TransferController {
    private final TransactionService transactionService;
    private final UserValidationService userValidation;
    private final SessionUserProvider sessionUser;

    public TransferController(TransactionService transactionService, UserValidationService userValidation,
                              SessionUserProvider sessionUser) {
        this.transactionService = transactionService;
        this.userValidation = userValidation;
        this.sessionUser = sessionUser;
    }

    @GetMapping("/transfer")
    public String transferPage(HttpSession session, Model model) {
        model.addAttribute("user", sessionUser.getLoggedInUser(session));
        return "transfer";
    }

    @PostMapping("/transfer/confirm")
    public String showConfirmation(
            @RequestParam String recipientMobile,
            @RequestParam BigDecimal amount,
            HttpSession session,
            Model model) {

        User sender = sessionUser.getLoggedInUser(session);

        try {
            // Check if user is attempting to send to themselves
            if (sender.getMobileNumber().equals(recipientMobile.trim())) {
                throw new BankingException("You cannot transfer money to your own account.");
            }

            // Check balance before confirmation
            if (sender.getBalance().compareTo(amount) < 0) {
                throw new BankingException("Insufficient balance.");
            }

            // Validate recipient exists & fetch masked name
            String maskedName = userValidation.getMaskedNameByMobile(recipientMobile.trim());

            model.addAttribute("user", sender);
            model.addAttribute("recipientName", maskedName);
            model.addAttribute("recipientMobile", recipientMobile.trim());
            model.addAttribute("amount", amount);

            return "confirm-transfer";

        } catch (Exception ex) {
            // Route errors back to transfer.html instead of uncaught exception view
            model.addAttribute("user", sender);
            model.addAttribute("error", ex.getMessage() != null ? ex.getMessage() : "Invalid transfer details.");
            return "transfer";
        }
    }

    @PostMapping("/transfer/execute")
    public String executeTransfer(
            @RequestParam String recipientMobile,
            @RequestParam BigDecimal amount,
            HttpSession session,
            Model model) {

        User sender = sessionUser.getLoggedInUser(session);

        try {
            Transaction transaction = transactionService.transfer(
                    sender.getId(),
                    recipientMobile.trim(),
                    amount
            );

            // Redirect so refreshing the receipt doesn't resubmit the transfer
            return "redirect:/transfer/receipt/" + transaction.getId();

        } catch (Exception ex) {
            model.addAttribute("user", sender);
            model.addAttribute("recipientName", userValidation.getMaskedNameByMobile(recipientMobile.trim()));
            model.addAttribute("recipientMobile", recipientMobile.trim());
            model.addAttribute("amount", amount);
            model.addAttribute("error", ex.getMessage() != null ? ex.getMessage() : "Transfer failed.");
            return "confirm-transfer";
        }
    }

    @GetMapping("/transfer/receipt/{id}")
    public String transferReceipt(@PathVariable Long id, HttpSession session, Model model) {
        User sender = sessionUser.getLoggedInUser(session);
        Transaction transaction = transactionService.getUserTransaction(sender.getId(), id, TransactionType.TRANSFER_SENT);
        String recipientMobile = transactionService.getTransferRecipientMobile(transaction);

        model.addAttribute("transaction", transaction);
        model.addAttribute("recipientName", userValidation.getMaskedNameByMobile(recipientMobile));
        model.addAttribute("recipientMobile", recipientMobile);

        return "transaction-receipt";
    }
}
