package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FilmControllerTest {

    private final FilmController controller = new FilmController();

    @Test
    void shouldNotAllowEmptyName() {
        Film film = new Film();
        film.setName("");
        film.setDescription("desc");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        assertThrows(ValidationException.class, () -> controller.createFilm(film));
    }

    @Test
    void shouldNotAllowDescriptionOver200Chars() {
        Film film = new Film();
        film.setName("Valid");
        film.setDescription("a".repeat(201));
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        assertThrows(ValidationException.class, () -> controller.createFilm(film));
    }

    @Test
    void shouldNotAllowReleaseBefore1895() {
        Film film = new Film();
        film.setName("Old");
        film.setDescription("desc");
        film.setReleaseDate(LocalDate.of(1895, 12, 27));
        film.setDuration(120);

        assertThrows(ValidationException.class, () -> controller.createFilm(film));
    }

    @Test
    void shouldNotAllowNonPositiveDuration() {
        Film film = new Film();
        film.setName("Short");
        film.setDescription("desc");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(0);

        assertThrows(ValidationException.class, () -> controller.createFilm(film));
    }

    @Test
    void shouldAllowValidFilm() {
        Film film = new Film();
        film.setName("Valid Film");
        film.setDescription("Good movie");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        Film saved = controller.createFilm(film);
        assertEquals(1, saved.getId());
    }
}