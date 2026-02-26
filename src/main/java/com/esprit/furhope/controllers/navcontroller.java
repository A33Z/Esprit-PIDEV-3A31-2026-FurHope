package com.esprit.furhope.controllers;

import com.esprit.furhope.utils.AppSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

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

    private feedcontroller activeFeedController;

    @FXML
    public void initialize() {
        configureIcons();
        wireSearch();
        if (sessionLabel != null) {
            sessionLabel.setText("Logged in as " + AppSession.getCurrentDisplayName());
        }
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
}
