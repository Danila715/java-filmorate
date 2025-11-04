-- Таблица рейтингов MPA
CREATE TABLE IF NOT EXISTS mpa_rating (
    mpa_id INTEGER PRIMARY KEY,
    mpa_name VARCHAR(10) NOT NULL,
    description VARCHAR(255)
);

-- Таблица жанров
CREATE TABLE IF NOT EXISTS genre (
    genre_id INTEGER PRIMARY KEY,
    genre_name VARCHAR(50) NOT NULL
);

-- Таблица пользователей
CREATE TABLE IF NOT EXISTS users (
    user_id SERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    login VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255),
    birthday DATE NOT NULL
);

-- Таблица фильмов
CREATE TABLE IF NOT EXISTS films (
    film_id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(200),
    release_date DATE NOT NULL,
    duration INTEGER NOT NULL,
    mpa_id INTEGER NOT NULL,
    CONSTRAINT fk_mpa FOREIGN KEY (mpa_id) REFERENCES mpa_rating(mpa_id)
);

-- Таблица связи фильмов и жанров
CREATE TABLE IF NOT EXISTS film_genre (
    film_id INTEGER NOT NULL,
    genre_id INTEGER NOT NULL,
    PRIMARY KEY (film_id, genre_id),
    CONSTRAINT fk_film_genre_film FOREIGN KEY (film_id) REFERENCES films(film_id) ON DELETE CASCADE,
    CONSTRAINT fk_film_genre_genre FOREIGN KEY (genre_id) REFERENCES genre(genre_id)
);

-- Таблица лайков
CREATE TABLE IF NOT EXISTS film_likes (
    film_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    PRIMARY KEY (film_id, user_id),
    CONSTRAINT fk_likes_film FOREIGN KEY (film_id) REFERENCES films(film_id) ON DELETE CASCADE,
    CONSTRAINT fk_likes_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Таблица дружбы
CREATE TABLE IF NOT EXISTS friendship (
    user_id INTEGER NOT NULL,
    friend_id INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    PRIMARY KEY (user_id, friend_id),
    CONSTRAINT fk_friendship_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_friendship_friend FOREIGN KEY (friend_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT check_not_self CHECK (user_id <> friend_id)
);

-- Заполнение справочника MPA
INSERT INTO mpa_rating (mpa_id, mpa_name, description) VALUES
(1, 'G', 'У фильма нет возрастных ограничений'),
(2, 'PG', 'Детям рекомендуется смотреть фильм с родителями'),
(3, 'PG-13', 'Детям до 13 лет просмотр не желателен'),
(4, 'R', 'Лицам до 17 лет просматривать фильм можно только в присутствии взрослого'),
(5, 'NC-17', 'Лицам до 18 лет просмотр запрещён');

-- Заполнение справочника жанров
INSERT INTO genre (genre_id, genre_name) VALUES
(1, 'Комедия'),
(2, 'Драма'),
(3, 'Мультфильм'),
(4, 'Триллер'),
(5, 'Документальный'),
(6, 'Боевик');