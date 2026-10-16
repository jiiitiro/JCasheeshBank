package com.jcashbank.controller;

import com.jcashbank.exception.BankingException;
import com.jcashbank.model.Transaction;
import com.jcashbank.model.User;
import com.jcashbank.service.TransactionService;
import com.jcashbank.service.UserService;
import com.jcashbank.service.UserValidationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Controller
public class BankingController {
    private static final String SESSION_USER_ID = "loggedInUserId";
    private static final String SESSION_LOGIN_ATTEMPTS = "loginAttempts";

    private final UserService userService;
    private final TransactionService transactionService;
    private final UserValidationService userValidation;

    public BankingController(UserService userService, TransactionService transactionService, UserValidationService userValidation) {
        this.userService = userService;
        this.transactionService = transactionService;
        this.userValidation = userValidation;
    }

    @GetMapping({"/", "/login"})
    public String loginPage(
            HttpSession session,
            Model model,
            @RequestParam(required = false) String success) {

        // If already logged in, go straight to dashboard
        if (session.getAttribute(SESSION_USER_ID) != null) {
            return "redirect:/dashboard";
        }

        if (success != null && !success.isEmpty()) {
            model.addAttribute("success", success);
        }

        return "login";
    }

    @PostMapping("/login")
    public String login(
            @RequestParam String mobileNumber,
            @RequestParam String pin,
            HttpSession session,
            Model model) {

        try {
            // Authenticate directly against DB via UserService
            User user = userService.authenticate(mobileNumber.trim(), pin.trim());

            // Store active session user ID
            session.setAttribute(SESSION_USER_ID, user.getId());

            return "redirect:/dashboard";

        } catch (BankingException ex) {
            // Display exact dynamic message from UserService (e.g. remaining attempts or actual email lockout)
            model.addAttribute("error", ex.getMessage());
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

    @PostMapping("/cash-in/confirm")
    public String confirmCashIn(
            @RequestParam BigDecimal amount,
            HttpSession session,
            Model model) {

        User user = getLoggedInUser(session);

        try {
            if (amount == null || amount.compareTo(new BigDecimal("5.00")) < 0 || amount.compareTo(new BigDecimal("10000.00")) > 0) {
                throw new BankingException("Cash in amount must be between ₱5.00 and ₱10,000.00.");
            }

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

        User user = getLoggedInUser(session);

        try {
            Transaction transaction = transactionService.cashIn(user.getId(), amount);

            model.addAttribute("user", userService.findById(user.getId()));
            model.addAttribute("transaction", transaction);
            model.addAttribute("amount", amount);

            return "cash-in-receipt";

        } catch (Exception ex) {
            model.addAttribute("user", user);
            model.addAttribute("amount", amount);
            model.addAttribute("error", ex.getMessage() != null ? ex.getMessage() : "Cash in failed.");
            return "confirm-cash-in";
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

    @GetMapping("/forgot-pin")
    public String forgotPinPage() {
        return "forgot-pin";
    }

    @PostMapping("/forgot-pin")
    public String processForgotPin(@RequestParam String email, Model model) {
        try {
            userService.processForgotPassword(email.trim());
            model.addAttribute("success", "Password reset instructions have been sent to your email.");
        } catch (BankingException ex) {
            model.addAttribute("error", ex.getMessage());
        }
        return "forgot-pin";
    }

    @GetMapping("/reset-pin")
    public String resetPinPage(@RequestParam String token, Model model) {
        model.addAttribute("token", token);
        return "reset-pin";
    }

    @PostMapping("/reset-pin")
    public String processResetPin(
            @RequestParam String token,
            @RequestParam String newPin,
            HttpSession session, // 👈 Inject HttpSession
            Model model) {
        try {
            userService.resetPin(token, newPin);

            // Clear session failure counter so user gets fresh 3 attempts
            session.removeAttribute("failedLoginAttempts");

            return "redirect:/login?success=PIN successfully reset. You can now log in.";
        } catch (BankingException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("token", token);
            return "reset-pin";
        }
    }


    @GetMapping("/register")
    public String registerPage(HttpSession session) {
        if (session.getAttribute(SESSION_USER_ID) != null) {
            return "redirect:/dashboard";
        }
        return "register";
    }

    @PostMapping("/register")
    public String register(
            @RequestParam String mobileNumber,
            @RequestParam String email,
            @RequestParam String fullName,
            @RequestParam String pin,
            Model model) {
        try {
            User user = new User();
            user.setMobileNumber(mobileNumber.trim());
            user.setEmail(email.trim());
            user.setFullName(fullName.trim());

            userService.registerUser(user, pin.trim());

            return "redirect:/login?success=Registration successful! Please check your email to verify your account before logging in.";
        } catch (Exception ex) {
            String errorMessage = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();

            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "An unexpected error occurred during registration.";
            }

            model.addAttribute("error", errorMessage);
            return "login";
        }
    }

    @GetMapping("/verify")
    public String verifyEmail(@RequestParam String token, Model model) {
        try {
            userService.verifyEmail(token);
            model.addAttribute("success", "Email verified successfully! You can now log in to your account.");
        } catch (BankingException ex) {
            model.addAttribute("error", ex.getMessage());
        }
        return "login";
    }

    // --- EXPRESS SEND FLOW ---

    @PostMapping("/transfer/confirm")
    public String showConfirmation(
            @RequestParam String recipientMobile,
            @RequestParam BigDecimal amount,
            HttpSession session,
            Model model) {

        User sender = getLoggedInUser(session);

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

        User sender = getLoggedInUser(session);

        try {
            Transaction transaction = transactionService.transfer(
                    sender.getId(),
                    recipientMobile.trim(),
                    amount
            );

            model.addAttribute("transaction", transaction);
            model.addAttribute("recipientName", userValidation.getMaskedNameByMobile(recipientMobile.trim()));
            model.addAttribute("recipientMobile", recipientMobile.trim());

            return "transaction-receipt";

        } catch (Exception ex) {
            model.addAttribute("user", sender);
            model.addAttribute("recipientName", userValidation.getMaskedNameByMobile(recipientMobile.trim()));
            model.addAttribute("recipientMobile", recipientMobile.trim());
            model.addAttribute("amount", amount);
            model.addAttribute("error", ex.getMessage() != null ? ex.getMessage() : "Transfer failed.");
            return "confirm-transfer";
        }
    }
}
