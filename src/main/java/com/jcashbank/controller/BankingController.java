package com.jcashbank.controller;

import com.jcashbank.exception.BankingException;
import com.jcashbank.model.User;
import com.jcashbank.service.TransactionService;
import com.jcashbank.service.UserService;
import jakarta.servlet.http.HttpSession;
//import jakarta.validation.constraints.NotBlank;
//import jakarta.validation.constraints.Pattern;
//import jakarta.validation.constraints.Positive;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Controller
public class BankingController {
    private static final String SESSION_USER_ID = "loggedInUserId";
    private static final String SESSION_LOGIN_ATTEMPTS = "loginAttempts";
    private static final int MAX_LOGIN_ATTEMPTS = 3;

    private final UserService userService;
    private final TransactionService transactionService;

    public BankingController(UserService userService, TransactionService transactionService) {
        this.userService = userService;
        this.transactionService = transactionService;
    }

    @GetMapping({"/", "/login"})
    public String loginPage(HttpSession session) {
        if (session.getAttribute(SESSION_USER_ID) != null) {
            return "redirect:/dashboard";
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(
            @RequestParam String mobileNumber,
            @RequestParam String pin,
            HttpSession session,
            Model model) {

        int attempts = getLoginAttempts(session);
        if (attempts >= MAX_LOGIN_ATTEMPTS) {
            model.addAttribute("error", "Maximum of 3 login attempts reached. Please start a new session.");
            return "login";
        }

        try {
            User user = userService.authenticate(mobileNumber.trim(), pin.trim());
            session.setAttribute(SESSION_USER_ID, user.getId());
            session.setAttribute(SESSION_LOGIN_ATTEMPTS, 0);
            return "redirect:/dashboard";
        } catch (BankingException ex) {
            attempts++;
            session.setAttribute(SESSION_LOGIN_ATTEMPTS, attempts);
            int remaining = MAX_LOGIN_ATTEMPTS - attempts;
            model.addAttribute("error", ex.getMessage() + (remaining > 0 ? " Attempts remaining: " + remaining : ""));
            return "login";
        }
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User user = getLoggedInUser(session);
        model.addAttribute("user", user);
        model.addAttribute("transactions", transactionService.getTransactions(user.getId()));
        return "dashboard";
    }

    @GetMapping("/cash-in")
    public String cashInPage(HttpSession session, Model model) {
        model.addAttribute("user", getLoggedInUser(session));
        return "cash-in";
    }

    @PostMapping("/cash-in")
    public String cashIn(
            @RequestParam BigDecimal amount,
            HttpSession session,
            Model model) {
        User user = getLoggedInUser(session);
        try {
            transactionService.cashIn(user.getId(), amount);
            return "redirect:/dashboard?success=Cash-in completed successfully.";
        } catch (BankingException | IllegalArgumentException ex) {
            model.addAttribute("user", userService.findById(user.getId()));
            model.addAttribute("error", ex.getMessage());
            return "cash-in";
        }
    }

    @GetMapping("/transfer")
    public String transferPage(HttpSession session, Model model) {
        model.addAttribute("user", getLoggedInUser(session));
        return "transfer";
    }

    @PostMapping("/transfer")
    public String transfer(
            @RequestParam String receiverMobileNumber,
            @RequestParam BigDecimal amount,
            HttpSession session,
            Model model) {
        User sender = getLoggedInUser(session);
        try {
            transactionService.transfer(sender.getId(), receiverMobileNumber.trim(), amount);
            return "redirect:/dashboard?success=Transfer completed successfully.";
        } catch (BankingException | IllegalArgumentException ex) {
            model.addAttribute("user", userService.findById(sender.getId()));
            model.addAttribute("error", ex.getMessage());
            return "transfer";
        }
    }

    @GetMapping("/transactions")
    public String transactions(HttpSession session, Model model) {
        User user = getLoggedInUser(session);
        model.addAttribute("user", user);
        model.addAttribute("transactions", transactionService.getTransactions(user.getId()));
        return "transactions";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    private User getLoggedInUser(HttpSession session) {
        Object id = session.getAttribute(SESSION_USER_ID);
        if (!(id instanceof Long userId)) {
            throw new BankingException("Please log in first.");
        }
        return userService.findById(userId);
    }

    private int getLoginAttempts(HttpSession session) {
        Object value = session.getAttribute(SESSION_LOGIN_ATTEMPTS);
        return value instanceof Integer ? (Integer) value : 0;
    }
}
