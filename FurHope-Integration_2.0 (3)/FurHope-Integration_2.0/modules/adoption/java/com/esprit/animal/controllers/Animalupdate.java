package com.esprit.animal.controllers;

import com.esprit.animal.utils.StageSceneHelper;
import com.esprit.animal.Services.animalServices;
import com.esprit.animal.entities.animal;
import com.esprit.animal.i18n.LanguageManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class Animalupdate {

    @FXML
    private TextField ageField;

    @FXML
    private TextField breedField;

    @FXML
    private TextArea descriptionField;

    @FXML
    private TextField genderField;

    @FXML
    private ImageView imageView;

    @FXML
    private TextField nameField;

    @FXML
    private Button saveButton;

    @FXML
    private TextField speciesField;

    @FXML
    private TextField statusField;

    private animal animal;
    private ListView<animal> listView;
    private String image;

    public void setAnimal(animal animal) {
        this.animal = animal;

        nameField.setText(animal.getName());
        speciesField.setText(animal.getSpecies());
        breedField.setText(animal.getBreed());
        ageField.setText(String.valueOf(animal.getAge()));
        genderField.setText(animal.getGender().toString());
        descriptionField.setText(animal.getDescription());
        statusField.setText(animal.getStatus().toString());

        if (animal.getImage() != null) {
            File file = new File("images/" + animal.getImage());
            if (file.exists()) {
                imageView.setImage(new Image(file.toURI().toString()));
            }
        }
    }

    @FXML
    void handleChangeImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Image");

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );

        File file = fileChooser.showOpenDialog(null);

        if (file != null) {
            image = file.getAbsolutePath();

            Image im = new Image(file.toURI().toString());
            imageView.setImage(im);
        }
    }


    public void setListView(ListView<animal> listView) {
        this.listView = listView;
    }

    @FXML
    void handleSave(ActionEvent event) {
        try {
            animal.setName(nameField.getText());
            animal.setSpecies(speciesField.getText());
            animal.setBreed(breedField.getText());
            animal.setAge(Integer.parseInt(ageField.getText()));
            animal.setGender(com.esprit.animal.entities.animal.gender.valueOf(genderField.getText()));
            animal.setDescription(descriptionField.getText());
            animal.setStatus(com.esprit.animal.entities.animal.status.valueOf(statusField.getText()));

            if (image != null) {
                animal.setImage(image);
            }

            // sauvegarder dans la BD
            animalServices service = new animalServices();
            service.modifier(animal);

            if (listView != null) {
                listView.refresh();

            }

            // Close the current window
            Stage currentStage = (Stage) saveButton.getScene().getWindow();
            currentStage.close();

// Load the new FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/animal/AfficherAnimal.fxml"), LanguageManager.getBundle());
            Parent root = loader.load();

// Open the new window
            Stage newStage = new Stage();
            newStage.setTitle("Request Details");
            StageSceneHelper.setScene(newStage, root);
            newStage.setMaximized(true);
            newStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    }





