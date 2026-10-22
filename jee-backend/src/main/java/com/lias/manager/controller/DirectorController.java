package com.lias.manager.controller;
import com.lias.manager.entity.*;
import com.lias.manager.entity.Member;
import com.lias.manager.repository.*;
import com.lias.manager.service.EventService;
import com.lias.manager.service.PublicationService;
import com.lias.manager.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/director")
public class DirectorController {

    private final MemberRepository memberRepository;
    private final TeamRepository teamRepository;
    private final PublicationService publicationService;
    private final EventService eventService;
    private final AdhesionRequestRepository adhesionRequestRepository;
    private final MandateRepository mandateRepository;
    private final EquipmentRepository equipmentRepository;
    private final PVMeetingRepository pvMeetingRepository;
    private final ConventionRepository conventionRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final NotificationRepository notificationRepository;
    private final UserService userService;
    private final EquipmentRequestRepository equipmentRequestRepository;
    private final EquipmentAssignmentRepository equipmentAssignmentRepository;
    private final com.lias.manager.service.FileStorageService fileStorageService;
    private final com.lias.manager.service.MemberService memberService;
    private final EquipmentAssignmentHistoryRepository equipmentAssignmentHistoryRepository;
    private final DocumentHistoryRepository documentHistoryRepository;
    private final MemberProfileHistoryRepository memberProfileHistoryRepository;
    private final RoleHistoryRepository roleHistoryRepository;
    private final MemberAffiliationHistoryRepository memberAffiliationHistoryRepository;

    @GetMapping("/members")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String members(Model model) {
        model.addAttribute("members", memberRepository.findAll());
        return "members";
    }

    @GetMapping("/members/edit/{id}")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String editMemberForm(@PathVariable Long id, Model model) {
        Member m = memberRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Membre non trouvé"));
        model.addAttribute("member", m);
        model.addAttribute("teams", teamRepository.findAll());
        
        String currentRole = "MEMBER";
        List<Mandate> mandates = mandateRepository.findAll();
        for (Mandate mand : mandates) {
            if (mand.getMember() != null && mand.getMember().getId().equals(id) && mand.isActive()) {
                currentRole = mand.getRoleTitle();
            }
        }
        model.addAttribute("currentRole", currentRole);
        return "director-member-edit";
    }

    @PostMapping("/members/edit/{id}")
    @org.springframework.transaction.annotation.Transactional
    public String editMemberSave(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam String roleTitle,
            @RequestParam(required = false) Long teamId,
            Model model) {
        try {
            memberService.updateMemberLifecycle(id, status, roleTitle, teamId);
            return "redirect:/director/members?success";
        } catch (Exception e) {
            System.err.println("Error editing member: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/documents")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String documents(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String filterType,
            Model model) {
        java.util.List<Document> docs = documentRepository.findAll();
        if (search != null && !search.trim().isEmpty()) {
            final String s = search.toLowerCase().trim();
            docs = docs.stream()
                .filter(d -> (d.getName() != null && d.getName().toLowerCase().contains(s)) || (d.getType() != null && d.getType().toLowerCase().contains(s)))
                .toList();
        }
        if (filterType != null && !filterType.isEmpty()) {
            docs = docs.stream()
                .filter(d -> filterType.equals(d.getType()))
                .toList();
        }

        java.util.List<Document> activeDocs = docs.stream().filter(d -> d != null && !d.isArchived()).toList();
        java.util.List<Document> archivedDocs = docs.stream().filter(d -> d != null && d.isArchived()).toList();

        model.addAttribute("activeDocuments", activeDocs);
        model.addAttribute("archivedDocuments", archivedDocs);
        
        java.util.List<DocumentHistory> historyList = documentHistoryRepository.findAll();
        model.addAttribute("historyList", historyList);

        model.addAttribute("newDocument", new Document());
        model.addAttribute("search", search);
        model.addAttribute("filterType", filterType);
        model.addAttribute("isDirector", true);
        return "documents";
    }

    @PostMapping("/documents")
    @org.springframework.transaction.annotation.Transactional
    public String uploadDocument(@ModelAttribute Document document, @RequestParam("docFile") org.springframework.web.multipart.MultipartFile docFile, java.security.Principal principal) {
        if (docFile != null && !docFile.isEmpty()) {
            String storedPath = fileStorageService.storeFile(docFile);
            document.setFileName(docFile.getOriginalFilename());
            document.setFilePath(storedPath);
            document.setPath(docFile.getOriginalFilename());
        }
        document.setUploadDate(java.time.LocalDateTime.now());
        document.setDateUpload(java.time.LocalDateTime.now());
        if (principal != null) {
            document.setUploadedBy(principal.getName());
        } else {
            document.setUploadedBy("Anonymous");
        }
        System.out.println("BEFORE SAVE New Document = " + document);
        Document saved = documentRepository.saveAndFlush(document);
        System.out.println("SAVED = " + saved);

        // Document History Logging
        DocumentHistory hist = DocumentHistory.builder()
            .documentName(saved.getName())
            .actionType("UPLOADED")
            .actionDate(java.time.LocalDateTime.now())
            .build();
        documentHistoryRepository.saveAndFlush(hist);

        // Notify document addition
        Notification opt = Notification.builder()
            .message("Nouveau document chargé: " + document.getName())
            .read(false)
            .type("DOCUMENT_UPLOAD")
            .date(LocalDateTime.now())
            .user("all")
            .build();
        notificationRepository.saveAndFlush(opt);

        return "redirect:/director/documents?success";
    }

    @PostMapping("/documents/delete/{id}")
    @org.springframework.transaction.annotation.Transactional
    public String deleteDocument(@PathVariable Long id) {
        documentRepository.findById(id).ifPresent(doc -> {
            DocumentHistory hist = DocumentHistory.builder()
                .documentName(doc.getName())
                .actionType("DELETED")
                .actionDate(java.time.LocalDateTime.now())
                .build();
            documentHistoryRepository.saveAndFlush(hist);
            documentRepository.deleteById(id);
        });
        return "redirect:/director/documents?deleted";
    }

    @PostMapping("/documents/archive/{id}")
    @org.springframework.transaction.annotation.Transactional
    public String archiveDocument(@PathVariable Long id) {
        documentRepository.findById(id).ifPresent(doc -> {
            doc.setArchived(!doc.isArchived());
            documentRepository.saveAndFlush(doc);
            
            DocumentHistory hist = DocumentHistory.builder()
                .documentName(doc.getName())
                .actionType(doc.isArchived() ? "ARCHIVED" : "UNARCHIVED")
                .actionDate(java.time.LocalDateTime.now())
                .build();
            documentHistoryRepository.saveAndFlush(hist);
        });
        return "redirect:/director/documents?archived";
    }

    @GetMapping("/dashboard")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String dashboard(Model model) {
        model.addAttribute("membersCount", memberRepository.count());
        model.addAttribute("publicationsCount", publicationService.findAll().size());
        model.addAttribute("eventsCount", eventService.findAll().size());
        model.addAttribute("pendingCount", adhesionRequestRepository.countByStatus("PENDING"));
        return "dashboard-director";
    }

    @GetMapping("/adhesions")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String adhesions(@RequestParam(value = "status", required = false) String status, Model model) {
        java.util.List<AdhesionRequest> list;
        if (status == null || status.trim().isEmpty() || "ALL".equalsIgnoreCase(status)) {
            list = adhesionRequestRepository.findAll();
            model.addAttribute("selectedStatus", "ALL");
        } else {
            list = adhesionRequestRepository.findByStatus(status.toUpperCase());
            model.addAttribute("selectedStatus", status.toUpperCase());
        }
        model.addAttribute("requests", list);
        return "adhesions";
    }

    @PostMapping("/adhesions/approve/{id}")
    @org.springframework.transaction.annotation.Transactional
    public String approveAdhesion(@PathVariable Long id, @RequestParam String targetRole) {
        adhesionRequestRepository.findById(id).ifPresent(req -> {
            req.setStatus("ACCEPTED");
            System.out.println("UPDATE ID = " + req.getId());
            System.out.println("BEFORE SAVE = " + req);
            AdhesionRequest savedReq = adhesionRequestRepository.saveAndFlush(req);
            System.out.println("SAVED = " + savedReq);

            // Log notification of adhesion decisions (Requirement 17)
            String tempPassword = java.util.UUID.randomUUID().toString().substring(0, 8);
            Notification n = Notification.builder()
                .message("Votre demande est acceptée.\nVotre compte LIAS est créé.\nEmail: " + req.getEmail() + "\nMot de passe temporaire: " + tempPassword + "\nVeuillez changer votre mot de passe.")
                .read(false)
                .type("ADHESION_ACCEPTED")
                .date(LocalDateTime.now())
                .user(req.getEmail())
                .build();
            notificationRepository.saveAndFlush(n);

            if (memberRepository.findByEmail(req.getEmail()).isEmpty()) {
                // Determine first and last name
                String firstName = req.getFirstName();
                String lastName = req.getLastName();
                if ((firstName == null || firstName.trim().isEmpty()) && (lastName == null || lastName.trim().isEmpty())) {
                    String fullName = req.getName().trim();
                    int spaceIdx = fullName.indexOf(' ');
                    if (spaceIdx > 0) {
                        firstName = fullName.substring(0, spaceIdx).trim();
                        lastName = fullName.substring(spaceIdx + 1).trim();
                    } else {
                        firstName = fullName;
                        lastName = "";
                    }
                }

                String memberStatus = "PERMANENT";
                String userRoleName = "ROLE_MEMBER";

                if ("ASSOCIATE".equalsIgnoreCase(targetRole)) {
                    memberStatus = "ASSOCIATE";
                    userRoleName = "ROLE_ASSOCIATE";
                } else if ("DOCTORANT".equalsIgnoreCase(targetRole)) {
                    memberStatus = "DOCTORANT";
                    userRoleName = "ROLE_DOCTORANT";
                } else if ("PERMANENT_MEMBER".equalsIgnoreCase(targetRole) || "MEMBER_PERMANENT".equalsIgnoreCase(targetRole) || "PERMANENT".equalsIgnoreCase(targetRole)) {
                    memberStatus = "PERMANENT";
                    userRoleName = "ROLE_MEMBER";
                }

                String finalRoleName = userRoleName;

                // Create profile Member (Requirement 5)
                Member member = Member.builder()
                    .firstName(firstName)
                    .lastName(lastName)
                    .email(req.getEmail())
                    .status(memberStatus)
                    .institution(req.getInstitution() != null ? req.getInstitution() : "Faculté des Sciences Ben M'Sik")
                    .originLab(req.getOriginLab() != null ? req.getOriginLab() : "LIAS")
                    .build();
                System.out.println("BEFORE SAVE New Member = " + member);
                Member savedMember = memberRepository.saveAndFlush(member);
                System.out.println("SAVED = " + savedMember);

                // Create user account
                Role standardRole = roleRepository.findByName(finalRoleName)
                    .orElseGet(() -> roleRepository.saveAndFlush(new Role(null, finalRoleName)));

                User user = new User();
                user.setUsername(req.getEmail());
                user.setEmail(req.getEmail());
                user.setPassword(tempPassword);
                user.setPasswordNeedChange(true);
                user.setEnabled(true);
                user.setRoles(new java.util.HashSet<>());
                user.getRoles().add(standardRole);

                userService.save(user, true);
            }
        });
        return "redirect:/director/adhesions?approved";
    }

    @PostMapping("/adhesions/reject/{id}")
    @org.springframework.transaction.annotation.Transactional
    public String rejectAdhesion(@PathVariable Long id) {
        adhesionRequestRepository.findById(id).ifPresent(req -> {
            req.setStatus("REJECTED");
            System.out.println("UPDATE ID = " + req.getId());
            System.out.println("BEFORE SAVE = " + req);
            AdhesionRequest savedReq = adhesionRequestRepository.saveAndFlush(req);
            System.out.println("SAVED = " + savedReq);

            // Log notification
            Notification notification = Notification.builder()
                .message("Votre demande d'adhésion a été refusée.")
                .read(false)
                .type("ADHESION_REJECTED")
                .date(LocalDateTime.now())
                .user(req.getEmail())
                .build();
            notificationRepository.saveAndFlush(notification);
        });
        return "redirect:/director/adhesions?rejected";
    }

    private boolean isRole(String roleTitle, String targetRole) {
        if (roleTitle == null) return false;
        String normalized = roleTitle.trim().toUpperCase()
            .replace(" ", "_")
            .replace("-", "_")
            .replace("É", "E")
            .replace("È", "E")
            .replace("’", "'")
            .replace("''", "'");
        if ("DIRECTOR".equals(targetRole)) {
            return "DIRECTOR".equals(normalized) || "DIRECTEUR".equals(normalized) || "DIRECTRICE".equals(normalized);
        }
        if ("VICE_DIRECTOR".equals(targetRole)) {
            return "VICE_DIRECTOR".equals(normalized) || "VICE_DIRECTEUR".equals(normalized) || "VICE_DIRECTRICE".equals(normalized) || "VICE_DIR".equals(normalized);
        }
        if ("TEAM_LEADER".equals(targetRole)) {
            return "TEAM_LEADER".equals(normalized) || "TEAMLEADER".equals(normalized) || 
                   "RESPONSABLE_EQUIPE".equals(normalized) || "CHEF_EQUIPE".equals(normalized) || 
                   "CHEF_D'EQUIPE".equals(normalized) || "CHEF_D_EQUIPE".equals(normalized) ||
                   "CHEF_D''EQUIPE".equals(normalized) || "CHEF_D’EQUIPE".equals(normalized);
        }
        return false;
    }

    @GetMapping("/governance")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String governance(Model model) {
        List<Mandate> allMandates = mandateRepository.findAllWithMemberAndTeam();

        Mandate dirMandate = allMandates.stream()
            .filter(m -> m.isActive() && isRole(m.getRoleTitle(), "DIRECTOR"))
            .findFirst().orElse(null);

        Mandate vdirMandate = allMandates.stream()
            .filter(m -> m.isActive() && isRole(m.getRoleTitle(), "VICE_DIRECTOR"))
            .findFirst().orElse(null);

        List<Mandate> teamLeaders = allMandates.stream()
            .filter(m -> m.isActive() && isRole(m.getRoleTitle(), "TEAM_LEADER"))
            .collect(java.util.stream.Collectors.toList());

        // Display every mandate (active and finished) ordered by start_date DESC
        List<Mandate> previousMandates = allMandates.stream()
            .sorted((m1, m2) -> {
                if (m1.getStartDate() == null && m2.getStartDate() == null) return 0;
                if (m1.getStartDate() == null) return 1;
                if (m2.getStartDate() == null) return -1;
                return m2.getStartDate().compareTo(m1.getStartDate());
            })
            .collect(java.util.stream.Collectors.toList());

        model.addAttribute("dirMandate", dirMandate);
        model.addAttribute("vdirMandate", vdirMandate);
        model.addAttribute("teamLeaders", teamLeaders);
        model.addAttribute("previousMandates", previousMandates);

        // Keep legacy variables to prevent template breakage if referenced elsewhere
        String dirName = dirMandate != null && dirMandate.getMember() != null ? 
            dirMandate.getMember().getFirstName() + " " + dirMandate.getMember().getLastName() : "Non désigné";
        String vdirName = vdirMandate != null && vdirMandate.getMember() != null ? 
            vdirMandate.getMember().getFirstName() + " " + vdirMandate.getMember().getLastName() : "Non désigné";
        String leaderNames = teamLeaders.stream()
            .map(m -> m.getMember() != null ? m.getMember().getFirstName() + " " + m.getMember().getLastName() : "N/A")
            .collect(java.util.stream.Collectors.joining(", "));
        
        model.addAttribute("director", dirName);
        model.addAttribute("viceDirector", vdirName);
        model.addAttribute("teamLeadersString", leaderNames);

        return "governance";
    }

    @GetMapping("/mandates")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String mandates(Model model) {
        model.addAttribute("mandates", mandateRepository.findAll());
        model.addAttribute("newMandate", new Mandate());
        model.addAttribute("members", memberRepository.findAll());
        return "mandates";
    }

    @PostMapping("/mandates/save")
    @org.springframework.transaction.annotation.Transactional
    public String saveMandate(@ModelAttribute Mandate mandate) {
        mandate.setStartDate(LocalDate.now());
        mandate.setActive(true);
        if (mandate.getId() != null) {
            Mandate existing = mandateRepository.findById(mandate.getId()).orElse(null);
            if (existing != null) {
                System.out.println("UPDATE ID = " + existing.getId());
                System.out.println("BEFORE SAVE = " + existing);
                existing.setRoleTitle(mandate.getRoleTitle());
                existing.setTeamAssigned(mandate.getTeamAssigned());
                existing.setStartDate(mandate.getStartDate());
                existing.setEndDate(mandate.getEndDate());
                existing.setActive(mandate.isActive());
                existing.setMember(mandate.getMember());
                Mandate saved = mandateRepository.saveAndFlush(existing);
                System.out.println("SAVED = " + saved);
                return "redirect:/director/mandates?success";
            }
        }
        System.out.println("BEFORE SAVE New Mandate = " + mandate);
        Mandate saved = mandateRepository.saveAndFlush(mandate);
        System.out.println("SAVED = " + saved);
        return "redirect:/director/mandates?success";
    }

    @GetMapping("/equipment")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String equipment(Model model) {
        System.out.println("OPENING EQUIPMENT PAGE");
        try {
            List<Publication> publications = publicationService.findAll();
            List<Event> events = eventService.findAll();
            List<Member> members = memberRepository.findAll();
            List<Equipment> equipments = equipmentRepository.findAll();
            List<EquipmentRequest> requests = equipmentRequestRepository.findAll();
            List<EquipmentAssignmentHistory> historyList = equipmentAssignmentHistoryRepository.findAll();

            List<Equipment> equipmentList = equipments != null ? equipments : java.util.Collections.emptyList();
            List<EquipmentRequest> equipmentRequests = requests != null ? requests : java.util.Collections.emptyList();
            List<EquipmentAssignment> assignments = equipmentAssignmentRepository.findAll();
            List<Member> allMembers = members != null ? members : java.util.Collections.emptyList();

            long totalEquipmentCount = equipmentList.stream()
                .filter(e -> e != null)
                .mapToLong(e -> e.getQuantity() != null ? e.getQuantity() : 0)
                .sum();
            long availableEquipmentCount = equipmentList.stream()
                .filter(e -> e != null && ("AVAILABLE".equalsIgnoreCase(e.getStatus()) || e.getStatus() == null || "".equals(e.getStatus().trim())))
                .mapToLong(e -> e.getQuantity() != null ? e.getQuantity() : 0)
                .sum();
            long assignedEquipmentCount = equipmentList.stream()
                .filter(e -> e != null && "ASSIGNED".equalsIgnoreCase(e.getStatus()))
                .mapToLong(e -> e.getQuantity() != null ? e.getQuantity() : 0)
                .sum();
            long pendingRequestsCount = equipmentRequests.stream()
                .filter(r -> r != null && "PENDING".equalsIgnoreCase(r.getStatus()))
                .count();

            model.addAttribute("totalEquipmentCount", totalEquipmentCount);
            model.addAttribute("availableEquipmentCount", availableEquipmentCount);
            model.addAttribute("assignedEquipmentCount", assignedEquipmentCount);
            model.addAttribute("pendingRequestsCount", pendingRequestsCount);

            model.addAttribute("publications", publications != null ? publications : java.util.Collections.emptyList());
            model.addAttribute("events", events != null ? events : java.util.Collections.emptyList());
            model.addAttribute("members", allMembers);
            model.addAttribute("equipments", equipmentList);
            model.addAttribute("requests", equipmentRequests);
            model.addAttribute("history", historyList != null ? historyList : java.util.Collections.emptyList());

            model.addAttribute("equipmentList", equipmentList);
            model.addAttribute("newEquipment", new Equipment());
            model.addAttribute("equipmentRequests", equipmentRequests);
            model.addAttribute("assignments", assignments != null ? assignments : java.util.Collections.emptyList());

            assignments = assignments != null ? assignments : java.util.Collections.emptyList();
            final List<EquipmentAssignment> finalAssignments = assignments;

            List<Member> candidates = allMembers.stream()
                .filter(mem -> mem != null && mem.getStatus() != null)
                .filter(mem -> !"ADMIN".equalsIgnoreCase(mem.getStatus()) &&
                               !"RETIRED".equalsIgnoreCase(mem.getStatus()) &&
                               !"FORMER_MEMBER".equalsIgnoreCase(mem.getStatus()))
                .collect(java.util.stream.Collectors.toList());

            List<Member> membersWhoReceived = candidates.stream()
                .filter(mem -> finalAssignments.stream().anyMatch(asg -> asg != null && asg.getMember() != null && mem.getId() != null && mem.getId().equals(asg.getMember().getId())))
                .collect(java.util.stream.Collectors.toList());

            List<Member> membersWhoDidNotReceive = candidates.stream()
                .filter(mem -> equipmentRequests.stream().anyMatch(req -> req != null && req.getMember() != null && mem.getId() != null && mem.getId().equals(req.getMember().getId()) && "PENDING".equalsIgnoreCase(req.getStatus())))
                .collect(java.util.stream.Collectors.toList());

            List<String> categories = equipmentList.stream()
                .filter(e -> e != null && e.getCategory() != null && !e.getCategory().trim().isEmpty())
                .map(Equipment::getCategory)
                .distinct()
                .sorted()
                .collect(java.util.stream.Collectors.toList());

            model.addAttribute("membersWhoReceived", membersWhoReceived != null ? membersWhoReceived : java.util.Collections.emptyList());
            model.addAttribute("membersWhoDidNotReceive", membersWhoDidNotReceive != null ? membersWhoDidNotReceive : java.util.Collections.emptyList());
            model.addAttribute("categories", categories);
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR IN equipment ENDPOINT: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("errorMessage", "Erreur lors du chargement des matériels : " + e.getMessage());
            model.addAttribute("exceptionClass", e.getClass().getName());
            return "error";
        }
        return "equipment";
    }

    @PostMapping("/equipment/save")
    @org.springframework.transaction.annotation.Transactional
    public String saveEquipment(@ModelAttribute Equipment eq) {
        if (eq.getArrivalDate() == null) {
            eq.setArrivalDate(java.time.LocalDate.now());
        }
        if (eq.getStatus() == null || eq.getStatus().trim().isEmpty()) {
            eq.setStatus("AVAILABLE");
        }
        if (eq.getId() != null) {
            Equipment existing = equipmentRepository.findById(eq.getId()).orElse(null);
            if (existing != null) {
                existing.setName(eq.getName());
                existing.setDescription(eq.getDescription());
                existing.setQuantity(eq.getQuantity());
                existing.setCategory(eq.getCategory());
                existing.setArrivalDate(eq.getArrivalDate());
                existing.setStatus(eq.getStatus());
                existing.setLocation(eq.getLocation());
                equipmentRepository.saveAndFlush(existing);
            } else {
                equipmentRepository.saveAndFlush(eq);
            }
        } else {
            equipmentRepository.saveAndFlush(eq);
        }
        return "redirect:/director/equipment?success";
    }

    @PostMapping("/equipment/delete/{id}")
    @org.springframework.transaction.annotation.Transactional
    public String deleteEquipment(@PathVariable Long id) {
        equipmentRepository.findById(id).ifPresent(eq -> {
            List<EquipmentAssignment> assignments = equipmentAssignmentRepository.findAll();
            for (EquipmentAssignment asg : assignments) {
                if (asg.getEquipment() != null && asg.getEquipment().getId().equals(id)) {
                    equipmentAssignmentRepository.delete(asg);
                }
            }
            equipmentRepository.delete(eq);
        });
        return "redirect:/director/equipment?deleted";
    }

    @PostMapping("/equipment/requests/approve/{id}")
    @org.springframework.transaction.annotation.Transactional
    public String approveEquipmentRequest(@PathVariable Long id) {
        equipmentRequestRepository.findById(id).ifPresent(req -> {
            req.setStatus("APPROVED");
            req.setDecisionDate(LocalDate.now());
            System.out.println("UPDATE ID = " + req.getId());
            System.out.println("BEFORE SAVE = " + req);
            EquipmentRequest savedReq = equipmentRequestRepository.saveAndFlush(req);
            System.out.println("SAVED = " + savedReq);

            // Fetch or create equipment model
            Equipment eq = equipmentRepository.findAll().stream()
                .filter(e -> e.getName().equalsIgnoreCase(req.getEquipmentName()))
                .findFirst()
                .orElseGet(() -> {
                     Equipment newEq = Equipment.builder()
                         .name(req.getEquipmentName())
                         .quantity(req.getQuantity())
                         .status("ASSIGNED")
                         .build();
                     return equipmentRepository.saveAndFlush(newEq);
                });

            eq.setStatus("ASSIGNED");
            equipmentRepository.saveAndFlush(eq);

            // Create assignment (Requirement 12 / 13)
            EquipmentAssignment assignment = EquipmentAssignment.builder()
                .equipment(eq)
                .member(req.getMember())
                .distributionDate(LocalDate.now())
                .build();
            System.out.println("BEFORE SAVE New EquipmentAssignment = " + assignment);
            EquipmentAssignment savedAssignment = equipmentAssignmentRepository.saveAndFlush(assignment);
            System.out.println("SAVED = " + savedAssignment);

            // Equipment Assignment History Auditing Log
            EquipmentAssignmentHistory eqHist = EquipmentAssignmentHistory.builder()
                .equipment(eq)
                .member(req.getMember())
                .actionType("ASSIGNED")
                .actionDate(java.time.LocalDateTime.now())
                .build();
            equipmentAssignmentHistoryRepository.saveAndFlush(eqHist);

            // Log notification (Requirement 17)
            Notification notification = Notification.builder()
                .message("Demande de matériel '" + req.getEquipmentName() + "' approuvée pour " + req.getMember().getFirstName() + " " + req.getMember().getLastName())
                .read(false)
                .type("EQUIPMENT_APPROVED")
                .date(LocalDateTime.now())
                .user(req.getMember().getEmail())
                .build();
            notificationRepository.saveAndFlush(notification);
        });
        return "redirect:/director/equipment?success";
    }

    @PostMapping("/equipment/requests/reject/{id}")
    @org.springframework.transaction.annotation.Transactional
    public String rejectEquipmentRequest(@PathVariable Long id) {
        equipmentRequestRepository.findById(id).ifPresent(req -> {
            req.setStatus("REJECTED");
            req.setDecisionDate(LocalDate.now());
            System.out.println("UPDATE ID = " + req.getId());
            System.out.println("BEFORE SAVE = " + req);
            EquipmentRequest savedReq = equipmentRequestRepository.saveAndFlush(req);
            System.out.println("SAVED = " + savedReq);

            // Log notification (Requirement 17)
            Notification notification = Notification.builder()
                .message("Demande de matériel '" + req.getEquipmentName() + "' rejetée.")
                .read(false)
                .type("EQUIPMENT_REJECTED")
                .date(LocalDateTime.now())
                .user(req.getMember().getEmail())
                .build();
            notificationRepository.saveAndFlush(notification);
        });
        return "redirect:/director/equipment?rejected";
    }

    @GetMapping("/pv")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String pvMeetings(
            @RequestParam(required = false) String search,
            Model model) {
        java.util.List<PVMeeting> meetings = pvMeetingRepository.findAll();
        
        if (search != null && !search.trim().isEmpty()) {
            final String s = search.toLowerCase().trim();
            meetings = meetings.stream()
                .filter(m -> (m.getTitle() != null && m.getTitle().toLowerCase().contains(s))
                          || (m.getAgenda() != null && m.getAgenda().toLowerCase().contains(s)))
                .toList();
        }
        
        java.util.List<PVMeeting> activeMeetings = meetings.stream()
            .filter(m -> m != null && !m.isArchived())
            .toList();
            
        java.util.List<PVMeeting> archivedMeetings = meetings.stream()
            .filter(m -> m != null && m.isArchived())
            .toList();
            
        model.addAttribute("activeMeetings", activeMeetings);
        model.addAttribute("archivedMeetings", archivedMeetings);
        model.addAttribute("newMeeting", new PVMeeting());
        model.addAttribute("search", search);
        model.addAttribute("isDirector", true);
        return "pv";
    }

    @PostMapping("/pv/save")
    @org.springframework.transaction.annotation.Transactional
    public String saveMeeting(@ModelAttribute PVMeeting pv, @RequestParam(value = "pvFile", required = false) org.springframework.web.multipart.MultipartFile pvFile) {
        if (pv.getId() != null) {
            PVMeeting existing = pvMeetingRepository.findById(pv.getId()).orElse(null);
            if (existing != null) {
                System.out.println("UPDATE ID = " + existing.getId());
                System.out.println("BEFORE SAVE = " + existing);
                existing.setTitle(pv.getTitle());
                existing.setWriter(pv.getWriter());
                existing.setType(pv.getType());
                existing.setAgenda(pv.getAgenda());
                existing.setDecisions(pv.getDecisions());
                existing.setLocation(pv.getLocation());
                existing.setDate(pv.getDate() != null ? pv.getDate() : LocalDate.now());
                if (pvFile != null && !pvFile.isEmpty()) {
                    String storedPath = fileStorageService.storeFile(pvFile);
                    existing.setFileName(pvFile.getOriginalFilename());
                    existing.setFilePath(storedPath);
                    existing.setFile(pvFile.getOriginalFilename());
                }
                PVMeeting saved = pvMeetingRepository.saveAndFlush(existing);
                System.out.println("SAVED = " + saved);

                // Notify meeting update (Requirement 17)
                try {
                    Notification notification = Notification.builder()
                        .message("Nouveau procès-verbal de réunion disponible: " + saved.getTitle() + " du " + saved.getDate())
                        .read(false)
                        .type("PV_UPLOAD")
                        .date(LocalDateTime.now())
                        .user("all")
                        .build();
                    notificationRepository.saveAndFlush(notification);
                } catch (Exception e) {
                    System.err.println("Failed to write PV upload notification: " + e.getMessage());
                }

                return "redirect:/director/pv?success";
            }
        }
        System.out.println("BEFORE SAVE New Meeting = " + pv);
        if (pvFile != null && !pvFile.isEmpty()) {
            String storedPath = fileStorageService.storeFile(pvFile);
            pv.setFileName(pvFile.getOriginalFilename());
            pv.setFilePath(storedPath);
            pv.setFile(pvFile.getOriginalFilename());
        }
        if (pv.getDate() == null) {
            pv.setDate(LocalDate.now());
        }
        pv.setUploadDate(LocalDateTime.now());
        PVMeeting saved = pvMeetingRepository.saveAndFlush(pv);
        System.out.println("SAVED = " + saved);

        // Notify meeting creation (Requirement 17)
        try {
            Notification notification = Notification.builder()
                .message("Nouveau procès-verbal de réunion disponible: " + saved.getTitle() + " du " + saved.getDate())
                .read(false)
                .type("PV_UPLOAD")
                .date(LocalDateTime.now())
                .user("all")
                .build();
            notificationRepository.saveAndFlush(notification);
        } catch (Exception e) {
            System.err.println("Failed to write PV upload notification: " + e.getMessage());
        }

        return "redirect:/director/pv?success";
    }

    @PostMapping("/pv/delete/{id}")
    @org.springframework.transaction.annotation.Transactional
    public String deletePV(@PathVariable Long id) {
        pvMeetingRepository.deleteById(id);
        return "redirect:/director/pv?deleted";
    }

    @PostMapping("/pv/approve/{id}")
    @org.springframework.transaction.annotation.Transactional
    public String approvePV(@PathVariable Long id) {
        pvMeetingRepository.findById(id).ifPresent(pv -> {
            pv.setStatus("APPROVED");
            pvMeetingRepository.saveAndFlush(pv);
        });
        return "redirect:/director/pv?approved";
    }

    @PostMapping("/pv/reject/{id}")
    @org.springframework.transaction.annotation.Transactional
    public String rejectPV(@PathVariable Long id) {
        pvMeetingRepository.findById(id).ifPresent(pv -> {
            pv.setStatus("REJECTED");
            pvMeetingRepository.saveAndFlush(pv);
        });
        return "redirect:/director/pv?rejected";
    }

    @PostMapping("/pv/archive/{id}")
    @org.springframework.transaction.annotation.Transactional
    public String archivePV(@PathVariable Long id) {
        pvMeetingRepository.findById(id).ifPresent(pv -> {
            pv.setArchived(!pv.isArchived());
            pvMeetingRepository.saveAndFlush(pv);
        });
        return "redirect:/director/pv?archived";
    }

    @GetMapping("/conventions")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String conventions(
            @RequestParam(required = false) String search,
            Model model) {
        java.util.List<Convention> conventions = conventionRepository.findAll();
        if (search != null && !search.trim().isEmpty()) {
            final String s = search.toLowerCase().trim();
            conventions = conventions.stream()
                .filter(c -> (c.getPartner() != null && c.getPartner().toLowerCase().contains(s))
                          || (c.getDescription() != null && c.getDescription().toLowerCase().contains(s)))
                .toList();
        }
        model.addAttribute("conventions", conventions);
        model.addAttribute("newConvention", new Convention());
        model.addAttribute("search", search);
        return "conventions";
    }

    @PostMapping("/conventions/save")
    @org.springframework.transaction.annotation.Transactional
    public String saveConvention(@ModelAttribute Convention convention, @RequestParam(value = "conventionFile", required = false) org.springframework.web.multipart.MultipartFile conventionFile) {
        if (convention.getId() != null) {
            Convention existing = conventionRepository.findById(convention.getId()).orElse(null);
            if (existing != null) {
                System.out.println("UPDATE ID = " + existing.getId());
                System.out.println("BEFORE SAVE = " + existing);
                existing.setPartner(convention.getPartner());
                existing.setDescription(convention.getDescription());
                existing.setActivitiesTracked(convention.getActivitiesTracked());
                existing.setDate(convention.getDate() != null ? convention.getDate() : java.time.LocalDate.now());
                existing.setStartDate(convention.getStartDate());
                existing.setEndDate(convention.getEndDate());
                if (conventionFile != null && !conventionFile.isEmpty()) {
                    String storedPath = fileStorageService.storeFile(conventionFile);
                    existing.setDocumentPath(storedPath);
                } else if (convention.getDocumentPath() != null && !convention.getDocumentPath().isEmpty()) {
                    existing.setDocumentPath(convention.getDocumentPath());
                }
                Convention saved = conventionRepository.saveAndFlush(existing);
                System.out.println("SAVED = " + saved);
                return "redirect:/director/conventions?success";
            }
        }
        System.out.println("BEFORE SAVE New Convention = " + convention);
        if (convention.getDate() == null) {
            convention.setDate(java.time.LocalDate.now());
        }
        if (conventionFile != null && !conventionFile.isEmpty()) {
            String storedPath = fileStorageService.storeFile(conventionFile);
            convention.setDocumentPath(storedPath);
        }
        Convention saved = conventionRepository.saveAndFlush(convention);
        System.out.println("SAVED = " + saved);
        return "redirect:/director/conventions?success";
    }

    @PostMapping("/conventions/delete/{id}")
    @org.springframework.transaction.annotation.Transactional
    public String deleteConvention(@PathVariable Long id) {
        conventionRepository.deleteById(id);
        return "redirect:/director/conventions?deleted";
    }

    @PostMapping("/conventions/approve/{id}")
    @org.springframework.transaction.annotation.Transactional
    public String approveConvention(@PathVariable Long id) {
        conventionRepository.findById(id).ifPresent(c -> {
            c.setStatus("APPROVED");
            conventionRepository.saveAndFlush(c);

            Notification notification = Notification.builder()
                .message("La convention de partenariat avec '" + c.getPartner() + "' a été approuvée.")
                .read(false)
                .type("CONVENTION_APPROVED")
                .date(LocalDateTime.now())
                .user("all")
                .build();
            notificationRepository.saveAndFlush(notification);
        });
        return "redirect:/director/conventions?approved";
    }

    @PostMapping("/conventions/reject/{id}")
    @org.springframework.transaction.annotation.Transactional
    public String rejectConvention(@PathVariable Long id) {
        conventionRepository.findById(id).ifPresent(c -> {
            c.setStatus("REJECTED");
            conventionRepository.saveAndFlush(c);
        });
        return "redirect:/director/conventions?rejected";
    }

    @GetMapping("/annual-report")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String annualReport(Model model) {
        System.out.println("OPENING ANNUAL REPORT");
        try {
            var publications = publicationService.findAll();
            var events = eventService.findAll();
            var members = memberRepository.findAll();
            var equipments = equipmentRepository.findAll();
            var requests = equipmentRequestRepository.findAll();
            var historyList = equipmentAssignmentHistoryRepository.findAll();
            var assignments = equipmentAssignmentRepository.findAll();

            model.addAttribute("publications", publications != null ? publications : java.util.Collections.emptyList());
            model.addAttribute("events", events != null ? events : java.util.Collections.emptyList());
            model.addAttribute("members", members != null ? members : java.util.Collections.emptyList());
            model.addAttribute("equipments", equipments != null ? equipments : java.util.Collections.emptyList());
            model.addAttribute("requests", requests != null ? requests : java.util.Collections.emptyList());
            model.addAttribute("history", historyList != null ? historyList : java.util.Collections.emptyList());
            model.addAttribute("assignments", assignments != null ? assignments : java.util.Collections.emptyList());

            model.addAttribute("publicationsCount", publications != null ? publications.size() : 0);
            model.addAttribute("membersCount", members != null ? members.size() : 0);
            model.addAttribute("eventsCount", events != null ? events.size() : 0);
            model.addAttribute("equipmentsCount", equipments != null ? equipments.size() : 0);

            long totalResearchers = 0;
            long permanentsCount = 0;
            long doctorantsCount = 0;
            long associatesCount = 0;

            if (members != null) {
                totalResearchers = members.stream()
                    .filter(m -> m != null && !"RETIRED".equalsIgnoreCase(m.getStatus()) && !"FORMER_MEMBER".equalsIgnoreCase(m.getStatus()))
                    .count();
                permanentsCount = members.stream()
                    .filter(m -> m != null && "PERMANENT".equalsIgnoreCase(m.getStatus()))
                    .count();
                doctorantsCount = members.stream()
                    .filter(m -> m != null && "DOCTORANT".equalsIgnoreCase(m.getStatus()))
                    .count();
                associatesCount = members.stream()
                    .filter(m -> m != null && "ASSOCIATE".equalsIgnoreCase(m.getStatus()))
                    .count();
            }

            model.addAttribute("totalResearchers", totalResearchers);
            model.addAttribute("permanentsCount", permanentsCount);
            model.addAttribute("doctorantsCount", doctorantsCount);
            model.addAttribute("associatesCount", associatesCount);

            var teams = teamRepository.findAll();
            java.util.List<TeamActivityStats> teamStats = new java.util.ArrayList<>();
            if (teams != null) {
                for (var t : teams) {
                    if (t == null) continue;
                    Long teamId = t.getId();
                    String teamName = t.getName();
                    if (teamId == null || teamName == null) continue;

                    long mCount = 0;
                    if (members != null) {
                        mCount = members.stream()
                            .filter(m -> m != null && m.getTeam() != null && teamId.equals(m.getTeam().getId()) 
                                    && !"RETIRED".equalsIgnoreCase(m.getStatus()) 
                                    && !"FORMER_MEMBER".equalsIgnoreCase(m.getStatus()))
                            .count();
                    }

                    long pCount = 0;
                    if (publications != null) {
                        pCount = publications.stream()
                            .filter(p -> p != null && p.getTeam() != null && teamName.equalsIgnoreCase(p.getTeam()))
                            .count();
                    }

                    long eCount = 0;
                    if (events != null) {
                        eCount = events.stream()
                            .filter(e -> e != null && e.getOrganizers() != null)
                            .filter(e -> e.getOrganizers().stream()
                                .anyMatch(org -> org != null && org.getTeam() != null && teamId.equals(org.getTeam().getId())))
                            .count();
                    }

                    teamStats.add(new TeamActivityStats(teamName, mCount, pCount, eCount));
                }
            }
            model.addAttribute("teamStats", teamStats);
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR IN annualReport ENDPOINT: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("errorMessage", "Erreur lors de la génération du rapport : " + e.getMessage());
            model.addAttribute("exceptionClass", e.getClass().getName());
            return "error";
        }
        return "annual-report";
    }

    @GetMapping("/annual-report/export")
    @ResponseBody
    public org.springframework.http.ResponseEntity<byte[]> exportReport() {
        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            com.lowagie.text.Document document = new com.lowagie.text.Document(com.lowagie.text.PageSize.A4, 36, 36, 54, 36);
            com.lowagie.text.pdf.PdfWriter.getInstance(document, baos);
            
            document.open();
            
            // Fonts
            com.lowagie.text.Font titleFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 20, new java.awt.Color(0, 51, 102));
            com.lowagie.text.Font subtitleFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_OBLIQUE, 11, java.awt.Color.GRAY);
            com.lowagie.text.Font headerFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 13, new java.awt.Color(0, 51, 102));
            com.lowagie.text.Font textFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA, 9, java.awt.Color.BLACK);
            com.lowagie.text.Font boldFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 9, java.awt.Color.BLACK);
            com.lowagie.text.Font italicFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_OBLIQUE, 9, java.awt.Color.GRAY);
            
            // Header
            com.lowagie.text.Paragraph title = new com.lowagie.text.Paragraph("RAPPORT D'ACTIVITÉ ANNUEL SCIENTIFIQUE\nLABORATOIRE LIAS", titleFont);
            title.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            title.setSpacingAfter(8);
            document.add(title);
            
            com.lowagie.text.Paragraph subtitle = new com.lowagie.text.Paragraph("Généré le: " + java.time.LocalDate.now().toString() + " | Direction du Laboratoire", subtitleFont);
            subtitle.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(25);
            document.add(subtitle);
            
            // Stats Summary Section
            document.add(new com.lowagie.text.Paragraph("1. INDICATEURS SCIENTIFIQUES GÉNÉRAUX", headerFont));
            document.add(new com.lowagie.text.Paragraph("\n"));
            
            List<Member> members = memberRepository.findAll();
            long countActive = 0;
            long countPermanent = 0;
            long countAssociate = 0;
            long countDoctorant = 0;
            if (members != null) {
                countActive = members.stream().filter(m -> m != null && !"RETIRED".equalsIgnoreCase(m.getStatus()) && !"FORMER_MEMBER".equalsIgnoreCase(m.getStatus())).count();
                countPermanent = members.stream().filter(m -> m != null && "PERMANENT".equalsIgnoreCase(m.getStatus())).count();
                countAssociate = members.stream().filter(m -> m != null && "ASSOCIATE".equalsIgnoreCase(m.getStatus())).count();
                countDoctorant = members.stream().filter(m -> m != null && "DOCTORANT".equalsIgnoreCase(m.getStatus())).count();
            }
            
            List<Publication> publications = publicationService.findAll();
            int pubSize = publications != null ? publications.size() : 0;
            List<Event> events = eventService.findAll();
            int evSize = events != null ? events.size() : 0;
            List<Equipment> equipments = equipmentRepository.findAll();
            int eqSize = equipments != null ? equipments.size() : 0;
            
            document.add(new com.lowagie.text.Paragraph("• Effectif total des chercheurs actifs (hors retraités/anciens): " + countActive + " membres", textFont));
            document.add(new com.lowagie.text.Paragraph("    - Permanents: " + countPermanent, textFont));
            document.add(new com.lowagie.text.Paragraph("    - Associés: " + countAssociate, textFont));
            document.add(new com.lowagie.text.Paragraph("    - Doctorants: " + countDoctorant, textFont));
            document.add(new com.lowagie.text.Paragraph("• Total des publications scientifiques répertoriées: " + pubSize, textFont));
            document.add(new com.lowagie.text.Paragraph("• Total des événements académiques organisés: " + evSize, textFont));
            document.add(new com.lowagie.text.Paragraph("• Nombre d'équipements logistiques au laboratoire: " + eqSize, textFont));
            document.add(new com.lowagie.text.Paragraph("\n\n"));
            
            // 2. Research Teams Activity Section
            document.add(new com.lowagie.text.Paragraph("2. ACTIVITÉ DES ÉQUIPES DE RECHERCHE", headerFont));
            document.add(new com.lowagie.text.Paragraph("\n"));
            
            com.lowagie.text.pdf.PdfPTable teamStatsTable = new com.lowagie.text.pdf.PdfPTable(4);
            teamStatsTable.setWidthPercentage(100);
            teamStatsTable.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph("Équipe de recherche", boldFont)));
            teamStatsTable.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph("Membres actifs", boldFont)));
            teamStatsTable.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph("Publications", boldFont)));
            teamStatsTable.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph("Événements", boldFont)));
            
            var teamsList = teamRepository.findAll();
            boolean hasTeams = false;
            if (teamsList != null) {
                for (var t : teamsList) {
                    if (t == null) continue;
                    Long tId = t.getId();
                    String tName = t.getName();
                    if (tId == null || tName == null) continue;
                    hasTeams = true;
                    
                    long mCount = 0;
                    if (members != null) {
                        mCount = members.stream()
                            .filter(m -> m != null && m.getTeam() != null && tId.equals(m.getTeam().getId()) 
                                    && !"RETIRED".equalsIgnoreCase(m.getStatus()) 
                                    && !"FORMER_MEMBER".equalsIgnoreCase(m.getStatus()))
                            .count();
                    }
                    long pCount = 0;
                    if (publications != null) {
                        pCount = publications.stream()
                            .filter(p -> p != null && p.getTeam() != null && tName.equalsIgnoreCase(p.getTeam()))
                            .count();
                    }
                    long eCount = 0;
                    if (events != null) {
                        eCount = events.stream()
                            .filter(e -> e != null && e.getOrganizers() != null)
                            .filter(e -> e.getOrganizers().stream()
                                .anyMatch(org -> org != null && org.getTeam() != null && tId.equals(org.getTeam().getId())))
                            .count();
                    }
                    
                    teamStatsTable.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph(tName, textFont)));
                    teamStatsTable.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph(String.valueOf(mCount), textFont)));
                    teamStatsTable.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph(String.valueOf(pCount), textFont)));
                    teamStatsTable.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph(String.valueOf(eCount), textFont)));
                }
            }
            if (!hasTeams) {
                com.lowagie.text.pdf.PdfPCell emptyCell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph("Aucune équipe de recherche répertoriée", italicFont));
                emptyCell.setColspan(4);
                emptyCell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                teamStatsTable.addCell(emptyCell);
            }
            document.add(teamStatsTable);
            document.add(new com.lowagie.text.Paragraph("\n\n"));
            
            // 3. Equipment Section
            document.add(new com.lowagie.text.Paragraph("3. PARC DES ÉQUIPEMENTS DU LABORATOIRE", headerFont));
            document.add(new com.lowagie.text.Paragraph("\n"));
            
            com.lowagie.text.pdf.PdfPTable eqTable = new com.lowagie.text.pdf.PdfPTable(5);
            eqTable.setWidthPercentage(100);
            eqTable.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph("Équipement", boldFont)));
            eqTable.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph("Catégorie", boldFont)));
            eqTable.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph("Quantité", boldFont)));
            eqTable.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph("Statut", boldFont)));
            eqTable.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph("Chercheur assigné", boldFont)));
            
            var assignments = equipmentAssignmentRepository.findAll();
            boolean hasEquip = false;
            if (equipments != null && !equipments.isEmpty()) {
                hasEquip = true;
                for (Equipment eq : equipments) {
                    if (eq == null) continue;
                    String eqName = eq.getName() != null ? eq.getName() : "Inconnu";
                    String eqCat = eq.getCategory() != null ? eq.getCategory() : "Autre";
                    String eqQty = String.valueOf(eq.getQuantity() != null ? eq.getQuantity() : 0);
                    String eqStatus = eq.getStatus() != null ? eq.getStatus() : "AVAILABLE";
                    
                    StringBuilder assignedStr = new StringBuilder();
                    if (assignments != null) {
                        for (var asg : assignments) {
                            if (asg != null && asg.getEquipment() != null && eq.getId().equals(asg.getEquipment().getId()) && asg.getMember() != null) {
                                if (assignedStr.length() > 0) assignedStr.append(", ");
                                assignedStr.append(asg.getMember().getFirstName() != null ? asg.getMember().getFirstName() : "")
                                           .append(" ")
                                           .append(asg.getMember().getLastName() != null ? asg.getMember().getLastName() : "");
                            }
                        }
                    }
                    if (assignedStr.length() == 0) {
                        assignedStr.append("Aucun");
                    }
                    
                    eqTable.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph(eqName, textFont)));
                    eqTable.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph(eqCat, textFont)));
                    eqTable.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph(eqQty, textFont)));
                    eqTable.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph(eqStatus, textFont)));
                    eqTable.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph(assignedStr.toString(), textFont)));
                }
            }
            if (!hasEquip) {
                com.lowagie.text.pdf.PdfPCell emptyCell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph("Aucun matériel ou équipement répertorié", italicFont));
                emptyCell.setColspan(5);
                emptyCell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                eqTable.addCell(emptyCell);
            }
            document.add(eqTable);
            document.add(new com.lowagie.text.Paragraph("\n\n"));
            
            // 4. Publications Section
            document.add(new com.lowagie.text.Paragraph("4. PRODUCTION SCIENTIFIQUE (PUBLICATIONS)", headerFont));
            document.add(new com.lowagie.text.Paragraph("\n"));
            
            com.lowagie.text.pdf.PdfPTable pTable = new com.lowagie.text.pdf.PdfPTable(4);
            pTable.setWidthPercentage(100);
            pTable.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph("Titre de la publication", boldFont)));
            pTable.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph("Auteur(s)", boldFont)));
            pTable.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph("Année", boldFont)));
            pTable.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph("Équipe", boldFont)));
            
            boolean hasPub = false;
            if (publications != null && !publications.isEmpty()) {
                hasPub = true;
                for (Publication p : publications) {
                    if (p == null) continue;
                    String pTitle = p.getTitle() != null ? p.getTitle() : "";
                    String pAuthor = p.getAuthor() != null ? p.getAuthor() : "";
                    String pYear = String.valueOf(p.getYear());
                    String pTeam = p.getTeam() != null ? p.getTeam() : "Général";
                    
                    pTable.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph(pTitle, textFont)));
                    pTable.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph(pAuthor, textFont)));
                    pTable.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph(pYear, textFont)));
                    pTable.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph(pTeam, textFont)));
                }
            }
            if (!hasPub) {
                com.lowagie.text.pdf.PdfPCell emptyCell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph("Aucune publication enregistrée", italicFont));
                emptyCell.setColspan(4);
                emptyCell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                pTable.addCell(emptyCell);
            }
            document.add(pTable);
            document.add(new com.lowagie.text.Paragraph("\n\n"));
            
            // 5. Events Section
            document.add(new com.lowagie.text.Paragraph("5. ANIMATION SCIENTIFIQUE (ÉVÉNEMENTS)", headerFont));
            document.add(new com.lowagie.text.Paragraph("\n"));
            
            com.lowagie.text.pdf.PdfPTable evTable = new com.lowagie.text.pdf.PdfPTable(5);
            evTable.setWidthPercentage(100);
            evTable.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph("Titre de l'événement", boldFont)));
            evTable.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph("Type", boldFont)));
            evTable.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph("Date", boldFont)));
            evTable.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph("Lieu", boldFont)));
            evTable.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph("Organisateurs", boldFont)));
            
            boolean hasEv = false;
            if (events != null && !events.isEmpty()) {
                hasEv = true;
                for (Event ev : events) {
                    if (ev == null) continue;
                    String evTitle = ev.getTitle() != null ? ev.getTitle() : "";
                    String evType = ev.getType() != null ? ev.getType() : "";
                    String evDate = ev.getDate() != null ? ev.getDate().toString() : "";
                    String evLoc = ev.getLocation() != null ? ev.getLocation() : "";
                    
                    StringBuilder orgs = new StringBuilder();
                    if (ev.getOrganizers() != null) {
                        for (Member org : ev.getOrganizers()) {
                            if (org != null) {
                                if (orgs.length() > 0) orgs.append(", ");
                                orgs.append(org.getFirstName() != null ? org.getFirstName() : "")
                                    .append(" ")
                                    .append(org.getLastName() != null ? org.getLastName() : "");
                            }
                        }
                    }
                    if (orgs.length() == 0) {
                        orgs.append("Non spécifié");
                    }
                    
                    evTable.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph(evTitle, textFont)));
                    evTable.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph(evType, textFont)));
                    evTable.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph(evDate, textFont)));
                    evTable.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph(evLoc, textFont)));
                    evTable.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph(orgs.toString(), textFont)));
                }
            }
            if (!hasEv) {
                com.lowagie.text.pdf.PdfPCell emptyCell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph("Aucun événement enregistré", italicFont));
                emptyCell.setColspan(5);
                emptyCell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                evTable.addCell(emptyCell);
            }
            document.add(evTable);
            
            document.close();
            
            byte[] bytes = baos.toByteArray();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
            headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment().filename("Rapport_Annuel_LIAS.pdf").build());
            
            return new org.springframework.http.ResponseEntity<>(bytes, headers, org.springframework.http.HttpStatus.OK);
        } catch (Exception ex) {
            ex.printStackTrace();
            byte[] errorBytes = ("Erreur lors de la génération du PDF: " + ex.getMessage()).getBytes(java.nio.charset.StandardCharsets.UTF_8);
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.TEXT_PLAIN);
            return new org.springframework.http.ResponseEntity<>(errorBytes, headers, org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/teams")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String directorTeams(Model model) {
        List<Team> teams = teamRepository.findAll();
        model.addAttribute("teams", teams);
        model.addAttribute("newTeam", new Team());

        List<Publication> allPubs = publicationService.findAll();
        var allMembers = memberRepository.findAll();

        // Map teamId to publication rates (Pub count / researcher count)
        java.util.Map<Long, Double> publicationRates = new java.util.HashMap<>();
        java.util.Map<Long, Long> teamPubCounts = new java.util.HashMap<>();

        for (Team t : teams) {
            long pubCount = allPubs.stream()
                .filter(p -> p.getTeam() != null && p.getTeam().equalsIgnoreCase(t.getName()))
                .count();
            teamPubCounts.put(t.getId(), pubCount);

            long memberSize = allMembers.stream()
                .filter(m -> m.getTeam() != null && m.getTeam().getId().equals(t.getId()))
                .count();

            double rate = memberSize > 0 ? (double) pubCount / memberSize : 0.0;
            publicationRates.put(t.getId(), rate);
        }

        model.addAttribute("teamPubCounts", teamPubCounts);
        model.addAttribute("publicationRates", publicationRates);
        return "director-teams";
    }

    @PostMapping("/teams/save")
    @org.springframework.transaction.annotation.Transactional
    public String saveTeam(@ModelAttribute Team team) {
        Team saved;
        if (team.getId() != null) {
            Team existing = teamRepository.findById(team.getId()).orElse(null);
            if (existing != null) {
                System.out.println("UPDATE ID = " + existing.getId());
                System.out.println("BEFORE SAVE = " + existing);
                existing.setName(team.getName());
                existing.setDescription(team.getDescription());
                saved = teamRepository.saveAndFlush(existing);
                System.out.println("SAVED = " + saved);
            } else {
                System.out.println("BEFORE SAVE New Team = " + team);
                saved = teamRepository.saveAndFlush(team);
                System.out.println("SAVED = " + saved);
            }
        } else {
            System.out.println("BEFORE SAVE New Team = " + team);
            saved = teamRepository.saveAndFlush(team);
            System.out.println("SAVED = " + saved);
        }
        
        Notification notification = Notification.builder()
            .message("Nouveau budget d'axe et équipe '" + saved.getName() + "' formalisés.")
            .read(false)
            .type("TEAM_BUDGET")
            .date(LocalDateTime.now())
            .user("all")
            .build();
        notificationRepository.saveAndFlush(notification);
        
        return "redirect:/director/teams?success";
    }

    @GetMapping("/teams/delete/{id}")
    public String deleteTeam(@PathVariable Long id) {
        teamRepository.deleteById(id);
        return "redirect:/director/teams?deleted";
    }

    private String resolveCurrentUserEmail() {
        String loginName = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        var userOpt = userRepository.findByEmail(loginName);
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
        Member director = memberRepository.findByEmail(email)
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
        model.addAttribute("member", director);
        if (director != null && director.getId() != null) {
            model.addAttribute("history", memberProfileHistoryRepository.findByMemberIdOrderByChangeDateDesc(director.getId()));
            model.addAttribute("roleHistory", roleHistoryRepository.findByMemberId(director.getId()));
            model.addAttribute("affiliationHistory", memberAffiliationHistoryRepository.findByMemberIdOrderByStartDateDesc(director.getId()));
            
            // Retrieve member's publications
            String ln = director.getLastName() != null ? director.getLastName().toLowerCase() : "";
            String fn = director.getFirstName() != null ? director.getFirstName().toLowerCase() : "";
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
        return "redirect:/director/profile?success";
    }

    @ExceptionHandler(Exception.class)
    public String handleException(Exception e, Model model) {
        model.addAttribute("errorMessage", e.getMessage() != null ? e.getMessage() : "Une erreur de traitement interne est survenue.");
        model.addAttribute("exceptionClass", e.getClass().getName());
        return "error";
    }

    public static class TeamActivityStats {
        private final String teamName;
        private final long membersCount;
        private final long publicationsCount;
        private final long eventsCount;

        public TeamActivityStats(String teamName, long membersCount, long publicationsCount, long eventsCount) {
            this.teamName = teamName;
            this.membersCount = membersCount;
            this.publicationsCount = publicationsCount;
            this.eventsCount = eventsCount;
        }

        public String getTeamName() { return teamName; }
        public long getMembersCount() { return membersCount; }
        public long getPublicationsCount() { return publicationsCount; }
        public long getEventsCount() { return eventsCount; }
    }
}
