package com.lias.manager.service;

import com.lias.manager.entity.Event;
import com.lias.manager.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;

    public List<Event> findAll() {
        return eventRepository.findAll();
    }

    public Optional<Event> findById(Long id) {
        return eventRepository.findById(id);
    }

    @Transactional
    public Event save(Event event) {
        if (event.getId() != null) {
            Optional<Event> existingOpt = eventRepository.findById(event.getId());
            if (existingOpt.isPresent()) {
                Event existing = existingOpt.get();
                System.out.println("UPDATE ID = " + existing.getId());
                System.out.println("BEFORE SAVE = " + existing);
                existing.setTitle(event.getTitle());
                existing.setType(event.getType());
                existing.setDescription(event.getDescription());
                existing.setDate(event.getDate());
                existing.setOrganizers(event.getOrganizers());
                existing.setArchived(event.isArchived());
                Event saved = eventRepository.saveAndFlush(existing);
                System.out.println("SAVED = " + saved);
                return saved;
            }
        }
        System.out.println("BEFORE SAVE New Event = " + event);
        Event saved = eventRepository.saveAndFlush(event);
        System.out.println("SAVED = " + saved);
        return saved;
    }

    @Transactional
    public void deleteById(Long id) {
        eventRepository.deleteById(id);
    }
}
