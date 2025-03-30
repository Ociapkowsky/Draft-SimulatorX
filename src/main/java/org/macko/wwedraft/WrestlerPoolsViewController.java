package org.macko.wwedraft;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import org.macko.wwedraft.Main.Wrestler;
public class WrestlerPoolsViewController {

    // Referencja do głównej aplikacji (aby wywołać np. createWrestlerListView)
    private Main mainApp;

    // Pola połączone z FXML
    @FXML
    private ListView<Wrestler> mainEventMidCardListView;
    @FXML
    private ListView<Wrestler> lowCardJobbersListView;
    @FXML
    private ListView<Wrestler> tagTeamsListView;

    // Metoda inicjalizująca, wywoływana przez Main po załadowaniu FXML
    public void setMainApp(Main mainApp) {
        this.mainApp = mainApp;
    }

    /**
     * Wypełnia listy danymi zawodników otrzymanymi z klasy Main.
     * Ta metoda musi być wywołana z Main.showWrestlerList() po załadowaniu FXML.
     */
    public void populateLists(ObservableList<Wrestler> meMcList,
                              ObservableList<Wrestler> lcJoList,
                              ObservableList<Wrestler> ttList)
    {
        // Ustawienie danych dla każdej listy
        mainEventMidCardListView.setItems(meMcList);
        lowCardJobbersListView.setItems(lcJoList);
        tagTeamsListView.setItems(ttList);

        // Ustawienie wyglądu komórek (CellFactory) - wywołujemy metodę z Main
        // Zakładamy, że createWrestlerListView i loadImageForWrestler są dostępne w mainApp
        // lub tworzymy CellFactory bezpośrednio tutaj.
        // Dla uproszczenia na razie załóżmy, że mainApp.createWrestlerListView() konfiguruje CellFactory
        // LUB możemy skopiować logikę setCellFactory tutaj:

        mainEventMidCardListView.setCellFactory(param -> mainApp.createWrestlerListCellWithImage()); // Użyj helpera z Main
        lowCardJobbersListView.setCellFactory(param -> mainApp.createWrestlerListCellWithImage());
        tagTeamsListView.setCellFactory(param -> mainApp.createWrestlerListCellWithImage());

        // Alternatywnie, jeśli createWrestlerListCellWithImage byłoby w tej klasie:
        // mainEventMidCardListView.setCellFactory(param -> createWrestlerListCellWithImage());
        // lowCardJobbersListView.setCellFactory(param -> createWrestlerListCellWithImage());
        // tagTeamsListView.setCellFactory(param -> createWrestlerListCellWithImage());

        System.out.println("WrestlerPoolsViewController: Listy wypełnione danymi.");
    }

    // Można tu przenieść lub skopiować metodę createWrestlerListCellWithImage z Main.java
    // jeśli nie chcemy wywoływać jej z mainApp. Poniżej zakomentowane:
     /*
     private ListCell<Wrestler> createWrestlerListCellWithImage() {
         return new ListCell<>() {
             private final ImageView imageView = new ImageView();
             private final HBox content = new HBox(10);
             private final Label label = new Label();
             { ... } // Blok inicjalizacyjny
             @Override
             protected void updateItem(Wrestler wrestler, boolean empty) {
                 // ... Logika updateItem ...
                 // WAŻNE: Potrzebowałaby dostępu do metody loadImageForWrestler z Main
                 //          lub loadImageForWrestler musiałaby być statyczna/w klasie Utils.
             }
         };
     }
     */

    // Metoda Initialize (opcjonalnie, jeśli potrzebujesz czegoś na starcie kontrolera)
    @FXML
    private void initialize() {
        System.out.println("WrestlerPoolsViewController zainicjalizowany.");
        // Tutaj można ustawić placeholdery dla list, jeśli są puste itp.
        // mainEventMidCardListView.setPlaceholder(new Label("Brak zawodników w tej kategorii"));
        // lowCardJobbersListView.setPlaceholder(new Label("Brak zawodników w tej kategorii"));
        // tagTeamsListView.setPlaceholder(new Label("Brak zawodników w tej kategorii"));
    }
}