package com.lias.manager.controller;

import com.lias.manager.entity.Notification;
import com.lias.manager.repository.NotificationRepository;
import com.lias.manager.repository.UserRepository;
import com.lias.manager.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    @GetMapping("/notifications")
    public String viewNotifications(Model model) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return "redirect:/login";
        }

        String username = auth.getName();
        String userEmail = userRepository.findByUsername(username)
                .map(com.lias.manager.entity.User::getEmail)
                .orElseGet(() -> userRepository.findByEmail(username)
                        .map(com.lias.manager.entity.User::getEmail)
                        .orElse(username));

        List<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        List<Notification> allNotifications = notificationRepository.findAll();

        // Sort descending by date so newest is first
        allNotifications.sort((a, b) -> {
            var dateA = a.getDate() != null ? a.getDate() : (a.getCreatedDate() != null ? a.getCreatedDate() : java.time.LocalDateTime.MIN);
            var dateB = b.getDate() != null ? b.getDate() : (b.getCreatedDate() != null ? b.getCreatedDate() : java.time.LocalDateTime.MIN);
            return dateB.compareTo(dateA);
        });

        // Filter notifications based on the user's roles and target address
        List<Notification> filteredNotifications = allNotifications.stream().filter(notif -> {
            String type = notif.getType() != null ? notif.getType().toUpperCase() : "";
            String user = notif.getUser() != null ? notif.getUser().toLowerCase() : "";

            boolean matchesSpecificUser = user.isEmpty() || user.equals("all") || 
                                          user.equals(username.toLowerCase()) || 
                                          user.equals(userEmail.toLowerCase());

            if (roles.contains("ROLE_DIRECTOR")) {
                // Director can see anything addressed to "all", to their account, or explicitly to "director"
                return matchesSpecificUser || user.equals("director") || user.equals("director@lias-lab.ma") ||
                       type.contains("NEW_ADHESION") || type.contains("ADHESION_REQUEST") || type.contains("EQUIPMENT_REQUEST");
            }

            if (roles.contains("ROLE_ADMIN")) {
                return matchesSpecificUser || user.equals("admin") || type.contains("SYSTEM") || type.contains("ACCOUNT") || type.contains("SECURITY");
            }

            // Regular Members/Doctorants/Associates can only see their own notifications or public ones
            if (roles.contains("ROLE_MEMBER") || roles.contains("ROLE_DOCTORANT") || roles.contains("ROLE_ASSOCIATE")) {
                // They cannot see Director-specific or Admin-specific notifications
                if (user.equals("director") || user.equals("director@lias-lab.ma") || user.equals("admin")) {
                    return false;
                }
                return matchesSpecificUser;
            }

            return false;
        }).collect(Collectors.toList());

        model.addAttribute("notifications", filteredNotifications);
        return "notifications";
    }

    @PostMapping("/notifications/read/{id}")
    @org.springframework.transaction.annotation.Transactional
    public String markAsRead(@PathVariable Long id) {
        notificationRepository.findById(id).ifPresent(notif -> {
            notif.setRead(true);
            notificationRepository.saveAndFlush(notif);
        });
        return "redirect:/notifications";
    }

    @PostMapping("/notifications/read-all")
    @org.springframework.transaction.annotation.Transactional
    public String markAllAsRead() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return "redirect:/login";
        }

        String username = auth.getName();
        String userEmail = userRepository.findByUsername(username)
                .map(com.lias.manager.entity.User::getEmail)
                .orElseGet(() -> userRepository.findByEmail(username)
                        .map(com.lias.manager.entity.User::getEmail)
                        .orElse(username));

        List<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        List<Notification> allNotifications = notificationRepository.findAll();

        List<Notification> userNotifications = allNotifications.stream().filter(notif -> {
            if (notif.isRead()) {
                return false;
            }
            String type = notif.getType() != null ? notif.getType().toUpperCase() : "";
            String user = notif.getUser() != null ? notif.getUser().toLowerCase() : "";

            boolean matchesSpecificUser = user.isEmpty() || user.equals("all") || 
                                          user.equals(username.toLowerCase()) || 
                                          user.equals(userEmail.toLowerCase());

            if (roles.contains("ROLE_DIRECTOR")) {
                return matchesSpecificUser || user.equals("director") || user.equals("director@lias-lab.ma") ||
                       type.contains("NEW_ADHESION") || type.contains("ADHESION_REQUEST") || type.contains("EQUIPMENT_REQUEST");
            }

            if (roles.contains("ROLE_ADMIN")) {
                return matchesSpecificUser || user.equals("admin") || type.contains("SYSTEM") || type.contains("ACCOUNT") || type.contains("SECURITY");
            }

            if (roles.contains("ROLE_MEMBER") || roles.contains("ROLE_DOCTORANT") || roles.contains("ROLE_ASSOCIATE")) {
                if (user.equals("director") || user.equals("director@lias-lab.ma") || user.equals("admin")) {
                    return false;
                }
                return matchesSpecificUser;
            }

            return false;
        }).collect(Collectors.toList());

        for (Notification notif : userNotifications) {
            notif.setRead(true);
        }
        notificationRepository.saveAllAndFlush(userNotifications);

        return "redirect:/notifications";
    }

    @GetMapping("/change-password")
    public String changePasswordForm(Model model) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return "redirect:/login";
        }
        return "change-password";
    }

    @PostMapping("/change-password")
    public String changePasswordSubmit(
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Model model) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return "redirect:/login";
        }

        if (newPassword == null || newPassword.trim().isEmpty()) {
            model.addAttribute("error", "Le mot de passe ne peut pas être vide.");
            return "change-password";
        }

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Les deux mots de passe ne correspondent pas.");
            return "change-password";
        }

        String username = auth.getName();
        var userOpt = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username));

        if (userOpt.isPresent()) {
            var user = userOpt.get();
            user.setPassword(newPassword);
            user.setPasswordNeedChange(false);
            userService.save(user, true);
            return "redirect:/login?passwordChanged";
        }

        model.addAttribute("error", "Utilisateur non trouvé.");
        return "change-password";
    }
}
