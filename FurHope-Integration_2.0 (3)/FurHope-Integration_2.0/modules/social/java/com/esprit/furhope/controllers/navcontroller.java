package com.esprit.furhope.controllers;

import com.esprit.animal.Services.AnimalSchemaBootstrapService;
import com.esprit.animal.i18n.LanguageManager;
import controllers.SessionContext;
import com.esprit.utils.ThemeManager;
import com.esprit.furhope.services.FriendServiceJdbc;
import com.esprit.furhope.services.FriendServiceJdbc.UserMini;
import com.esprit.furhope.services.NotificationServiceJdbc;
import com.esprit.furhope.services.NotificationServiceJdbc.Notif;
import com.esprit.furhope.ui.AvatarViewFactory;
import com.esprit.furhope.utils.AppSession;
import com.esprit.furhope.utils.TimeUtils;
import entities.ManagerAccount;
import entities.User;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import utils.SessionManager;

public class navcontroller {

    @FXML
    private StackPane content;

    @FXML
    private TextField searchField;
    @FXML
    private Label sessionLabel;

    @FXML
    private Button homeBtn;

    @FXML
    private Button profileBtn;

    @FXML
    private Button shopBtn;

    @FXML
    private Button adoptBtn;

    @FXML
    private Button pharmacyBtn;

    @FXML
    private Button locationsBtn;

    @FXML
    private Button addFriendBtn;

    @FXML
    private Button backHomeBtn;

    @FXML
    private Button refreshBtn;

    @FXML
    private Button notifBtn;

    @FXML
    private Button requestsBtn;

    @FXML
    private Button friendsBtn;

    @FXML
    private Button logoutBtn;

    private final FriendServiceJdbc friendService = new FriendServiceJdbc();
    private final NotificationServiceJdbc notifSvc = new NotificationServiceJdbc();
    private Label notifBadgeLabel;
    private feedcontroller activeFeedController;

    @FXML
    public void initialize() {
        configureIcons();
        applyProfileTabAvatarIcon();
        configureNotificationBell();
        wireSearch();
        if (sessionLabel != null) {
            sessionLabel.setText("Logged in as " + AppSession.getDisplayName());
        }
        refreshNotificationBadge();
        setActiveNav(homeBtn);
        loadFeed();
    }

    @FXML
    private void onHomeClick() {
        setActiveNav(homeBtn);
        loadFeed();
    }

    @FXML
    private void onProfileClick() {
        setActiveNav(profileBtn);
        if (!ensureLoggedIn()) {
            return;
        }
        switchScene("/profile.fxml");
    }

    @FXML
    private void onShopClick() {
        setActiveNav(shopBtn);
        if (!ensureLoggedIn()) {
            return;
        }
        try {
            switchScene("/shop.fxml");
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Unable to open Shop module.").showAndWait();
        }
    }

    @FXML
    private void onAdoptClick() {
        setActiveNav(adoptBtn);
        if (!ensureLoggedIn()) {
            return;
        }
        User user = SessionContext.getCurrentUser();
        if (user == null) {
            new Alert(Alert.AlertType.WARNING, "Unable to resolve current user session.").showAndWait();
            return;
        }
        try {
            AnimalSchemaBootstrapService.ensureSchemaReady();
            bootstrapAnimalSession(user);
            switchScene("/animal/AfficherAnimal.fxml");
        } catch (RuntimeException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Unable to open Adoption module.").showAndWait();
        }
    }

    @FXML
    private void onPharmacyClick() {
        setActiveNav(pharmacyBtn);
        if (!ensureLoggedIn()) {
            return;
        }
        User user = SessionContext.getCurrentUser();
        if (user == null) {
            new Alert(Alert.AlertType.WARNING, "Unable to resolve current user session.").showAndWait();
            return;
        }
        syncLegacyDashboardSession(user);
        if (isVeterinarian(user)) {
            switchScene("/VetDashboard.fxml");
        } else {
            switchScene("/DashboardClient.fxml");
        }
    }

    @FXML
    private void onLocationsClick() {
        setActiveNav(locationsBtn);
        if (!ensureLoggedIn()) {
            return;
        }
        User user = SessionContext.getCurrentUser();
        if (user == null) {
            new Alert(Alert.AlertType.WARNING, "Unable to resolve current user session.").showAndWait();
            return;
        }
        try {
            if (isAdminOrManager(user)) {
                String managerId = user.getManagerId();
                if (managerId == null || managerId.trim().isEmpty()) {
                    managerId = "ADMIN-" + Math.max(user.getId(), 1);
                }
                services.SessionContext.startManagerSession(
                        new ManagerAccount(managerId, resolveDisplayName(user))
                );
                switchScene("/HotelManagerDashboard.fxml");
            } else {
                int userId = user.getId() > 0 ? user.getId() : 1;
                services.SessionContext.startUserSession(userId);
                switchScene("/UserDashboard.fxml");
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Unable to open Hotel module.").showAndWait();
        }
    }

    @FXML
    private void onAddFriendClick() {
        int currentUserId = AppSession.getCurrentUserId();
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Add Friend");
        dialog.setHeaderText("Search by name");
        styleDialog(dialog);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        TextField nameField = new TextField();
        nameField.setPromptText("Type a name...");
        nameField.getStyleClass().add("dialog-input");

        ListView<UserMini> resultsList = new ListView<>();
        resultsList.setPrefHeight(320);
        resultsList.getStyleClass().add("social-list-view");
        resultsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(UserMini item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(null);
                    HBox row = buildUserMiniRow(item, "User ID #" + item.getId());
                    setGraphic(row);
                }
            }
        });

        Button sendBtn = new Button("Send request");
        sendBtn.setDisable(true);
        sendBtn.getStyleClass().add("composer-btn-primary");

        resultsList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            sendBtn.setDisable(newVal == null);
        });

        Runnable searchAction = () -> {
            String text = nameField.getText() == null ? "" : nameField.getText().trim();
            runAsync(
                    () -> friendService.searchUsersByName(text, currentUserId),
                    users -> resultsList.getItems().setAll(users),
                    ex -> {
                        ex.printStackTrace();
                        resultsList.getItems().clear();
                    },
                    "friend-search"
            );
        };

        nameField.textProperty().addListener((obs, oldVal, newVal) -> searchAction.run());
        searchAction.run();

        sendBtn.setOnAction(e -> {
            UserMini selectedUser = resultsList.getSelectionModel().getSelectedItem();
            if (selectedUser == null) return;
            runAsync(
                    () -> friendService.sendRequest(currentUserId, selectedUser.getId()),
                    sent -> {
                        if (sent) {
                            new Alert(Alert.AlertType.INFORMATION, "Friend request sent to " + selectedUser.getName() + ".").showAndWait();
                        } else {
                            new Alert(Alert.AlertType.INFORMATION, "Request not sent (already pending/friends or invalid).").showAndWait();
                        }
                        searchAction.run();
                    },
                    ex -> {
                        ex.printStackTrace();
                        new Alert(Alert.AlertType.ERROR, "Failed to send friend request: " + ex.getMessage()).showAndWait();
                    },
                    "friend-send-request"
            );
        });

        VBox contentBox = new VBox(8, nameField, resultsList, sendBtn);
        contentBox.setPadding(new Insets(6));
        contentBox.getStyleClass().add("friend-list-box");
        dialog.getDialogPane().setContent(contentBox);
        dialog.showAndWait();
    }

    @FXML
    private void onRefreshClick() {
        if (activeFeedController != null) {
            activeFeedController.reloadFeed();
        } else {
            loadFeed();
        }
    }

    @FXML
    private void onNotificationsClick() {
        int currentUserId = AppSession.getCurrentUserId();
        refreshNotificationBadge();

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Notifications");
        dialog.setHeaderText("Recent activity");
        styleDialog(dialog);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox listBox = new VBox(8);
        listBox.setPadding(new Insets(6));
        ScrollPane scroll = new ScrollPane(listBox);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(340);

        Button markAllReadBtn = new Button("Mark all read");
        markAllReadBtn.setOnAction(e -> {
            runAsync(
                    () -> {
                        notifSvc.markAllRead(currentUserId);
                        return true;
                    },
                    ok -> {
                        refreshNotificationBadge();
                        renderNotificationsList(listBox, currentUserId, dialog::close);
                    },
                    ex -> {
                        ex.printStackTrace();
                        new Alert(Alert.AlertType.ERROR, "Failed to mark notifications as read: " + ex.getMessage()).showAndWait();
                    },
                    "notif-mark-all-read"
            );
        });

        VBox contentBox = new VBox(10, markAllReadBtn, scroll);
        contentBox.setPadding(new Insets(6));
        dialog.getDialogPane().setContent(contentBox);

        renderNotificationsList(listBox, currentUserId, dialog::close);
        dialog.showAndWait();
    }

    @FXML
    private void onFriendsClick() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Friends");
        dialog.setHeaderText("Your friends");
        styleDialog(dialog);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox listBox = new VBox(10);
        listBox.getStyleClass().add("friend-list-box");
        listBox.setPadding(new Insets(8));

        Label loading = new Label("Loading...");
        loading.getStyleClass().add("placeholder-subtitle");
        listBox.getChildren().add(loading);
        runAsync(
                () -> friendService.getFriends(AppSession.getCurrentUserId()),
                friends -> {
                    listBox.getChildren().clear();
                    if (friends.isEmpty()) {
                        Label empty = new Label("No friends yet.");
                        empty.getStyleClass().add("placeholder-subtitle");
                        listBox.getChildren().add(empty);
                        return;
                    }
                    for (UserMini user : friends) {
                        HBox row = buildUserMiniRow(user, "User ID #" + user.getId());
                        listBox.getChildren().add(row);
                    }
                },
                e -> {
                    e.printStackTrace();
                    Label error = new Label("Failed to load friends.");
                    error.getStyleClass().add("placeholder-subtitle");
                    listBox.getChildren().setAll(error);
                },
                "friend-list-load"
        );

        ScrollPane scroll = new ScrollPane(listBox);
        scroll.getStyleClass().add("social-dialog-scroll");
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(420);
        dialog.getDialogPane().setContent(scroll);
        dialog.showAndWait();
    }

    @FXML
    private void onRequestsClick() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Friend Requests");
        dialog.setHeaderText("Incoming requests");
        styleDialog(dialog);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox listBox = new VBox(10);
        listBox.getStyleClass().add("friend-list-box");
        listBox.setPadding(new Insets(8));
        ScrollPane scroll = new ScrollPane(listBox);
        scroll.getStyleClass().add("social-dialog-scroll");
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(420);
        dialog.getDialogPane().setContent(scroll);

        Runnable[] refreshHolder = new Runnable[1];
        refreshHolder[0] = () -> {
            listBox.getChildren().clear();
            Label loading = new Label("Loading...");
            loading.getStyleClass().add("placeholder-subtitle");
            listBox.getChildren().add(loading);
            int currentUserId = AppSession.getCurrentUserId();
            runAsync(
                    () -> friendService.getIncomingRequests(currentUserId),
                    incoming -> {
                        listBox.getChildren().clear();
                        if (incoming.isEmpty()) {
                            Label empty = new Label("No incoming requests.");
                            empty.getStyleClass().add("placeholder-subtitle");
                            listBox.getChildren().add(empty);
                            return;
                        }

                        for (UserMini sender : incoming) {
                            Label name = new Label(sender.getName());
                            name.getStyleClass().add("friend-name");

                            Label meta = new Label("User ID #" + sender.getId());
                            meta.getStyleClass().add("friend-meta");

                            VBox personBox = new VBox(3, name, meta);
                            HBox.setHgrow(personBox, Priority.ALWAYS);

                            Node avatarNode = AvatarViewFactory.createAvatar(
                                    sender.getProfileImagePath(),
                                    sender.getName(),
                                    34,
                                    "friend-avatar"
                            );

                            Button acceptBtn = new Button("Accept");
                            Button declineBtn = new Button("Decline");
                            acceptBtn.getStyleClass().add("composer-btn-primary");
                            declineBtn.getStyleClass().add("composer-btn-secondary");

                            acceptBtn.setOnAction(e -> runAsync(
                                    () -> friendService.acceptRequest(currentUserId, sender.getId()),
                                    ok -> {
                                        refreshHolder[0].run();
                                        if (activeFeedController != null) {
                                            activeFeedController.reloadFeed();
                                        }
                                    },
                                    ex -> {
                                        ex.printStackTrace();
                                        new Alert(Alert.AlertType.ERROR, "Failed to accept request: " + ex.getMessage()).showAndWait();
                                    },
                                    "friend-accept"
                            ));

                            declineBtn.setOnAction(e -> runAsync(
                                    () -> friendService.declineRequest(currentUserId, sender.getId()),
                                    ok -> {
                                        refreshHolder[0].run();
                                        if (activeFeedController != null) {
                                            activeFeedController.reloadFeed();
                                        }
                                    },
                                    ex -> {
                                        ex.printStackTrace();
                                        new Alert(Alert.AlertType.ERROR, "Failed to decline request: " + ex.getMessage()).showAndWait();
                                    },
                                    "friend-decline"
                            ));

                            HBox row = new HBox(10, avatarNode, personBox, acceptBtn, declineBtn);
                            row.setAlignment(Pos.CENTER_LEFT);
                            row.getStyleClass().add("friend-row-card");
                            listBox.getChildren().add(row);
                        }
                    },
                    ex -> {
                        ex.printStackTrace();
                        Label error = new Label("Failed to load requests.");
                        error.getStyleClass().add("placeholder-subtitle");
                        listBox.getChildren().setAll(error);
                    },
                    "friend-incoming-load"
            );
        };

        refreshHolder[0].run();
        dialog.showAndWait();
    }

    @FXML
    private void onLogoutClick() {
        AppSession.clear();
        SessionContext.clear();
        switchToAccueil();
    }

    @FXML
    private void onBackHomeClick() {
        switchToAccueil();
    }

    private HBox buildUserMiniRow(UserMini user, String metaText) {
        Label nameLabel = new Label(user.getName());
        nameLabel.getStyleClass().add("friend-name");

        Label metaLabel = new Label(metaText);
        metaLabel.getStyleClass().add("friend-meta");

        VBox textBox = new VBox(3, nameLabel, metaLabel);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Node avatarNode = AvatarViewFactory.createAvatar(
                user.getProfileImagePath(),
                user.getName(),
                34,
                "friend-avatar"
        );

        HBox row = new HBox(10, avatarNode, textBox);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("friend-row-card");
        return row;
    }

    private void switchToAccueil() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/accueil.fxml"));
            Scene scene = new Scene(loader.load());
            if (getClass().getResource("/css/app.css") != null) {
                scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
            }
            Stage stage = (Stage) content.getScene().getWindow();
            stage.setScene(scene);
            ThemeManager.applyToScene(scene);
            ThemeManager.applyToStage(stage);
            stage.setTitle("FurHope - Animal Shelter Platform");
        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Unable to open home screen.").showAndWait();
        }
    }

    private void wireSearch() {
        if (searchField == null) return;
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (activeFeedController != null) {
                activeFeedController.setSearchQuery(newVal);
            }
        });
    }

    private void configureIcons() {
        setTabIcon(homeBtn, "M12 3L3 10V21H9V15H15V21H21V10L12 3Z");
        setTabIcon(profileBtn, "M12 12A5 5 0 1 0 12 2A5 5 0 1 0 12 12ZM4 22C4 18 8 16 12 16C16 16 20 18 20 22Z");
        setTabIcon(shopBtn, "M5 9L6.5 4H17.5L19 9V20H5V9ZM8 9A2 2 0 1 0 12 9A2 2 0 1 0 16 9");
        setTabIcon(adoptBtn, "M12 21C12 21 4 16 4 10C4 7 6 5 9 5C10.8 5 12 6.2 12 6.2C12 6.2 13.2 5 15 5C18 5 20 7 20 10C20 16 12 21 12 21Z");
        setTabIcon(pharmacyBtn, "M10 3H14V8H19V12H14V17H10V12H5V8H10Z");
        setTabIcon(locationsBtn, "M12 22C12 22 5 15.5 5 10A7 7 0 1 1 19 10C19 15.5 12 22 12 22ZM12 13A3 3 0 1 0 12 7A3 3 0 1 0 12 13Z");
    }

    private void applyProfileTabAvatarIcon() {
        if (profileBtn == null) {
            return;
        }
        User currentUser = SessionContext.getCurrentUser();
        if (currentUser == null) {
            return;
        }
        Node avatarNode = AvatarViewFactory.createAvatar(
                currentUser.getProfileImagePath(),
                resolveDisplayName(currentUser),
                20,
                "tab-avatar"
        );
        profileBtn.setGraphic(avatarNode);
        profileBtn.setContentDisplay(ContentDisplay.TOP);
    }

    private void configureNotificationBell() {
        if (notifBtn == null) return;

        SVGPath bellIcon = new SVGPath();
        bellIcon.setContent("M12 22C13.1 22 14 21.1 14 20H10C10 21.1 10.9 22 12 22ZM18 16V11C18 7.9 16.4 5.3 13.5 4.4V4C13.5 3.2 12.8 2.5 12 2.5S10.5 3.2 10.5 4V4.4C7.6 5.3 6 7.9 6 11V16L4 18V19H20V18L18 16Z");
        bellIcon.setStyle("-fx-fill: #2d3748;");

        StackPane iconWrap = new StackPane(bellIcon);
        iconWrap.setMinSize(16, 16);
        iconWrap.setPrefSize(16, 16);
        iconWrap.setMaxSize(16, 16);

        notifBadgeLabel = new Label("0");
        notifBadgeLabel.setStyle(
                "-fx-background-color: #d62839;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 10px;" +
                        "-fx-font-weight: 700;" +
                        "-fx-background-radius: 999;" +
                        "-fx-padding: 1 5 1 5;"
        );
        StackPane.setAlignment(notifBadgeLabel, Pos.TOP_RIGHT);
        notifBadgeLabel.setTranslateX(8);
        notifBadgeLabel.setTranslateY(-7);

        StackPane bellGraphic = new StackPane(iconWrap, notifBadgeLabel);
        bellGraphic.setMinSize(20, 20);
        bellGraphic.setPrefSize(20, 20);

        notifBtn.setGraphic(bellGraphic);
        notifBtn.setText("");
    }

    private void refreshNotificationBadge() {
        if (notifBadgeLabel == null) return;
        runAsync(
                () -> notifSvc.countUnread(AppSession.getCurrentUserId()),
                unread -> {
                    notifBadgeLabel.setText(String.valueOf(unread));
                    boolean show = unread > 0;
                    notifBadgeLabel.setVisible(show);
                    notifBadgeLabel.setManaged(show);
                },
                e -> {
                    e.printStackTrace();
                    notifBadgeLabel.setVisible(false);
                    notifBadgeLabel.setManaged(false);
                    new Alert(Alert.AlertType.ERROR, "Failed to load unread notifications: " + e.getMessage()).showAndWait();
                },
                "notif-unread-count"
        );
    }

    private void renderNotificationsList(VBox listBox, int currentUserId, Runnable onNavigateComplete) {
        listBox.getChildren().clear();
        listBox.getChildren().add(new Label("Loading..."));
        runAsync(
                () -> notifSvc.getLatest(currentUserId, 30),
                notifs -> {
                    listBox.getChildren().clear();
                    if (notifs.isEmpty()) {
                        listBox.getChildren().add(new Label("No notifications yet."));
                        return;
                    }

                    for (Notif n : notifs) {
                        Label line1 = new Label((n.getActorName() == null ? "Someone" : n.getActorName()) + " - " + n.getMessage());
                        line1.setWrapText(true);
                        line1.setStyle("-fx-font-size: 13px; -fx-text-fill: #22354d;");

                        Label line2 = new Label(TimeUtils.formatAgo(n.getCreatedAt()));
                        line2.setStyle("-fx-font-size: 11px; -fx-text-fill: #6f8096;");

                        Node avatarNode = AvatarViewFactory.createAvatar(
                                n.getActorProfileImagePath(),
                                n.getActorName(),
                                34,
                                "friend-avatar"
                        );
                        VBox textBox = new VBox(3, line1, line2);
                        HBox.setHgrow(textBox, Priority.ALWAYS);

                        HBox row = new HBox(10, avatarNode, textBox);
                        row.setAlignment(Pos.CENTER_LEFT);
                        row.setPadding(new Insets(8, 10, 8, 10));
                        if (!n.isRead()) {
                            row.setStyle("-fx-background-color: #eef6ff; -fx-background-radius: 10;");
                        } else {
                            row.setStyle("-fx-background-color: #f7f9fc; -fx-background-radius: 10;");
                        }
                        row.setOnMouseClicked(event -> navigateToNotificationTarget(n, currentUserId, onNavigateComplete));
                        listBox.getChildren().add(row);
                    }
                },
                e -> {
                    e.printStackTrace();
                    new Alert(Alert.AlertType.ERROR, "Failed to load notifications: " + e.getMessage()).showAndWait();
                    listBox.getChildren().setAll(new Label("Failed to load notifications."));
                },
                "notif-list-load"
        );
    }

    private void navigateToNotificationTarget(Notif notif, int currentUserId, Runnable onNavigateComplete) {
        runAsync(
                () -> {
                    notifSvc.markAsRead(notif.getId(), currentUserId);
                    return true;
                },
                ok -> {
                    refreshNotificationBadge();
                    if (notif.getPostId() != null) {
                        if (searchField != null) {
                            searchField.clear();
                        }
                        setActiveNav(homeBtn);
                        if (activeFeedController == null) {
                            loadFeed();
                        }
                        if (activeFeedController != null) {
                            activeFeedController.focusPost(notif.getPostId());
                        }
                    }
                    if (onNavigateComplete != null) {
                        onNavigateComplete.run();
                    }
                },
                e -> {
                    e.printStackTrace();
                    if (notif.getPostId() != null) {
                        if (searchField != null) {
                            searchField.clear();
                        }
                        setActiveNav(homeBtn);
                        if (activeFeedController == null) {
                            loadFeed();
                        }
                        if (activeFeedController != null) {
                            activeFeedController.focusPost(notif.getPostId());
                        }
                    }
                    if (onNavigateComplete != null) {
                        onNavigateComplete.run();
                    }
                },
                "notif-open-target"
        );
    }

    private void setTabIcon(Button button, String svgPathData) {
        SVGPath icon = new SVGPath();
        icon.setContent(svgPathData);
        icon.getStyleClass().add("tab-icon-shape");

        StackPane iconWrap = new StackPane(icon);
        iconWrap.getStyleClass().add("tab-icon-wrap");
        iconWrap.setMinSize(18, 18);
        iconWrap.setPrefSize(18, 18);
        iconWrap.setMaxSize(18, 18);

        button.setGraphic(iconWrap);
        button.setContentDisplay(javafx.scene.control.ContentDisplay.TOP);
    }

    private void setActiveNav(Button active) {
        List<Button> all = Arrays.asList(homeBtn, profileBtn, shopBtn, adoptBtn, pharmacyBtn, locationsBtn);
        for (Button b : all) {
            b.getStyleClass().remove("active");
        }
        active.getStyleClass().add("active");
    }

    private void loadFeed() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/feed.fxml"));
            Node feedNode = loader.load();
            activeFeedController = loader.getController();
            if (activeFeedController != null) {
                activeFeedController.setSearchQuery(searchField != null ? searchField.getText() : "");
            }
            content.getChildren().clear();
            content.getChildren().add(feedNode);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadPlaceholder(String title, String subtitle) {
        VBox page = new VBox(10);
        page.getStyleClass().add("placeholder-page");
        page.setAlignment(Pos.CENTER);
        page.setPadding(new Insets(20));

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("placeholder-title");

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("placeholder-subtitle");
        subtitleLabel.setWrapText(true);

        page.getChildren().addAll(titleLabel, subtitleLabel);
        content.getChildren().clear();
        content.getChildren().add(page);
    }

    private boolean ensureLoggedIn() {
        if (!SessionContext.isLoggedIn()) {
            new Alert(Alert.AlertType.INFORMATION, "Please sign in first.").showAndWait();
            return false;
        }
        return true;
    }

    private void switchScene(String fxmlFile) {
        try {
            FXMLLoader loader;
            if (fxmlFile != null && fxmlFile.startsWith("/animal/")) {
                loader = new FXMLLoader(getClass().getResource(fxmlFile), LanguageManager.getBundle());
            } else {
                loader = new FXMLLoader(getClass().getResource(fxmlFile));
            }

            Scene scene = new Scene(loader.load());
            if ("/fxml/app.fxml".equals(fxmlFile) || "/fxml/feed.fxml".equals(fxmlFile)) {
                if (getClass().getResource("/css/app.css") != null) {
                    scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
                }
            }

              Stage stage = (Stage) content.getScene().getWindow();
              stage.setScene(scene);
              ThemeManager.applyToScene(scene);
              ThemeManager.applyToStage(stage);
              stage.show();
          } catch (Exception e) {
              e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Unable to open requested page.").showAndWait();
        }
    }

    private String resolveDisplayName(User user) {
        if (user == null) {
            return "Friend";
        }
        String firstName = user.getFirstName() == null ? "" : user.getFirstName().trim();
        if (!firstName.isEmpty()) {
            return firstName;
        }
        String email = user.getEmail() == null ? "" : user.getEmail().trim();
        if (!email.isEmpty()) {
            int atIndex = email.indexOf('@');
            return atIndex > 0 ? email.substring(0, atIndex) : email;
        }
        return "Friend";
    }

    private boolean isAdminOrManager(User user) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        String normalizedRole = user.getRole().trim();
        return "ADMIN".equalsIgnoreCase(normalizedRole)
                || "HOTEL_MANAGER".equalsIgnoreCase(normalizedRole)
                || "MANAGER".equalsIgnoreCase(normalizedRole);
    }

    private boolean isVeterinarian(User user) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        String normalizedRole = user.getRole()
                .trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
        return "VETERINAIRE".equals(normalizedRole)
                || "VETERINARIAN".equals(normalizedRole)
                || "VET".equals(normalizedRole);
    }

    private void syncLegacyDashboardSession(User user) {
        SessionManager.setUserId(user.getId());
        SessionManager.setUserNom(resolveDisplayName(user));
        SessionManager.setUserRole(user.getRole());
    }

    private void bootstrapAnimalSession(User user) {
        int fallbackUserId = Math.max(user.getId(), 1);
        String displayName = resolveDisplayName(user);

        com.esprit.animal.utils.Session.setUserId(fallbackUserId);
        com.esprit.animal.utils.Session.setUserName(displayName);
        com.esprit.animal.utils.Session.setUserEmail(user.getEmail());
        com.esprit.animal.utils.Session.setUserRole(user.getRole());
        com.esprit.animal.utils.Session.setUserPhone(parsePhoneAsInt(user.getPhone()));

        int compteId = resolveAnimalCompteId(user, fallbackUserId);
        com.esprit.animal.utils.Session.setCompteId(compteId > 0 ? compteId : fallbackUserId);
    }

    private int resolveAnimalCompteId(User user, int fallbackUserId) {
        try {
            Connection connection = com.esprit.animal.utils.MyDataBase.getInstance().getConnection();
            if (connection == null || connection.isClosed()) {
                return fallbackUserId;
            }

            Integer compteId = null;
            if (user != null && user.getEmail() != null && !user.getEmail().isBlank()) {
                for (String emailQuery : new String[] {
                        "SELECT c.id_compte FROM compte c JOIN user u ON c.user_id = u.id_user WHERE u.email = ? LIMIT 1",
                        "SELECT c.id_compte FROM compte c JOIN user u ON c.user_id = u.id WHERE u.email = ? LIMIT 1"
                }) {
                    compteId = querySingleInt(connection, emailQuery, user.getEmail());
                    if (compteId != null && compteId > 0) {
                        return compteId;
                    }
                }
            }

            compteId = querySingleInt(connection, "SELECT id_compte FROM compte WHERE user_id = ? LIMIT 1", fallbackUserId);
            if (compteId != null && compteId > 0) {
                return compteId;
            }
        } catch (SQLException ignored) {
            return fallbackUserId;
        }

        return fallbackUserId;
    }

    private Integer querySingleInt(Connection connection, String sql, Object parameter) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (parameter instanceof Integer) {
                statement.setInt(1, (Integer) parameter);
            } else {
                statement.setString(1, parameter == null ? null : parameter.toString());
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    int value = resultSet.getInt(1);
                    return resultSet.wasNull() ? null : value;
                }
            }
        } catch (SQLException ignored) {
            return null;
        }
        return null;
    }

    private int parsePhoneAsInt(String phoneRaw) {
        if (phoneRaw == null || phoneRaw.isBlank()) {
            return 0;
        }
        String digits = phoneRaw.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return 0;
        }
        if (digits.length() > 9) {
            digits = digits.substring(digits.length() - 9);
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private void styleDialog(Dialog<?> dialog) {
        DialogPane pane = dialog.getDialogPane();
        pane.getStyleClass().add("app-dialog-pane");
        pane.setPrefWidth(700);
        pane.setPrefHeight(560);
        pane.setMinWidth(700);
        if (getClass().getResource("/css/app.css") != null) {
            pane.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
        }
    }

    private <T> void runAsync(Callable<T> action, Consumer<T> onSuccess, Consumer<Throwable> onError, String threadName) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return action.call();
            }
        };
        task.setOnSucceeded(event -> onSuccess.accept(task.getValue()));
        task.setOnFailed(event -> onError.accept(task.getException()));
        Thread thread = new Thread(task, threadName == null ? "nav-jdbc-task" : threadName);
        thread.setDaemon(true);
        thread.start();
    }
}
