package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreStorage;
import ru.yandex.practicum.filmorate.storage.mpa.MpaStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FilmService {

    private final FilmStorage filmStorage;
    private final FilmDbStorage filmDbStorage;
    private final UserStorage userStorage;
    private final MpaStorage mpaStorage;
    private final GenreStorage genreStorage;

    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    public FilmService(@Qualifier("filmDbStorage") FilmStorage filmStorage,
                       FilmDbStorage filmDbStorage,
                       @Qualifier("userDbStorage") UserStorage userStorage,
                       MpaStorage mpaStorage,
                       GenreStorage genreStorage) {
        this.filmStorage = filmStorage;
        this.filmDbStorage = filmDbStorage;
        this.userStorage = userStorage;
        this.mpaStorage = mpaStorage;
        this.genreStorage = genreStorage;
    }

    public Film add(Film film) {
        validateFilm(film);
        validateMpa(film);
        validateGenres(film);
        return filmStorage.add(film);
    }

    public Film update(Film film) {
        validateFilm(film);
        validateMpa(film);
        validateGenres(film);
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
        User user = userStorage.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id = " + userId + " не найден"));

        filmDbStorage.addLike(film.getId(), user.getId());
        log.debug("Лайк добавлен: {} → {}", user.getLogin(), film.getName());
    }

    public void removeLike(int filmId, int userId) {
        Film film = getById(filmId);
        User user = userStorage.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id = " + userId + " не найден"));

        filmDbStorage.removeLike(film.getId(), user.getId());
        log.debug("Лайк удалён: {} → {}", user.getLogin(), film.getName());
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

    private void validateMpa(Film film) {
        if (film.getMpa() == null || film.getMpa().getId() <= 0) {
            throw new ValidationException("Рейтинг MPA обязателен");
        }

        // Проверяем существование MPA в БД
        mpaStorage.findById(film.getMpa().getId())
                .orElseThrow(() -> new NotFoundException("Рейтинг MPA с id = " + film.getMpa().getId() + " не найден"));
    }

    private void validateGenres(Film film) {
        if (film.getGenres() == null || film.getGenres().isEmpty()) {
            return; // Жанры не обязательны
        }

        // Собираем все ID жанров
        Set<Integer> genreIds = film.getGenres().stream()
                .peek(genre -> {
                    if (genre.getId() <= 0) {
                        throw new ValidationException("ID жанра должен быть положительным");
                    }
                })
                .map(Genre::getId)
                .collect(Collectors.toSet());

        List<Genre> existingGenres = genreStorage.getAll().stream()
                .filter(genre -> genreIds.contains(genre.getId()))
                .toList();

        if (existingGenres.size() != genreIds.size()) {
            Set<Integer> existingIds = existingGenres.stream()
                    .map(Genre::getId)
                    .collect(Collectors.toSet());

            Set<Integer> missingIds = genreIds.stream()
                    .filter(id -> !existingIds.contains(id))
                    .collect(Collectors.toSet());

            throw new NotFoundException("Жанры с id " + missingIds + " не найдены");
        }
    }
}