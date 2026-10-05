package com.jcashbank.service;

import com.jcashbank.exception.BankingException;
import com.jcashbank.model.User;
import com.jcashbank.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public User authenticate(String mobileNumber, String pin) {
        User user = userRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new BankingException("Mobile number or PIN is incorrect."));
        if (!user.getPin().equals(pin)) {
            throw new BankingException("Mobile number or PIN is incorrect.");
        }
        return user;
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BankingException("User account was not found."));
    }

    @Transactional(readOnly = true)
    public User findByMobileNumber(String mobileNumber) {
        return userRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new BankingException("Receiver account was not found."));
    }
}
