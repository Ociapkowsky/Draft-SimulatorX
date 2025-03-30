package org.macko.wwedraft; // Upewnij się, że pakiet jest poprawny

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import java.util.ResourceBundle;
import javafx.stage.Modality;
import javafx.scene.layout.VBox;
import javafx.scene.Scene;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

public class MainMenuViewController {

    // Referencje
    private Main mainApp;
    private Stage primaryStage;
    private ResourceBundle resources; // Pole na zasoby

    // Pola FXML
    @FXML private Button loadButton;
    @FXML private Button showPoolsButton;
    @FXML private Button startDraftButton;
    @FXML private Button showResultsButton;
    @FXML private Label titleLabel2; // Twoja etykieta
    @FXML private Button addButton;
    @FXML private Button exportButton;
    // Dodaj @FXML dla przycisków showBrandsButton i addButton, jeśli ich używasz w FXML

    // Metoda do ustawienia referencji
    public void setMainApp(Main mainApp, Stage primaryStage, ResourceBundle resources) {
        this.mainApp = mainApp;
        this.primaryStage = primaryStage;
        this.resources = resources;
    }

    // Metody obsługi zdarzeń
    @FXML void handleLoadAction(ActionEvent event) {
        if (mainApp != null) mainApp.loadWrestlersFromExcel(primaryStage); else logError("handleLoadAction");
    }
    @FXML void handleShowPoolsAction(ActionEvent event) {
        if (mainApp != null) mainApp.showWrestlerList(); else logError("handleShowPoolsAction");
    }
    @FXML void handleStartDraftAction(ActionEvent event) {
        if (mainApp != null) mainApp.startManualDraft(); else logError("handleStartDraftAction");
    }
    @FXML void handleShowResultsAction(ActionEvent event) {
        if (mainApp != null) mainApp.showDraftResultsView(); else logError("handleShowResultsAction");
    }
    // Dodaj metody handle dla addButton i showBrandsButton, jeśli ich używasz

    // Metody pomocnicze
    private void logError(String methodName) {
        String message = "MainMenuViewController: mainApp null w " + methodName;
        System.err.println(message);
        if (resources != null) {
            showAlert(Alert.AlertType.ERROR, resources.getString("error.critical.title"), resources.getString("error.critical.content.noMainApp"));
        } else {
            showAlert(Alert.AlertType.ERROR, "Błąd Krytyczny", "Brak połączenia z główną aplikacją!");
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(content);
        if (primaryStage != null && primaryStage.isShowing()) { alert.initOwner(primaryStage); }
        alert.showAndWait();
    }
    /**
     * Obsługa przycisku "Dodaj...".
     * Otwiera nowe, małe okno z opcjami dodawania.
     * Połączona z FXML przez: onAction="#handleAddAction"
     */
    @FXML
    void handleAddAction(ActionEvent event) {
        // Sprawdź, czy zasoby są dostępne
        if (resources == null) {
            logError("handleAddAction - resources is null");
            return;
        }

        // Utwórz nowe okno (Stage)
        Stage addStage = new Stage();
        addStage.initModality(Modality.WINDOW_MODAL);
        addStage.initOwner(primaryStage);
        // --- ZMIANA: Użyj klucza dla tytułu ---
        addStage.setTitle(resources.getString("addDialog.title"));

        // Przyciski w nowym oknie
        // --- ZMIANA: Użyj kluczy dla tekstów przycisków ---
        Button addWrestlerBtn = new Button(resources.getString("addDialog.button.addWrestler"));
        addWrestlerBtn.setOnAction(e -> {
            if (mainApp != null) mainApp.showAddWrestlerWindow(); else logError("handleAddAction -> Add Wrestler");
            addStage.close();
        });

        Button addBrandBtn = new Button(resources.getString("addDialog.button.addBrand"));
        addBrandBtn.setOnAction(e -> {
            if (mainApp != null) mainApp.showAddBrandWindow(); else logError("handleAddAction -> Add Brand");
            addStage.close();
        });
        // --- KONIEC ZMIAN W PRZYCISKACH ---


        // Layout dla nowego okna (bez zmian)
        VBox addLayout = new VBox(15, addWrestlerBtn, addBrandBtn);
        addLayout.setPadding(new Insets(20));
        addLayout.setAlignment(Pos.CENTER);

        // Scena dla nowego okna (bez zmian)
        Scene addScene = new Scene(addLayout, 250, 150);

        addStage.setScene(addScene);
        addStage.showAndWait(); // Pokaż okno i poczekaj
    }


    /**
     * Obsługa przycisku "Eksportuj Draft do Excela".
     */
    @FXML
    void handleExportAction(ActionEvent event) {
        if (mainApp != null) {
            mainApp.saveDraftToExcel(primaryStage); // Wywołaj metodę zapisu z Main
        } else {
            logError("handleExportAction");
        }
    }
} // Koniec klasy MainMenuViewController