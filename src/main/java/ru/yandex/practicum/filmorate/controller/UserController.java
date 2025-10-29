package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
@Slf4j
public class UserController {

    private final Map<Integer, User> users = new HashMap<>();
    private int nextId = 1;

    @GetMapping
    public List<User> getAllUsers() {
        log.info("Получен запрос на получение всех пользователей. Количество: {}", users.size());
        return new ArrayList<>(users.values());
    }

    @PostMapping
    public User createUser(@RequestBody User user) {
        validateUser(user);
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
            log.info("Имя не указано, использовано значение логина: {}", user.getLogin());
        }
        user.setId(nextId++);
        users.put(user.getId(), user);
        log.info("Пользователь добавлен: {} (ID: {})", user.getEmail(), user.getId());
        return user;
    }

    @PutMapping
    public User updateUser(@RequestBody User user) {
        if (user.getId() == 0 || !users.containsKey(user.getId())) {
            log.warn("Попытка обновить несуществующего пользователя с ID: {}", user.getId());
            throw new ValidationException("Пользователь с id = " + user.getId() + " не найден");
        }

        User existing = users.get(user.getId());

        validateUser(user);

        // Обновляем ТОЛЬКО НЕ-NULL ПОЛЯ из входящего user в existing
        if (user.getEmail() != null) {
            existing.setEmail(user.getEmail());
        }
        if (user.getLogin() != null) {
            existing.setLogin(user.getLogin());
        }
        if (user.getName() != null) {
            existing.setName(user.getName());
        }
        if (user.getBirthday() != null) {
            existing.setBirthday(user.getBirthday());
        }

        // Сохраняем existing, а не user!
        users.put(existing.getId(), existing);
        log.info("Пользователь обновлён: {} (ID: {})", existing.getEmail(), existing.getId());
        return existing;
    }

    private void validateUser(User user) {
        if (user.getEmail() == null || user.getEmail().isBlank() || !user.getEmail().contains("@")) {
            log.warn("Ошибка валидации: некорректный email: {}", user.getEmail());
            throw new ValidationException("Электронная почта не может быть пустой и должна содержать символ @");
        }
        if (user.getLogin() == null || user.getLogin().isBlank() || user.getLogin().contains(" ")) {
            log.warn("Ошибка валидации: некорректный логин: {}", user.getLogin());
            throw new ValidationException("Логин не может быть пустым и содержать пробелы");
        }
        if (user.getBirthday() != null && user.getBirthday().isAfter(LocalDate.now())) {
            log.warn("Ошибка валидации: дата рождения в будущем: {}", user.getBirthday());
            throw new ValidationException("Дата рождения не может быть в будущем");
        }
    }
}