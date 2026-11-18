-- Datos iniciales de los tests de integración (repositorios, service y web).
-- Se cargan en el @BeforeEach, justo después de vaciar las tablas, así cada test
-- parte siempre del mismo escenario:
--
--   Usuarios:   agomez, jperez, mlopez
--   Tweets:     uno original de cada usuario (agomez 10:00, jperez 11:00, mlopez 09:00)
--   Retweet:    agomez retuitea el tweet de jperez (12:00)
--   Follows:    agomez sigue a jperez
--
-- Las fechas son fijas para que el orden de las consultas sea predecible. Los ids no se
-- escriben: los asigna la base, y los tests los buscan por nombre o por contenido.

INSERT INTO users (username, password, createdAt, deleted)
VALUES ('agomez', 'password123', TIMESTAMP '2024-01-01 08:00:00', FALSE);

INSERT INTO users (username, password, createdAt, deleted)
VALUES ('jperez', 'password456', TIMESTAMP '2024-01-01 08:00:00', FALSE);

INSERT INTO users (username, password, createdAt, deleted)
VALUES ('mlopez', 'password789', TIMESTAMP '2024-01-01 08:00:00', FALSE);

INSERT INTO tweets (tweet_type, user_id, content, createdAt, deleted)
VALUES ('ORIGINAL', (SELECT id FROM users WHERE username = 'agomez'),
        'Primer tweet de Ana', TIMESTAMP '2024-01-01 10:00:00', FALSE);

INSERT INTO tweets (tweet_type, user_id, content, createdAt, deleted)
VALUES ('ORIGINAL', (SELECT id FROM users WHERE username = 'jperez'),
        'Tweet de Luis', TIMESTAMP '2024-01-01 11:00:00', FALSE);

INSERT INTO tweets (tweet_type, user_id, content, createdAt, deleted)
VALUES ('ORIGINAL', (SELECT id FROM users WHERE username = 'mlopez'),
        'Tweet de María', TIMESTAMP '2024-01-01 09:00:00', FALSE);

INSERT INTO tweets (tweet_type, user_id, origin_tweet_id, createdAt, deleted)
VALUES ('RETWEET', (SELECT id FROM users WHERE username = 'agomez'),
        (SELECT id FROM tweets WHERE content = 'Tweet de Luis'),
        TIMESTAMP '2024-01-01 12:00:00', FALSE);

INSERT INTO follows (follower_id, followed_id, followedAt)
VALUES ((SELECT id FROM users WHERE username = 'agomez'),
        (SELECT id FROM users WHERE username = 'jperez'),
        TIMESTAMP '2024-01-01 09:00:00');
