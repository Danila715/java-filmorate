package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;

    private static final Logger log = LoggerFactory.getLogger(FilmService.class);

    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    public Film add(Film film) {
        log.info("Добавление фильма: {}", film.getName());
        validateFilm(film);
        Film saved = filmStorage.add(film);
        log.debug("Фильм успешно добавлен с ID: {}", saved.getId());
        return saved;
    }

    public Film update(Film film) {
        log.info("Обновление фильма с ID: {}", film.getId());
        validateFilm(film);
        Film updated = filmStorage.update(film);
        log.debug("Фильм обновлён: {}", updated);
        return updated;
    }

    public List<Film> getAll() {
        return filmStorage.getAll();
    }

    public Film getById(int id) {
        return filmStorage.getById(id);
    }

    public void addLike(int filmId, int userId) {
        Film film = filmStorage.getById(filmId);
        if (!userStorage.contains(userId)) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден");
        }
        if (!film.getLikes().add(userId)) {
            throw new NotFoundException("Пользователь уже поставил лайк");
        }
    }

    public void removeLike(int filmId, int userId) {
        Film film = filmStorage.getById(filmId);
        if (!userStorage.contains(userId)) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден");
        }
        if (!film.getLikes().remove(userId)) {
            throw new NotFoundException("Лайк от пользователя " + userId + " не найден");
        }
    }

    public List<Film> getPopular(int count) {
        return filmStorage.getAll().stream()
                .sorted((f1, f2) -> Integer.compare(f2.getLikes().size(), f1.getLikes().size()))
                .limit(count)
                .collect(Collectors.toList());
    }

    private void validateFilm(Film film) {
        if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(MIN_RELEASE_DATE)) {
            log.warn("Попытка добавить фильм с некорректной датой: {}", film.getReleaseDate());
            throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года");
        }
    }
}