package com.khaleo.flashcard.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "study_algorithm_settings")
public class StudyAlgorithmSettings {

    @Id
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "weights_json", nullable = false, length = 2048)
    private String weightsJson;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
