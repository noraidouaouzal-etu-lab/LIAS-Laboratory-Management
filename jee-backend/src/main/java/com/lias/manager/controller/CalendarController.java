package com.lias.manager.controller;

import com.lias.manager.entity.Event;
import com.lias.manager.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class CalendarController {

    private final EventService eventService;

    @GetMapping("/calendar")
    public String viewCalendar(Model model) {
        List<Event> events = eventService.findAll();
        model.addAttribute("events", events);
        return "calendar";
    }
}
