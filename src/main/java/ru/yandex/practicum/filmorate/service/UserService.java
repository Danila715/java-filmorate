package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.FriendshipStatus;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserStorage userStorage;

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
        User user = getById(userId);
        User friend = getById(friendId);

        // Проверяем, есть ли уже запрос от friend к user
        FriendshipStatus friendStatus = friend.getFriends().get(userId);

        if (friendStatus == FriendshipStatus.UNCONFIRMED) {
            // Если friend уже отправил запрос, подтверждаем дружбу с обеих сторон
            user.getFriends().put(friendId, FriendshipStatus.CONFIRMED);
            friend.getFriends().put(userId, FriendshipStatus.CONFIRMED);
            log.debug("Дружба подтверждена: {} ↔ {}", userId, friendId);
        } else {
            // Иначе создаём неподтверждённый запрос
            user.getFriends().put(friendId, FriendshipStatus.UNCONFIRMED);
            friend.getFriends().put(userId, FriendshipStatus.UNCONFIRMED);
            log.debug("Запрос на дружбу отправлен: {} → {}", userId, friendId);
        }
    }

    public void removeFriend(int userId, int friendId) {
        User user = getById(userId);
        User friend = getById(friendId);
        user.getFriends().remove(friendId);
        friend.getFriends().remove(userId);
        log.debug("Дружба удалена: {} ↔ {}", userId, friendId);
    }

    public List<User> getFriends(int userId) {
        User user = getById(userId);
        return user.getFriends().keySet().stream()
                .map(userStorage::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    public List<User> getCommonFriends(int userId, int otherId) {
        return userStorage.getCommonFriends(userId, otherId);
    }
}