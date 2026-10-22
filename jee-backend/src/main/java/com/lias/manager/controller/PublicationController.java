package com.lias.manager.controller;

import com.lias.manager.entity.Publication;
import com.lias.manager.entity.Notification;
import com.lias.manager.service.PublicationService;
import com.lias.manager.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class PublicationController {

    private final PublicationService publicationService;
    private final NotificationRepository notificationRepository;

    private String getRedirectPath(Authentication auth) {
        if (auth != null) {
            var roles = auth.getAuthorities().stream().map(a -> a.getAuthority()).collect(Collectors.toSet());
            if (roles.contains("ROLE_DIRECTOR")) {
                return "/director/publications";
            } else if (roles.contains("ROLE_MEMBER")) {
                return "/member/publications";
            } else if (roles.contains("ROLE_DOCTORANT")) {
                return "/doctorant/publications";
            } else if (roles.contains("ROLE_ASSOCIATE")) {
                return "/associate/publications";
            }
        }
        return "/publications";
    }

    @GetMapping("/director/publications")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String directorPublications(Model model) {
        model.addAttribute("newPublication", new Publication());
        return "publications";
    }

    @PostMapping({"/director/publications/save", "/member/publications/save", "/doctorant/publications/save"})
    @org.springframework.transaction.annotation.Transactional
    public String savePublication(@ModelAttribute Publication publication) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/publications?error=unauthorized";
        }

        String username = auth.getName();
        boolean isDirector = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_DIRECTOR"));
        boolean isMember = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_MEMBER"));
        boolean isDoctorant = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_DOCTORANT"));

        if (!isDirector && !isMember && !isDoctorant) {
            return "redirect:" + getRedirectPath(auth) + "?error=unauthorized";
        }

        // Check ownership if editing
        if (publication.getId() != null) {
            Publication existing = publicationService.findById(publication.getId()).orElse(null);
            if (existing != null) {
                if (!isDirector && !username.equalsIgnoreCase(existing.getCreatedBy())) {
                    return "redirect:" + getRedirectPath(auth) + "?error=unauthorized";
                }
                // Retain owner
                publication.setCreatedBy(existing.getCreatedBy());
            } else {
                publication.setCreatedBy(username);
            }
        } else {
            // New publication
            publication.setCreatedBy(username);
        }

        publicationService.save(publication);

        try {
            Notification notification = Notification.builder()
                    .message(publication.getAuthor() + " a publié un nouvel article : " + publication.getTitle())
                    .read(false)
                    .type("NEW_PUBLICATION")
                    .date(LocalDateTime.now())
                    .user("all")
                    .build();
            notificationRepository.saveAndFlush(notification);
        } catch (Exception e) {
            System.err.println("Failed to write publication notification: " + e.getMessage());
        }

        return "redirect:" + getRedirectPath(auth) + "?success";
    }

    @GetMapping({"/director/publications/edit/{id}", "/member/publications/edit/{id}", "/doctorant/publications/edit/{id}"})
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String editPublicationForm(@PathVariable Long id, Model model) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/publications?error=unauthorized";
        }

        Publication pub = publicationService.findById(id).orElseThrow(() -> new IllegalArgumentException("Publication non trouvée"));

        String username = auth.getName();
        boolean isDirector = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_DIRECTOR"));

        if (!isDirector && !username.equalsIgnoreCase(pub.getCreatedBy())) {
            return "redirect:" + getRedirectPath(auth) + "?error=unauthorized";
        }

        model.addAttribute("newPublication", pub);
        return "publications";
    }

    @GetMapping({"/director/publications/delete/{id}", "/member/publications/delete/{id}", "/doctorant/publications/delete/{id}"})
    @org.springframework.transaction.annotation.Transactional
    public String deletePublication(@PathVariable Long id) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/publications?error=unauthorized";
        }

        String username = auth.getName();
        boolean isDirector = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_DIRECTOR"));

        Publication pub = publicationService.findById(id).orElse(null);
        if (pub != null) {
            if (isDirector || username.equalsIgnoreCase(pub.getCreatedBy())) {
                publicationService.deleteById(id);
            } else {
                return "redirect:" + getRedirectPath(auth) + "?error=unauthorized";
            }
        }

        return "redirect:" + getRedirectPath(auth) + "?deleted";
    }
}

@Component
class PublicationInterceptor implements HandlerInterceptor {

    @Autowired
    private PublicationService publicationService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String method = request.getMethod();
        if ("POST".equalsIgnoreCase(method)) {
            String uri = request.getRequestURI();
            // Prevent directly POSTing to base controller paths for member/doctorant/associate to bypass security
            if (uri.endsWith("/member/publications") || uri.endsWith("/doctorant/publications") || uri.endsWith("/associate/publications")) {
                var auth = SecurityContextHolder.getContext().getAuthentication();
                String redirectPath = "/publications";
                if (auth != null) {
                    var roles = auth.getAuthorities().stream().map(a -> a.getAuthority()).collect(Collectors.toSet());
                    if (roles.contains("ROLE_DIRECTOR")) redirectPath = "/director/publications";
                    else if (roles.contains("ROLE_MEMBER")) redirectPath = "/member/publications";
                    else if (roles.contains("ROLE_DOCTORANT")) redirectPath = "/doctorant/publications";
                    else if (roles.contains("ROLE_ASSOCIATE")) redirectPath = "/associate/publications";
                }
                response.sendRedirect(request.getContextPath() + redirectPath + "?error=unauthorized");
                return false;
            }
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        if (modelAndView != null && modelAndView.hasView()) {
            String viewName = modelAndView.getViewName();
            if ("publications".equals(viewName) || "public-publications".equals(viewName)) {
                // Get query parameters
                String title = request.getParameter("title");
                String author = request.getParameter("author");
                String team = request.getParameter("team");

                String yearStr = request.getParameter("year");
                Integer year = null;
                if (yearStr != null && !yearStr.trim().isEmpty()) {
                    try {
                        year = Integer.parseInt(yearStr.trim());
                    } catch (NumberFormatException e) {
                        // ignore invalid year search format
                    }
                }

                String sortBy = request.getParameter("sortBy");
                String sortDir = request.getParameter("sortDir");

                // Get all publications from DB
                List<Publication> list = publicationService.findAll();

                // Apply search and filter (Requirement 3)
                final String fTitle = (title != null) ? title.trim().toLowerCase() : "";
                final String fAuthor = (author != null) ? author.trim().toLowerCase() : "";
                final String fTeam = (team != null) ? team.trim().toLowerCase() : "";
                final Integer fYear = year;

                List<Publication> filtered = list.stream()
                        .filter(p -> p != null)
                        .filter(p -> fTitle.isEmpty() || (p.getTitle() != null && p.getTitle().toLowerCase().contains(fTitle)))
                        .filter(p -> fAuthor.isEmpty() || (p.getAuthor() != null && p.getAuthor().toLowerCase().contains(fAuthor)))
                        .filter(p -> fTeam.isEmpty() || (p.getTeam() != null && p.getTeam().toLowerCase().contains(fTeam)))
                        .filter(p -> fYear == null || (p.getYear() != null && p.getYear().equals(fYear)))
                        .collect(Collectors.toList());

                // Apply sorting (Requirement 4)
                java.util.Comparator<Publication> comparator = null;
                if ("title".equalsIgnoreCase(sortBy)) {
                    comparator = java.util.Comparator.comparing(
                            p -> p.getTitle() != null ? p.getTitle().toLowerCase() : "",
                            java.util.Comparator.naturalOrder()
                    );
                } else if ("author".equalsIgnoreCase(sortBy)) {
                    comparator = java.util.Comparator.comparing(
                            p -> p.getAuthor() != null ? p.getAuthor().toLowerCase() : "",
                            java.util.Comparator.naturalOrder()
                    );
                } else if ("year".equalsIgnoreCase(sortBy)) {
                    comparator = java.util.Comparator.comparing(
                            p -> p.getYear() != null ? p.getYear() : 0,
                            java.util.Comparator.naturalOrder()
                    );
                }

                if (comparator != null) {
                    if ("desc".equalsIgnoreCase(sortDir)) {
                        comparator = comparator.reversed();
                    }
                    filtered.sort(comparator);
                } else {
                    // Default sort: Year descending, then title ascending
                    java.util.Comparator<Publication> defaultComp = java.util.Comparator.comparing(
                            (Publication p) -> p.getYear() != null ? p.getYear() : 0
                    ).reversed().thenComparing(
                            p -> p.getTitle() != null ? p.getTitle().toLowerCase() : ""
                    );
                    filtered.sort(defaultComp);
                }

                // Add to model (overriding whatever is there)
                modelAndView.addObject("publications", filtered);

                // Add filter values back to model
                modelAndView.addObject("title", title);
                modelAndView.addObject("author", author);
                modelAndView.addObject("team", team);
                modelAndView.addObject("year", yearStr);
                modelAndView.addObject("sortBy", sortBy);
                modelAndView.addObject("sortDir", sortDir);

                // Add statistics (Requirement 6)
                long totalPublications = list.size();
                int currentYear = java.time.LocalDate.now().getYear();
                long publicationsThisYear = list.stream()
                        .filter(p -> p != null && p.getYear() != null && p.getYear() == currentYear)
                        .count();
                long uniqueTeamsCount = list.stream()
                        .filter(p -> p != null && p.getTeam() != null && !p.getTeam().trim().isEmpty())
                        .map(p -> p.getTeam().trim().toLowerCase())
                        .distinct()
                        .count();

                modelAndView.addObject("totalPublications", totalPublications);
                modelAndView.addObject("publicationsThisYear", publicationsThisYear);
                modelAndView.addObject("uniqueTeamsCount", uniqueTeamsCount);

                // Add current logged-in user details
                var auth = SecurityContextHolder.getContext().getAuthentication();
                String username = (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) ? auth.getName() : "";
                modelAndView.addObject("currentUsername", username);
            }
        }
    }
}

@Configuration
class PublicationWebConfig implements WebMvcConfigurer {
    @Autowired
    private PublicationInterceptor publicationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(publicationInterceptor)
                .addPathPatterns("/director/publications", "/member/publications", "/doctorant/publications", "/associate/publications", "/publications", "/public-publications");
    }
}
