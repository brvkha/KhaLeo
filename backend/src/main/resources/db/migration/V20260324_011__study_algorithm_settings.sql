CREATE TABLE study_algorithm_settings (
    id INT NOT NULL,
    weights_json VARCHAR(2048) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_study_algorithm_settings PRIMARY KEY (id)
);

INSERT INTO study_algorithm_settings (id, weights_json, version)
VALUES (
    1,
     '[1.2682, 1.2682, 6.4994, 16.1563, 6.9135, 0.6470, 2.5935, 0.0010, 1.7036, 0.1711, 1.1668, 2.0287, 0.0767, 0.4215, 2.5117, 0.2713, 3.6253, 0.4372, 0.0468]',
    0
);
