package com.jcashbank.controller;

import com.jcashbank.exception.BankingException;
import com.jcashbank.model.User;
import com.jcashbank.service.TransactionService;
import com.jcashbank.service.UserService;
import jakarta.servlet.http.HttpSession;
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
    public String loginPage(
            HttpSession session,
            Model model,
            @RequestParam(required = false) String success) { // 👈 Capture the query param here

        if (session.getAttribute(SESSION_USER_ID) != null) {
            return "redirect:/dashboard";
        }

        // If a success message was sent via redirect, pass it to the view
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

        // 1. Check session-level block
        Integer failedAttempts = (Integer) session.getAttribute("failedLoginAttempts");
        if (failedAttempts == null) {
            failedAttempts = 0;
        }

        if (failedAttempts >= 3) {
            model.addAttribute("error", "Maximum 3 failed login attempts reached for this session. Access is blocked.");
            return "login";
        }

        try {
            // 2. Attempt authentication
            User user = userService.authenticate(mobileNumber.trim(), pin.trim());

            // 3. Success! Reset failure count
            session.removeAttribute("failedLoginAttempts");
            session.setAttribute(SESSION_USER_ID, user.getId());
            return "redirect:/dashboard";

        } catch (BankingException ex) {
            String errorMsg = ex.getMessage();

            // Check if account is ALREADY locked in DB by UserService
            if (errorMsg != null && errorMsg.toLowerCase().contains("locked")) {
                model.addAttribute("error", errorMsg);
                return "login";
            }

            // Check if error is related to unverified email
            if (errorMsg != null && errorMsg.toLowerCase().contains("verify your email")) {
                model.addAttribute("error", errorMsg);
                return "login"; // Do not increment failed attempts counter
            }

            // Handle bad credentials / wrong PIN
            failedAttempts++;
            session.setAttribute("failedLoginAttempts", failedAttempts);

            int remainingAttempts = 3 - failedAttempts;
            if (remainingAttempts > 0) {
                model.addAttribute("error", "Invalid credentials. Attempts remaining: " + remainingAttempts);
            } else {
                model.addAttribute("error", "Maximum 3 failed login attempts reached. Access is blocked.");
            }

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
    public String processResetPin(@RequestParam String token, @RequestParam String newPin, Model model) {
        try {
            userService.resetPin(token, newPin);
            return "redirect:/login?success=PIN successfully reset. You can now log in.";
        } catch (BankingException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("token", token);
            return "reset-pin";
        }
    }

    // 1. Show Registration Page
    @GetMapping("/register")
    public String registerPage(HttpSession session) {
        if (session.getAttribute(SESSION_USER_ID) != null) {
            return "redirect:/dashboard";
        }
        return "register"; // Maps to your register.html template
    }

    // 2. Handle Registration Form Submission
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

            // Calls your service
            userService.registerUser(user, pin.trim());

            return "redirect:/login?success=Registration successful! Please check your email to verify your account before logging in.";
        } catch (Exception ex) {
            // This will catch ANY error (Database conflicts, BankingExceptions, etc.)
            // and safely display it on your register page instead of crashing
            String errorMessage = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();

            // Fallback if message is null
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "An unexpected error occurred during registration.";
            }

            model.addAttribute("error", errorMessage);
            return "login";
        }
    }

    // 3. Handle Email Verification Link Clicked from Inbox
    @GetMapping("/verify")
    public String verifyEmail(@RequestParam String token, Model model) {
        try {
            userService.verifyEmail(token);
            model.addAttribute("success", "Email verified successfully! You can now log in to your account.");
        } catch (BankingException ex) {
            model.addAttribute("error", ex.getMessage());
        }
        return "login"; // Returns them to login.html showing the success or error message
    }
}
