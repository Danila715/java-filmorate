package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserStorage userStorage;

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    public User add(User user) {
        log.info("Добавление пользователя: {}", user.getLogin());
        if (user.getLogin() == null || user.getLogin().isBlank()) {
            log.warn("Попытка создать пользователя без логина");
            throw new ValidationException("Логин не может быть пустым");
        }
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
            log.debug("Имя пользователя пустое — установлено как логин: {}", user.getLogin());
        }
        User saved = userStorage.add(user);
        log.debug("Пользователь добавлен с ID: {}", saved.getId());
        return saved;
    }

    public User update(User user) {
        log.info("Обновление пользователя с ID: {}", user.getId());
        User existing = userStorage.getById(user.getId());

        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            existing.setEmail(user.getEmail());
        }
        if (user.getLogin() != null && !user.getLogin().isBlank()) {
            existing.setLogin(user.getLogin());
        }
        if (user.getName() != null) {
            if (user.getName().isBlank()) {
                existing.setName(existing.getLogin());
                log.debug("Имя пустое — установлено как логин: {}", existing.getLogin());
            } else {
                existing.setName(user.getName());
            }
        }
        if (user.getBirthday() != null) {
            existing.setBirthday(user.getBirthday());
        }

        User updated = userStorage.update(existing);
        log.debug("Пользователь обновлён: {}", updated);
        return updated;
    }

    public List<User> getAll() {
        return userStorage.getAll();
    }

    public User getById(int id) {
        return userStorage.getById(id);
    }

    public void addFriend(int userId, int friendId) {
        User user = userStorage.getById(userId);
        User friend = userStorage.getById(friendId);
        user.getFriends().add(friendId);
        friend.getFriends().add(userId);
    }

    public void removeFriend(int userId, int friendId) {
        User user = userStorage.getById(userId);
        User friend = userStorage.getById(friendId);
        user.getFriends().remove(friendId);
        friend.getFriends().remove(userId);
    }

    public List<User> getFriends(int userId) {
        User user = userStorage.getById(userId);
        return user.getFriends().stream()
                .map(userStorage::getById)
                .collect(Collectors.toList());
    }

    public List<User> getCommonFriends(int userId, int otherId) {
        User user = userStorage.getById(userId);
        User other = userStorage.getById(otherId);
        Set<Integer> common = new HashSet<>(user.getFriends());
        common.retainAll(other.getFriends());
        return common.stream()
                .map(userStorage::getById)
                .collect(Collectors.toList());
    }
}