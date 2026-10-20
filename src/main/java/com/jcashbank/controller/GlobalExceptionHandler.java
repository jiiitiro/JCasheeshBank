package com.jcashbank.controller;

import com.jcashbank.exception.BankingException;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static com.jcashbank.controller.SessionUserProvider.SESSION_USER_ID;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BankingException.class)
    public String handleBankingException(BankingException ex, HttpSession session, Model model) {
        if (session.getAttribute(SESSION_USER_ID) == null) {
            return "redirect:/login";
        }
        model.addAttribute("error", ex.getMessage());
        return "error";
    }

    // Empty or non-numeric form fields (e.g. amount="" or amount="abc") fail before the controller runs
    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleBadRequestParameter(HttpSession session, Model model) {
        if (session.getAttribute(SESSION_USER_ID) == null) {
            return "redirect:/login";
        }
        model.addAttribute("error", "Some details were missing or invalid. Please check your input and try again.");
        return "error";
    }
}
