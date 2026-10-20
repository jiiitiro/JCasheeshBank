package com.jcashbank.controller;

import com.jcashbank.exception.BankingException;
import com.jcashbank.model.User;
import com.jcashbank.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

@Component
public class SessionUserProvider {
    public static final String SESSION_USER_ID = "loggedInUserId";

    private final UserService userService;

    public SessionUserProvider(UserService userService) {
        this.userService = userService;
    }

    public User getLoggedInUser(HttpSession session) {
        Object id = session.getAttribute(SESSION_USER_ID);
        if (!(id instanceof Long userId)) {
            throw new BankingException("Please log in first.");
        }
        return userService.findById(userId);
    }
}
