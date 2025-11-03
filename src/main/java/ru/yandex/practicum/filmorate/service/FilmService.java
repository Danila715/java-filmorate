package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;

    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    public Film add(Film film) {
        validateFilm(film);
        return filmStorage.add(film);
    }

    public Film update(Film film) {
        validateFilm(film);
        return filmStorage.findById(film.getId())
                .map(existing -> filmStorage.update(film))
                .orElseThrow(() -> new NotFoundException("Фильм с id = " + film.getId() + " не найден"));
    }

    public List<Film> getAll() {
        return filmStorage.getAll();
    }

    public Film getById(int id) {
        return filmStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Фильм с id = " + id + " не найден"));
    }

    public void addLike(int filmId, int userId) {
        Film film = getById(filmId);
        if (!userStorage.findById(userId).isPresent()) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден");
        }
        if (!film.getLikes().add(userId)) {
            throw new ValidationException("Пользователь уже поставил лайк");
        }
        log.debug("Лайк добавлен: user {} → film {}", userId, filmId);
    }

    public void removeLike(int filmId, int userId) {
        Film film = getById(filmId);
        if (!userStorage.findById(userId).isPresent()) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден");
        }
        if (!film.getLikes().remove(userId)) {
            throw new ValidationException("Лайк от пользователя " + userId + " не найден");
        }
        log.debug("Лайк удалён: user {} → film {}", userId, filmId);
    }

    public List<Film> getPopular(int count) {
        return filmStorage.getPopular(count);
    }

    private void validateFilm(Film film) {
        if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(MIN_RELEASE_DATE)) {
            log.warn("Некорректная дата релиза: {}", film.getReleaseDate());
            throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года");
        }
    }
}