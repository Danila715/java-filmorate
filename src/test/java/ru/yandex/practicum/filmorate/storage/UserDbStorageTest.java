package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@Import(UserDbStorage.class)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class UserDbStorageTest {

    private final UserDbStorage userStorage;
    private final JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // Очищаем данные перед каждым тестом
        jdbcTemplate.update("DELETE FROM friendship");
        jdbcTemplate.update("DELETE FROM film_likes");
        jdbcTemplate.update("DELETE FROM users");

        // Сбрасываем счетчик автоинкремента
        jdbcTemplate.update("ALTER TABLE users ALTER COLUMN user_id RESTART WITH 1");
    }

    @Test
    void testAddUser() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setLogin("testuser");
        user.setName("Test User");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        User savedUser = userStorage.add(user);

        assertThat(savedUser.getId()).isGreaterThan(0);
        assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
        assertThat(savedUser.getLogin()).isEqualTo("testuser");
        assertThat(savedUser.getName()).isEqualTo("Test User");
    }

    @Test
    void testFindUserById() {
        User user = new User();
        user.setEmail("find@example.com");
        user.setLogin("finduser");
        user.setName("Find User");
        user.setBirthday(LocalDate.of(1995, 5, 15));

        User savedUser = userStorage.add(user);

        Optional<User> foundUser = userStorage.findById(savedUser.getId());

        assertThat(foundUser)
                .isPresent()
                .hasValueSatisfying(u -> {
                    assertThat(u.getId()).isEqualTo(savedUser.getId());
                    assertThat(u.getEmail()).isEqualTo("find@example.com");
                    assertThat(u.getLogin()).isEqualTo("finduser");
                });
    }

    @Test
    void testUpdateUser() {
        User user = new User();
        user.setEmail("update@example.com");
        user.setLogin("updateuser");
        user.setName("Update User");
        user.setBirthday(LocalDate.of(1992, 3, 20));

        User savedUser = userStorage.add(user);

        savedUser.setName("Updated Name");
        savedUser.setEmail("updated@example.com");
        User updatedUser = userStorage.update(savedUser);

        assertThat(updatedUser.getName()).isEqualTo("Updated Name");
        assertThat(updatedUser.getEmail()).isEqualTo("updated@example.com");
    }

    @Test
    void testGetAllUsers() {
        User user1 = new User();
        user1.setEmail("user1@example.com");
        user1.setLogin("user1");
        user1.setName("User 1");
        user1.setBirthday(LocalDate.of(1990, 1, 1));

        User user2 = new User();
        user2.setEmail("user2@example.com");
        user2.setLogin("user2");
        user2.setName("User 2");
        user2.setBirthday(LocalDate.of(1991, 2, 2));

        userStorage.add(user1);
        userStorage.add(user2);

        List<User> users = userStorage.getAll();

        assertThat(users).hasSize(2);
    }

    @Test
    void testAddFriend() {
        User user1 = new User();
        user1.setEmail("friend1@example.com");
        user1.setLogin("friend1");
        user1.setName("Friend 1");
        user1.setBirthday(LocalDate.of(1990, 1, 1));

        User user2 = new User();
        user2.setEmail("friend2@example.com");
        user2.setLogin("friend2");
        user2.setName("Friend 2");
        user2.setBirthday(LocalDate.of(1991, 2, 2));

        User savedUser1 = userStorage.add(user1);
        User savedUser2 = userStorage.add(user2);

        userStorage.addFriend(savedUser1.getId(), savedUser2.getId());

        List<User> friends = userStorage.getFriends(savedUser1.getId());

        assertThat(friends).hasSize(1);
        assertThat(friends.get(0).getId()).isEqualTo(savedUser2.getId());
    }

    @Test
    void testRemoveFriend() {
        User user1 = new User();
        user1.setEmail("remove1@example.com");
        user1.setLogin("remove1");
        user1.setName("Remove 1");
        user1.setBirthday(LocalDate.of(1990, 1, 1));

        User user2 = new User();
        user2.setEmail("remove2@example.com");
        user2.setLogin("remove2");
        user2.setName("Remove 2");
        user2.setBirthday(LocalDate.of(1991, 2, 2));

        User savedUser1 = userStorage.add(user1);
        User savedUser2 = userStorage.add(user2);

        userStorage.addFriend(savedUser1.getId(), savedUser2.getId());
        userStorage.removeFriend(savedUser1.getId(), savedUser2.getId());

        List<User> friends = userStorage.getFriends(savedUser1.getId());

        assertThat(friends).isEmpty();
    }

    @Test
    void testGetCommonFriends() {
        User user1 = new User();
        user1.setEmail("common1@example.com");
        user1.setLogin("common1");
        user1.setName("Common 1");
        user1.setBirthday(LocalDate.of(1990, 1, 1));

        User user2 = new User();
        user2.setEmail("common2@example.com");
        user2.setLogin("common2");
        user2.setName("Common 2");
        user2.setBirthday(LocalDate.of(1991, 2, 2));

        User commonFriend = new User();
        commonFriend.setEmail("commonfriend@example.com");
        commonFriend.setLogin("commonfriend");
        commonFriend.setName("Common Friend");
        commonFriend.setBirthday(LocalDate.of(1992, 3, 3));

        User savedUser1 = userStorage.add(user1);
        User savedUser2 = userStorage.add(user2);
        User savedCommonFriend = userStorage.add(commonFriend);

        userStorage.addFriend(savedUser1.getId(), savedCommonFriend.getId());
        userStorage.addFriend(savedUser2.getId(), savedCommonFriend.getId());

        List<User> commonFriends = userStorage.getCommonFriends(savedUser1.getId(), savedUser2.getId());

        assertThat(commonFriends).hasSize(1);
        assertThat(commonFriends.get(0).getId()).isEqualTo(savedCommonFriend.getId());
    }
}