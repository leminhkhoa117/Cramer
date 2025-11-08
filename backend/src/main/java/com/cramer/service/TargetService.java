package com.cramer.service;

import com.cramer.dto.TargetDTO;
import com.cramer.entity.Target;
import com.cramer.repository.TargetRepository;
import com.cramer.util.EntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TargetService {

    private final TargetRepository targetRepository;

    @Transactional(readOnly = true)
    public Optional<TargetDTO> getTargetByUserId(UUID userId) {
        return targetRepository.findByUserId(userId).map(EntityMapper::toDTO);
    }

    @Transactional
    public TargetDTO createOrUpdateTarget(TargetDTO targetDTO, UUID userId) {
        // Ensure the DTO's userId matches the authenticated user's ID
        targetDTO.setUserId(userId);

        Target target = targetRepository.findByUserId(userId)
                .map(existingTarget -> {
                    // Update existing target
                    existingTarget.setExamName(targetDTO.getExamName());
                    existingTarget.setExamDate(targetDTO.getExamDate());
                    existingTarget.setListening(targetDTO.getListening());
                    existingTarget.setReading(targetDTO.getReading());
                    existingTarget.setWriting(targetDTO.getWriting());
                    existingTarget.setSpeaking(targetDTO.getSpeaking());
                    return existingTarget;
                })
                .orElseGet(() -> {
                    // Create new target
                    return EntityMapper.toEntity(targetDTO);
                });

        Target savedTarget = targetRepository.save(target);
        return EntityMapper.toDTO(savedTarget);
    }
}
