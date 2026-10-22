package com.lias.manager.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalExceptionHandler {

    @Autowired
    private com.lias.manager.repository.UserRepository userRepository;

    @Autowired
    private com.lias.manager.repository.MemberRepository memberRepository;

    @ModelAttribute("currentUserMember")
    public com.lias.manager.entity.Member getCurrentUserMember() {
        org.springframework.security.core.Authentication auth = 
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
            String loginName = auth.getName();
            var memberOpt = memberRepository.findByEmail(loginName);
            if (memberOpt.isPresent()) {
                return memberOpt.get();
            }
            var userOpt = userRepository.findByEmail(loginName);
            if (userOpt.isEmpty()) {
                userOpt = userRepository.findByUsername(loginName);
            }
            if (userOpt.isPresent()) {
                String email = userOpt.get().getEmail();
                if (email != null) {
                    var mOpt = memberRepository.findByEmail(email);
                    if (mOpt.isPresent()) {
                        return mOpt.get();
                    }
                    String first = "";
                    String last = "";
                    if (email != null && email.contains("@")) {
                        String prefix = email.split("@")[0];
                        if (prefix.contains(".")) {
                            String[] parts = prefix.split("\\.");
                            first = parts[0].substring(0, 1).toUpperCase() + (parts[0].length() > 1 ? parts[0].substring(1) : "");
                            last = parts[1].substring(0, 1).toUpperCase() + (parts[1].length() > 1 ? parts[1].substring(1) : "");
                        } else {
                            first = prefix.substring(0, 1).toUpperCase() + (prefix.length() > 1 ? prefix.substring(1) : "");
                            last = "Chercheur";
                        }
                    } else {
                        first = "Membre";
                        last = "LIAS";
                    }
                    com.lias.manager.entity.Member m = com.lias.manager.entity.Member.builder()
                        .firstName(first)
                        .lastName(last)
                        .email(email)
                        .status("PERMANENT")
                        .institution(null)
                        .originLab(null)
                        .biography(null)
                        .interests(null)
                        .build();
                    return m;
                }
            }
        }
        return null;
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public void handleNoResourceFoundException(org.springframework.web.servlet.resource.NoResourceFoundException ex, jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        response.sendError(jakarta.servlet.http.HttpServletResponse.SC_NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public String handleException(Exception ex, Model model) {
        System.err.println("GLOBAL EXCEPTION INTERCEPTED: " + ex.getMessage());
        ex.printStackTrace();
        
        model.addAttribute("exception", ex);
        model.addAttribute("errorMessage", ex.getMessage() != null ? ex.getMessage() : "Une erreur système interne est survenue.");
        model.addAttribute("exceptionClass", ex.getClass().getSimpleName());
        
        return "error";
    }
}
