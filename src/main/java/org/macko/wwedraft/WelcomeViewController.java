package org.macko.wwedraft;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label; // Dodaj, jeśli używasz titleLabel
import javafx.stage.Stage;
import java.util.ResourceBundle; // <-- DODAJ IMPORT

public class WelcomeViewController {

    private Main mainApp;
    private Stage primaryStage;
    private ResourceBundle resources; // <-- DODAJ POLE NA ZASOBY

    @FXML private Label titleLabel; // Jeśli chcesz nim manipulować
    @FXML private Button nextButton;
    @FXML private Button creatorButton;

    // Zaktualizuj setMainApp, aby przyjmował ResourceBundle
    public void setMainApp(Main mainApp, Stage primaryStage, ResourceBundle resources) {
        this.mainApp = mainApp;
        this.primaryStage = primaryStage;
        this.resources = resources; // <-- ZAPISZ ZASOBY
    }

    @FXML
    void handleNextAction(ActionEvent event) { /* ... bez zmian ... */
        if (mainApp != null) mainApp.showMainMenu(); else logError("handleNextAction");
    }

    @FXML
    void handleCreatorAction(ActionEvent event) {
        // Sprawdź, czy zasoby zostały wstrzyknięte
        System.out.println("--- Przycisk 'O Autorze' KLIKNIĘTY ---");
        if (resources == null) {

            logError("handleCreatorAction - resources is null");
            showAlert(Alert.AlertType.ERROR, "Błąd", "Nie można załadować tłumaczeń.");

            return;
        }
// Użyj kluczy do pobrania tekstów
        showAlert(Alert.AlertType.INFORMATION,
                resources.getString("creator.alert.title"),
                resources.getString("creator.alert.header") + "\n\n" + resources.getString("creator.alert.content"));
    }

    private void logError(String content) {
        System.err.println(content);
    }

    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}