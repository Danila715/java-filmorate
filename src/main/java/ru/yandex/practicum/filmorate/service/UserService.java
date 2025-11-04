package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.List;

@Service
@Slf4j
public class UserService {

    private final UserStorage userStorage;
    private final UserDbStorage userDbStorage;

    public UserService(@Qualifier("userDbStorage") UserStorage userStorage,
                       UserDbStorage userDbStorage) {
        this.userStorage = userStorage;
        this.userDbStorage = userDbStorage;
    }

    public User add(User user) {
        if (user.getLogin() == null || user.getLogin().isBlank()) {
            log.warn("Попытка создать пользователя без логина");
            throw new ValidationException("Логин не может быть пустым");
        }
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
            log.debug("Имя пустое — установлено как логин: {}", user.getLogin());
        }
        return userStorage.add(user);
    }

    public User update(User user) {
        return userStorage.findById(user.getId())
                .map(existing -> {
                    if (user.getEmail() != null && !user.getEmail().isBlank()) {
                        existing.setEmail(user.getEmail());
                    }
                    if (user.getLogin() != null && !user.getLogin().isBlank()) {
                        existing.setLogin(user.getLogin());
                    }
                    if (user.getName() != null) {
                        existing.setName(user.getName().isBlank() ? existing.getLogin() : user.getName());
                    }
                    if (user.getBirthday() != null) {
                        existing.setBirthday(user.getBirthday());
                    }
                    return userStorage.update(existing);
                })
                .orElseThrow(() -> new NotFoundException("Пользователь с id = " + user.getId() + " не найден"));
    }

    public List<User> getAll() {
        return userStorage.getAll();
    }

    public User getById(int id) {
        return userStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id = " + id + " не найден"));
    }

    public void addFriend(int userId, int friendId) {
        // Проверяем существование пользователей
        getById(userId);
        getById(friendId);

        // Добавляем одностороннюю дружбу (согласно новым требованиям)
        userDbStorage.addFriend(userId, friendId);
        log.debug("Дружба добавлена: {} → {}", userId, friendId);
    }

    public void removeFriend(int userId, int friendId) {
        // Проверяем существование пользователей
        getById(userId);
        getById(friendId);

        userDbStorage.removeFriend(userId, friendId);
        log.debug("Дружба удалена: {} → {}", userId, friendId);
    }

    public List<User> getFriends(int userId) {
        // Проверяем существование пользователя
        getById(userId);

        return userDbStorage.getFriends(userId);
    }

    public List<User> getCommonFriends(int userId, int otherId) {
        // Проверяем существование пользователей
        getById(userId);
        getById(otherId);

        return userStorage.getCommonFriends(userId, otherId);
    }
}