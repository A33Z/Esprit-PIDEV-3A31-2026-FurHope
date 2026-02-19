package com.esprit.controllers;

import com.esprit.Services.animalServices;
import com.esprit.entities.animal;
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

        try {
            Image img = new Image(new File(animal.getImage()).toURI().toString());
            imageView.setImage(img);
        } catch (Exception e) {
            System.out.println("Image non trouvée");
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
            //imagePath = file.getAbsolutePath();
        }
    }


    public void setListView(ListView<animal> listView) {
        this.listView = listView;
    }

    @FXML
    void handleSave(ActionEvent event) {
        try {
            // mettre à jour l'objet animal
            animal.setName(nameField.getText());
            animal.setSpecies(speciesField.getText());
            animal.setBreed(breedField.getText());
            animal.setAge(Integer.parseInt(ageField.getText()));
            animal.setGender(com.esprit.entities.animal.gender.valueOf(genderField.getText()));
            animal.setDescription(descriptionField.getText());
            animal.setStatus(com.esprit.entities.animal.status.valueOf(statusField.getText()));

            // mettre à jour l'image si une nouvelle a été sélectionnée
            if (image != null) {
                animal.setImage(image);
            }

            // sauvegarder dans la BD
            animalServices service = new animalServices();
            service.modifier(animal);

            // mettre à jour la ListView
            if (listView != null) {
                // Si l'objet est le même dans la liste, juste refresh
                listView.refresh();

            }

            // fermer la fenêtre
            Stage stage = (Stage) saveButton.getScene().getWindow();
            stage.close();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AfficherAnimal.fxml"));
            Parent root = loader.load();

            Stage listStage = new Stage();
            listStage.setScene(new Scene(root));
            listStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    }


