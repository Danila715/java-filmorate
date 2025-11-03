package ru.yandex.practicum.filmorate.storage.user;

import ru.yandex.practicum.filmorate.model.User;

import java.util.List;
import java.util.Optional;

public interface UserStorage {
    User add(User user);

    User update(User user);

    List<User> getAll();

    Optional<User> findById(int id);

    List<User> getCommonFriends(int userId, int otherId);
}