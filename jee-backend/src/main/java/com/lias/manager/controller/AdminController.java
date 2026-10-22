package com.lias.manager.controller;

import com.lias.manager.entity.User;
import com.lias.manager.entity.Member;
import com.lias.manager.entity.Publication;
import com.lias.manager.repository.RoleRepository;
import com.lias.manager.repository.MemberRepository;
import com.lias.manager.repository.TeamRepository;
import com.lias.manager.repository.UserRepository;
import com.lias.manager.repository.MemberProfileHistoryRepository;
import com.lias.manager.repository.RoleHistoryRepository;
import com.lias.manager.repository.MemberAffiliationHistoryRepository;
import com.lias.manager.service.UserService;
import com.lias.manager.service.MemberService;
import com.lias.manager.service.PublicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final RoleRepository roleRepository;
    private final MemberRepository memberRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final MemberProfileHistoryRepository memberProfileHistoryRepository;
    private final RoleHistoryRepository roleHistoryRepository;
    private final MemberAffiliationHistoryRepository memberAffiliationHistoryRepository;
    private final MemberService memberService;
    private final PublicationService publicationService;

    private String resolveCurrentUserEmail() {
        String loginName = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        var userOpt = userService.findByEmail(loginName);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByUsername(loginName);
        }
        if (userOpt.isPresent()) {
            return userOpt.get().getEmail();
        }
        return loginName;
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        String email = resolveCurrentUserEmail();
        Member adminMember = memberRepository.findByEmail(email)
                .orElseGet(() -> {
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
                    Member fallback = Member.builder()
                            .firstName(first)
                            .lastName(last)
                            .email(email)
                            .status("PERMANENT")
                            .institution(null)
                            .originLab(null)
                            .biography(null)
                            .interests(null)
                            .build();
                    return memberRepository.saveAndFlush(fallback);
                });
        model.addAttribute("member", adminMember);
        if (adminMember != null && adminMember.getId() != null) {
            model.addAttribute("history", memberProfileHistoryRepository.findByMemberIdOrderByChangeDateDesc(adminMember.getId()));
            model.addAttribute("roleHistory", roleHistoryRepository.findByMemberId(adminMember.getId()));
            model.addAttribute("affiliationHistory", memberAffiliationHistoryRepository.findByMemberIdOrderByStartDateDesc(adminMember.getId()));
            
            // Retrieve member's publications
            String ln = adminMember.getLastName() != null ? adminMember.getLastName().toLowerCase() : "";
            String fn = adminMember.getFirstName() != null ? adminMember.getFirstName().toLowerCase() : "";
            java.util.List<Publication> myPubs = publicationService.findAll().stream()
                .filter(p -> p != null && p.getAuthor() != null && 
                    (p.getAuthor().toLowerCase().contains(ln) || p.getAuthor().toLowerCase().contains(fn)))
                .toList();
            model.addAttribute("myPublications", myPubs);
        } else {
            model.addAttribute("myPublications", java.util.List.of());
        }
        model.addAttribute("teams", teamRepository.findAll());
        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@ModelAttribute Member edited) {
        String email = resolveCurrentUserEmail();
        memberService.updateMemberProfile(email, edited);
        return "redirect:/admin/profile?success";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        return "dashboard-admin";
    }

    @GetMapping("/config")
    public String config(Model model) {
        return "config";
    }

    @GetMapping("/users")
    public String listUsers(Model model) {
        model.addAttribute("users", userService.findAll());
        model.addAttribute("roles", roleRepository.findAll());
        model.addAttribute("newUser", new User());
        return "users";
    }

    @PostMapping("/users/save")
    @org.springframework.transaction.annotation.Transactional
    public String saveUser(@ModelAttribute User user, @RequestParam(required = false) Integer roleId) {
        if (user.getId() != null) {
            java.util.Optional<User> existingOpt = userService.findById(user.getId());
            if (existingOpt.isPresent()) {
                User existing = existingOpt.get();
                System.out.println("BEFORE SAVE User: " + existing);
                existing.setUsername(user.getUsername());
                existing.setEmail(user.getEmail());
                if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                    existing.setPassword(user.getPassword());
                }
                if (roleId != null) {
                    java.util.Optional<com.lias.manager.entity.Role> roleOpt = roleRepository.findById(roleId);
                    if (roleOpt.isPresent()) {
                        existing.setRoles(new java.util.HashSet<>());
                        existing.getRoles().add(roleOpt.get());
                    }
                }
                User saved = userService.save(existing, user.getPassword() != null && !user.getPassword().isEmpty());
                System.out.println("AFTER SAVE User: " + saved);
            } else {
                if (user.getRoles() == null) {
                    user.setRoles(new java.util.HashSet<>());
                }
                if (roleId != null) {
                    java.util.Optional<com.lias.manager.entity.Role> roleOpt = roleRepository.findById(roleId);
                    if (roleOpt.isPresent()) {
                        user.getRoles().add(roleOpt.get());
                    }
                }
                System.out.println("BEFORE SAVE New User: " + user);
                User saved = userService.save(user, true);
                System.out.println("AFTER SAVE New User: " + saved);
            }
        } else {
            if (user.getRoles() == null) {
                user.setRoles(new java.util.HashSet<>());
            }
            if (roleId != null) {
                java.util.Optional<com.lias.manager.entity.Role> roleOpt = roleRepository.findById(roleId);
                if (roleOpt.isPresent()) {
                    user.getRoles().add(roleOpt.get());
                }
            }
            System.out.println("BEFORE SAVE New User: " + user);
            User saved = userService.save(user, true);
            System.out.println("AFTER SAVE New User: " + saved);
        }
        return "redirect:/admin/users?success";
    }

    @PostMapping("/users/toggle/{id}")
    @org.springframework.transaction.annotation.Transactional
    public String toggleUserStatus(@PathVariable Long id) {
        java.util.Optional<User> userOpt = userService.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            System.out.println("BEFORE SAVE User Status Toggle: " + user);
            user.setEnabled(!user.isEnabled());
            User saved = userService.save(user, false);
            System.out.println("AFTER SAVE User Status Toggle: " + saved);
        }
        return "redirect:/admin/users?toggled";
    }

    @GetMapping("/roles")
    public String listRoles(Model model) {
        model.addAttribute("roles", roleRepository.findAll());
        return "roles";
    }

    @GetMapping("/logs")
    public String viewLogs(Model model) {
        var mockLogs = new ArrayList<String>();
        mockLogs.add("[2026-06-17 10:20] ADHESION APPROVED: Candidate Salma Alami accepted as Doctorant.");
        mockLogs.add("[2026-06-17 09:12] MANDATE CREATED: Pr. Abdelouahad Ennassiri assigned as Lab Director.");
        mockLogs.add("[2026-06-16 16:45] USER CREATED: User login active for Khadija Benjelloun.");
        mockLogs.add("[2026-06-16 11:30] EQUIPMENT REGISTERED: Card Nvidia Jetson added to system inventory.");
        model.addAttribute("logs", mockLogs);
        return "logs";
    }
}
