package com.lias.manager.service;

import com.lias.manager.entity.*;
import com.lias.manager.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final RoleHistoryRepository roleHistoryRepository;
    private final MemberAffiliationHistoryRepository memberAffiliationHistoryRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final MandateRepository mandateRepository;
    private final MemberProfileHistoryRepository memberProfileHistoryRepository;

    private void logProfileHistoryIfChanged(Member member, String fieldName, Object oldValue, Object newValue) {
        String oldStr = oldValue != null ? oldValue.toString() : "";
        String newStr = newValue != null ? newValue.toString() : "";
        if (oldStr.trim().equals(newStr.trim())) {
            return;
        }
        MemberProfileHistory hist = MemberProfileHistory.builder()
            .member(member)
            .firstName(member.getFirstName())
            .lastName(member.getLastName())
            .email(member.getEmail())
            .status(member.getStatus())
            .institution(member.getInstitution())
            .originLab(member.getOriginLab())
            .biography(member.getBiography())
            .interests(member.getInterests())
            .fieldName(fieldName)
            .oldValue(oldStr)
            .newValue(newStr)
            .changeDate(LocalDateTime.now())
            .build();
        memberProfileHistoryRepository.save(hist);
    }

    @Transactional
    public Member updateMemberProfile(String email, Member edited) {
        Long id = edited.getId();
        Member existing;
        if (id != null) {
            existing = memberRepository.findById(id).orElseThrow(() -> new RuntimeException("Membre introuvable."));
        } else {
            existing = memberRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Membre introuvable."));
        }

        // Track and log field-by-field changes to profile history
        logProfileHistoryIfChanged(existing, "first_name", existing.getFirstName(), edited.getFirstName());
        logProfileHistoryIfChanged(existing, "last_name", existing.getLastName(), edited.getLastName());
        logProfileHistoryIfChanged(existing, "biography", existing.getBiography(), edited.getBiography());
        logProfileHistoryIfChanged(existing, "interests", existing.getInterests(), edited.getInterests());
        logProfileHistoryIfChanged(existing, "photo", existing.getPhoto(), edited.getPhoto());
        logProfileHistoryIfChanged(existing, "origin_lab", existing.getOriginLab(), edited.getOriginLab());
        logProfileHistoryIfChanged(existing, "academic_year", existing.getAcademicYear(), edited.getAcademicYear());

        // Update fields on the managed entity
        existing.setFirstName(edited.getFirstName());
        existing.setLastName(edited.getLastName());
        existing.setBiography(edited.getBiography());
        existing.setInterests(edited.getInterests());
        existing.setPhoto(edited.getPhoto());
        existing.setOriginLab(edited.getOriginLab());
        existing.setAcademicYear(edited.getAcademicYear());
        if (edited.getStatus() != null) {
            existing.setStatus(edited.getStatus());
        }

        System.out.println("BEFORE SAVE : " + existing);
        Member saved = memberRepository.saveAndFlush(existing);
        System.out.println("AFTER SAVE : " + saved);
        return saved;
    }

    @Transactional
    public void updateMemberLifecycle(Long memberId, String status, String roleTitle, Long teamId) {
        Member m = memberRepository.findById(memberId).orElseThrow(() -> new RuntimeException("Membre introuvable."));
        System.out.println("UPDATE ID = " + m.getId());
        System.out.println("BEFORE SAVE = " + m);
        
        // 1. Check/Update Status
        String oldStatus = m.getStatus();
        if (!java.util.Objects.equals(oldStatus, status)) {
            logProfileHistoryIfChanged(m, "status", oldStatus, status);
            m.setStatus(status);
            RoleHistory history = RoleHistory.builder()
                    .member(m)
                    .oldRole(oldStatus != null ? oldStatus : "NONE")
                    .newRole(status)
                    .changedAt(LocalDateTime.now())
                    .build();
            roleHistoryRepository.saveAndFlush(history);
        }
        
        // 2. Check/Update Team & MemberAffiliationHistory
        Team oldTeam = m.getTeam();
        if ((oldTeam == null && teamId != null) || (oldTeam != null && !oldTeam.getId().equals(teamId))) {
            String oldTeamName = oldTeam != null ? oldTeam.getName() : "None";
            Team newTeam = null;
            if (teamId != null) {
                newTeam = teamRepository.findById(teamId).orElse(null);
            }
            String newTeamName = newTeam != null ? newTeam.getName() : "None";
            
            logProfileHistoryIfChanged(m, "team", oldTeamName, newTeamName);
            
            if (oldTeam != null) {
                List<MemberAffiliationHistory> affs = memberAffiliationHistoryRepository.findByMemberIdOrderByStartDateDesc(m.getId());
                if (!affs.isEmpty()) {
                    MemberAffiliationHistory activeAff = affs.get(0);
                    if (activeAff.getEndDate() == null) {
                        activeAff.setEndDate(LocalDate.now());
                        memberAffiliationHistoryRepository.saveAndFlush(activeAff);
                    }
                }
            }
            
            m.setTeam(newTeam);
            
            if (newTeam != null) {
                MemberAffiliationHistory newAff = MemberAffiliationHistory.builder()
                        .member(m)
                        .startDate(LocalDate.now())
                        .teamName(newTeam.getName())
                        .build();
                memberAffiliationHistoryRepository.saveAndFlush(newAff);
            }
        }
        
        // 3. User roles synchronization
        Optional<User> uOpt = userRepository.findByEmail(m.getEmail());
        if (uOpt.isPresent()) {
            User u = uOpt.get();
            u.getRoles().clear();
            String roleName = "ROLE_MEMBER";
            if ("DIRECTOR".equalsIgnoreCase(roleTitle)) {
                roleName = "ROLE_DIRECTOR";
            } else if ("VICE_DIRECTOR".equalsIgnoreCase(roleTitle)) {
                roleName = "ROLE_DIRECTOR";
            } else if ("TEAM_LEADER".equalsIgnoreCase(roleTitle)) {
                roleName = "ROLE_TEAM_LEADER";
            } else if ("ASSOCIATE".equalsIgnoreCase(status)) {
                roleName = "ROLE_ASSOCIATE";
            } else if ("DOCTORANT".equalsIgnoreCase(status)) {
                roleName = "ROLE_DOCTORANT";
            }
            
            Optional<Role> rOpt = roleRepository.findByName(roleName);
            if (rOpt.isPresent()) {
                u.getRoles().add(rOpt.get());
            }
            userRepository.saveAndFlush(u);
        }
        
        // 4. Mandate update/creation
        List<Mandate> mandates = mandateRepository.findAll();
        for (Mandate mand : mandates) {
            if (mand.getMember() != null && mand.getMember().getId().equals(m.getId()) && mand.isActive()) {
                mand.setActive(false);
                mand.setEndDate(LocalDate.now());
                mandateRepository.saveAndFlush(mand);
            }
        }
        
        if (roleTitle != null && !roleTitle.isEmpty() && !"MEMBER".equalsIgnoreCase(roleTitle)) {
            String teamName = m.getTeam() != null ? m.getTeam().getName() : "LIAS Laboratory";
            Mandate newMandate = Mandate.builder()
                    .member(m)
                    .roleTitle(roleTitle)
                    .teamAssigned(teamName)
                    .startDate(LocalDate.now())
                    .active(true)
                    .build();
            mandateRepository.saveAndFlush(newMandate);
        }
        
        Member saved = memberRepository.saveAndFlush(m);
        System.out.println("SAVED = " + saved);
    }

    public List<Member> findAll() {
        return memberRepository.findAll();
    }

    public Optional<Member> findById(Long id) {
        return memberRepository.findById(id);
    }

    public List<Member> findByStatus(String status) {
        return memberRepository.findByStatus(status);
    }

    @Transactional
    public Member save(Member member) {
        if (member.getId() != null) {
            Optional<Member> existingOpt = memberRepository.findById(member.getId());
            if (existingOpt.isPresent()) {
                Member existing = existingOpt.get();
                System.out.println("UPDATE ID = " + existing.getId());
                System.out.println("BEFORE SAVE = " + existing);
                
                if (!java.util.Objects.equals(existing.getStatus(), member.getStatus())) {
                    RoleHistory history = RoleHistory.builder()
                            .member(existing)
                            .oldRole(existing.getStatus())
                            .newRole(member.getStatus())
                            .changedAt(LocalDateTime.now())
                            .build();
                    roleHistoryRepository.saveAndFlush(history);
                }
                
                existing.setFirstName(member.getFirstName());
                existing.setLastName(member.getLastName());
                existing.setEmail(member.getEmail());
                existing.setPhoto(member.getPhoto());
                existing.setAcademicYear(member.getAcademicYear());
                existing.setStatus(member.getStatus());
                existing.setInstitution(member.getInstitution());
                existing.setOriginLab(member.getOriginLab());
                existing.setBiography(member.getBiography());
                existing.setInterests(member.getInterests());
                existing.setBirthDate(member.getBirthDate());
                existing.setHireDate(member.getHireDate());
                existing.setAffiliationDate(member.getAffiliationDate());
                existing.setTeam(member.getTeam());
                
                Member saved = memberRepository.saveAndFlush(existing);
                System.out.println("SAVED = " + saved);
                return saved;
            }
        }
        System.out.println("BEFORE SAVE New Member = " + member);
        Member saved = memberRepository.saveAndFlush(member);
        System.out.println("SAVED = " + saved);
        return saved;
    }

    @Transactional
    public void deleteById(Long id) {
        memberRepository.deleteById(id);
    }
}
