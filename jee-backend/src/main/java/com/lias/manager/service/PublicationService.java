package com.lias.manager.service;

import com.lias.manager.entity.Publication;
import com.lias.manager.repository.PublicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PublicationService {

    private final PublicationRepository publicationRepository;

    public List<Publication> findAll() {
        return publicationRepository.findAll();
    }

    public List<Publication> searchAndFilter(String author, Integer year, String team) {
        return publicationRepository.findAll().stream()
                .filter(p -> author == null || author.trim().isEmpty() || p.getAuthor().toLowerCase().contains(author.trim().toLowerCase()))
                .filter(p -> year == null || p.getYear().equals(year))
                .filter(p -> team == null || team.trim().isEmpty() || (p.getTeam() != null && p.getTeam().toLowerCase().contains(team.trim().toLowerCase())))
                .collect(java.util.stream.Collectors.toList());
    }

    public Optional<Publication> findById(Long id) {
        return publicationRepository.findById(id);
    }

    @Transactional
    public Publication save(Publication publication) {
        if (publication.getId() != null) {
            Optional<Publication> existingOpt = publicationRepository.findById(publication.getId());
            if (existingOpt.isPresent()) {
                Publication existing = existingOpt.get();
                System.out.println("UPDATE ID = " + existing.getId());
                System.out.println("BEFORE SAVE = " + existing);
                existing.setAuthor(publication.getAuthor());
                existing.setTitle(publication.getTitle());
                existing.setYear(publication.getYear());
                existing.setTeam(publication.getTeam());
                Publication saved = publicationRepository.saveAndFlush(existing);
                System.out.println("SAVED = " + saved);
                return saved;
            }
        }
        System.out.println("BEFORE SAVE New Publication = " + publication);
        Publication saved = publicationRepository.saveAndFlush(publication);
        System.out.println("SAVED = " + saved);
        return saved;
    }

    @Transactional
    public void deleteById(Long id) {
        publicationRepository.deleteById(id);
    }
}
