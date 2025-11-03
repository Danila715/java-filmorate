package ru.yandex.practicum.filmorate.storage.film;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class InMemoryFilmStorage implements FilmStorage {

    private final Map<Integer, Film> films = new HashMap<>();
    private int idGenerator = 1;

    @Override
    public Film add(Film film) {
        film.setId(idGenerator++);
        films.put(film.getId(), film);
        log.debug("Фильм добавлен: {} (ID: {})", film.getName(), film.getId());
        return film;
    }

    @Override
    public Film update(Film film) {
        if (!films.containsKey(film.getId())) {
            throw new NotFoundException("Фильм с id = " + film.getId() + " не найден");
        }
        films.put(film.getId(), film);
        log.debug("Фильм обновлён: {} (ID: {})", film.getName(), film.getId());
        return film;
    }

    @Override
    public List<Film> getAll() {
        return new ArrayList<>(films.values());
    }

    @Override
    public Optional<Film> findById(int id) {
        return Optional.ofNullable(films.get(id));
    }

    @Override
    public List<Film> getPopular(int count) {
        return films.values().stream()
                .sorted((f1, f2) -> Integer.compare(f2.getLikes().size(), f1.getLikes().size()))
                .limit(count)
                .collect(Collectors.toList());
    }
}