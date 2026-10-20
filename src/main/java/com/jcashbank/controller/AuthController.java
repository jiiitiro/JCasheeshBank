package com.jcashbank.controller;

import com.jcashbank.exception.BankingException;
import com.jcashbank.model.User;
import com.jcashbank.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static com.jcashbank.controller.SessionUserProvider.SESSION_USER_ID;

@Controller
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping({"/", "/login"})
    public String loginPage(HttpSession session) {

        // If already logged in, go straight to dashboard
        if (session.getAttribute(SESSION_USER_ID) != null) {
            return "redirect:/dashboard";
        }

        // Any "success" message arrives as a flash attribute, already in the model
        return "login";
    }

    @PostMapping("/login")
    public String login(
            @RequestParam String mobileNumber,
            @RequestParam String pin,
            HttpServletRequest request,
            HttpSession session,
            Model model) {

        try {
            // Authenticate directly against DB via UserService
            User user = userService.authenticate(mobileNumber.trim(), pin.trim());

            // New session ID on login so a session ID planted before login (session fixation) is useless
            request.changeSessionId();

            // Store active session user ID
            session.setAttribute(SESSION_USER_ID, user.getId());

            return "redirect:/dashboard";

        } catch (BankingException ex) {
            // Display exact dynamic message from UserService (e.g. remaining attempts or actual email lockout)
            model.addAttribute("error", ex.getMessage());
            return "login";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    // Registration form lives on the login page (register tab)
    @GetMapping("/register")
    public String registerPage() {
        return "redirect:/login";
    }

    @PostMapping("/register")
    public String register(
            @RequestParam String mobileNumber,
            @RequestParam String email,
            @RequestParam String fullName,
            @RequestParam String pin,
            RedirectAttributes redirectAttributes,
            Model model) {
        try {
            User user = new User();
            user.setMobileNumber(mobileNumber.trim());
            user.setEmail(email.trim());
            user.setFullName(fullName.trim());

            userService.registerUser(user, pin.trim());

            redirectAttributes.addFlashAttribute("success",
                    "Registration successful! Please check your email to verify your account before logging in.");
            return "redirect:/login";
        } catch (BankingException ex) {
            model.addAttribute("registerError", ex.getMessage());
        } catch (Exception ex) {
            // Don't show raw DB/mail errors to the user
            log.error("Registration failed for {}", mobileNumber, ex);
            model.addAttribute("registerError", "An unexpected error occurred during registration.");
        }

        // Re-open the register tab so the error shows next to the form
        model.addAttribute("showRegister", true);
        return "login";
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
            RedirectAttributes redirectAttributes,
            Model model) {
        try {
            // resetPin also clears the stored failed-login counter and unlocks the account
            userService.resetPin(token, newPin.trim());

            redirectAttributes.addFlashAttribute("success", "PIN successfully reset. You can now log in.");
            return "redirect:/login";
        } catch (BankingException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("token", token);
            return "reset-pin";
        }
    }
}
