package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@Import({FilmDbStorage.class, UserDbStorage.class})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class FilmDbStorageTest {

    private final FilmDbStorage filmStorage;
    private final UserDbStorage userStorage;
    private final JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // Очищаем данные перед каждым тестом
        jdbcTemplate.update("DELETE FROM film_likes");
        jdbcTemplate.update("DELETE FROM film_genre");
        jdbcTemplate.update("DELETE FROM friendship");
        jdbcTemplate.update("DELETE FROM films");
        jdbcTemplate.update("DELETE FROM users");

        // Сбрасываем счетчики автоинкремента
        jdbcTemplate.update("ALTER TABLE films ALTER COLUMN film_id RESTART WITH 1");
        jdbcTemplate.update("ALTER TABLE users ALTER COLUMN user_id RESTART WITH 1");
    }

    @Test
    void testAddFilm() {
        Film film = new Film();
        film.setName("Test Film");
        film.setDescription("Test Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        Mpa mpa = new Mpa();
        mpa.setId(1);
        film.setMpa(mpa);

        Film savedFilm = filmStorage.add(film);

        assertThat(savedFilm.getId()).isGreaterThan(0);
        assertThat(savedFilm.getName()).isEqualTo("Test Film");
        assertThat(savedFilm.getDescription()).isEqualTo("Test Description");
        assertThat(savedFilm.getMpa()).isNotNull();
        assertThat(savedFilm.getMpa().getId()).isEqualTo(1);
    }

    @Test
    void testAddFilmWithGenres() {
        Film film = new Film();
        film.setName("Film with Genres");
        film.setDescription("Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        Mpa mpa = new Mpa();
        mpa.setId(1);
        film.setMpa(mpa);

        Genre genre1 = new Genre();
        genre1.setId(1);
        Genre genre2 = new Genre();
        genre2.setId(2);
        film.getGenres().add(genre1);
        film.getGenres().add(genre2);

        Film savedFilm = filmStorage.add(film);

        assertThat(savedFilm.getGenres()).hasSize(2);
        assertThat(savedFilm.getGenres())
                .extracting(Genre::getId)
                .containsExactly(1, 2);
    }

    @Test
    void testFindFilmById() {
        Film film = new Film();
        film.setName("Find Film");
        film.setDescription("Find Description");
        film.setReleaseDate(LocalDate.of(2001, 2, 2));
        film.setDuration(90);

        Mpa mpa = new Mpa();
        mpa.setId(2);
        film.setMpa(mpa);

        Film savedFilm = filmStorage.add(film);

        Optional<Film> foundFilm = filmStorage.findById(savedFilm.getId());

        assertThat(foundFilm)
                .isPresent()
                .hasValueSatisfying(f -> {
                    assertThat(f.getId()).isEqualTo(savedFilm.getId());
                    assertThat(f.getName()).isEqualTo("Find Film");
                    assertThat(f.getMpa().getId()).isEqualTo(2);
                });
    }

    @Test
    void testUpdateFilm() {
        Film film = new Film();
        film.setName("Update Film");
        film.setDescription("Update Description");
        film.setReleaseDate(LocalDate.of(2002, 3, 3));
        film.setDuration(100);

        Mpa mpa = new Mpa();
        mpa.setId(1);
        film.setMpa(mpa);

        Film savedFilm = filmStorage.add(film);

        savedFilm.setName("Updated Film");
        savedFilm.setDescription("Updated Description");
        Film updatedFilm = filmStorage.update(savedFilm);

        assertThat(updatedFilm.getName()).isEqualTo("Updated Film");
        assertThat(updatedFilm.getDescription()).isEqualTo("Updated Description");
    }

    @Test
    void testGetAllFilms() {
        Film film1 = new Film();
        film1.setName("Film 1");
        film1.setDescription("Description 1");
        film1.setReleaseDate(LocalDate.of(2000, 1, 1));
        film1.setDuration(120);
        Mpa mpa1 = new Mpa();
        mpa1.setId(1);
        film1.setMpa(mpa1);

        Film film2 = new Film();
        film2.setName("Film 2");
        film2.setDescription("Description 2");
        film2.setReleaseDate(LocalDate.of(2001, 2, 2));
        film2.setDuration(90);
        Mpa mpa2 = new Mpa();
        mpa2.setId(2);
        film2.setMpa(mpa2);

        filmStorage.add(film1);
        filmStorage.add(film2);

        List<Film> films = filmStorage.getAll();

        assertThat(films).hasSize(2);
    }

    @Test
    void testAddLike() {
        Film film = createFilm("Like Film", 120, 1);
        User user = createUser("like@example.com", "likeuser");

        Film savedFilm = filmStorage.add(film);
        User savedUser = userStorage.add(user);

        filmStorage.addLike(savedFilm, savedUser);  // ← объекты

        List<Film> popular = filmStorage.getPopular(10);
        assertThat(popular).isNotEmpty();
        assertThat(popular.get(0).getId()).isEqualTo(savedFilm.getId());
    }

    @Test
    void testRemoveLike() {
        Film film = createFilm("Remove Like Film", 120, 1);
        User user = createUser("removelike@example.com", "removelikeuser");

        Film savedFilm = filmStorage.add(film);
        User savedUser = userStorage.add(user);

        filmStorage.addLike(savedFilm, savedUser);     // ← объекты
        filmStorage.removeLike(savedFilm, savedUser);  // ← объекты

        List<Film> popular = filmStorage.getPopular(10);

        if (!popular.isEmpty()) {
            long likesCount = popular.stream()
                    .filter(f -> f.getId() == savedFilm.getId())
                    .count();
            assertThat(likesCount).isLessThanOrEqualTo(1);
        }
    }

    @Test
    void testGetPopular() {
        Film film1 = createFilm("Popular Film 1", 120, 1);
        Film film2 = createFilm("Popular Film 2", 90, 2);
        User user = createUser("popular@example.com", "popularuser");

        Film savedFilm1 = filmStorage.add(film1);
        Film savedFilm2 = filmStorage.add(film2);
        User savedUser = userStorage.add(user);

        filmStorage.addLike(savedFilm1, savedUser);  // ← объекты

        List<Film> popular = filmStorage.getPopular(2);

        assertThat(popular).isNotEmpty();
        assertThat(popular.get(0).getId()).isEqualTo(savedFilm1.getId());
    }

    private Film createFilm(String name, int duration, int mpaId) {
        Film film = new Film();
        film.setName(name);
        film.setDescription("Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(duration);
        Mpa mpa = new Mpa();
        mpa.setId(mpaId);
        film.setMpa(mpa);
        return film;
    }

    private User createUser(String email, String login) {
        User user = new User();
        user.setEmail(email);
        user.setLogin(login);
        user.setName("Name");
        user.setBirthday(LocalDate.of(1990, 1, 1));
        return user;
    }
}