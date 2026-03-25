CREATE TABLE study_algorithm_settings (
    id INT NOT NULL,
    weights_json VARCHAR(2048) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_study_algorithm_settings PRIMARY KEY (id)
);

INSERT INTO study_algorithm_settings (id, weights_json, version)
VALUES (
    1,
     '[1.2682, 1.2682, 0.7310, 1.7540, 7.9650, 0.6470, 2.5935, 0.0010, 1.2670, 0.1510, 1.5040, 2.0287, 0.0767, 0.4215, 2.5117, 0.2713, 1.3240, 0.4372, 0.0468]',
    0
);
