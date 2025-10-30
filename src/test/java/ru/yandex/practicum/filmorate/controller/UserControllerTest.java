package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class UserControllerTest {

    private UserController controller;

    @BeforeEach
    void setUp() {
        controller = new UserController();
    }

    @Test
    void shouldNotAllowEmptyEmail() {
        User user = new User();
        user.setEmail("");
        user.setLogin("validLogin");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> controller.createUser(user)
        );
        assertTrue(exception.getMessage().contains("должна содержать символ @"));
    }

    @Test
    void shouldNotAllowEmailWithoutAtSymbol() {
        User user = new User();
        user.setEmail("invalid-email");
        user.setLogin("validLogin");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> controller.createUser(user)
        );
        assertTrue(exception.getMessage().contains("должна содержать символ @"));
    }

    @Test
    void shouldNotAllowEmptyLogin() {
        User user = new User();
        user.setEmail("user@example.com");
        user.setLogin("");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> controller.createUser(user)
        );
        assertTrue(exception.getMessage().contains("не может быть пустым"));
    }

    @Test
    void shouldNotAllowLoginWithSpaces() {
        User user = new User();
        user.setEmail("user@example.com");
        user.setLogin("invalid login");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> controller.createUser(user)
        );
        assertEquals("Логин не может быть пустым и содержать пробелы", exception.getMessage());
    }

    @Test
    void shouldNotAllowBirthdayInFuture() {
        User user = new User();
        user.setEmail("user@example.com");
        user.setLogin("validLogin");
        user.setBirthday(LocalDate.now().plusDays(1));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> controller.createUser(user)
        );
        assertTrue(exception.getMessage().contains("не может быть в будущем"));
    }

    @Test
    void shouldUseLoginAsNameIfNameIsEmpty() {
        User user = new User();
        user.setEmail("user@example.com");
        user.setLogin("cooluser");
        user.setName("");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        User created = controller.createUser(user);

        assertEquals("cooluser", created.getName());
        assertEquals(1, created.getId());
    }

    @Test
    void shouldCreateValidUser() {
        User user = new User();
        user.setEmail("user@example.com");
        user.setLogin("validLogin");
        user.setName("John Doe");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        User created = controller.createUser(user);

        assertNotNull(created);
        assertEquals(1, created.getId());
        assertEquals("user@example.com", created.getEmail());
        assertEquals("John Doe", created.getName());
    }

    @Test
    void shouldUpdateUserPartially() {
        // Создаём пользователя
        User user = new User();
        user.setEmail("old@example.com");
        user.setLogin("oldLogin");
        user.setName("Old Name");
        user.setBirthday(LocalDate.of(1980, 1, 1));
        User created = controller.createUser(user);

        // Обновляем: УКАЗЫВАЕМ СТАРЫЙ ЛОГИН, чтобы не было null
        User update = new User();
        update.setId(created.getId());
        update.setEmail("new@example.com");
        update.setName("New Name");
        update.setLogin("oldLogin"); // Явно передаём старый логин

        User updated = controller.updateUser(update);

        assertEquals("new@example.com", updated.getEmail());
        assertEquals("New Name", updated.getName());
        assertEquals("oldLogin", updated.getLogin());
        assertEquals(LocalDate.of(1980, 1, 1), updated.getBirthday());
    }

    @Test
    void shouldNotUpdateNonExistentUser() {
        User user = new User();
        user.setId(999);
        user.setEmail("test@example.com");
        user.setLogin("test");
        user.setName("Test");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> controller.updateUser(user)
        );
        assertTrue(exception.getMessage().contains("не найден"));
    }
}