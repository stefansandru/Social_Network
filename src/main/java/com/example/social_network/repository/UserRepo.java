package com.example.social_network.repository;

import com.example.social_network.domain.User;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserRepo implements Repository<Long, User> {
    private static final Logger logger = LoggerFactory.getLogger(UserRepo.class);

    private final String url;
    private final String user;
    private final String password;
    private final String photosFolder;

    public UserRepo(
            String url,
            String user,
            String password,
            String photosFolder) {
        this.url = url;
        this.user = user;
        this.password = password;
        this.photosFolder = photosFolder;
    }

    @Override
    public Optional<User> findOne(Long id) {
        String query = "SELECT * FROM users WHERE ID = ?";
        User user = null;

        try (Connection connection = DriverManager.getConnection(url, this.user, password);
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setLong(1, id);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                Long userID = resultSet.getLong("ID");
                String name = resultSet.getString("name");
                String password = resultSet.getString("password");
                String profileImagePath = resultSet.getString("profile_image_path");
                user = new User(userID, name, password, photosFolder + profileImagePath);
            }
        } catch (SQLException e) {
            logger.error("Database error: User findOne.", e);        }
        return Optional.ofNullable(user);
    }

    @Override
    public Iterable<User> findAll() {
        Map<Long, User> users = new HashMap<>();
        String query = "SELECT * FROM users";
        try (Connection connection = DriverManager.getConnection(url, this.user, password);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            while (resultSet.next()) {
                Long id = resultSet.getLong("id");
                String name = resultSet.getString("name");
                String password = resultSet.getString("password");
                String profileImagePath = resultSet.getString("profile_image_path");
                User user = new User(id, name, password, profileImagePath);
                users.put(id, user);
            }
        } catch (SQLException e) {
            logger.error("Database error: Iterable<User> findAll", e);        }
        return users.values();
    }

    public Iterable<User> findUsersByPrefix(String prefix, Long userID) {
        Map<Long, User> users = new HashMap<>();
        String query = "SELECT * FROM users WHERE name LIKE ? AND ID != ?";
        try (Connection connection = DriverManager.getConnection(url, this.user, password);
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, prefix + "%");
            statement.setLong(2, userID);
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                Long id = resultSet.getLong("id");
                String name = resultSet.getString("name");
                String password = resultSet.getString("password");
                String profileImagePath = resultSet.getString("profile_image_path");
                User user = new User(id, name, password, profileImagePath);
                users.put(id, user);
            }
        } catch (SQLException e) {
            logger.error("Database error: Iterable<User> findUsersByPrefix", e);        }
        return users.values();
    }

    @Override
    public Optional<User> save(User entity) {
        String query = "INSERT INTO users (name, password, profile_image_path) VALUES (?, ?, ?)";
        try (Connection connection = DriverManager.getConnection(url, this.user, password);
             PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, entity.getName());
            statement.setString(2, entity.getPassword());
            statement.setString(3, entity.getProfileImagePath());

            int affectedRows = statement.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        entity.setId(generatedKeys.getLong(1));
                    }
                }
                return Optional.of(entity);
            }
        } catch (SQLException e) {
            logger.error("Database error: Optional<User> save", e);        }
        return Optional.empty();
    }

    @Override
    public Optional<User> delete(Long id) {
        Optional<User> userToDelete = findOne(id);
        if (userToDelete.isEmpty()) {
            return Optional.empty();
        }

        String query = "DELETE FROM users WHERE ID = ?";
        try (Connection connection = DriverManager.getConnection(url, this.user, password);
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setLong(1, id);
            statement.executeUpdate();

        } catch (SQLException e) {
            logger.error("Database error: Optional<User> delete", e);
            return Optional.empty();
        }
        return userToDelete;
    }

    @Override
    public Optional<User> update(User entity) {
        if (entity == null || entity.getId() == null) {
            throw new IllegalArgumentException("User or User ID cannot be null!");
        }

        String query = "UPDATE users SET name = ? WHERE ID = ?";
        try (Connection connection = DriverManager.getConnection(url, this.user, password);
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, entity.getName());
            statement.setLong(2, entity.getId());

            int rowsUpdated = statement.executeUpdate();
            if (rowsUpdated == 0) {
                return Optional.empty();
            }

        } catch (SQLException e) {
            logger.error("Database error: Optional<User> update", e);        }
        return Optional.of(entity);
    }

    public Optional<User> findUserByUsername(String username) {
        String query = "SELECT * FROM users WHERE name = ?";
        try (Connection connection = DriverManager.getConnection(url, this.user, password);
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                Long id = resultSet.getLong("id");
                String name = resultSet.getString("name");
                String password = resultSet.getString("password");
                String profileImagePath = resultSet.getString( "profile_image_path");
                User user = new User(id, name, password,photosFolder +  profileImagePath);
                return Optional.of(user);
            }

        } catch (SQLException e) {
            logger.error("Database error: Optional<User> findUserByUsername", e);        }
        return Optional.empty();
    }
}