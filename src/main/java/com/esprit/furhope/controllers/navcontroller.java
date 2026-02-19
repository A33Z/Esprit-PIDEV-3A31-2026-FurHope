
package com.esprit.furhope.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.Node;

import java.io.IOException;

public class navcontroller {

    @FXML
    private StackPane content;
    @FXML
    private Button feedbtn;
    @FXML
    private Button commentsbtn;
    @FXML
    private Button settingsbtn;

    @FXML
    public void initialize() {
        setActiveNav(feedbtn);
        loadFeed();
    }

    @FXML
    private void onFeedClick() {
        setActiveNav(feedbtn);
        loadFeed();
    }

    @FXML
    private void onCommentsClick() {
        setActiveNav(commentsbtn);
        loadComments();
    }

    @FXML
    private void onSettingsClick() {
        setActiveNav(settingsbtn);
        // TODO: load settings view
    }

    private void setActiveNav(Button active) {
        feedbtn.getStyleClass().remove("active");
        commentsbtn.getStyleClass().remove("active");
        if (settingsbtn != null) settingsbtn.getStyleClass().remove("active");
        active.getStyleClass().add("active");
    }

    private void loadFeed() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/feed.fxml"));
            Node feedNode = loader.load();
            content.getChildren().clear();
            content.getChildren().add(feedNode);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadComments() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/comments.fxml"));
            Node commentsNode = loader.load();
            content.getChildren().clear();
            content.getChildren().add(commentsNode);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
