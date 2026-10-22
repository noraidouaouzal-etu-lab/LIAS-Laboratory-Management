package com.lias.manager.controller;

import com.lias.manager.entity.Member;
import com.lias.manager.entity.Event;
import com.lias.manager.entity.Publication;
import com.lias.manager.entity.Document;
import com.lias.manager.entity.MemberProfileHistory;
import com.lias.manager.service.MemberService;
import com.lias.manager.service.PublicationService;
import com.lias.manager.repository.DocumentRepository;
import com.lias.manager.repository.TeamRepository;
import com.lias.manager.service.EventService;
import com.lias.manager.repository.NotificationRepository;
import com.lias.manager.repository.MemberRepository;
import com.lias.manager.repository.MemberProfileHistoryRepository;
import com.lias.manager.repository.RoleHistoryRepository;
import com.lias.manager.repository.MemberAffiliationHistoryRepository;
import com.lias.manager.repository.MessageRepository;
import com.lias.manager.repository.EquipmentRequestRepository;
import com.lias.manager.repository.EquipmentAssignmentRepository;
import com.lias.manager.entity.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
@RequestMapping("/member")
public class MemberController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MemberController.class);

    private final MemberService memberService;
    private final PublicationService publicationService;
    private final DocumentRepository documentRepository;
    private final TeamRepository teamRepository;
    private final EventService eventService;
    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;
    private final MemberProfileHistoryRepository memberProfileHistoryRepository;
    private final RoleHistoryRepository roleHistoryRepository;
    private final MemberAffiliationHistoryRepository memberAffiliationHistoryRepository;
    private final EquipmentRequestRepository equipmentRequestRepository;
    private final EquipmentAssignmentRepository equipmentAssignmentRepository;
    private final MessageRepository messageRepository;
    private final com.lias.manager.service.FileStorageService fileStorageService;
    private final com.lias.manager.repository.DocumentHistoryRepository documentHistoryRepository;
    private final com.lias.manager.repository.UserRepository userRepository;
    private final com.lias.manager.repository.ConventionRepository conventionRepository;
    private final com.lias.manager.repository.PVMeetingRepository pvMeetingRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        String email = resolveCurrentUserEmail();
        log.info("Accessing member dashboard. Current user: {}", email);
        Member member = memberRepository.findByEmail(email).orElse(null);
        if (member != null) {
            log.info("Member found: ID={}, Name={} {}", member.getId(), member.getFirstName(), member.getLastName());
            model.addAttribute("currentUserMember", member);
            
            String teamName = member.getTeam() != null ? member.getTeam().getName() : "Non renseigné";
            model.addAttribute("teamName", teamName);
            
            long doctorantsCount = 0;
            if (member.getTeam() != null) {
                doctorantsCount = memberRepository.findAll().stream()
                    .filter(m -> "DOCTORANT".equalsIgnoreCase(m.getStatus()))
                    .filter(m -> m.getTeam() != null && m.getTeam().getId().equals(member.getTeam().getId()))
                    .count();
            }
            model.addAttribute("doctorantsCount", doctorantsCount);
            
            String ln = member.getLastName() != null ? member.getLastName().toLowerCase() : "";
            String fn = member.getFirstName() != null ? member.getFirstName().toLowerCase() : "";
            long myPubsCount = publicationService.findAll().stream()
                .filter(p -> p != null && p.getAuthor() != null && 
                    (p.getAuthor().toLowerCase().contains(ln) || p.getAuthor().toLowerCase().contains(fn)))
                .count();
            model.addAttribute("myPublicationsCount", myPubsCount);
        } else {
            log.warn("Member profile not found for user: {}", email);
            model.addAttribute("teamName", "Non renseigné");
            model.addAttribute("doctorantsCount", 0);
            model.addAttribute("myPublicationsCount", 0);
        }
        return "dashboard-member";
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
        Member member = memberRepository.findByEmail(email)
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
        model.addAttribute("member", member);
        if (member != null && member.getId() != null) {
            model.addAttribute("history", memberProfileHistoryRepository.findByMemberIdOrderByChangeDateDesc(member.getId()));
            model.addAttribute("roleHistory", roleHistoryRepository.findByMemberId(member.getId()));
            model.addAttribute("affiliationHistory", memberAffiliationHistoryRepository.findByMemberIdOrderByStartDateDesc(member.getId()));
            
            // Retrieve member's publications
            String ln = member.getLastName() != null ? member.getLastName().toLowerCase() : "";
            String fn = member.getFirstName() != null ? member.getFirstName().toLowerCase() : "";
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
        return "redirect:/member/profile?success";
    }

    @GetMapping("/profile/{id}")
    public String viewProfile(@PathVariable Long id, Model model) {
        Member member = memberRepository.findById(id).orElse(null);
        model.addAttribute("member", member);
        if (member != null) {
            model.addAttribute("history", memberProfileHistoryRepository.findByMemberIdOrderByChangeDateDesc(member.getId()));
            model.addAttribute("roleHistory", roleHistoryRepository.findByMemberId(member.getId()));
            model.addAttribute("affiliationHistory", memberAffiliationHistoryRepository.findByMemberIdOrderByStartDateDesc(member.getId()));
            model.addAttribute("consultOnly", true);
            
            // Retrieve member's publications
            String ln = member.getLastName() != null ? member.getLastName().toLowerCase() : "";
            String fn = member.getFirstName() != null ? member.getFirstName().toLowerCase() : "";
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

    @GetMapping("/members")
    public String listMembers(Model model) {
        model.addAttribute("members", memberService.findAll());
        return "members";
    }

    @GetMapping("/documents")
    public String listDocuments(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String filterType,
            Model model) {
        java.util.List<com.lias.manager.entity.Document> docs = documentRepository.findAll();
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

        java.util.List<com.lias.manager.entity.Document> activeDocs = docs.stream().filter(d -> d != null && !d.isArchived()).toList();
        java.util.List<com.lias.manager.entity.Document> archivedDocs = docs.stream().filter(d -> d != null && d.isArchived()).toList();

        model.addAttribute("activeDocuments", activeDocs);
        model.addAttribute("archivedDocuments", archivedDocs);
        
        java.util.List<com.lias.manager.entity.DocumentHistory> historyList = documentHistoryRepository.findAll();
        model.addAttribute("historyList", historyList);

        model.addAttribute("newDocument", new com.lias.manager.entity.Document());
        model.addAttribute("search", search);
        model.addAttribute("filterType", filterType);
        model.addAttribute("isDirector", false);
        return "documents";
    }

    @GetMapping("/publications")
    public String publications(
            @RequestParam(required = false) String author,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String team,
            Model model) {
        model.addAttribute("publications", publicationService.searchAndFilter(author, year, team));
        model.addAttribute("newPublication", new Publication());
        model.addAttribute("author", author);
        model.addAttribute("year", year);
        model.addAttribute("team", team);
        return "publications";
    }

    @PostMapping("/publications")
    @org.springframework.transaction.annotation.Transactional
    public String savePublication(@ModelAttribute Publication publication) {
        publicationService.save(publication);

        try {
            com.lias.manager.entity.Notification notification = com.lias.manager.entity.Notification.builder()
                .message(publication.getAuthor() + " a publié un nouvel article: " + publication.getTitle())
                .read(false)
                .type("NEW_PUBLICATION")
                .date(LocalDateTime.now())
                .user("all")
                .build();
            notificationRepository.saveAndFlush(notification);
        } catch (Exception e) {
            System.err.println("Failed to write publication notification: " + e.getMessage());
        }

        return "redirect:/member/publications?success";
    }

    @GetMapping("/events")
    public String events(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String filterType,
            Model model) {
        java.util.List<Event> allEvents = eventService.findAll();
        
        java.util.stream.Stream<Event> stream = allEvents.stream();
        if (search != null && !search.trim().isEmpty()) {
            String lowerSearch = search.trim().toLowerCase();
            stream = stream.filter(e -> e.getTitle() != null && e.getTitle().toLowerCase().contains(lowerSearch));
        }
        if (filterType != null && !filterType.trim().isEmpty() && !"ALL".equalsIgnoreCase(filterType)) {
            stream = stream.filter(e -> filterType.equalsIgnoreCase(e.getType()));
        }
        
        java.util.List<Event> filteredList = stream.collect(java.util.stream.Collectors.toList());
        
        java.util.List<Event> upcomingEvents = filteredList.stream()
                .filter(e -> e != null && !e.isArchived())
                .collect(java.util.stream.Collectors.toList());
                
        java.util.List<Event> archivedEvents = filteredList.stream()
                .filter(e -> e != null && e.isArchived())
                .collect(java.util.stream.Collectors.toList());
                
        model.addAttribute("upcomingEvents", upcomingEvents);
        model.addAttribute("archivedEvents", archivedEvents);
        model.addAttribute("events", filteredList);
        
        model.addAttribute("search", search);
        model.addAttribute("filterType", filterType);
        
        model.addAttribute("newEvent", new Event());
        model.addAttribute("allMembers", memberRepository.findAll());
        return "events";
    }


    @GetMapping("/equipment")
    public String memberEquipment(Model model) {
        String email = resolveCurrentUserEmail();
        Member member = memberRepository.findByEmail(email).orElse(null);
        model.addAttribute("member", member);
        if (member != null) {
            model.addAttribute("requests", equipmentRequestRepository.findByMemberIdOrderByRequestDateDesc(member.getId()));
            model.addAttribute("assignments", equipmentAssignmentRepository.findByMemberId(member.getId()));
        } else {
            model.addAttribute("requests", java.util.List.of());
            model.addAttribute("assignments", java.util.List.of());
        }
        return "member-equipment";
    }

    @PostMapping("/equipment/request")
    @org.springframework.transaction.annotation.Transactional
    public String requestEquipment(@RequestParam String equipmentName, @RequestParam Integer quantity) {
        String email = resolveCurrentUserEmail();
        Member member = memberRepository.findByEmail(email).orElse(null);
        if (member != null) {
            if (!"PERMANENT".equalsIgnoreCase(member.getStatus())) {
                return "redirect:/member/equipment?error=cahier_not_allowed";
            }
            var req = com.lias.manager.entity.EquipmentRequest.builder()
                .member(member)
                .equipmentName(equipmentName.trim())
                .quantity(quantity)
                .status("PENDING")
                .requestDate(java.time.LocalDate.now())
                .build();
            System.out.println("BEFORE SAVE New EquipmentRequest = " + req);
            com.lias.manager.entity.EquipmentRequest saved = equipmentRequestRepository.saveAndFlush(req);
            System.out.println("SAVED = " + saved);

            // Log notification to DIRECTOR (Requirement 17)
            try {
                com.lias.manager.entity.Notification notification = com.lias.manager.entity.Notification.builder()
                    .message("Demande de matériel " + equipmentName.trim() + " soumise par " + member.getFirstName() + " " + member.getLastName())
                    .read(false)
                    .type("EQUIPMENT_REQUEST")
                    .date(LocalDateTime.now())
                    .user("director@lias-lab.ma")
                    .build();
                notificationRepository.saveAndFlush(notification);
            } catch (Exception e) {
                System.err.println("Failed to write equipment request notification: " + e.getMessage());
            }
        }
        return "redirect:/member/equipment?success";
    }

    @GetMapping("/conventions")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String memberConventions(
            @RequestParam(required = false) String search,
            Model model) {
        java.util.List<com.lias.manager.entity.Convention> conventions = conventionRepository.findAll();
        if (search != null && !search.trim().isEmpty()) {
            final String s = search.toLowerCase().trim();
            conventions = conventions.stream()
                .filter(c -> (c.getPartner() != null && c.getPartner().toLowerCase().contains(s))
                          || (c.getDescription() != null && c.getDescription().toLowerCase().contains(s)))
                .toList();
        }
        model.addAttribute("conventions", conventions);
        model.addAttribute("newConvention", new com.lias.manager.entity.Convention());
        model.addAttribute("search", search);
        return "conventions";
    }

    @GetMapping("/pv")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String pvMeetings(
            @RequestParam(required = false) String search,
            Model model) {
        java.util.List<com.lias.manager.entity.PVMeeting> meetings = pvMeetingRepository.findAll();
        
        if (search != null && !search.trim().isEmpty()) {
            final String s = search.toLowerCase().trim();
            meetings = meetings.stream()
                .filter(m -> (m.getTitle() != null && m.getTitle().toLowerCase().contains(s))
                          || (m.getAgenda() != null && m.getAgenda().toLowerCase().contains(s)))
                .toList();
        }
        
        java.util.List<com.lias.manager.entity.PVMeeting> activeMeetings = meetings.stream()
            .filter(m -> m != null && !m.isArchived())
            .toList();
            
        java.util.List<com.lias.manager.entity.PVMeeting> archivedMeetings = meetings.stream()
            .filter(m -> m != null && m.isArchived())
            .toList();
            
        model.addAttribute("activeMeetings", activeMeetings);
        model.addAttribute("archivedMeetings", archivedMeetings);
        model.addAttribute("newMeeting", new com.lias.manager.entity.PVMeeting());
        model.addAttribute("search", search);
        model.addAttribute("isDirector", false);
        return "pv";
    }
}
