package com.cramer.engagement.service;

import com.cramer.engagement.domain.Target;
import com.cramer.engagement.repository.TargetRepository;
import com.cramer.engagement.web.dto.TargetRequest;
import com.cramer.engagement.web.dto.TargetView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * IELTS goal management (SPEC-16 §5): one {@link Target} per user, upserted. Bands are validated
 * 0–9 at the DTO layer (matching the DB check constraints).
 */
@Service
public class TargetService {

    private final TargetRepository targets;

    public TargetService(TargetRepository targets) {
        this.targets = targets;
    }

    @Transactional(readOnly = true)
    public Optional<TargetView> current(UUID userId) {
        return targets.findByUserId(userId).map(TargetView::of);
    }

    @Transactional
    public TargetView upsert(UUID userId, TargetRequest req) {
        Target t = targets.findByUserId(userId).orElseGet(() -> {
            Target created = new Target();
            created.setId(UUID.randomUUID());
            created.setUserId(userId);
            return created;
        });
        t.setExamName(req.examName());
        t.setExamDate(req.examDate());
        t.setListening(req.listening());
        t.setReading(req.reading());
        t.setWriting(req.writing());
        t.setSpeaking(req.speaking());
        return TargetView.of(targets.save(t));
    }
}
