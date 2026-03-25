package com.khaleo.flashcard.service.study;

import com.khaleo.flashcard.entity.StudyAlgorithmSettings;
import com.khaleo.flashcard.repository.StudyAlgorithmSettingsRepository;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudyAlgorithmSettingsService {

    private static final int SETTINGS_ROW_ID = 1;
    private final StudyAlgorithmSettingsRepository repository;

    @Transactional(readOnly = true)
    public double[] loadOrDefault(double[] defaultWeights) {
        return repository.findById(SETTINGS_ROW_ID)
                .map(entity -> deserialize(entity.getWeightsJson(), defaultWeights))
                .orElse(defaultWeights.clone());
    }

    @Transactional
    public void save(double[] weights) {
        StudyAlgorithmSettings existing = repository.findById(SETTINGS_ROW_ID)
                .orElseGet(() -> StudyAlgorithmSettings.builder().id(SETTINGS_ROW_ID).build());
        existing.setWeightsJson(serialize(weights));
        repository.save(existing);
    }

    private String serialize(double[] weights) {
        return Arrays.toString(weights);
    }

    private double[] deserialize(String raw, double[] fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback.clone();
        }

        String normalized = raw.trim();
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }

        String[] parts = normalized.split(",");
        if (parts.length != fallback.length) {
            return fallback.clone();
        }

        double[] values = new double[parts.length];
        try {
            for (int i = 0; i < parts.length; i++) {
                values[i] = Double.parseDouble(parts[i].trim());
            }
            return values;
        } catch (NumberFormatException ex) {
            return fallback.clone();
        }
    }
}
