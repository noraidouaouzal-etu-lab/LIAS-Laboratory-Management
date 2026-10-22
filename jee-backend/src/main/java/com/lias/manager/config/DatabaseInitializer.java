package com.lias.manager.config;

import com.lias.manager.entity.Role;
import com.lias.manager.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DatabaseInitializer implements ApplicationRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // Enforce role existence check first
        ensureRoleExists(1, "ROLE_VISITOR");
        ensureRoleExists(2, "ROLE_DOCTORANT");
        ensureRoleExists(3, "ROLE_MEMBER");
        ensureRoleExists(4, "ROLE_DIRECTOR");
        ensureRoleExists(5, "ROLE_ADMIN");
        ensureRoleExists(6, "ROLE_ASSOCIATE");
    }

    private void ensureRoleExists(int id, String name) {
        Optional<Role> roleOpt = roleRepository.findByName(name);
        if (roleOpt.isEmpty()) {
            Role role = new Role();
            role.setId(id);
            role.setName(name);
            roleRepository.save(role);
        }
    }
}
