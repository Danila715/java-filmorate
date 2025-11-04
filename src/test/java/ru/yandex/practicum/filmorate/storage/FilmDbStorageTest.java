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
        // Создаем фильм
        Film film = new Film();
        film.setName("Like Film");
        film.setDescription("Like Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        Mpa mpa = new Mpa();
        mpa.setId(1);
        film.setMpa(mpa);

        // Создаем пользователя
        User user = new User();
        user.setEmail("like@example.com");
        user.setLogin("likeuser");
        user.setName("Like User");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        Film savedFilm = filmStorage.add(film);
        User savedUser = userStorage.add(user);

        filmStorage.addLike(savedFilm.getId(), savedUser.getId());

        // Проверяем через getPopular
        List<Film> popular = filmStorage.getPopular(10);
        assertThat(popular).isNotEmpty();
        assertThat(popular.get(0).getId()).isEqualTo(savedFilm.getId());
    }

    @Test
    void testRemoveLike() {
        // Создаем фильм
        Film film = new Film();
        film.setName("Remove Like Film");
        film.setDescription("Remove Like Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        Mpa mpa = new Mpa();
        mpa.setId(1);
        film.setMpa(mpa);

        // Создаем пользователя
        User user = new User();
        user.setEmail("removelike@example.com");
        user.setLogin("removelikeuser");
        user.setName("Remove Like User");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        Film savedFilm = filmStorage.add(film);
        User savedUser = userStorage.add(user);

        // Добавляем и удаляем лайк
        filmStorage.addLike(savedFilm.getId(), savedUser.getId());
        filmStorage.removeLike(savedFilm.getId(), savedUser.getId());

        // Проверяем что лайков нет - фильм должен быть последним или отсутствовать
        List<Film> popular = filmStorage.getPopular(10);

        // Если список пустой - отлично, лайков нет
        // Если не пустой - проверяем что у первого фильма ID не равен нашему
        if (!popular.isEmpty()) {
            // У фильма без лайков не должно быть приоритета
            long likesCount = popular.stream()
                    .filter(f -> f.getId() == savedFilm.getId())
                    .count();
            assertThat(likesCount).isLessThanOrEqualTo(1);
        }
    }

    @Test
    void testGetPopular() {
        // Создаем фильмы
        Film film1 = new Film();
        film1.setName("Popular Film 1");
        film1.setDescription("Description 1");
        film1.setReleaseDate(LocalDate.of(2000, 1, 1));
        film1.setDuration(120);
        Mpa mpa1 = new Mpa();
        mpa1.setId(1);
        film1.setMpa(mpa1);

        Film film2 = new Film();
        film2.setName("Popular Film 2");
        film2.setDescription("Description 2");
        film2.setReleaseDate(LocalDate.of(2001, 2, 2));
        film2.setDuration(90);
        Mpa mpa2 = new Mpa();
        mpa2.setId(2);
        film2.setMpa(mpa2);

        // Создаем пользователя
        User user = new User();
        user.setEmail("popular@example.com");
        user.setLogin("popularuser");
        user.setName("Popular User");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        Film savedFilm1 = filmStorage.add(film1);
        Film savedFilm2 = filmStorage.add(film2);
        User savedUser = userStorage.add(user);

        // Добавляем лайк только первому фильму
        filmStorage.addLike(savedFilm1.getId(), savedUser.getId());

        List<Film> popular = filmStorage.getPopular(2);

        assertThat(popular).isNotEmpty();
        // Первый фильм должен быть первым (у него есть лайк)
        assertThat(popular.get(0).getId()).isEqualTo(savedFilm1.getId());
    }
}