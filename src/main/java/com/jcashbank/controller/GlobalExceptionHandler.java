package com.jcashbank.controller;

import com.jcashbank.exception.BankingException;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BankingException.class)
    public String handleBankingException(BankingException ex, HttpSession session, Model model) {
        if (session.getAttribute("loggedInUserId") == null) {
            return "redirect:/login";
        }
        model.addAttribute("error", ex.getMessage());
        return "error";
    }
}
