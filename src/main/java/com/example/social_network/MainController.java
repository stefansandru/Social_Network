package com.example.social_network;

import com.example.social_network.repository.MessageRepo;
import com.example.social_network.repository.FriendshipRepo;
import com.example.social_network.repository.UserRepo;
import com.example.social_network.service.SocialNetworkService;
import com.example.social_network.validator.UserValidator;
import com.example.social_network.domain.Constants;
import com.example.social_network.domain.User;
import com.example.social_network.paging.Page;
import com.example.social_network.paging.Pageable;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.image.ImageView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;

public class MainController {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    private Long userId;
    private String username;
    private String profileImagePath;
    private final SocialNetworkService service;
    private final ObservableList<User> notFriendsList = FXCollections.observableArrayList();
    private final ObservableMap<User, LocalDateTime> pendingFriendshipsList = FXCollections.observableHashMap();
    private ContextMenu currentContextMenu;
    private int currentPage = 0;

    @FXML
    private Label labelPage;

    @FXML
    private Button buttonPrevious;

    @FXML
    private Button buttonNext;

    @FXML
    private VBox friendsVBox;

    @FXML
    private ListView<User> searchResultsListView;

    @FXML
    private ListView<User> pendingFriendshipsListView;

    @FXML
    private TextField searchField;

    @FXML
    private Label notificationLabel;

    @FXML
    private ImageView profileImageView;

    @FXML
    private Label usernameLabel;

    public MainController() {
        String url = DBConnectionAndProfileImagesPath.INSTANCE.getUrl();
        String user = DBConnectionAndProfileImagesPath.INSTANCE.getUser();
        String password = DBConnectionAndProfileImagesPath.INSTANCE.getPassword();
        String photosFolder = DBConnectionAndProfileImagesPath.INSTANCE.getPhotosFolder();

        UserRepo userRepo = new UserRepo(
                url,
                user,
                password,
                photosFolder);
        FriendshipRepo friendshipRepo = new FriendshipRepo(
                url,
                user,
                password,
                photosFolder);
        MessageRepo messageRepo = new MessageRepo(url, user, password, userRepo);

        UserValidator userValidator = new UserValidator();
        this.service = new SocialNetworkService(userRepo, friendshipRepo, messageRepo, userValidator);
    }

    public void setUser(Long userId, String username, String profileImagePath) {
        this.userId = userId;
        this.username = username;
        this.profileImagePath = profileImagePath;

        usernameLabel.setText(username);

        try {
            File file = new File(profileImagePath);
            Image image = new Image(new FileInputStream(file));
            profileImageView.setImage(image);
        } catch (FileNotFoundException e) {
            logger.error("Failed to load profile image: {}", e.getMessage(), e);
        }

        loadFriends();
        loadNotFriends();
        loadPendingFriendships();

        int pendingRequests = pendingFriendshipsList.size();
        if (pendingRequests > 0) {
            showNotification("You have " + pendingRequests + " pending friend requests!");
        }
    }

    @FXML
    private void handleGlobalClick(MouseEvent event) {
        // Clear selection when clicking outside the list views

        Object source = event.getTarget();

        if (!(source instanceof ListView)) {
            searchResultsListView.getSelectionModel().clearSelection();
            pendingFriendshipsListView.getSelectionModel().clearSelection();

            if (currentContextMenu != null) {
                currentContextMenu.hide();
                currentContextMenu = null;
            }
        }
    }

    private void loadFriends() {
        int pageSize = 1;
        Page<User> page = service.findAllOnPage(new Pageable(pageSize, currentPage), userId);
        int maxPage = (int) Math.ceil((double) page.getTotalNumberOfElements() / pageSize) - 1;
        if (maxPage == -1) {
            maxPage = 0;
        }
        if (currentPage > maxPage) {
            currentPage = maxPage;
            page = service.findAllOnPage(new Pageable(pageSize, currentPage), userId);
        }
        int totalNumberOfElements = page.getTotalNumberOfElements();
        buttonPrevious.setDisable(currentPage == 0);
        buttonNext.setDisable((currentPage + 1) * pageSize >= totalNumberOfElements);
        List<User> friends = StreamSupport.stream(page.getElementsOnPage().spliterator(), false)
                .toList();
        friendsVBox.getChildren().clear();
        for (User user : friends) {
            try {
                // Load user image
                File file = new File(user.getProfileImagePath());
                Image image = new Image(new FileInputStream(file));
                ImageView imageView = new ImageView(image);
                imageView.setFitHeight(70);
                imageView.setFitWidth(70);

                // Get number of friends and total messages
                int numberOfFriends = service.getNumberOfFriends(user.getId());
                int totalMessages = service.getTotalMessagesSent(user.getId());

                Button unfriendButton = new Button("Unfriend");
                unfriendButton.setOnAction(e -> handleUnfriend(user));
                Button chatButton = new Button("Chat");
                chatButton.setOnAction(e -> handleChat(user));

                // Create a VBox to hold the user details
                VBox userVBox = new VBox();
                userVBox.setAlignment(Pos.CENTER);
                userVBox.setPadding(new Insets(60, 10, 60, 10));
                userVBox.setSpacing(10);
                userVBox.getChildren().addAll(
                        imageView,
                        new Label(user.getName()) {{
                            setStyle("-fx-font-size: 24");
                        }},
                        new Label(numberOfFriends == 1 ? "1 friend" : numberOfFriends + " friends") {{
                            setStyle("-fx-font-size: 18");
                        }},
                        new Label(totalMessages + " messages sent") {{
                            setStyle("-fx-font-size: 18");
                        }},
                        unfriendButton,
                        chatButton
                );

                friendsVBox.getChildren().add(userVBox);
            } catch (FileNotFoundException e) {
                logger.error("Failed to load friend's image: {}", e.getMessage(), e);
            }
        }
        labelPage.setText("Page " + (currentPage + 1) + " of " + (maxPage + 1));
    }

    private void loadNotFriends() {
        notFriendsList.setAll(service.getNotFriends(userId));
        searchResultsListView.setItems(notFriendsList);
    }

    private void loadPendingFriendships() {
        pendingFriendshipsList.clear();
        pendingFriendshipsList.putAll(service.getPendingFriendships(userId));
        pendingFriendshipsListView.setItems(FXCollections.observableArrayList(pendingFriendshipsList.keySet()));
        pendingFriendshipsListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                } else {
                    setText(user.getName() + " - " + pendingFriendshipsList.get(user).toString());
                }
            }
        });
    }

    @FXML
    private void handleSearch() {
        String searchText = searchField.getText().toLowerCase();
        List<User> filteredList = service.findNotFriendsByPrefix(searchText, userId);
        notFriendsList.setAll(filteredList);
        searchResultsListView.setItems(notFriendsList);
    }

    private void showContextMenu(ListView<User> listView, MouseEvent event, String... actions) {
        if (currentContextMenu != null) {
            currentContextMenu.hide();
        }
        ContextMenu contextMenu = new ContextMenu();
        for (String action : actions) {
            MenuItem menuItem = new MenuItem(action);
            menuItem.setOnAction(e -> handleAction(action, listView.getSelectionModel().getSelectedItem()));
            contextMenu.getItems().add(menuItem);
        }
        contextMenu.show(listView, event.getScreenX(), event.getScreenY());
        currentContextMenu = contextMenu;
    }

    private void handleAction(String action, User selectedItem) {
        switch (action) {
            case "Add User":
                handleAddUser(selectedItem);
                break;
            case "Accept":
                handleAcceptFriendship(selectedItem);
                break;
            case "Reject":
                handleRejectFriendship(selectedItem);
                break;
            case "Unfriend":
                handleUnfriend(selectedItem);
                break;
            case "Chat":
                handleChat(selectedItem);
                break;
        }
    }

    @FXML
    private void handleSearchResultsClick(MouseEvent event) {
        if (event.getClickCount() == 1) {
            showContextMenu(searchResultsListView, event, "Chat", "Add User");
        }
    }

    @FXML
    private void handlePendingFriendshipsClick(MouseEvent event) {
        if (event.getClickCount() == 1) {
            showContextMenu(pendingFriendshipsListView, event, "Chat", "Accept", "Reject");
        }
    }

    private void handleUnfriend(User selectedFriend) {
        if (selectedFriend != null) {
            service.removeFriendship(userId, selectedFriend.getId());
            loadFriends();
            loadNotFriends();
            loadPendingFriendships();
        }
    }

    private void handleAddUser(User selectedUser) {
        if (selectedUser != null) {
            service.addFriendship(userId, selectedUser.getId(), Constants.PENDING + selectedUser.getId());
            loadFriends();
            loadNotFriends();
            loadPendingFriendships();
        }
    }

    private void handleAcceptFriendship(User user) {
        if (user != null && !user.getId().equals(userId)) {
            service.updateFriendshipStatus(userId, user.getId(), Constants.ACTIVE);
            loadFriends();
            loadNotFriends();
            loadPendingFriendships();
        }
    }

    private void handleRejectFriendship(User user) {
        if (user != null) {
            service.removeFriendship(userId, user.getId());
            loadFriends();
            loadNotFriends();
            loadPendingFriendships();
        }
    }

    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("login-view.fxml"));
            Stage stage = (Stage) friendsVBox.getScene().getWindow();
            Scene scene = new Scene(loader.load(), 500, 500);
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("style.css")).toExternalForm());
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            logger.error("Error during logout: {}", e.getMessage(), e);
        }
    }

    @FXML
    private void handleChat(User selectedUser) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/social_network/chat-view.fxml"));
            Stage stage = (Stage) friendsVBox.getScene().getWindow();
            Scene scene = new Scene(loader.load(), 500, 600);
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/com/example/social_network/style.css")).toExternalForm());
            ChatController controller = loader.getController();
            controller.setUsers(userId, username, profileImagePath, selectedUser.getId(), selectedUser.getName());
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            logger.error("Error initializing chat: {}", e.getMessage(), e);
        }
    }

    @FXML
    public void handleMultipleMessage() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/social_network/multiple-message.fxml"));
            Stage stage = (Stage) friendsVBox.getScene().getWindow();
            Scene scene = new Scene(loader.load(), 500, 600);
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/com/example/social_network/style.css")).toExternalForm());
            MultipleMessage controller = loader.getController();
            controller.setUser(userId, username, profileImagePath);
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            logger.error("Error initializing multiple message: {}", e.getMessage(), e);
        }
    }

    @FXML
    private void showNotification(String message) {
        notificationLabel.setText(message);
        notificationLabel.setVisible(true);

        Timeline timeline = new Timeline(new KeyFrame(
                Duration.seconds(6),
                event -> notificationLabel.setVisible(false)
        ));
        timeline.setCycleCount(1);
        timeline.play();
    }

    public void handleNext() {
        currentPage ++;
        loadFriends();
    }

    public void handlePrevious() {
        currentPage --;
        loadFriends();
    }
}