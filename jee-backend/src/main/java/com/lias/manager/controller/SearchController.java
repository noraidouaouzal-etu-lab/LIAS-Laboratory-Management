package com.lias.manager.controller;

import com.lias.manager.entity.Member;
import com.lias.manager.entity.Document;
import com.lias.manager.entity.Event;
import com.lias.manager.entity.Publication;
import com.lias.manager.repository.MemberRepository;
import com.lias.manager.repository.DocumentRepository;
import com.lias.manager.repository.EventRepository;
import com.lias.manager.repository.PublicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class SearchController {

    private final MemberRepository memberRepository;
    private final DocumentRepository documentRepository;
    private final EventRepository eventRepository;
    private final PublicationRepository publicationRepository;

    @GetMapping("/search")
    public String search(@RequestParam(value = "q", required = false, defaultValue = "") String query, Model model) {
        String cleanQuery = query.trim().toLowerCase();
        model.addAttribute("query", query);

        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
        
        boolean isDirector = false;
        boolean isMember = false;
        
        if (isAuthenticated) {
            isDirector = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_DIRECTOR"));
            isMember = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_MEMBER") || 
                                   a.getAuthority().equals("ROLE_DOCTORANT") || 
                                   a.getAuthority().equals("ROLE_ASSOCIATE") ||
                                   a.getAuthority().equals("ROLE_ADMIN"));
        }

        model.addAttribute("isAuthenticated", isAuthenticated);
        model.addAttribute("isDirector", isDirector);
        model.addAttribute("isMember", isMember);

        if (cleanQuery.isEmpty()) {
            model.addAttribute("members", List.of());
            model.addAttribute("documents", List.of());
            model.addAttribute("events", List.of());
            model.addAttribute("publications", List.of());
            return "search";
        }

        final boolean finalIsDirector = isDirector;
        final boolean finalIsMember = isMember;

        List<Member> members = memberRepository.findAll().stream()
                .filter(m -> {
                    if (m == null) return false;
                    String first = m.getFirstName();
                    String last = m.getLastName();
                    String email = m.getEmail();
                    String inst = m.getInstitution();
                    var team = m.getTeam();
                    String teamName = team != null ? team.getName() : null;

                    return (first != null && first.toLowerCase().contains(cleanQuery)) ||
                           (last != null && last.toLowerCase().contains(cleanQuery)) ||
                           (email != null && email.toLowerCase().contains(cleanQuery)) ||
                           (inst != null && inst.toLowerCase().contains(cleanQuery)) ||
                           (teamName != null && teamName.toLowerCase().contains(cleanQuery));
                })
                .collect(Collectors.toList());

        List<Publication> publications = publicationRepository.findAll().stream()
                .filter(p -> {
                    if (p == null) return false;
                    String title = p.getTitle();
                    String author = p.getAuthor();
                    String team = p.getTeam();
                    Integer year = p.getYear();

                    return (title != null && title.toLowerCase().contains(cleanQuery)) ||
                           (author != null && author.toLowerCase().contains(cleanQuery)) ||
                           (team != null && team.toLowerCase().contains(cleanQuery)) ||
                           (year != null && String.valueOf(year).contains(cleanQuery));
                })
                .collect(Collectors.toList());

        List<Event> events = eventRepository.findAll().stream()
                .filter(e -> {
                    if (e == null) return false;
                    
                    // Security filter
                    if (!finalIsDirector) {
                        // Visitors and Members can only search non-archived events
                        if (e.isArchived()) return false;
                    }
                    
                    String title = e.getTitle();
                    String type = e.getType();
                    String location = e.getLocation();

                    return (title != null && title.toLowerCase().contains(cleanQuery)) ||
                           (type != null && type.toLowerCase().contains(cleanQuery)) ||
                           (location != null && location.toLowerCase().contains(cleanQuery));
                })
                .collect(Collectors.toList());

        List<Document> documents;
        if (finalIsDirector || finalIsMember) {
            documents = documentRepository.findAll().stream()
                    .filter(d -> {
                        if (d == null) return false;
                        
                        // Security check
                        if (finalIsMember && d.isArchived()) return false;
                        
                        String name = d.getName();
                        String type = d.getType();

                        return (name != null && name.toLowerCase().contains(cleanQuery)) ||
                               (type != null && type.toLowerCase().contains(cleanQuery));
                    })
                    .collect(Collectors.toList());
        } else {
            documents = List.of();
        }

        model.addAttribute("members", members);
        model.addAttribute("documents", documents);
        model.addAttribute("events", events);
        model.addAttribute("publications", publications);

        return "search";
    }
}
