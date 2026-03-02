package com.esprit.furhope.controllers;

import controllers.SessionContext;
import com.esprit.furhope.services.api.ApiFriendService;
import com.esprit.furhope.services.api.ApiFriendService.UserMini;
import com.esprit.furhope.services.api.ApiNotificationService;
import com.esprit.furhope.services.api.ApiNotificationService.Notif;
import com.esprit.furhope.utils.AppSession;
import com.esprit.furhope.utils.TimeUtils;
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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

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

    private final ApiFriendService friendService = new ApiFriendService();
    private final ApiNotificationService notifSvc = new ApiNotificationService();
    private Label notifBadgeLabel;
    private feedcontroller activeFeedController;

    @FXML
    public void initialize() {
        configureIcons();
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
        activeFeedController = null;
        loadPlaceholder("My Profile", "Your posts, media, and account info.");
    }

    @FXML
    private void onShopClick() {
        setActiveNav(shopBtn);
        activeFeedController = null;
        loadPlaceholder("Pet Shop", "Food, toys, accessories, and daily offers.");
    }

    @FXML
    private void onAdoptClick() {
        setActiveNav(adoptBtn);
        activeFeedController = null;
        loadPlaceholder("Adopt", "Browse pets or add an animal for adoption.");
    }

    @FXML
    private void onPharmacyClick() {
        setActiveNav(pharmacyBtn);
        activeFeedController = null;
        loadPlaceholder("Pharmacy", "Pet medication, supplements, and care products.");
    }

    @FXML
    private void onLocationsClick() {
        setActiveNav(locationsBtn);
        activeFeedController = null;
        loadPlaceholder("Locations", "Find clinics, shelters, and pet-friendly places.");
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

        ListView<UserMini> resultsList = new ListView<>();
        resultsList.setPrefHeight(240);
        resultsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(UserMini item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + " (#" + item.getId() + ")");
                }
            }
        });

        Button sendBtn = new Button("Send request");
        sendBtn.setDisable(true);

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
                        renderNotificationsList(listBox, currentUserId);
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

        renderNotificationsList(listBox, currentUserId);
        dialog.showAndWait();
    }

    @FXML
    private void onFriendsClick() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Friends");
        dialog.setHeaderText("Your friends");
        styleDialog(dialog);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox listBox = new VBox(8);
        listBox.setPadding(new Insets(6));

        listBox.getChildren().add(new Label("Loading..."));
        runAsync(
                () -> friendService.getFriends(AppSession.getCurrentUserId()),
                friends -> {
                    listBox.getChildren().clear();
                    if (friends.isEmpty()) {
                        listBox.getChildren().add(new Label("No friends yet."));
                        return;
                    }
                    for (UserMini user : friends) {
                        Label row = new Label(user.getName() + " (#" + user.getId() + ")");
                        listBox.getChildren().add(row);
                    }
                },
                e -> {
                    e.printStackTrace();
                    listBox.getChildren().setAll(new Label("Failed to load friends."));
                },
                "friend-list-load"
        );

        ScrollPane scroll = new ScrollPane(listBox);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(260);
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

        VBox listBox = new VBox(8);
        listBox.setPadding(new Insets(6));
        ScrollPane scroll = new ScrollPane(listBox);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(300);
        dialog.getDialogPane().setContent(scroll);

        Runnable[] refreshHolder = new Runnable[1];
        refreshHolder[0] = () -> {
            listBox.getChildren().clear();
            listBox.getChildren().add(new Label("Loading..."));
            int currentUserId = AppSession.getCurrentUserId();
            runAsync(
                    () -> friendService.getIncomingRequests(currentUserId),
                    incoming -> {
                        listBox.getChildren().clear();
                        if (incoming.isEmpty()) {
                            listBox.getChildren().add(new Label("No incoming requests."));
                            return;
                        }

                        for (UserMini sender : incoming) {
                            Label name = new Label(sender.getName() + " (#" + sender.getId() + ")");
                            HBox.setHgrow(name, Priority.ALWAYS);

                            Button acceptBtn = new Button("Accept");
                            Button declineBtn = new Button("Decline");

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

                            HBox row = new HBox(8, name, acceptBtn, declineBtn);
                            row.setAlignment(Pos.CENTER_LEFT);
                            listBox.getChildren().add(row);
                        }
                    },
                    ex -> {
                        ex.printStackTrace();
                        listBox.getChildren().setAll(new Label("Failed to load requests."));
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

    private void switchToAccueil() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/accueil.fxml"));
            Scene scene = new Scene(loader.load());
            if (getClass().getResource("/css/app.css") != null) {
                scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
            }
            Stage stage = (Stage) content.getScene().getWindow();
            stage.setScene(scene);
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

    private void renderNotificationsList(VBox listBox, int currentUserId) {
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

                        VBox row = new VBox(3, line1, line2);
                        row.setPadding(new Insets(8, 10, 8, 10));
                        if (!n.isRead()) {
                            row.setStyle("-fx-background-color: #eef6ff; -fx-background-radius: 10;");
                        } else {
                            row.setStyle("-fx-background-color: #f7f9fc; -fx-background-radius: 10;");
                        }
                        row.setOnMouseClicked(event -> {
                            if (activeFeedController != null && n.getPostId() != null) {
                                activeFeedController.focusPost(n.getPostId());
                            }
                        });
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

    private void styleDialog(Dialog<?> dialog) {
        DialogPane pane = dialog.getDialogPane();
        pane.getStyleClass().add("app-dialog-pane");
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
        Thread thread = new Thread(task, threadName == null ? "nav-api-task" : threadName);
        thread.setDaemon(true);
        thread.start();
    }
}
