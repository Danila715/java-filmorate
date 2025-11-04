package ru.yandex.practicum.filmorate.storage.film;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Repository("filmDbStorage")
@RequiredArgsConstructor
@Slf4j
public class FilmDbStorage implements FilmStorage {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Film add(Film film) {
        String sql = "INSERT INTO films (name, description, release_date, duration, mpa_id) VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"film_id"});
            ps.setString(1, film.getName());
            ps.setString(2, film.getDescription());
            ps.setDate(3, Date.valueOf(film.getReleaseDate()));
            ps.setInt(4, film.getDuration());
            ps.setInt(5, film.getMpa().getId());
            return ps;
        }, keyHolder);

        film.setId(keyHolder.getKey().intValue());

        // Сохраняем жанры
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            saveGenres(film.getId(), film.getGenres());
        }

        log.debug("Фильм добавлен в БД: {} (ID: {})", film.getName(), film.getId());
        return findById(film.getId()).orElse(film);
    }

    @Override
    public Film update(Film film) {
        String sql = "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, mpa_id = ? WHERE film_id = ?";
        jdbcTemplate.update(sql,
                film.getName(),
                film.getDescription(),
                Date.valueOf(film.getReleaseDate()),
                film.getDuration(),
                film.getMpa().getId(),
                film.getId());

        // Обновляем жанры
        deleteGenres(film.getId());
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            saveGenres(film.getId(), film.getGenres());
        }

        log.debug("Фильм обновлён в БД: {} (ID: {})", film.getName(), film.getId());
        return findById(film.getId()).orElse(film);
    }

    @Override
    public List<Film> getAll() {
        String sql = "SELECT f.*, m.mpa_name FROM films f JOIN mpa_rating m ON f.mpa_id = m.mpa_id";
        List<Film> films = jdbcTemplate.query(sql, new FilmRowMapper());
        loadGenresForFilms(films);
        return films;
    }

    @Override
    public Optional<Film> findById(int id) {
        String sql = "SELECT f.*, m.mpa_name FROM films f JOIN mpa_rating m ON f.mpa_id = m.mpa_id WHERE f.film_id = ?";
        List<Film> films = jdbcTemplate.query(sql, new FilmRowMapper(), id);
        if (films.isEmpty()) {
            return Optional.empty();
        }
        Film film = films.get(0);
        loadGenresForFilm(film);
        return Optional.of(film);
    }

    @Override
    public List<Film> getPopular(int count) {
        String sql = """
                SELECT f.*, m.mpa_name, COUNT(fl.user_id) AS likes_count
                FROM films f
                LEFT JOIN mpa_rating m ON f.mpa_id = m.mpa_id
                LEFT JOIN film_likes fl ON f.film_id = fl.film_id
                GROUP BY f.film_id
                ORDER BY likes_count DESC
                LIMIT ?
                """;
        List<Film> films = jdbcTemplate.query(sql, new FilmRowMapper(), count);
        loadGenresForFilms(films);
        return films;
    }

    public void addLike(Film film, User user) {
        String sql = "MERGE INTO film_likes (film_id, user_id) KEY (film_id, user_id) VALUES (?, ?)";
        jdbcTemplate.update(sql, film.getId(), user.getId());
        log.debug("Лайк добавлен в БД: фильм {}, пользователь {}", film.getId(), user.getId());
    }

    public void removeLike(Film film, User user) {
        String sql = "DELETE FROM film_likes WHERE film_id = ? AND user_id = ?";
        jdbcTemplate.update(sql, film.getId(), user.getId());
        log.debug("Лайк удалён из БД: фильм {}, пользователь {}", film.getId(), user.getId());
    }

    private void saveGenres(int filmId, Set<Genre> genres) {
        String sql = "INSERT INTO film_genre (film_id, genre_id) VALUES (?, ?)";
        for (Genre genre : genres) {
            jdbcTemplate.update(sql, filmId, genre.getId());
        }
    }

    private void deleteGenres(int filmId) {
        String sql = "DELETE FROM film_genre WHERE film_id = ?";
        jdbcTemplate.update(sql, filmId);
    }

    private void loadGenresForFilm(Film film) {
        String sql = """
                SELECT g.genre_id, g.genre_name
                FROM genre g
                JOIN film_genre fg ON g.genre_id = fg.genre_id
                WHERE fg.film_id = ?
                ORDER BY g.genre_id
                """;
        List<Genre> genres = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Genre genre = new Genre();
            genre.setId(rs.getInt("genre_id"));
            genre.setName(rs.getString("genre_name"));
            return genre;
        }, film.getId());

        film.getGenres().clear();
        film.getGenres().addAll(genres);
    }

    private void loadGenresForFilms(List<Film> films) {
        if (films.isEmpty()) {
            return;
        }

        String inSql = String.join(",", Collections.nCopies(films.size(), "?"));
        String sql = String.format("""
                SELECT fg.film_id, g.genre_id, g.genre_name
                FROM film_genre fg
                JOIN genre g ON fg.genre_id = g.genre_id
                WHERE fg.film_id IN (%s)
                ORDER BY g.genre_id
                """, inSql);

        Map<Integer, Set<Genre>> filmGenres = new HashMap<>();
        jdbcTemplate.query(sql, rs -> {
            int filmId = rs.getInt("film_id");
            Genre genre = new Genre();
            genre.setId(rs.getInt("genre_id"));
            genre.setName(rs.getString("genre_name"));
            filmGenres.computeIfAbsent(filmId, k -> new LinkedHashSet<>()).add(genre);
        }, films.stream().map(Film::getId).toArray());

        films.forEach(film -> {
            film.getGenres().clear();
            Set<Genre> genres = filmGenres.get(film.getId());
            if (genres != null) {
                film.getGenres().addAll(genres);
            }
        });
    }

    private static class FilmRowMapper implements RowMapper<Film> {
        @Override
        public Film mapRow(ResultSet rs, int rowNum) throws SQLException {
            Film film = new Film();
            film.setId(rs.getInt("film_id"));
            film.setName(rs.getString("name"));
            film.setDescription(rs.getString("description"));
            film.setReleaseDate(rs.getDate("release_date").toLocalDate());
            film.setDuration(rs.getInt("duration"));

            Mpa mpa = new Mpa();
            mpa.setId(rs.getInt("mpa_id"));
            mpa.setName(rs.getString("mpa_name"));
            film.setMpa(mpa);

            return film;
        }
    }
}