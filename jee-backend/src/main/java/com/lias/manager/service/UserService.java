package com.lias.manager.service;

import com.lias.manager.entity.User;
import com.lias.manager.entity.RoleHistory;
import com.lias.manager.repository.UserRepository;
import com.lias.manager.repository.MemberRepository;
import com.lias.manager.repository.RoleHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;
    private final RoleHistoryRepository roleHistoryRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
                .or(() -> userRepository.findByUsername(username))
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé: " + username));

        boolean active = user.isEnabled();
        // Check associated member's status (RETIRED or FORMER_MEMBER)
        var member = memberRepository.findByEmail(user.getEmail()).orElse(null);
        if (member != null) {
            String status = member.getStatus();
            if ("RETIRED".equalsIgnoreCase(status) || "FORMER_MEMBER".equalsIgnoreCase(status)) {
                active = false;
            }
        }

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                active,
                true, true, true,
                user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority(role.getName()))
                        .collect(Collectors.toList())
        );
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional
    public User save(User user, boolean encodePassword) {
        if (user.getId() != null) {
            Optional<User> existingOpt = userRepository.findById(user.getId());
            if (existingOpt.isPresent()) {
                User existing = existingOpt.get();
                String oldRoles = existing.getRoles().stream()
                        .map(com.lias.manager.entity.Role::getName)
                        .sorted()
                        .collect(Collectors.joining(", "));
                String newRoles = user.getRoles().stream()
                        .map(com.lias.manager.entity.Role::getName)
                        .sorted()
                        .collect(Collectors.joining(", "));
                if (!oldRoles.equals(newRoles)) {
                    memberRepository.findByEmail(user.getEmail()).ifPresent(m -> {
                        RoleHistory history = RoleHistory.builder()
                                .member(m)
                                .oldRole(oldRoles.isEmpty() ? "None" : oldRoles)
                                .newRole(newRoles.isEmpty() ? "None" : newRoles)
                                .changedAt(LocalDateTime.now())
                                .build();
                        roleHistoryRepository.saveAndFlush(history);
                    });
                }
                existing.setUsername(user.getUsername());
                existing.setEmail(user.getEmail());
                if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                    if (encodePassword) {
                        existing.setPassword(passwordEncoder.encode(user.getPassword()));
                    } else {
                        existing.setPassword(user.getPassword());
                    }
                }
                existing.setEnabled(user.isEnabled());
                existing.setPasswordNeedChange(user.isPasswordNeedChange());
                existing.setRoles(user.getRoles());
                
                System.out.println("BEFORE SAVE = " + existing);
                User saved = userRepository.saveAndFlush(existing);
                System.out.println("SAVED = " + saved);
                return saved;
            }
        }
        if (encodePassword && user.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        System.out.println("BEFORE SAVE New User = " + user);
        User saved = userRepository.saveAndFlush(user);
        System.out.println("SAVED = " + saved);
        return saved;
    }

    @Transactional
    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }
}
