package com.lias.manager.controller;

import com.lias.manager.entity.Event;
import com.lias.manager.entity.Member;
import com.lias.manager.entity.Notification;
import com.lias.manager.service.EventService;
import com.lias.manager.repository.MemberRepository;
import com.lias.manager.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.HashSet;

@Controller
@RequiredArgsConstructor
@RequestMapping("/director/events")
public class EventController {

    private final EventService eventService;
    private final MemberRepository memberRepository;
    private final NotificationRepository notificationRepository;

    @GetMapping
    public String listEvents(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String filterType,
            Model model) {
        List<Event> allEvents = eventService.findAll();
        
        // Apply filters using Java Streams
        java.util.stream.Stream<Event> stream = allEvents.stream();
        if (search != null && !search.trim().isEmpty()) {
            String lowerSearch = search.trim().toLowerCase();
            stream = stream.filter(e -> e.getTitle() != null && e.getTitle().toLowerCase().contains(lowerSearch));
        }
        if (filterType != null && !filterType.trim().isEmpty() && !"ALL".equalsIgnoreCase(filterType)) {
            stream = stream.filter(e -> filterType.equalsIgnoreCase(e.getType()));
        }
        
        List<Event> filteredList = stream.collect(java.util.stream.Collectors.toList());
        
        // Partition into upcoming (not archived) and archived
        List<Event> upcomingEvents = filteredList.stream()
                .filter(e -> e != null && !e.isArchived())
                .collect(java.util.stream.Collectors.toList());
                
        List<Event> archivedEvents = filteredList.stream()
                .filter(e -> e != null && e.isArchived())
                .collect(java.util.stream.Collectors.toList());
                
        model.addAttribute("upcomingEvents", upcomingEvents);
        model.addAttribute("archivedEvents", archivedEvents);
        model.addAttribute("events", filteredList); // For backward compatibility or calendar parsing
        
        model.addAttribute("search", search);
        model.addAttribute("filterType", filterType);
        
        model.addAttribute("newEvent", new Event());
        model.addAttribute("allMembers", memberRepository.findAll());
        return "events";
    }

    @PostMapping("/save")
    @org.springframework.transaction.annotation.Transactional
    public String saveEvent(@ModelAttribute Event event, @RequestParam(required = false) List<Long> organizerIds) {
        if (organizerIds != null && !organizerIds.isEmpty()) {
            event.setOrganizers(new HashSet<>(memberRepository.findAllById(organizerIds)));
        } else {
            event.setOrganizers(new HashSet<>());
        }
        
        // Keep archived status if updating
        if (event.getId() != null) {
            eventService.findById(event.getId()).ifPresent(existing -> {
                event.setArchived(existing.isArchived());
            });
        }
        
        eventService.save(event);

        // Notify event creation
        Notification notification = Notification.builder()
                .message("Nouvel événement académique créé: " + event.getTitle() + " le " + event.getDate())
                .read(false)
                .type("NEW_EVENT")
                .date(java.time.LocalDateTime.now())
                .user("all")
                .build();
        notificationRepository.saveAndFlush(notification);

        return "redirect:/director/events?success";
    }

    @GetMapping("/archive/{id}")
    @org.springframework.transaction.annotation.Transactional
    public String archiveEvent(@PathVariable Long id) {
        Event event = eventService.findById(id).orElse(null);
        if (event != null) {
            event.setArchived(!event.isArchived());
            eventService.save(event);
        }
        return "redirect:/director/events?success";
    }

    @GetMapping("/edit/{id}")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String editEventForm(
            @PathVariable Long id,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String filterType,
            Model model) {
        Event event = eventService.findById(id).orElseThrow(() -> new IllegalArgumentException("Événement non trouvé"));
        
        List<Event> allEvents = eventService.findAll();
        java.util.stream.Stream<Event> stream = allEvents.stream();
        if (search != null && !search.trim().isEmpty()) {
            String lowerSearch = search.trim().toLowerCase();
            stream = stream.filter(e -> e.getTitle() != null && e.getTitle().toLowerCase().contains(lowerSearch));
        }
        if (filterType != null && !filterType.trim().isEmpty() && !"ALL".equalsIgnoreCase(filterType)) {
            stream = stream.filter(e -> filterType.equalsIgnoreCase(e.getType()));
        }
        
        List<Event> filteredList = stream.collect(java.util.stream.Collectors.toList());
        List<Event> upcomingEvents = filteredList.stream()
                .filter(e -> e != null && !e.isArchived())
                .collect(java.util.stream.Collectors.toList());
        List<Event> archivedEvents = filteredList.stream()
                .filter(e -> e != null && e.isArchived())
                .collect(java.util.stream.Collectors.toList());
                
        model.addAttribute("upcomingEvents", upcomingEvents);
        model.addAttribute("archivedEvents", archivedEvents);
        model.addAttribute("events", filteredList);
        
        model.addAttribute("search", search);
        model.addAttribute("filterType", filterType);
        
        model.addAttribute("newEvent", event);
        model.addAttribute("allMembers", memberRepository.findAll());
        return "events";
    }

    @GetMapping("/delete/{id}")
    public String deleteEvent(@PathVariable Long id) {
        eventService.deleteById(id);
        return "redirect:/director/events?deleted";
    }
}
