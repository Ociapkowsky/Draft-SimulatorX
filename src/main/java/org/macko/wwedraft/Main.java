package org.macko.wwedraft; // Upewnij się, że to poprawna nazwa pakietu

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader; // <-- Dodany import
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.net.URL; // <-- Dodany import
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import javafx.scene.text.Font;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ScrollPane;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import java.text.MessageFormat;

public class Main extends Application {

    // --- Enumy i Klasy Wewnętrzne (bez zmian) ---

    public enum WrestlerCategory {
        MAIN_EVENT("Main Event"), MID_CARD("Mid Card"), LOW_CARD("Low Card"),
        JOBBER("Jobber"), TAG_TEAM("Tag Team");
        private final String displayName;
        WrestlerCategory(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
        public static WrestlerCategory fromString(String text) {
            if (text == null || text.trim().isEmpty()) return null;
            try {
                return WrestlerCategory.valueOf(text.trim().toUpperCase().replace(" ", "_"));
            } catch (IllegalArgumentException e) {
                System.err.println("Nieznana kategoria: '" + text + "'"); return null;
            }
        }
    }

    public static class Wrestler {
        private String name; private WrestlerCategory category; private boolean isTagTeam;
        private String tagTeamName; private String imageFileName;
        public Wrestler(String name, WrestlerCategory category, String imageFileName) {
            this.name = name; this.category = category; this.isTagTeam = false; this.tagTeamName = null;
            this.imageFileName = (imageFileName != null && !imageFileName.trim().isEmpty()) ? imageFileName.trim() : null;
        }
        public Wrestler(String name, WrestlerCategory ignoredCategory, String tagTeamName, String imageFileName) {
            this.name = name; this.category = WrestlerCategory.TAG_TEAM; this.isTagTeam = true;
            this.tagTeamName = (tagTeamName != null && !tagTeamName.trim().isEmpty()) ? tagTeamName.trim() : null;
            if (this.tagTeamName == null) { this.isTagTeam = false; this.category = WrestlerCategory.MID_CARD; System.err.println("Błąd: Konstruktor Tag Team..."); }
            this.imageFileName = (imageFileName != null && !imageFileName.trim().isEmpty()) ? imageFileName.trim() : null;
        }
        public String getName() { return name; } public WrestlerCategory getCategory() { return category; }
        public boolean isTagTeam() { return isTagTeam; } public String getTagTeamName() { return tagTeamName; }
        public String getImageFileName() { return imageFileName; }
        @Override public String toString() { /* ... (bez zmian) ... */
            String catDisplay = (category != null) ? category.getDisplayName() : "Brak";
            if (isTagTeam && tagTeamName != null) { return name + " [" + tagTeamName + "]"; }
            else { return name + " (" + catDisplay + ")"; }
        }
        @Override public boolean equals(Object o) { /* ... (bez zmian - porównanie po name) ... */
            if (this == o) return true; if (o == null || getClass() != o.getClass()) return false;
            Wrestler wrestler = (Wrestler) o; return Objects.equals(name, wrestler.name);
        }
        @Override public int hashCode() { return Objects.hash(name); }
    }

    public static class Brand {
        private String name;
        private String logoFileName; // <-- DODANE POLE

        // Zaktualizowany konstruktor
        public Brand(String name, String logoFileName) {
            this.name = name;
            // Upewnij się, że zapisujemy null, jeśli nazwa pliku jest pusta/null
            this.logoFileName = (logoFileName != null && !logoFileName.trim().isEmpty()) ? logoFileName.trim() : null;
        }

        // Dodajemy konstruktor bez logo dla kompatybilności wstecznej lub ręcznego dodawania
        public Brand(String name) {
            this(name, null); // Wywołaj główny konstruktor z logo = null
        }

        // Gettery
        public String getName() { return name; }
        public String getLogoFileName() { return logoFileName; } // <-- DODANY GETTER

        @Override public String toString() { return name; }
        @Override public boolean equals(Object o) { if (this == o) return true; if (o == null || getClass() != o.getClass()) return false; Brand brand = (Brand) o; return Objects.equals(name, brand.name); }
        @Override public int hashCode() { return Objects.hash(name); }
    }

    // --- Pola Klasy Main ---
    private Stage primaryStage;
    private VBox rootLayout;
    private BorderPane mainContentArea; // Główny obszar na podmieniane widoki
    private ObservableList<Wrestler> allWrestlers = FXCollections.observableArrayList();
    // W sekcji Pól Klasy Main
    // W sekcji Pól Klasy Main
    private ObservableList<Brand> allBrands = FXCollections.observableArrayList(
            new Brand("RAW", "raw_logo.png"), // <-- Dodano nazwę pliku logo
            new Brand("SmackDown", "sd_logo.png"), // <-- Dodano nazwę pliku logo
            new Brand("NXT", "nxt_logo.png"), // <-- Dodano nazwę pliku logo
            new Brand("ECW", "ecw_logo.png"), // <-- Dodano nazwę pliku logo
            new Brand("WCW", "wcw_logo.png"), // <-- Dodano nazwę pliku logo
            new Brand("NJPW", "njpw_logo.png"), // <-- Dodano nazwę pliku logo
            new Brand("AEW", "aew_logo.png"), // <-- Dodano nazwę pliku logo
            new Brand("TNA", "tna_logo.png"), // <-- Dodano nazwę pliku logo
            new Brand("ROH", "roh_logo.png") // <-- Dodano nazwę pliku logo
            // Pamiętaj, aby pliki o tych nazwach istniały w src/main/resources/images/
    );
    private ObservableList<Wrestler> mainEventMidCard = FXCollections.observableArrayList();
    private ObservableList<Wrestler> lowCardJobbers = FXCollections.observableArrayList();
    private ObservableList<Wrestler> tagTeams = FXCollections.observableArrayList();
    private Map<Brand, ObservableList<Wrestler>> brandRosters = new HashMap<>();
    private GridPane draftGrid; // Do wyświetlania wyników
    private final Random random = new Random();

    // Pola do zarządzania stanem draftu (DODAJ TE LINIE)
    private boolean autoFinishing = false;         // Flaga auto-dokańczania
    private Set<Brand> finishedBrands = new HashSet<>(); // Zbiór brandów, które skończyły
    private Brand currentPickingBrand = null;      // Aktualnie wybierający brand
    private int currentDraftRound = 0;           // Bieżąca runda draftu
    private int currentBrandPickIndex = 0;       // Bieżący indeks brandu w rundzie
    private List<Brand> currentDraftOrder;       // Ustalona kolejność dla całego draftu

    // Widoki list dla showWrestlerList()
    private ListView<Wrestler> mainEventMidCardView;
    private ListView<Wrestler> lowCardJobbersView;
    private ListView<Wrestler> tagTeamsView;
    private ListView<Brand> brandListView = new ListView<>(allBrands); // Do okna listy brandów

    // Referencje do kontrolek nagłówka (jeśli używasz - upewnij się, że są)
    private Label pickingInfoLabelHeader;
    private Button finishForBrandButtonHeader;
    private Button autoFinishButtonHeader;

    // --- Metoda start() ---
    @Override
    public void start(Stage primaryStage) {
        // --- TUTAJ WKLEJ KOD ŁADOWANIA CZCIONKI ---
        try {
            // Upewnij się, że nazwa pliku jest DOKŁADNA i plik jest w src/main/resources/fonts/
            Font.loadFont(getClass().getResourceAsStream("/fonts/RawhideRaw2016.otf"), 10); // Rozmiar 10 jest tylko placeholderem
            System.out.println("Załadowano czcionkę Rawhide Raw 2016");
        } catch (Exception e) {
            System.err.println("Nie udało się załadować czcionki Rawhide Raw 2016. Użyta zostanie czcionka domyślna. Błąd: " + e.getMessage());
        }
        // --- DODAJ ŁADOWANIE ZASOBÓW ---
        // Na razie ustawiamy polski jako domyślny
        Locale currentLocale = Locale.getDefault(); // Zaciąga język i zapisuje w zmiennej currentLocale
        String currentLanguage = currentLocale.getLanguage(); // Robi to samo tylko że Language to małe literki np, pl z pl_PL albo en z en_US
        String[] supportedLangs = {"pl", "en"}; // Wypisujemy wspierane języki (Language)


        // --- POCZĄTEK POPRAWIONEGO ŁADOWANIA ZASOBÓW (BEZ Utf8Control) ---
        Locale systemLocale = Locale.getDefault();
        System.out.println("System default locale: " + systemLocale);

        Locale localePL = new Locale("pl", "PL");
        Locale localeEN_US = new Locale("en", "US");

        if (systemLocale.getLanguage().equals(localePL.getLanguage())) {
            currentLocale = localePL;
        } else {
            currentLocale = localeEN_US; // Domyślnie EN_US
        }
        System.out.println("Locale wybrane dla aplikacji: " + currentLocale);

        String bundlePath = "org.macko.wwedraft.messages";
        try {
            // Wywołanie BEZ Utf8Control - Java 9+ domyślnie użyje UTF-8
            currentBundle = ResourceBundle.getBundle(bundlePath, currentLocale); // <-- USUNIĘTO new Utf8Control()
            System.out.println("Załadowano ResourceBundle dla: " + currentLocale);
        } catch (MissingResourceException e) {
            System.err.println("Nie znaleziono Bundle dla " + currentLocale + ". Próbuję ostatecznego fallbacku.");
            // Spróbuj załadować domyślny (np. messages.properties lub messages_pl_PL)
            try{
                currentBundle = ResourceBundle.getBundle(bundlePath); // <-- Bez locale i bez Control
                currentLocale = currentBundle.getLocale(); // Zobaczmy, co załadował
                System.out.println("Załadowano domyślny ResourceBundle dla: " + currentLocale);
                // Upewnij się, że fallback jest akceptowalny (np. angielski lub polski)
                if (!currentLocale.getLanguage().equals(localeEN_US.getLanguage()) && !currentLocale.getLanguage().equals(localePL.getLanguage())) {
                    // Jeśli domyślny to jeszcze coś innego, to jest problem
                    throw new MissingResourceException("Domyślny bundle nie jest PL ani EN", bundlePath, "");
                }
            } catch (MissingResourceException e2) {
                // Ostateczny fallback - pusty bundle
                System.err.println("Nie znaleziono żadnych plików ResourceBundle! Błąd: " + e2.getMessage());
                currentLocale = Locale.ROOT;
                currentBundle = new ListResourceBundle() { @Override protected Object[][] getContents() { return new Object[0][]; } };
                showAlert(Alert.AlertType.ERROR, "Błąd Krytyczny Zasobów", "Nie znaleziono plików tłumaczeń!");
            }
        }
        // --- KONIEC POPRAWIONEGO ŁADOWANIA ZASOBÓW ---

        // --- KONIEC KODU ŁADOWANIA CZCIONKI ---
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("Draft Simulator by Ociapkowsky");
        // --- POCZĄTEK: Kod tworzenia folderu na obrazki (z tłumaczeniami) ---
        try {
            String userDocuments = System.getProperty("user.home") + File.separator + "Documents";
            String imageFolderName = "WWE_Draft_Images";
            File imageDir = new File(userDocuments, imageFolderName);

            if (!imageDir.exists()) {
                System.out.println("Folder na obrazki nie istnieje, próba utworzenia: " + imageDir.getAbsolutePath());
                boolean created = imageDir.mkdirs();
                if (created) {
                    System.out.println("Utworzono folder na obrazki: " + imageDir.getAbsolutePath());
                    // --- ZMIANA TUTAJ ---
                    if (currentBundle != null) {
                        showAlert(Alert.AlertType.INFORMATION,
                                currentBundle.getString("alert.imgfolder.created.title"), // Tytuł bez zmian
                                // Nowa treść wyjaśniająca cel folderu
                                MessageFormat.format("Utworzono folder:\n{0}\n\nMożesz umieścić w nim własne obrazki (.png/.jpg), aby nadpisać domyślne lub dodać nowe. Nazwy plików muszą zgadzać się z tymi w Excelu.", imageDir.getAbsolutePath()));
                        // Możesz stworzyć nowy klucz w .properties dla tej treści
                    } else {
                        showAlert(Alert.AlertType.INFORMATION, "Folder Obrazków", "Utworzono folder na obrazki w Dokumentach. Możesz tam dodać własne pliki."); // Fallback
                    }
                } else {
                    System.err.println("BŁĄD: Nie można utworzyć folderu na obrazki: " + imageDir.getAbsolutePath());
                    // Alert błędu (z tłumaczeniem)
                    if (currentBundle != null) {
                        showAlert(Alert.AlertType.WARNING,
                                currentBundle.getString("alert.imgfolder.error.title"),
                                MessageFormat.format(currentBundle.getString("alert.imgfolder.error.content"), imageDir.getAbsolutePath()));
                    } else {
                        showAlert(Alert.AlertType.WARNING, "Błąd", "Nie udało się utworzyć folderu..."); // Fallback
                    }
                }
            } else {
                System.out.println("Folder na obrazki już istnieje: " + imageDir.getAbsolutePath());
            }
        } catch (SecurityException se) {
            System.err.println("Błąd uprawnień przy tworzeniu folderu: " + se.getMessage());
            // Alert błędu uprawnień (z tłumaczeniem)
            if (currentBundle != null) {
                showAlert(Alert.AlertType.ERROR,
                        currentBundle.getString("alert.imgfolder.permission.title"),
                        currentBundle.getString("alert.imgfolder.permission.content"));
            } else {
                showAlert(Alert.AlertType.ERROR, "Błąd Uprawnień", "Brak uprawnień..."); // Fallback
            }
        } catch (Exception e) {
            System.err.println("Nieoczekiwany błąd przy tworzeniu folderu: " + e.getMessage());
            e.printStackTrace();
            // Można dodać ogólny alert błędu
            showAlert(Alert.AlertType.ERROR, "Błąd", "Nieoczekiwany błąd przy tworzeniu folderu: " + e.getMessage());
        }
        // --- KONIEC: Kod tworzenia folderu na obrazki ---
        updateCategoryLists(); // Inicjalizacja list kategorii
        for (Brand brand : allBrands) {
            brandRosters.put(brand, FXCollections.observableArrayList()); // Inicjalizacja mapy rosterów
        }

        // Inicjalizacja widoków ListView
        mainEventMidCardView = createWrestlerListView(mainEventMidCard);
        lowCardJobbersView = createWrestlerListView(lowCardJobbers);
        tagTeamsView = createWrestlerListView(tagTeams);

        initRootLayout(); // Inicjalizacja głównego layoutu (VBox[MenuBar, BorderPane])
        loadWelcomeView(); // Załadowanie ekranu powitalnego FXML

        Scene scene = new Scene(rootLayout, 1000, 700); // Tworzenie sceny

        // Ładowanie CSS (bez zmian)
        try {
            URL cssUrl = getClass().getResource("/style.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
                System.out.println("CSS loaded from: " + cssUrl.toExternalForm());
            } else {
                System.err.println("Nie znaleziono pliku CSS: /style.css");
            }
        } catch (Exception e) {
            System.err.println("Błąd podczas ładowania CSS: " + e.getMessage());
            e.printStackTrace();
        }

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // --- Metody UI ---
    private void initRootLayout() {
        rootLayout = new VBox();
        // MenuBar menuBar = createMenuBar(); // <-- Już nie tworzymy menuBar
        mainContentArea = new BorderPane();
        mainContentArea.setId("mainContentArea");
        // --- ZMIANA: Dodajemy tylko mainContentArea ---
        rootLayout.getChildren().add(mainContentArea); // <-- Poprawiona linia
        VBox.setVgrow(mainContentArea, Priority.ALWAYS);
    }

    // Tworzy główny pasek menu (bez zmian)
    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("Plik");
        MenuItem loadExcelItem = new MenuItem("Wczytaj z Excela...");
        loadExcelItem.setOnAction(e -> loadWrestlersFromExcel(primaryStage));
        MenuItem saveDraftItem = new MenuItem("Zapisz Draft do Excela...");
        saveDraftItem.setOnAction(e -> saveDraftToExcel(primaryStage));
        MenuItem exitItem = new MenuItem("Wyjście");
        exitItem.setOnAction(e -> Platform.exit());
        fileMenu.getItems().addAll(loadExcelItem, saveDraftItem, new SeparatorMenuItem(), exitItem);

        Menu viewMenu = new Menu("Widok");
        MenuItem showWrestlersItem = new MenuItem("Pokaż Pule Zawodników");
        showWrestlersItem.setOnAction(e -> showWrestlerList()); // Poprawna nazwa
        MenuItem showDraftResultsItem = new MenuItem("Pokaż Wyniki Draftu");
        showDraftResultsItem.setOnAction(e -> showDraftResultsView());
        viewMenu.getItems().addAll(showWrestlersItem, showDraftResultsItem);

        Menu actionsMenu = new Menu("Akcje");
        MenuItem startManualDraftItem = new MenuItem("Rozpocznij Ręczny Draft");
        startManualDraftItem.setOnAction(e -> startManualDraft());
        MenuItem addBrandItem = new MenuItem("Dodaj Brand...");
        addBrandItem.setOnAction(e -> showAddBrandWindow());
        MenuItem addWrestlerItem = new MenuItem("Dodaj Zawodnika...");
        addWrestlerItem.setOnAction(e -> showAddWrestlerWindow());
        actionsMenu.getItems().addAll(startManualDraftItem, new SeparatorMenuItem(), addBrandItem, addWrestlerItem);

        menuBar.getMenus().addAll(fileMenu, viewMenu, actionsMenu);
        return menuBar;
    }

    // Ładuje widok powitalny z FXML
    // Zmodyfikuj loadWelcomeView, aby przekazywał ResourceBundle
    private void loadWelcomeView() {
        System.out.println("Próba załadowania welcome-view.fxml...");
        try {
            URL fxmlUrl = Main.class.getResource("/org/macko/wwedraft/welcome-view.fxml");
            if (fxmlUrl == null) throw new IOException("Nie znaleziono zasobu FXML: /org/macko/wwedraft/welcome-view.fxml");

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            loader.setResources(currentBundle); // <-- WAŻNE: Ustaw ResourceBundle DLA FXML
            Node welcomeNode = loader.load();

            WelcomeViewController controller = loader.getController();
            if (controller == null) throw new IllegalStateException("Kontroler dla welcome-view.fxml nie ustawiony w FXML!");
            // Przekaż też ResourceBundle do kontrolera
            controller.setMainApp(this, primaryStage, currentBundle); // <-- ZMIANA: Dodano currentBundle

            mainContentArea.setCenter(welcomeNode);
            mainContentArea.setTop(null); mainContentArea.setBottom(null); mainContentArea.setLeft(null); mainContentArea.setRight(null);
            System.out.println("Załadowano welcome-view.fxml pomyślnie.");

        } catch (IOException | IllegalStateException e) { /* ... obsługa błędów ... */ }
    }

    // Ładuje widok głównego menu z FXML (wywoływane z WelcomeViewController)
    public void showMainMenu() {
        System.out.println("Próba załadowania main-menu-view.fxml...");
        try {
            URL fxmlUrl = Main.class.getResource("/org/macko/wwedraft/main-menu-view.fxml");
            if (fxmlUrl == null) throw new IOException("Nie znaleziono zasobu FXML: ...");

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            // --- WAŻNE: Ustaw ResourceBundle PRZED load() ---
            loader.setResources(currentBundle);
            Node mainMenuNode = loader.load();

            MainMenuViewController controller = loader.getController();
            if (controller == null) throw new IllegalStateException("Kontroler dla main-menu-view.fxml nie ustawiony...");
            // --- ZMIANA: Przekaż ResourceBundle do setMainApp ---
            controller.setMainApp(this, primaryStage, currentBundle);

            mainContentArea.setCenter(mainMenuNode);
            mainContentArea.setTop(null); /* ... etc ... */
            System.out.println("Załadowano main-menu-view.fxml pomyślnie.");

        } catch (IOException | IllegalStateException e) { /* ... obsługa błędów ... */ }
    }

    /**
     * Tworzy nagłówek z przyciskiem powrotu i tytułem.
     * Opcjonalnie dodaje przyciski kontroli draftu.
     * @param title Tytuł do wyświetlenia.
     * @param showDraftControls Czy pokazać przyciski "Zakończ dla..." i "Dokończ Auto"?
     * @return Węzeł HBox reprezentujący nagłówek.
     */
    private Node createDraftHeader(String title, boolean showDraftControls) {
        // --- ZMIANA TUTAJ: Użyj klucza dla przycisku powrotu ---
        String backButtonText = (currentBundle != null) ? currentBundle.getString("button.backToMenu") : "<< Powrót do Menu";
        Button backButton = new Button(backButtonText);
        // --- KONIEC ZMIANY ---
        backButton.setOnAction(e -> showMainMenu());

        // Etykieta tytułowa (bez zmian)
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        titleLabel.setMaxWidth(Double.MAX_VALUE); titleLabel.setAlignment(Pos.CENTER); HBox.setHgrow(titleLabel, Priority.ALWAYS);

        // Kontener na przyciski draftu (bez zmian)
        HBox draftControlButtons = new HBox(10); draftControlButtons.setAlignment(Pos.CENTER_RIGHT);

        // Dodawanie przycisków draftu (bez zmian)
        if (showDraftControls) {
            finishForBrandButtonHeader = new Button("Zakończ dla Brandu"); // Tekst zmieniany w draftBrandChoice
            finishForBrandButtonHeader.setDisable(true); finishForBrandButtonHeader.setOnAction(e -> handleFinishForCurrentBrand());
            autoFinishButtonHeader = new Button("Dokończ Automatycznie"); // Tekst może być z resources
            autoFinishButtonHeader.setOnAction(e -> handleAutoFinish());
            draftControlButtons.getChildren().addAll(finishForBrandButtonHeader, autoFinishButtonHeader);
            pickingInfoLabelHeader = titleLabel; // Zapisz referencję do dynamicznej aktualizacji
        } else {
            finishForBrandButtonHeader = null; autoFinishButtonHeader = null; pickingInfoLabelHeader = null;
        }

        // Główny HBox nagłówka (bez zmian)
        HBox header = new HBox(10); header.setPadding(new Insets(10)); header.setAlignment(Pos.CENTER_LEFT);
        header.getChildren().addAll(backButton, titleLabel);
        if (showDraftControls) { header.getChildren().add(draftControlButtons); }
        header.setStyle("-fx-background-color: rgba(240, 240, 240, 0.8); -fx-border-color: lightgray; -fx-border-width: 0 0 1 0;");
        return header;
    }

    /**
     * Przeciążona wersja createDraftHeader dla wygody.
     * Domyślnie nie pokazuje przycisków kontroli draftu.
     * @param title Tytuł do wyświetlenia.
     * @return Węzeł HBox reprezentujący nagłówek.
     */
    private Node createDraftHeader(String title) {
        // Ta metoda pozostaje bez zmian, wywołuje wersję z 'false'
        return createDraftHeader(title, false);
    }
    public void showAddWrestlerWindow() {
        // Sprawdź, czy zasoby są dostępne
        if (currentBundle == null) {
            showAlert(Alert.AlertType.ERROR, "Błąd", "Brak załadowanych zasobów językowych!");
            return;
        }

        Stage addWrestlerStage = new Stage();
        addWrestlerStage.initModality(Modality.WINDOW_MODAL);
        addWrestlerStage.initOwner(primaryStage);
        addWrestlerStage.setTitle(currentBundle.getString("addWrestler.window.title")); // Tytuł z zasobów

        TextField wrestlerNameField = new TextField();
        wrestlerNameField.setPromptText(currentBundle.getString("addWrestler.prompt.name")); // Prompt z zasobów
        ComboBox<WrestlerCategory> wrestlerCategoryComboBox = new ComboBox<>();
        wrestlerCategoryComboBox.getItems().addAll(
                WrestlerCategory.MAIN_EVENT, WrestlerCategory.MID_CARD,
                WrestlerCategory.LOW_CARD, WrestlerCategory.JOBBER);
        wrestlerCategoryComboBox.setValue(WrestlerCategory.MID_CARD);

        CheckBox tagTeamCheckBox = new CheckBox(currentBundle.getString("addWrestler.checkbox.tagTeam")); // Tekst CheckBoxa z zasobów
        TextField wrestlerTagTeamField = new TextField();
        wrestlerTagTeamField.setPromptText(currentBundle.getString("addWrestler.prompt.tagTeam"));
        wrestlerTagTeamField.setDisable(true);
        tagTeamCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            wrestlerTagTeamField.setDisable(!newVal);
            wrestlerCategoryComboBox.setDisable(newVal);
            if (newVal) wrestlerCategoryComboBox.setValue(null);
            else wrestlerCategoryComboBox.setValue(WrestlerCategory.MID_CARD);
        });

        TextField imageFileNameField = new TextField();
        imageFileNameField.setPromptText(currentBundle.getString("addWrestler.prompt.image"));

        Button addWrestlerConfirmButton = new Button(currentBundle.getString("addWrestler.button.add")); // Tekst przycisku z zasobów
        addWrestlerConfirmButton.setOnAction(e -> {
            String name = wrestlerNameField.getText().trim();
            WrestlerCategory category = wrestlerCategoryComboBox.getValue();
            String imageFileName = imageFileNameField.getText().trim();
            String errorTitle = currentBundle.getString("addWrestler.alert.error.title");

            if (name.isEmpty()) { showAlert(Alert.AlertType.ERROR, errorTitle, currentBundle.getString("addWrestler.alert.nameEmpty.content")); return; }

            Wrestler newWrestler;
            if (tagTeamCheckBox.isSelected()) {
                String tagTeam = wrestlerTagTeamField.getText().trim();
                if (tagTeam.isEmpty()) { showAlert(Alert.AlertType.ERROR, errorTitle, currentBundle.getString("addWrestler.alert.tagTeamEmpty.content")); return; }
                newWrestler = new Wrestler(name, WrestlerCategory.TAG_TEAM, tagTeam, imageFileName.isEmpty() ? null : imageFileName);
            } else {
                if (category == null) { showAlert(Alert.AlertType.ERROR, errorTitle, currentBundle.getString("addWrestler.alert.categoryNull.content")); return; }
                newWrestler = new Wrestler(name, category, imageFileName.isEmpty() ? null : imageFileName);
            }
            // Sprawdzenie duplikatu z użyciem MessageFormat
            final String finalName = name; // Potrzebne dla lambdy
            if (allWrestlers.stream().anyMatch(w -> w.getName().equalsIgnoreCase(finalName))) {
                String warningTitle = currentBundle.getString("addWrestler.alert.warning.title");
                String warningPattern = currentBundle.getString("addWrestler.alert.duplicate.content");
                showAlert(Alert.AlertType.WARNING, warningTitle, MessageFormat.format(warningPattern, finalName)); return;
            }

            allWrestlers.add(newWrestler);
            updateCategoryLists();
            addWrestlerStage.close();
            // Alert sukcesu z użyciem MessageFormat
            String successTitle = currentBundle.getString("addWrestler.alert.success.title");
            String successPattern = currentBundle.getString("addWrestler.alert.success.content");
            showAlert(Alert.AlertType.INFORMATION, successTitle, MessageFormat.format(successPattern, name));
        });

        GridPane addWrestlerGrid = new GridPane();
        addWrestlerGrid.setPadding(new Insets(10)); addWrestlerGrid.setVgap(8); addWrestlerGrid.setHgap(10);
        // Użycie kluczy dla etykiet
        addWrestlerGrid.add(new Label(currentBundle.getString("addWrestler.label.name")), 0, 0); addWrestlerGrid.add(wrestlerNameField, 1, 0);
        addWrestlerGrid.add(new Label(currentBundle.getString("addWrestler.label.category")), 0, 1); addWrestlerGrid.add(wrestlerCategoryComboBox, 1, 1);
        addWrestlerGrid.add(tagTeamCheckBox, 0, 2); addWrestlerGrid.add(wrestlerTagTeamField, 1, 2);
        addWrestlerGrid.add(new Label(currentBundle.getString("addWrestler.label.image")), 0, 3); addWrestlerGrid.add(imageFileNameField, 1, 3);
        addWrestlerGrid.add(addWrestlerConfirmButton, 1, 4);

        Scene addWrestlerScene = new Scene(addWrestlerGrid, 450, 300); // Trochę większe okno
        addWrestlerStage.setScene(addWrestlerScene);
        addWrestlerStage.show();
    }

    public void showAddBrandWindow() {
        Stage addBrandStage = new Stage(); addBrandStage.initModality(Modality.WINDOW_MODAL); addBrandStage.initOwner(primaryStage);
        addBrandStage.setTitle(currentBundle != null ? currentBundle.getString("addBrand.window.title") : "Dodaj Brand");

        TextField brandNameField = new TextField();
        brandNameField.setPromptText(currentBundle != null ? currentBundle.getString("addBrand.prompt.name") : "Nazwa brandu");

        // --- DODANE POLE NA LOGO ---
        TextField logoFileNameField = new TextField();
        logoFileNameField.setPromptText("Nazwa pliku logo (np. roh_logo.png)");
        // --- KONIEC DODAWANIA POLA ---

        Button addBrandConfirmButton = new Button(currentBundle != null ? currentBundle.getString("addBrand.button.add") : "Dodaj Brand");

        addBrandConfirmButton.setOnAction(e -> {
            String brandName = brandNameField.getText().trim();
            String logoName = logoFileNameField.getText().trim(); // Pobierz nazwę pliku logo
            String errorTitle = (currentBundle != null) ? currentBundle.getString("addBrand.alert.error.title") : "Błąd";

            if (brandName.isEmpty()) { showAlert(Alert.AlertType.ERROR, errorTitle, (currentBundle != null) ? currentBundle.getString("addBrand.alert.nameEmpty.content") : "Nazwa nie może być pusta."); return; }
            final String finalBrandName = brandName; // Dla lambdy
            if (allBrands.stream().anyMatch(b -> b.getName().equalsIgnoreCase(finalBrandName))) {
                String warningTitle = (currentBundle != null) ? currentBundle.getString("addBrand.alert.warning.title") : "Ostrzeżenie";
                String warningPattern = (currentBundle != null) ? currentBundle.getString("addBrand.alert.duplicate.content") : "Brand ''{0}'' już istnieje.";
                showAlert(Alert.AlertType.WARNING, warningTitle, MessageFormat.format(warningPattern, finalBrandName)); return;
            }
            // --- ZMIANA: Tworzenie brandu z logo ---
            Brand newBrand = new Brand(brandName, logoName.isEmpty() ? null : logoName); // Przekaż nazwę logo
            allBrands.add(newBrand);
            brandRosters.putIfAbsent(newBrand, FXCollections.observableArrayList());
            addBrandStage.close();
            String successTitle = (currentBundle != null) ? currentBundle.getString("addBrand.alert.success.title") : "Sukces";
            String successPattern = (currentBundle != null) ? currentBundle.getString("addBrand.alert.success.content") : "Dodano brand: {0}";
            showAlert(Alert.AlertType.INFORMATION, successTitle, MessageFormat.format(successPattern, brandName));
        });

        // --- ZMIANA: Dodaj etykietę i pole logo do layoutu ---
        VBox addBrandLayout = new VBox(10,
                new Label(currentBundle != null ? currentBundle.getString("addBrand.label.name") : "Nazwa brandu:"),
                brandNameField,
                new Label("Nazwa pliku logo (opcjonalnie):"), // Możesz dodać klucz tłumaczenia
                logoFileNameField, // Dodane pole
                addBrandConfirmButton);
        // --- KONIEC ZMIANY LAYOUTU ---
        addBrandLayout.setPadding(new Insets(20));
        Scene addBrandScene = new Scene(addBrandLayout, 300, 200); // Zwiększona wysokość
        addBrandStage.setScene(addBrandScene);
        addBrandStage.show();
    }


    // Metoda do aktualizacji list kategorii
    private void updateCategoryLists() {
        mainEventMidCard.setAll(allWrestlers.stream()
                .filter(w -> w != null && (w.getCategory() == WrestlerCategory.MAIN_EVENT || w.getCategory() == WrestlerCategory.MID_CARD))
                .collect(Collectors.toList()));
        lowCardJobbers.setAll(allWrestlers.stream()
                .filter(w -> w != null && (w.getCategory() == WrestlerCategory.LOW_CARD || w.getCategory() == WrestlerCategory.JOBBER))
                .collect(Collectors.toList()));
        tagTeams.setAll(allWrestlers.stream()
                .filter(w -> w != null && w.getCategory() == WrestlerCategory.TAG_TEAM)
                .collect(Collectors.toList()));
        System.out.println("Zaktualizowano listy kategorii: ME/MC=" + mainEventMidCard.size() +
                ", LC/J=" + lowCardJobbers.size() + ", TT=" + tagTeams.size());
    }
    /**
     * Wyświetla okno dialogowe pozwalające użytkownikowi wybrać, które brandy
     * wezmą udział w drafcie.
     * @return Optional zawierający listę wybranych brandów lub pusty Optional, jeśli anulowano.
     */
    /**
     * Wyświetla dialog do ustawienia kolejności dla PODANEJ listy brandów.
     * @param brandsToOrder Lista brandów, których kolejność ma być ustalona.
     * @return Optional zawierający nową listę z ustaloną kolejnością lub pusty, jeśli anulowano.
     */
    /**
     * Wyświetla dialog do ustawienia kolejności dla PODANEJ listy brandów.
     * @param brandsToOrder Lista brandów, których kolejność ma być ustalona.
     * @return Optional zawierający nową listę z ustaloną kolejnością lub pusty, jeśli anulowano.
     */
    private Optional<List<Brand>> showBrandOrderDialog(List<Brand> brandsToOrder) { // <-- POPRAWNA PIERWSZA LINIA Z PARAMETREM
        Dialog<List<Brand>> dialog = new Dialog<>();
        dialog.setTitle(currentBundle.getString("brandOrder.dialog.title"));
        String headerPattern = currentBundle.getString("brandOrder.dialog.header");
        String brandListString = brandsToOrder.stream().map(Brand::getName).collect(Collectors.joining(", "));
        dialog.setHeaderText(MessageFormat.format(headerPattern, brandListString));
        dialog.initOwner(primaryStage);
        dialog.initModality(Modality.APPLICATION_MODAL);

        BorderPane borderPane = new BorderPane();
        borderPane.setPadding(new Insets(10));

        // Użyj przekazanej listy 'brandsToOrder' do inicjalizacji ObservableList
        ObservableList<Brand> orderedList = FXCollections.observableArrayList(brandsToOrder); // <-- TERAZ TO ZADZIAŁA
        ListView<Brand> listView = new ListView<>(orderedList);
        listView.setPrefHeight(200);
        borderPane.setCenter(listView);

        Button upButton = new Button(currentBundle.getString("brandOrder.button.up"));
        upButton.setDisable(true);
        Button downButton = new Button(currentBundle.getString("brandOrder.button.down"));
        downButton.setDisable(true);

        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            int selectedIndex = listView.getSelectionModel().getSelectedIndex();
            upButton.setDisable(newSelection == null || selectedIndex == 0);
            downButton.setDisable(newSelection == null || selectedIndex >= orderedList.size() - 1);
        });

        upButton.setOnAction(e -> {
            int index = listView.getSelectionModel().getSelectedIndex();
            if (index > 0) {
                Collections.swap(orderedList, index, index - 1);
                listView.getSelectionModel().select(index - 1);
            }
        });

        downButton.setOnAction(e -> {
            int index = listView.getSelectionModel().getSelectedIndex();
            if (index != -1 && index < orderedList.size() - 1) {
                Collections.swap(orderedList, index, index + 1);
                listView.getSelectionModel().select(index + 1);
            }
        });

        VBox buttonBox = new VBox(10, upButton, downButton);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(0, 0, 0, 10));
        borderPane.setRight(buttonBox);

        dialog.getDialogPane().setContent(borderPane);

        ButtonType okButtonType = ButtonType.OK;
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                return new ArrayList<>(orderedList); // Zwróć kopię uporządkowanej listy
            }
            return null; // Anulowano
        });

        return dialog.showAndWait();
    }
    private Optional<List<Brand>> showBrandSelectionDialog() {
        Dialog<List<Brand>> dialog = new Dialog<>();
        dialog.setTitle(currentBundle.getString("brandSelect.dialog.title"));
        dialog.setHeaderText(currentBundle.getString("brandSelect.dialog.header"));
        dialog.initOwner(primaryStage);
        dialog.initModality(Modality.APPLICATION_MODAL);

        // Layout dla checkboxów
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(20));
        ScrollPane scrollPane = new ScrollPane(vbox); // Użyj ScrollPane
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(250); // Ogranicz wysokość

        Map<CheckBox, Brand> checkBoxBrandMap = new HashMap<>();
        for (Brand brand : allBrands) { // Iterujemy po WSZYSTKICH dostępnych brandach
            CheckBox cb = new CheckBox(brand.getName());
            // Domyślne zaznaczenie dla pierwszych 3 (można usunąć lub zmienić)
            if (brand.getName().equals("RAW") || brand.getName().equals("SmackDown") || brand.getName().equals("NXT")) {
                cb.setSelected(true);
            }
            checkBoxBrandMap.put(cb, brand);
            vbox.getChildren().add(cb);
        }
        dialog.getDialogPane().setContent(scrollPane);

        // Przyciski OK / Anuluj
        ButtonType okButtonType = ButtonType.OK;
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        // Walidacja przycisku OK
        Node okButton = dialog.getDialogPane().lookupButton(okButtonType);
        // Funkcja do sprawdzania stanu
        Runnable updateOkButtonState = () -> {
            long selectedCount = checkBoxBrandMap.keySet().stream().filter(CheckBox::isSelected).count();
            okButton.setDisable(selectedCount < 2 || selectedCount > 7); // Min 2, Max 7
        };
        // Dodaj listenery
        for (CheckBox cb : checkBoxBrandMap.keySet()) {
            cb.selectedProperty().addListener((obs, oldVal, newVal) -> updateOkButtonState.run());
        }
        updateOkButtonState.run(); // Ustaw stan początkowy

        // Konwerter wyniku
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                return checkBoxBrandMap.entrySet().stream()
                        .filter(entry -> entry.getKey().isSelected())
                        .map(Map.Entry::getValue)
                        .collect(Collectors.toList()); // Zwróć listę wybranych
            }
            return null; // Anulowano
        });
        return dialog.showAndWait(); // Pokaż i zwróć wynik
    }
    // --- Dodaj pola na górze klasy ---
    private Locale currentLocale;
    private ResourceBundle currentBundle;
    // ---
    public void startRandomDraft() {
        if (allWrestlers.isEmpty()) {
            if (currentBundle != null) {
                showAlert(Alert.AlertType.WARNING,
                        currentBundle.getString("alert.nowrestlers.title"),
                        currentBundle.getString("alert.nowrestlers.content"));
            } else {
                showAlert(Alert.AlertType.WARNING, "Brak zawodników", "Wczytaj lub dodaj zawodników."); // Fallback
            }
            return;
        }

        // Wybór brandów
        Optional<List<Brand>> participatingBrandsOpt = showBrandSelectionDialog();
        if (!participatingBrandsOpt.isPresent()) { System.out.println("Wybór brandów anulowany."); return; }
        List<Brand> participatingBrands = participatingBrandsOpt.get();
        if (participatingBrands.size() < 2) { showAlert(Alert.AlertType.WARNING, "Za Mało Brandów", "Wybierz co najmniej 2 brandy."); return; }
        System.out.println("Brandy w losowym drafcie: " + participatingBrands);

        // Czyszczenie rosterów
        for (Brand brand : allBrands) { brandRosters.computeIfAbsent(brand, k -> FXCollections.observableArrayList()).clear(); }

        List<Wrestler> wrestlersCopy = new ArrayList<>(allWrestlers); Collections.shuffle(wrestlersCopy);
        int currentBrandIndex = 0;
        List<Brand> draftBrands = participatingBrands; // Użyj wybranych

        for (Wrestler wrestler : wrestlersCopy) {
            if (draftBrands.isEmpty()) break;
            Brand selectedBrand = draftBrands.get(currentBrandIndex);
            // Upewnij się, że roster istnieje w mapie
            brandRosters.computeIfAbsent(selectedBrand, k -> FXCollections.observableArrayList()).add(wrestler);
            currentBrandIndex = (currentBrandIndex + 1) % draftBrands.size();
        }
        showDraftResultsView(participatingBrands); // Pokaż wyniki tylko dla uczestniczących
        showAlert(Alert.AlertType.INFORMATION, "Losowy Draft", "Losowy draft zakończony!");
    }

    /**
     * Ustawia/aktualizuje siatkę wyników draftu.
     * UŻYWA ROZSZERZONEJ PĘTLI FOR.
     * @param brandsToDisplay Lista brandów do wyświetlenia.
     */
    private void setupDraftGrid(List<Brand> brandsToDisplay) {
        if (draftGrid == null) {
            draftGrid = new GridPane();
            draftGrid.setHgap(10);
            draftGrid.setVgap(10);
            draftGrid.setPadding(new Insets(10));
        } else {
            draftGrid.getChildren().clear();
            draftGrid.getColumnConstraints().clear();
            draftGrid.getRowConstraints().clear();
        }

        if (brandsToDisplay == null || brandsToDisplay.isEmpty()) {
            System.err.println("setupDraftGrid: Brak brandów do wyświetlenia!");
            // Opcjonalnie dodaj jakąś informację w UI
            Label noBrandsLabel = new Label("Nie wybrano brandów do wyświetlenia.");
            draftGrid.add(noBrandsLabel, 0, 0);
            return;
        }

        // --- PĘTLA 1: Dodaj nagłówki brandów ---
        int colIndex = 0; // Potrzebujemy indeksu kolumny
        for (int i = 0; i < brandsToDisplay.size(); i++) {
            Brand brand = brandsToDisplay.get(i); // Pobierz brand
            Image logo = loadImageForBrand(brand); // Załaduj logo
            ImageView logoView = new ImageView(logo); // Stwórz ImageView
            logoView.setFitHeight(30); // Ustaw wysokość loga w nagłówku
            logoView.setPreserveRatio(true);

            // Opcjonalnie: Tooltip z nazwą brandu
            Tooltip.install(logoView, new Tooltip(brand.getName()));

            draftGrid.add(logoView, i, 0); // Dodaj ImageView do siatki
            GridPane.setHalignment(logoView, javafx.geometry.HPos.CENTER); // Wyśrodkuj logo

            // Ograniczenia kolumn (bez zmian)
            ColumnConstraints colConstraint = new ColumnConstraints();
            colConstraint.setPercentWidth(100.0 / brandsToDisplay.size());
            draftGrid.getColumnConstraints().add(colConstraint);
        }
        // --- KONIEC PĘTLI 1 ---

        // --- PĘTLA 2: Wypełnij siatkę rosterami ---
        int maxRows = 0;
        colIndex = 0; // Zresetuj indeks kolumny
        for (Brand brand : brandsToDisplay) { // Rozszerzona pętla for
            ObservableList<Wrestler> roster = brandRosters.getOrDefault(brand, FXCollections.observableArrayList());
            if (roster.size() > maxRows) {
                maxRows = roster.size();
            }

            ListView<Wrestler> rosterListView = createWrestlerListView(roster); // Użyj helpera
            draftGrid.add(rosterListView, colIndex, 1); // Używamy colIndex
            GridPane.setVgrow(rosterListView, Priority.ALWAYS);

            colIndex++; // Zwiększ indeks kolumny
        }
        // --- KONIEC PĘTLI 2 ---

        // Ograniczenia wierszy (bez zmian)
        RowConstraints headerRowConstraint = new RowConstraints();
        headerRowConstraint.setMinHeight(30);
        draftGrid.getRowConstraints().add(headerRowConstraint);

        RowConstraints dataRowConstraint = new RowConstraints();
        dataRowConstraint.setVgrow(Priority.ALWAYS);
        draftGrid.getRowConstraints().add(dataRowConstraint);
    }

    private <T> List<T> limitListSize(List<T> list, int maxSize) { /* ... KOD Z POPRZEDNIEJ ODPOWIEDZI ... */
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        if (list.size() <= maxSize) {
            return new ArrayList<>(list);
        } else {
            List<T> shuffledList = new ArrayList<>(list);
            Collections.shuffle(shuffledList, random);
            return shuffledList.subList(0, maxSize);
        }
    }

    public void startManualDraft() {
        System.out.println("--- startManualDraft: Rozpoczęto ---"); // LOG 1

        if (allWrestlers.isEmpty()) {
            if (currentBundle != null) {
                showAlert(Alert.AlertType.WARNING,
                        currentBundle.getString("alert.nowrestlers.title"),
                        currentBundle.getString("alert.nowrestlers.content"));
            } else {
                showAlert(Alert.AlertType.WARNING, "Brak zawodników", "Wczytaj lub dodaj zawodników."); // Fallback
            }
            return;
        }

        // 1. WYBÓR BRANDÓW
        System.out.println("   startManualDraft: Otwieram dialog wyboru brandów..."); // LOG 2
        Optional<List<Brand>> participatingBrandsOpt = showBrandSelectionDialog();

        if (!participatingBrandsOpt.isPresent()) {
            System.out.println("   startManualDraft: Wybór brandów anulowany. Zakończono."); // LOG 3
            return;
        }
        List<Brand> participatingBrands = participatingBrandsOpt.get();
        System.out.println("   startManualDraft: Wybrano brandy: " + participatingBrands); // LOG 4

        if (participatingBrands.size() < 2 || participatingBrands.size() > 7) {
            showAlert(Alert.AlertType.ERROR, "Błąd!", "Wybierz od 2 do 7 brandów."); return;
        }

        // 2. RESET STANU
        System.out.println("   startManualDraft: Resetowanie stanu draftu..."); // LOG 5
        for (Brand brand : allBrands) { brandRosters.computeIfAbsent(brand, k-> FXCollections.observableArrayList()).clear(); }
        finishedBrands.clear(); autoFinishing = false; currentPickingBrand = null;
        currentDraftRound = 0; currentBrandPickIndex = 0; currentDraftOrder = null;
        updateCategoryLists();
        System.out.println("   startManualDraft: Stan zresetowany."); // LOG 6

        // 3. WYBÓR KOLEJNOŚCI
        System.out.println("   startManualDraft: Otwieram dialog ustalania kolejności..."); // LOG 7
        Optional<List<Brand>> orderedBrandsOptional = showBrandOrderDialog(participatingBrands);

        if (!orderedBrandsOptional.isPresent()) {
            System.out.println("   startManualDraft: Anulowano ustawianie kolejności. Zakończono."); // LOG 8
            return;
        }
        this.currentDraftOrder = orderedBrandsOptional.get();
        System.out.println("   startManualDraft: Ustalona kolejność: " + currentDraftOrder); // LOG 9

        // 4. UI i START PĘTLI
        System.out.println("   startManualDraft: Przygotowuję UI (setupDraftGrid)..."); // LOG 10
        setupDraftGrid(currentDraftOrder); // Użyj finalnej listy
        mainContentArea.setTop(createDraftHeader(currentBundle.getString("draft.header.title.ongoing")));
        mainContentArea.setCenter(new ScrollPane(draftGrid));
        if (autoFinishButtonHeader != null) autoFinishButtonHeader.setDisable(false);
        if (finishForBrandButtonHeader != null) finishForBrandButtonHeader.setDisable(true);
        System.out.println("   startManualDraft: UI przygotowane. Wywołuję Platform.runLater(draftLoop)..."); // LOG 11

        Platform.runLater(() -> {
            System.out.println("   startManualDraft: Wewnątrz Platform.runLater - Uruchamiam draftLoop(0)..."); // LOG 12
            draftLoop(currentDraftOrder, 0); // Rozpocznij z ustaloną kolejnością
        });

        System.out.println("--- startManualDraft: Zakończono (zaplanowano start draftLoop) ---"); // LOG 13
    }


    // REKURENCYJNA funkcja obsługująca JEDNĄ rundę draftu (cykl kategorii)
    private void draftLoop(List<Brand> brandsInDraft, int round) {
        System.out.println("\n=== draftLoop: runda " + round + " ===");

        // Sprawdź tryb auto-finish (bez zmian)
        if (autoFinishing) {
            System.out.println("   Auto-Finish. Uruchamiam runAutoFinish().");
            runAutoFinish(brandsInDraft); return;
        }

        // Warunek końca draftu: wszystkie listy kategorii są puste
        if (mainEventMidCard.isEmpty() && lowCardJobbers.isEmpty() && tagTeams.isEmpty()) {
            System.out.println("!!! KONIEC DRAFTU: Kategorie puste !!!");

            // --- TUTAJ WKLEJ TEN BLOK ---
            Platform.runLater(() -> {
                setupDraftGrid(brandsInDraft); // Przekaż listę brandów biorących udział
                showAlert(Alert.AlertType.INFORMATION, "Koniec Draftu", "Draft zakończony! Wszyscy dostępni zawodnicy zostali wybrani.");
                if (mainContentArea.getTop() != null) mainContentArea.setTop(createDraftHeader("Wyniki Draftu")); // Zmień tytuł nagłówka
            });
            // --- KONIEC BLOKU ---

            return; // Zakończ rekurencję
        }

        // Określ, która kategoria jest wybierana w tej rundzie
        int categoryRoundType = round % 3; // 0: Main/Mid, 1: Low/Jobber, 2: Tag Team
        ObservableList<Wrestler> currentCategoryList;
        String currentCategoryName;

        if (categoryRoundType == 0) {
            currentCategoryList = mainEventMidCard;
            currentCategoryName = "Main Event / Mid Card";
        } else if (categoryRoundType == 1) {
            currentCategoryList = lowCardJobbers;
            currentCategoryName = "Low Card / Jobber";
        } else {
            currentCategoryList = tagTeams;
            currentCategoryName = "Tag Team";
        }
        System.out.println("   Runda " + (round + 1) + " (indeks " + round + "), typ: " + categoryRoundType + " -> Kategoria: " + currentCategoryName + ", Dostępnych: " + currentCategoryList.size());


        // Jeśli lista dla bieżącej kategorii jest pusta, przejdź rekurencyjnie do następnej rundy
        if (currentCategoryList.isEmpty()) {
            System.out.println("   Runda " + (round + 1) + ": Kategoria " + currentCategoryName + " jest pusta. Przechodzę do następnej.");
            draftLoop(brandsInDraft, round + 1); // Przejdź do następnej rundy (kolejna kategoria)
            return;
        }

        // --- ZMIANA LOGIKI ODWRACANIA ---
        // Ustal kolejność brandów dla tej rundy (co rundę zmiana kierunku)
        boolean reverseOrder = round % 2 != 0; // Zmieniono z (round / 3) % 2 != 0
        // --- KONIEC ZMIANY ---

        List<Brand> roundBrands = new ArrayList<>(brandsInDraft); // Użyj listy przekazanej jako argument
        if (reverseOrder) {
            Collections.reverse(roundBrands); // Odwróć kolejność
            System.out.println("   Runda " + (round + 1) + ": Odwrócona kolejność (serpentyna)");
        } else {
            System.out.println("   Runda " + (round + 1) + ": Kolejność normalna.");
        }
        System.out.println("   Kolejność wybierania w rundzie " + (round + 1) + ": " + roundBrands.stream().map(Brand::getName).collect(Collectors.joining(", ")));


        // Rozpocznij wybór dla pierwszego brandu w tej rundzie kategorii
        System.out.println("   Rozpoczynam wybory w rundzie " + (round+1) + ". Dostępnych w kat '" + currentCategoryName + "': " + currentCategoryList.size());
        // Przekazujemy listę brandów dla tej konkretnej rundy (roundBrands) i pełną listę (brandsInDraft)
        draftBrandChoice(roundBrands, brandsInDraft, 0, round, currentCategoryList, currentCategoryName);
    }

    /**
     * Rekurencyjna pętla wyboru dla brandów w DANEJ rundzie kategorii.
     * Obsługuje wybór jednego brandu i przechodzi do następnego.
     *
     * @param roundBrandsOrder Lista brandów w kolejności dla TEJ rundy.
     * @param allBrandsInDraft Pełna lista brandów biorących udział w drafcie (do przekazania do draftLoop).
     * @param brandIndex       Indeks bieżącego brandu w roundBrandsOrder.
     * @param round            Numer bieżącej GŁÓWNEJ rundy draftu (cyklu kategorii).
     * @param currentCategoryList Lista zawodników DOSTĘPNYCH w bieżącej kategorii.
     * @param categoryName        Nazwa bieżącej kategorii (do wyświetlania).
     */
    private void draftBrandChoice(List<Brand> roundBrandsOrder, List<Brand> allBrandsInDraft, int brandIndex, int round,
                                  ObservableList<Wrestler> currentCategoryList, String categoryName) {

        // ---- Warunki zakończenia/pominięcia dla tej rundy kategorii ----

        // 1. Jeśli przeszliśmy przez wszystkie brandy w tej rundzie LUB kategoria się opróżniła
        if (brandIndex >= roundBrandsOrder.size() || currentCategoryList.isEmpty()) {
            String reason = currentCategoryList.isEmpty() ? "Kategoria opróżniona" : "Wszystkie brandy wybrały";
            System.out.println("   " + reason + " w rundzie " + (round + 1) + " dla kat. " + categoryName + ". -> Następna runda (kategorii): " + (round + 2)); // Poprawiono log
            // Przejdź do następnej GŁÓWNEJ rundy draftu (innej kategorii)
            draftLoop(allBrandsInDraft, round + 1);
            return;
        }

        // --- Wybór bieżącego brandu ---
        Brand currentBrand = roundBrandsOrder.get(brandIndex);
        this.currentPickingBrand = currentBrand; // Zapamiętaj, kto aktualnie wybiera

        // Odśwież nagłówek, pokazując kto wybiera i włączając/wyłączając przyciski
        Platform.runLater(() -> {
            if (pickingInfoLabelHeader != null) {
                pickingInfoLabelHeader.setText("Runda " + (round + 1) + ", Kat: " + categoryName + ", Wybiera: " + currentBrand.getName());
            }
            if (finishForBrandButtonHeader != null) {
                boolean alreadyFinished = finishedBrands.contains(currentBrand);
                finishForBrandButtonHeader.setText(alreadyFinished ? "Draft Zakończony" : "Zakończ dla " + currentBrand.getName());
                // Wyłącz przycisk jeśli brand skończył LUB jeśli jest tryb auto-finish
                finishForBrandButtonHeader.setDisable(alreadyFinished || autoFinishing);
            }
            if (autoFinishButtonHeader != null) {
                autoFinishButtonHeader.setDisable(autoFinishing); // Wyłącz, jeśli już włączono auto-finish
            }
        });
        System.out.println("  >> Runda " + (round + 1) + ", Kat: " + categoryName + ", Wybiera: " + currentBrand.getName() + " <<");

        // 2. Pomiń, jeśli ten brand już zakończył draft
        if (finishedBrands.contains(currentBrand)) {
            System.out.println("    Brand " + currentBrand.getName() + " już zakończył draft - pomijam.");
            // Przejdź rekurencyjnie do następnego brandu w TEJ rundzie
            draftBrandChoice(roundBrandsOrder, allBrandsInDraft, brandIndex + 1, round, currentCategoryList, categoryName);
            return;
        }

        // --- Przygotowanie listy zawodników do dialogu ---
        List<WrestlerCategory> categoriesForDialog;
        String categoryNameToDisplay;

        if (round == 0) { // Specjalna pierwsza runda - tylko Main Event
            categoriesForDialog = List.of(WrestlerCategory.MAIN_EVENT);
            categoryNameToDisplay = "Main Event";
        } else {
            // Logika dla pozostałych rund (Main/Mid, Low/Jobber, Tag Team lub pozostali)
            boolean hasPreferred;
            int categoryType = round % 3;
            if (categoryType == 0) { // Main/Mid
                hasPreferred = currentCategoryList.stream().anyMatch(w -> w.getCategory() == WrestlerCategory.MAIN_EVENT || w.getCategory() == WrestlerCategory.MID_CARD);
                categoriesForDialog = List.of(WrestlerCategory.MAIN_EVENT, WrestlerCategory.MID_CARD);
            } else if (categoryType == 1) { // Low/Jobber
                hasPreferred = currentCategoryList.stream().anyMatch(w -> w.getCategory() == WrestlerCategory.LOW_CARD || w.getCategory() == WrestlerCategory.JOBBER);
                categoriesForDialog = List.of(WrestlerCategory.LOW_CARD, WrestlerCategory.JOBBER);
            } else { // Tag Team
                hasPreferred = currentCategoryList.stream().anyMatch(w -> w.getCategory() == WrestlerCategory.TAG_TEAM);
                categoriesForDialog = List.of(WrestlerCategory.TAG_TEAM);
            }

            // Jeśli nie ma preferowanych, a lista nie jest pusta, pokaż pozostałych z tej listy kategorii
            if (!hasPreferred && !currentCategoryList.isEmpty()) {
                categoriesForDialog = currentCategoryList.stream().map(Wrestler::getCategory).distinct().collect(Collectors.toList());
                categoryNameToDisplay = "Pozostali z " + categoryName;
                System.out.println("    Brak preferowanych, pokazuję: " + categoriesForDialog);
            } else if (!hasPreferred) {
                // Jeśli nie ma preferowanych I lista jest pusta (co nie powinno się zdarzyć przez sprawdzenie na początku draftLoop)
                System.out.println("    Brak jakichkolwiek zawodników do wyboru dla " + currentBrand.getName() + ". Pomięcie.");
                draftBrandChoice(roundBrandsOrder, allBrandsInDraft, brandIndex + 1, round, currentCategoryList, categoryName);
                return;
            } else {
                categoryNameToDisplay = categoryName; // Użyj standardowej nazwy grupy
            }
        }

        // Dodatkowe zabezpieczenie: Jeśli po filtrowaniu nie ma co pokazać
        if (categoriesForDialog.isEmpty() && !currentCategoryList.isEmpty()) {
            System.out.println("    Brak zawodników pasujących do kategorii " + categoriesForDialog + ". Pomięcie.");
            draftBrandChoice(roundBrandsOrder, allBrandsInDraft, brandIndex + 1, round, currentCategoryList, categoryName);
            return;
        }

        // --- Wyświetlenie Dialogu i Obsługa Wyniku ---
        showCategoryChoiceDialog(currentCategoryList, currentBrand, round, categoryNameToDisplay, categoriesForDialog, (selectedWrestler) -> {
            // Ten kod wykona się PO zamknięciu dialogu przez użytkownika

            // Zawsze wyłącz przycisk "Zakończ dla..." po powrocie z dialogu
            Platform.runLater(() -> { if(finishForBrandButtonHeader != null) finishForBrandButtonHeader.setDisable(true); });

            // 1. NAJPIERW sprawdź, czy tryb auto-finish został właśnie włączony W DIALOGU
            if (autoFinishing) {
                System.out.println("    Callback: Wykryto autoFinishing=true. Uruchamiam runAutoFinish.");
                runAutoFinish(allBrandsInDraft); // Uruchom auto-dokańczanie
                return; // Zakończ callback, nie przechodź do następnego brandu ręcznie
            }

            // 2. Jeśli NIE auto-finish, sprawdź, czy użytkownik coś wybrał
            if (selectedWrestler != null) {
                // Użytkownik wybrał zawodnika
                System.out.println("    --" + currentBrand.getName() + " wybrał: " + selectedWrestler.getName() + " [" + selectedWrestler.getCategory().getDisplayName() + "]");

                // Sprawdzenie dla pewności
                if (!currentCategoryList.contains(selectedWrestler)){
                    System.err.println("    BŁĄD KRYTYCZNY: Wybrany " + selectedWrestler.getName() + " nie na liście " + categoryName);
                }

                // Dodaj do rosteru brandu
                brandRosters.get(currentBrand).add(selectedWrestler);

                // Specjalna obsługa Tag Teamów - dodaj i usuń partnera
                if (selectedWrestler.getCategory() == WrestlerCategory.TAG_TEAM && selectedWrestler.getTagTeamName() != null) {
                    String teamName = selectedWrestler.getTagTeamName();
                    // Szukaj partnera na GŁÓWNEJ liście kategorii
                    Wrestler partner = currentCategoryList.stream()
                            .filter(w -> !w.equals(selectedWrestler) && teamName.equals(w.getTagTeamName())) // Użyj equals() dla pewności
                            .findFirst()
                            .orElse(null);
                    if (partner != null) {
                        System.out.println("    >>>> Znaleziono i dodano partnera: " + partner.getName());
                        brandRosters.get(currentBrand).add(partner); // Dodaj partnera do rosteru
                        currentCategoryList.remove(partner); // Usuń partnera z GŁÓWNEJ listy kategorii
                    } else {
                        System.out.println("    INFO: Nie znaleziono dostępnego partnera dla " + selectedWrestler.getName());
                    }
                }

                // Usuń WYBRANEGO zawodnika z GŁÓWNEJ listy kategorii
                boolean removed = currentCategoryList.remove(selectedWrestler);
                System.out.println("    Usuwanie '" + selectedWrestler.getName() + "' z listy '" + categoryName + "'. Sukces: " + removed);
                System.out.println("    Pozostało w kat. '" + categoryName + "': " + currentCategoryList.size());

                // Odśwież widok siatki draftu w wątku JavaFX
                Platform.runLater(() -> setupDraftGrid(allBrandsInDraft)); // Przekaż PEŁNĄ listę brandów biorących udział

            } else {
                // selectedWrestler jest null (Pomiń, Anuluj, lub Zakończ dla Brandu)
                System.out.println("    --" + currentBrand.getName() + " pominął wybór lub zakończył draft w tej turze.");
                // finishedBrands zostało ustawione w dialogu, jeśli kliknięto Zakończ dla Brandu
            }

            // 3. Przejdź rekurencyjnie do następnego brandu w TEJ rundzie kategorii
            draftBrandChoice(roundBrandsOrder, allBrandsInDraft, brandIndex + 1, round, currentCategoryList, categoryName);
        });
    }

    /**
     * Wyświetla niestandardowy dialog wyboru zawodnika z dodatkowymi opcjami.
     * Używa Dialog<Wrestler> zamiast ChoiceDialog.
     *
     * @param currentCategoryList Lista zawodników dostępnych w BIEŻĄCEJ kategorii.
     * @param currentBrand        Brand, który aktualnie wybiera.
     * @param round               Numer bieżącej rundy draftu.
     * @param categoryNameToDisplay Nazwa kategorii wyświetlana użytkownikowi.
     * @param categoriesForFilter   Faktyczne kategorie do filtrowania (np. tylko MAIN_EVENT w rundzie 0).
     * @param onChoiceFinished      Callback wywoływany po zamknięciu dialogu (przekazuje wybranego wrestlera lub null).
     */
    private void showCategoryChoiceDialog(ObservableList<Wrestler> currentCategoryList, Brand currentBrand, int round,
                                          String categoryNameToDisplay, List<WrestlerCategory> categoriesForFilter,
                                          java.util.function.Consumer<Wrestler> onChoiceFinished) {

        // Filtruj i ogranicz listę do pokazania (max 6 losowych)
        List<Wrestler> filteredAvailable = currentCategoryList.stream()
                .filter(w -> categoriesForFilter.contains(w.getCategory()))
                .collect(Collectors.toList());
        List<Wrestler> limitedWrestlers = limitListSize(filteredAvailable, 6);

        if (limitedWrestlers.isEmpty()) {
            System.out.println("      showCategoryChoiceDialog: Brak dostępnych w kat. " + categoryNameToDisplay);
            Platform.runLater(() -> onChoiceFinished.accept(null)); // Asynchronicznie
            return;
        }
        System.out.println("      showCategoryChoiceDialog: Lista do wyboru (" + limitedWrestlers.size() + "): " + limitedWrestlers.stream().map(Wrestler::getName).collect(Collectors.joining(", ")));

        // --- POCZĄTEK ZMIANY ---
        // Utwórz listę DO WYŚWIETLENIA (displayList) TUTAJ, przed tworzeniem dialogu
        ObservableList<Wrestler> displayList = FXCollections.observableArrayList(limitedWrestlers);
        displayList.sort(Comparator.comparing(Wrestler::getName)); // Sortuj
        Wrestler defaultChoice = displayList.isEmpty() ? null : displayList.get(0); // Bezpieczny wybór domyślny
        // --- KONIEC ZMIANY ---

        // --- Tworzenie niestandardowego Dialogu ---
        Dialog<Wrestler> dialog = new Dialog<>();
        dialog.setTitle(MessageFormat.format(currentBundle.getString("wrestlerSelect.dialog.title"), round + 1));
        // Teraz displayList już istnieje i można użyć displayList.size()
        dialog.setHeaderText(MessageFormat.format(currentBundle.getString("wrestlerSelect.dialog.header"), currentBrand.getName(), categoryNameToDisplay, displayList.size()));
        dialog.initOwner(primaryStage);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setGraphic(null); // Opcjonalnie

        // --- Przyciski Dialogu (bez zmian) ---
        ButtonType selectButtonType = new ButtonType(currentBundle.getString("wrestlerSelect.button.select"), ButtonBar.ButtonData.OK_DONE);
        // ... (reszta definicji ButtonType bez zmian) ...
        ButtonType skipButtonType = new ButtonType(currentBundle.getString("wrestlerSelect.button.skip"), ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType finishBrandButtonType = new ButtonType(MessageFormat.format(currentBundle.getString("wrestlerSelect.button.finishForBrand"), currentBrand.getName()), ButtonBar.ButtonData.FINISH);
        ButtonType autoFinishButtonType = new ButtonType(currentBundle.getString("wrestlerSelect.button.finishAuto"), ButtonBar.ButtonData.HELP_2);
        dialog.getDialogPane().getButtonTypes().addAll(selectButtonType, skipButtonType, finishBrandButtonType, autoFinishButtonType);


        // --- Zawartość Dialogu (używa displayList zdefiniowanej wcześniej) ---
        VBox dialogContent = new VBox(10); dialogContent.setPadding(new Insets(10));
        Label infoLabel = new Label(currentBundle.getString("wrestlerSelect.label.info"));
        ListView<Wrestler> dialogListView = createWrestlerListView(displayList); // Przekaż displayList
        dialogListView.setPrefHeight(250);
        dialogContent.getChildren().addAll(infoLabel, dialogListView);
        dialog.getDialogPane().setContent(dialogContent);
        dialog.getDialogPane().setMinWidth(500);

        // --- Logika Przycisków (używa displayList) ---
        Node selectButton = dialog.getDialogPane().lookupButton(selectButtonType);
        // Upewnij się, że defaultChoice nie jest null, jeśli displayList nie jest pusta
        if (defaultChoice != null) {
            selectButton.setDisable(false); // Włącz, jeśli jest domyślny wybór
            // Opcjonalnie zaznacz domyślny wybór w ListView
            // Platform.runLater(() -> dialogListView.getSelectionModel().select(defaultChoice));
        } else {
            selectButton.setDisable(true);
        }
        dialogListView.getSelectionModel().selectedItemProperty().addListener((obs, oldS, newS) -> selectButton.setDisable(newS == null));

        // --- Konwerter wyniku (bez zmian) ---
        dialog.setResultConverter(dialogButton -> { /* ... jak poprzednio ... */
            final int MIN_PICKS_FOR_BRAND_FINISH = 10; if (dialogButton == selectButtonType) { return dialogListView.getSelectionModel().getSelectedItem(); } else if (dialogButton == finishBrandButtonType) { int currentBrandPicks = brandRosters.getOrDefault(currentBrand, FXCollections.observableArrayList()).size(); if (currentBrandPicks < MIN_PICKS_FOR_BRAND_FINISH) { Platform.runLater(()->showAlert(Alert.AlertType.WARNING,
                    currentBundle.getString("alert.draft.finishBrandTooEarly.title"), // Pobierz tytuł
                    MessageFormat.format(currentBundle.getString("alert.draft.finishBrandTooEarly.content.revised"), // Pobierz WZORZEC
                            currentBrand.getName(), MIN_PICKS_FOR_BRAND_FINISH, currentBrandPicks) // Wstaw dane
            )); return null; } else { System.out.println("!!! Dialog: Zakończ dla: " + currentBrand.getName() + " !!!"); finishedBrands.add(currentBrand); return null; } } else if (dialogButton == autoFinishButtonType) { System.out.println("!!! Dialog: Dokończ Auto !!!"); autoFinishing = true; Platform.runLater(()->{ if (finishForBrandButtonHeader != null) finishForBrandButtonHeader.setDisable(true); if (autoFinishButtonHeader != null) autoFinishButtonHeader.setDisable(true); }); return null; } System.out.println("        Dialog pominięty/anulowany."); return null;
        });

        // Pokaż dialog i poczekaj na wynik (bez zmian)
        Optional<Wrestler> result = dialog.showAndWait();
        onChoiceFinished.accept(result.orElse(null));
    } // Koniec metody showCategoryChoiceDialog
    /**
     * Automatycznie dokańcza draft, losowo przypisując pozostałych zawodników.
     * @param brandsInDraft Lista brandów biorących udział w drafcie (w oryginalnej kolejności).
     */
    // --- DODAJ TE DWIE METODY do klasy Main ---

    /**
     * Metoda wywoływana po kliknięciu przycisku "Zakończ dla [Brand]" w nagłówku.
     * Sprawdza warunek minimum 10 picków dla danego brandu.
     */

    // --- KONIEC DODAWANYCH METOD ---
    // --- DODAJ TE DWIE METODY ---

    /**
     * Metoda wywoływana po kliknięciu przycisku "Zakończ dla [Brand]" w nagłówku.
     * Sprawdza warunek minimum 10 picków dla danego brandu.
     */
    private void handleFinishForCurrentBrand() {
        if (currentPickingBrand != null && !finishedBrands.contains(currentPickingBrand)) {
            final int MIN_PICKS_FOR_BRAND_FINISH = 10; // Minimum picków DLA TEGO BRANDU
            // Pobierz aktualny roster dla sprawdzanego brandu
            int currentBrandPicks = brandRosters.getOrDefault(currentPickingBrand, FXCollections.observableArrayList()).size();

            if (currentBrandPicks < MIN_PICKS_FOR_BRAND_FINISH) {
                showAlert(Alert.AlertType.WARNING, "Za Wcześnie",
                        currentPickingBrand.getName() + " musi wybrać co najmniej " + MIN_PICKS_FOR_BRAND_FINISH + " zawodników, aby zakończyć draft.\n" +
                                "Aktualnie ma: " + currentBrandPicks);
                return; // Nie rób nic, jeśli warunek niespełniony
            }
            // Warunek spełniony - zakończ dla brandu
            System.out.println("!!! Użytkownik (przycisk nagłówka) zakończył draft dla: " + currentPickingBrand.getName() + " !!!");
            finishedBrands.add(currentPickingBrand); // Dodaj do zbioru zakończonych
            // Zaktualizuj stan przycisku (np. wyłącz go)
            if (finishForBrandButtonHeader != null) {
                finishForBrandButtonHeader.setText("Draft Zakończony");
                finishForBrandButtonHeader.setDisable(true);
            }
            // Pętla draftu sama pominie ten brand przy następnej okazji
            // Można by spróbować wymusić przejście dalej, ale obecna logika powinna wystarczyć
        } else {
            System.out.println("handleFinishForCurrentBrand: Brak bieżącego brandu lub już zakończono.");
        }
    }

    /**
     * Metoda wywoływana po kliknięciu przycisku "Dokończ Automatycznie" w nagłówku.
     * Ustawia flagę autoFinishing (bez warunku minimum).
     */
    private void handleAutoFinish() {
        // Brak warunku minimum dla tego przycisku
        if (!autoFinishing) {
            System.out.println("!!! Włączono tryb auto-finish przez przycisk w nagłówku !!!");
            autoFinishing = true; // Ustaw flagę

            // Wyłącz przyciski w nagłówku
            if (finishForBrandButtonHeader != null) finishForBrandButtonHeader.setDisable(true);
            if (autoFinishButtonHeader != null) autoFinishButtonHeader.setDisable(true);

            showAlert(Alert.AlertType.INFORMATION, "Auto-Finish", "Automatyczne dokańczanie rozpocznie się...");
            // Pętla draftLoop sama wykryje flagę przy następnym wywołaniu
            // (lub callback w draftBrandChoice, jeśli dialog był otwarty)
        }
    }
    // --- KONIEC DODAWANYCH METOD ---
    private void runAutoFinish(List<Brand> brandsInDraft) {
        System.out.println("--- Rozpoczynam automatyczne dokańczanie draftu ---");
        // Zbierz WSZYSTKICH pozostałych zawodników ze WSZYSTKICH list kategorii
        List<Wrestler> remainingWrestlers = new ArrayList<>();
        remainingWrestlers.addAll(mainEventMidCard);
        remainingWrestlers.addAll(lowCardJobbers);
        remainingWrestlers.addAll(tagTeams);

        if (remainingWrestlers.isEmpty()) {
            System.out.println("--- Brak pozostałych zawodników do automatycznego draftu. ---");
            // Wywołaj logikę końca draftu (może być powtórzenie z draftLoop)
            Platform.runLater(() -> {
                setupDraftGrid(brandsInDraft); // <-- DODAJ TUTAJ ARGUMENT 'brandsInDraft'
                showAlert(Alert.AlertType.INFORMATION, "Koniec Draftu", "Draft zakończony! Wszyscy zawodnicy zostali wybrani (lub nie było więcej).");
                if (mainContentArea.getTop() != null) mainContentArea.setTop(createDraftHeader("Wyniki Draftu"));
            });
            return;
        }

        Collections.shuffle(remainingWrestlers); // Potasuj pozostałych
        System.out.println("Pozostało " + remainingWrestlers.size() + " zawodników do automatycznego przydzielenia.");

        // Znajdź ostatni indeks brandu, który wybierał, aby kontynuować sekwencję
        // Używamy currentBrandPickIndex, który był ustawiony przed wejściem w auto-finish
        int currentBrandIndex = this.currentBrandPickIndex; // Zacznij od następnego po ostatnim ręcznym
        if (currentBrandIndex >= brandsInDraft.size()) {
            currentBrandIndex = 0; // Zabezpieczenie, jeśli ostatni był ostatnim na liście
        }


        List<Wrestler> wrestlersToAssign = new ArrayList<>(remainingWrestlers); // Kopia do iteracji i usuwania

        while(!wrestlersToAssign.isEmpty()){
            // Znajdź następny *aktywny* brand
            Brand selectedBrand = null;
            int searchStartIndex = currentBrandIndex; // Zapamiętaj, gdzie zaczynamy szukać w tej turze
            boolean foundActive = false;
            do {
                Brand potentialBrand = brandsInDraft.get(currentBrandIndex);
                if (!finishedBrands.contains(potentialBrand)) {
                    selectedBrand = potentialBrand; // Znaleziono aktywny brand
                    foundActive = true;
                    currentBrandIndex = (currentBrandIndex + 1) % brandsInDraft.size(); // Przygotuj indeks na następny wybór
                    break; // Przestań szukać w tej turze
                }
                currentBrandIndex = (currentBrandIndex + 1) % brandsInDraft.size(); // Sprawdź następny brand
            } while (currentBrandIndex != searchStartIndex); // Sprawdź tylko jeden pełny cykl

            // Jeśli nie ma już aktywnych brandów (wszyscy kliknęli "Zakończ"), przerwij
            if (!foundActive) {
                System.out.println("   Brak aktywnych brandów do automatycznego przydzielenia. Przerywam auto-finish.");
                break;
            }


            // Przydziel pierwszego zawodnika z listy pozostałych
            Wrestler wrestlerToDraft = wrestlersToAssign.remove(0); // Pobierz i usuń z listy 'do przydzielenia'

            brandRosters.get(selectedBrand).add(wrestlerToDraft); // Dodaj do rosteru
            System.out.println("  Auto-Draft: " + selectedBrand.getName() + " otrzymuje " + wrestlerToDraft.getName());

            // Usuń również z odpowiedniej listy kategorii (aby stan był spójny)
            // Nie jest to krytyczne, bo i tak czyścimy je na końcu, ale dla poprawności:
            if(mainEventMidCard.contains(wrestlerToDraft)) mainEventMidCard.remove(wrestlerToDraft);
            else if(lowCardJobbers.contains(wrestlerToDraft)) lowCardJobbers.remove(wrestlerToDraft);
            else if(tagTeams.contains(wrestlerToDraft)) tagTeams.remove(wrestlerToDraft);


            // Specjalna obsługa tag teamów - znajdź i usuń partnera z POZOSTAŁYCH
            if (wrestlerToDraft.getCategory() == WrestlerCategory.TAG_TEAM && wrestlerToDraft.getTagTeamName() != null) {
                String teamName = wrestlerToDraft.getTagTeamName();
                // Szukaj partnera na liście POZOSTAŁYCH (wrestlersToAssign)
                Iterator<Wrestler> iterator = wrestlersToAssign.iterator(); // Użyj iteratora do bezpiecznego usuwania
                while(iterator.hasNext()){
                    Wrestler potentialPartner = iterator.next();
                    if(!potentialPartner.equals(wrestlerToDraft) && teamName.equals(potentialPartner.getTagTeamName())){ // Porównanie po nazwie tag teamu
                        System.out.println("    >>>> Auto-Draft: Dodano partnera " + potentialPartner.getName() + " do " + selectedBrand.getName());
                        brandRosters.get(selectedBrand).add(potentialPartner); // Dodaj partnera do rosteru
                        iterator.remove(); // Usuń partnera z listy do przydzielenia
                        // Usuń partnera także z list kategorii
                        if(mainEventMidCard.contains(potentialPartner)) mainEventMidCard.remove(potentialPartner);
                        else if(lowCardJobbers.contains(potentialPartner)) lowCardJobbers.remove(potentialPartner);
                        else if(tagTeams.contains(potentialPartner)) tagTeams.remove(potentialPartner);
                        break; // Załóżmy, że jest tylko jeden partner
                    }
                }
            }
        } // Koniec pętli while po zawodnikach

        // Wyczyść listy kategorii na wszelki wypadek, bo draft się zakończył
        mainEventMidCard.clear(); lowCardJobbers.clear(); tagTeams.clear();

        System.out.println("--- Zakończono automatyczne dokańczanie draftu ---");

        // Pokaż finalne wyniki
        Platform.runLater(() -> {
            setupDraftGrid(brandsInDraft); // Odśwież widok siatki z odpowiednimi brandami
            // --- KONIEC POPRAWKI ---
            if (currentBundle != null) {
                showAlert(Alert.AlertType.INFORMATION,
                        currentBundle.getString("alert.draft.autoFinishComplete.title"),
                        currentBundle.getString("alert.draft.autoFinishComplete.content"));
            } else {
                showAlert(Alert.AlertType.INFORMATION, "Koniec Draftu", "Automatyczne dokańczanie zakończone!"); // Fallback
            }
            // --- POCZĄTEK POPRAWKI ---
            if (mainContentArea.getTop() != null) {
                String headerTitle = (currentBundle != null) ?
                        currentBundle.getString("draft.header.title.results") : // Pobierz z zasobów
                        "Wyniki Draftu"; // Fallback
                mainContentArea.setTop(createDraftHeader(headerTitle)); // Użyj przetłumaczonego tytułu
            }
            // --- KONIEC POPRAWKI ---
        });
    } // Koniec runAutoFinish

    // --- Metody Wyświetlania ---
    /**
     * Pokazuje widok wyników draftu dla podanej listy brandów.
     * @param draftedBrands Lista brandów, których wyniki mają być pokazane.
     */
    public void showDraftResultsView(List<Brand> draftedBrands) { // Publiczny dostęp
        if (draftedBrands == null || draftedBrands.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "draft.header.title.results", "Nie wybrano żadnych brandów do pokazania wyników.");
            showMainMenu(); // Wróć do menu
            return;
        }
        setupDraftGrid(draftedBrands); // Użyj setupDraftGrid do stworzenia siatki tylko dla tych brandów
        mainContentArea.setCenter(new ScrollPane(draftGrid)); // Umieść w ScrollPane
        String headerTitle = (currentBundle != null) ?
                currentBundle.getString("draft.header.title.results") :
                "draft.header.title.results"; // Fallback
        mainContentArea.setTop(createDraftHeader(headerTitle)); // // Dodaj nagłówek
    }

    /**
     * Wersja metody showDraftResultsView wywoływana z menu Widok.
     * Pokazuje wyniki dla wszystkich brandów, które mają jakiekolwiek przypisane osoby,
     * lub pokazuje wszystkie, jeśli draft się jeszcze nie odbył.
     */
    public void showDraftResultsView() { // Publiczny dostęp
        // Sprawdź, czy jakikolwiek draft się odbył
        boolean draftConducted = brandRosters.values().stream().anyMatch(list -> !list.isEmpty());
        List<Brand> brandsToShow;
        if (draftConducted) {
            // Pokaż tylko brandy, które mają kogoś w rosterze
            brandsToShow = allBrands.stream()
                    .filter(brand -> brandRosters.containsKey(brand) && !brandRosters.get(brand).isEmpty())
                    .collect(Collectors.toList());
            if (brandsToShow.isEmpty()) { // Na wszelki wypadek
                brandsToShow = new ArrayList<>(allBrands);
            }
        } else {
            // Jeśli draft się nie odbył, pokaż strukturę dla wszystkich brandów
            brandsToShow = new ArrayList<>(allBrands);
            // --- ZMIANA TUTAJ ---
            if (currentBundle != null) {
                showAlert(Alert.AlertType.INFORMATION,
                        currentBundle.getString("alert.results.nodraft.title"), // Tytuł z zasobów
                        currentBundle.getString("alert.results.nodraft.content")); // Treść z zasobów
            } else {
                showAlert(Alert.AlertType.INFORMATION,"draft.header.title.results", "Draft jeszcze się nie odbył."); // Fallback
            }
            // --- KONIEC ZMIANY ---
            // Można rozważyć 'return;' tutaj, jeśli nie chcesz pokazywać pustej siatki
        }
        // Wywołaj wersję z argumentem
        showDraftResultsView(brandsToShow);
    }

    public void showWrestlerList() { // Upewnij się, że jest public
        System.out.println("Próba załadowania wrestler-pools-view.fxml...");
        try {
            // Zawsze aktualizuj listy przed pokazaniem widoku
            updateCategoryLists();

            URL fxmlUrl = Main.class.getResource("/org/macko/wwedraft/wrestler-pools-view.fxml");
            if (fxmlUrl == null) throw new IOException("Nie znaleziono zasobu FXML: /org/macko/wwedraft/wrestler-pools-view.fxml");
            System.out.println("Znaleziono FXML pod URL: " + fxmlUrl);

            FXMLLoader loader = new FXMLLoader(fxmlUrl);

            // --- WAŻNE: Ustaw ResourceBundle PRZED load() ---
            if (currentBundle != null) { // Sprawdź, czy bundle został załadowany
                loader.setResources(currentBundle);
                System.out.println("Ustawiono ResourceBundle dla FXML wrestler-pools-view.");
            } else {
                System.err.println("BŁĄD: currentBundle jest null w showWrestlerList! Teksty FXML nie zostaną przetłumaczone.");
                // Można rzucić wyjątek lub użyć domyślnego języka
            }
            // --- KONIEC USTAWIANIA ResourceBundle ---

            Node poolsNode = loader.load(); // Ładuje FXML, teraz użyje zasobów do %kluczy

            WrestlerPoolsViewController controller = loader.getController();
            if (controller == null) throw new IllegalStateException("Kontroler dla wrestler-pools-view.fxml nie ustawiony w FXML!");

            // Przekaż referencję do Main i listy danych do kontrolera
            controller.setMainApp(this); // Przekaż instancję Main
            controller.populateLists(mainEventMidCard, lowCardJobbers, tagTeams); // Przekaż listy

            mainContentArea.setCenter(poolsNode); // Ustaw widok w centrum

            // --- ZMIANA: Użyj klucza dla tytułu nagłówka ---
            mainContentArea.setTop(createDraftHeader(currentBundle.getString("view.wrestlerPools.title")));
            // --- KONIEC ZMIANY ---

            // Wyczyść pozostałe regiony
            mainContentArea.setBottom(null); mainContentArea.setLeft(null); mainContentArea.setRight(null);
            System.out.println("Załadowano wrestler-pools-view.fxml pomyślnie.");

        } catch (IOException | IllegalStateException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Błąd FXML", "Nie można załadować widoku puli zawodników.\nBłąd: " + e.getMessage());
        } catch (MissingResourceException e) { // Złap błąd braku klucza
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Błąd Tłumaczeń", "Nie znaleziono klucza w pliku zasobów: " + e.getKey() + "\n" + e.getMessage());
        }
    }

    private ListView<Wrestler> createWrestlerListView(ObservableList<Wrestler> wrestlerList) { /* ... KOD Z POPRZEDNIEJ ODPOWIEDZI ... */
        ListView<Wrestler> listView = new ListView<>(wrestlerList);
        listView.setCellFactory(param -> new ListCell<Wrestler>() {
            private final ImageView imageView = new ImageView();
            private final HBox content = new HBox(10);
            private final Label label = new Label();
            {
                imageView.setFitHeight(40);
                imageView.setFitWidth(40);
                imageView.setPreserveRatio(true);
                label.setWrapText(true);
                content.getChildren().addAll(imageView, label);
                content.setAlignment(Pos.CENTER_LEFT);
            }
            @Override
            protected void updateItem(Wrestler wrestler, boolean empty) {
                super.updateItem(wrestler, empty);
                if (empty || wrestler == null) {
                    setText(null); setGraphic(null); setTooltip(null);
                } else {
                    label.setText(wrestler.getName());
                    setTooltip(new Tooltip(wrestler.toString()));
                    Image image = loadImageForWrestler(wrestler);
                    imageView.setImage(image);
                    setGraphic(content);
                    setText(null);
                }
            }
        });
        return listView;
    }

    /**
     * Ładuje obrazek dla wrestlera. Najpierw szuka w folderze zewnętrznym
     * (Dokumenty/WWE_Draft_Images), a jeśli nie znajdzie, szuka w zasobach wewnętrznych.
     * @param wrestler Obiekt Wrestler.
     * @return Obiekt Image lub null, jeśli plik nie istnieje nigdzie lub wystąpił błąd.
     */
    private Image loadImageForWrestler(Wrestler wrestler) {
        if (wrestler == null || wrestler.getImageFileName() == null || wrestler.getImageFileName().trim().isEmpty()) { return null; }
        String fileName = wrestler.getImageFileName().trim();

        // --- Ścieżka do folderu zewnętrznego ---
        String userDocuments = System.getProperty("user.home") + File.separator + "Documents";
        String imageFolderName = "WWE_Draft_Images";
        File externalImageFile = new File(userDocuments + File.separator + imageFolderName, fileName);

        // 1. Spróbuj załadować z folderu zewnętrznego
        if (externalImageFile.exists() && externalImageFile.isFile()) {
            try (FileInputStream inputStream = new FileInputStream(externalImageFile)) {
                System.out.println("Ładowanie obrazka (W) z pliku zewn.: " + externalImageFile.getAbsolutePath());
                return new Image(inputStream, 50, 50, true, true);
            } catch (IOException e) {
                System.err.println("Błąd odczytu zewn. obrazka '" + fileName + "': " + e.getMessage());
                // Nie zwracaj null jeszcze, spróbuj z zasobów wewnętrznych
            } catch (Exception e) { // Złap też inne możliwe błędy Image
                System.err.println("Inny błąd ładowania zewn. obrazka '" + fileName + "': " + e.getMessage());
            }
        }

        // 2. Jeśli nie znaleziono/błąd przy zewnętrznym, spróbuj załadować z zasobów wewnętrznych
        String resourcePath = "/images/" + fileName;
        try (InputStream imageStream = getClass().getResourceAsStream(resourcePath)) {
            if (imageStream != null) {
                System.out.println("Ładowanie obrazka (W) z zasobów wewn.: " + resourcePath);
                return new Image(imageStream, 50, 50, true, true);
            } else {
                System.err.println("Nie znaleziono obrazka (W) ani zewn. ani wewn.: " + fileName);
                return null; // Nie znaleziono nigdzie
            }
        } catch (Exception e) {
            System.err.println("Błąd ładowania obrazka (W) z zasobów wewn. '" + resourcePath + "': " + e.getMessage());
            return null; // Błąd ładowania z zasobów
        }
    }
    /**
     * Ładuje logo dla brandu. Najpierw szuka w folderze zewnętrznym, potem w zasobach.
     * @param brand Obiekt Brand.
     * @return Obiekt Image lub null.
     */
    private Image loadImageForBrand(Brand brand) {
        if (brand == null || brand.getLogoFileName() == null || brand.getLogoFileName().trim().isEmpty()) { return null; }
        String fileName = brand.getLogoFileName().trim();

        // Ścieżka do folderu zewnętrznego
        String userDocuments = System.getProperty("user.home") + File.separator + "Documents";
        String imageFolderName = "WWE_Draft_Images";
        File externalImageFile = new File(userDocuments + File.separator + imageFolderName, fileName);

        // 1. Spróbuj załadować z folderu zewnętrznego
        if (externalImageFile.exists() && externalImageFile.isFile()) {
            try (FileInputStream inputStream = new FileInputStream(externalImageFile)) {
                System.out.println("Ładowanie logo (B) z pliku zewn.: " + externalImageFile.getAbsolutePath());
                return new Image(inputStream, 60, 30, true, true); // Dostosuj rozmiar loga
            } catch (IOException e) {
                System.err.println("Błąd odczytu zewn. logo '" + fileName + "': " + e.getMessage());
            } catch (Exception e) {
                System.err.println("Inny błąd ładowania zewn. logo '" + fileName + "': " + e.getMessage());
            }
        }

        // 2. Spróbuj załadować z zasobów wewnętrznych
        String resourcePath = "/images/" + fileName;
        try (InputStream imageStream = getClass().getResourceAsStream(resourcePath)) {
            if (imageStream != null) {
                System.out.println("Ładowanie logo (B) z zasobów wewn.: " + resourcePath);
                return new Image(imageStream, 60, 30, true, true); // Dostosuj rozmiar loga
            } else {
                System.err.println("Nie znaleziono logo (B) ani zewn. ani wewn.: " + fileName);
                return null;
            }
        } catch (Exception e) {
            System.err.println("Błąd ładowania logo (B) z zasobów wewn. '" + resourcePath + "': " + e.getMessage());
            return null;
        }
    }

    private VBox createWrestlerCategoryVBox(String categoryName, ListView<Wrestler> listView) { /* ... KOD Z POPRZEDNIEJ ODPOWIEDZI ... */
        listView.setPrefHeight(400);
        Label label = new Label(categoryName);
        label.setStyle("-fx-font-weight: bold;");
        return new VBox(5, label, listView);
    }

    // DODANA Metoda pomocnicza do tworzenia komórki dla ComboBox
    public ListCell<Wrestler> createWrestlerListCellWithImage() { /* ... KOD Z POPRZEDNIEJ ODPOWIEDZI ... */
        return new ListCell<>() {
            private final ImageView imageView = new ImageView();
            private final Label label = new Label();
            private final HBox content = new HBox(10);
            {
                imageView.setFitHeight(40);
                imageView.setFitWidth(40);
                imageView.setPreserveRatio(true);
                content.getChildren().addAll(imageView, label);
                content.setAlignment(Pos.CENTER_LEFT);
            }
            @Override
            protected void updateItem(Wrestler wrestler, boolean empty) {
                super.updateItem(wrestler, empty);
                if (empty || wrestler == null) {
                    setText(null); setGraphic(null); setTooltip(null);
                } else {
                    label.setText(wrestler.toString()); // Pełny opis w ComboBox
                    Image image = loadImageForWrestler(wrestler);
                    imageView.setImage(image);
                    setGraphic(content);
                    setText(null);
                    setTooltip(new Tooltip(wrestler.toString()));
                }
            }
        };
    }


    public void showBrandList() { /* ... KOD Z POPRZEDNIEJ ODPOWIEDZI ... */
        Stage brandStage = new Stage();
        brandStage.initModality(Modality.WINDOW_MODAL);
        brandStage.initOwner(primaryStage);
        brandStage.setTitle("Lista Brandów");
        brandListView.setItems(allBrands);
        VBox brandLayout = new VBox(10, new Label("Dostępne brandy:"), brandListView);
        brandLayout.setPadding(new Insets(20));
        Scene brandScene = new Scene(brandLayout, 300, 300);
        brandStage.setScene(brandScene);
        brandStage.show();
    }


    // --- Metody obsługi Excela ---
    public void loadWrestlersFromExcel(Stage ownerStage) { /* ... KOD Z POPRZEDNIEJ ODPOWIEDZI (z poprawkami !isBlank na !trim().isEmpty) ... */
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Wybierz plik Excel z zawodnikami");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Pliki Excel", "*.xlsx"));
        File selectedFile = fileChooser.showOpenDialog(ownerStage);

        if (selectedFile == null) { return; }

        try (FileInputStream fis = new FileInputStream(selectedFile); Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();
            if (rowIterator.hasNext()) rowIterator.next();

            List<Wrestler> newlyLoaded = new ArrayList<>();

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                org.apache.poi.ss.usermodel.Cell nameCell = row.getCell(0, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                org.apache.poi.ss.usermodel.Cell categoryCell = row.getCell(1, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                org.apache.poi.ss.usermodel.Cell tagTeamCell = row.getCell(2, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                org.apache.poi.ss.usermodel.Cell imageCell = row.getCell(3, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);

                String name = getCellValueAsString(nameCell);
                String categoryStr = getCellValueAsString(categoryCell);
                String tagTeamName = getCellValueAsString(tagTeamCell);
                String imageName = getCellValueAsString(imageCell);

                if (name != null && !name.trim().isEmpty()) {
                    WrestlerCategory category = WrestlerCategory.fromString(categoryStr);
                    boolean isActuallyTagTeam = (tagTeamName != null && !tagTeamName.trim().isEmpty());

                    if (isActuallyTagTeam) {
                        category = WrestlerCategory.TAG_TEAM;
                    } else if (category == WrestlerCategory.TAG_TEAM) {
                        System.err.println("Pominięto wiersz " + (row.getRowNum() + 1) + ": Kategoria TAG_TEAM, ale brak nazwy Tag Teamu dla '" + name + "'.");
                        continue;
                    } else if (category == null) {
                        System.err.println("Pominięto wiersz " + (row.getRowNum() + 1) + ": Nieprawidłowa/brak kategorii dla zawodnika solo '" + name + "'. Odczytano: '" + categoryStr + "'");
                        continue;
                    }

                    Wrestler newWrestler;
                    if (category == WrestlerCategory.TAG_TEAM && tagTeamName != null && !tagTeamName.trim().isEmpty()) {
                        newWrestler = new Wrestler(name, category, tagTeamName, imageName);
                    } else if (category != WrestlerCategory.TAG_TEAM) {
                        newWrestler = new Wrestler(name, category, imageName);
                    } else {
                        System.err.println("Niespodziewany błąd logiki przy tworzeniu wrestlera (wiersz " + (row.getRowNum() + 1) + "): " + name + ", kat:" + category + ", tt:" + tagTeamName);
                        continue;
                    }

                    if (newlyLoaded.stream().noneMatch(w -> w.getName().equalsIgnoreCase(name))) {
                        newlyLoaded.add(newWrestler);
                    } else {
                        System.err.println("Pominięto wiersz " + (row.getRowNum() + 1) + ": Duplikat zawodnika '" + name + "' w tym pliku Excel.");
                    }
                } else if (row.getRowNum() > 0 && (categoryStr != null || tagTeamName != null || imageName != null)) {
                    System.err.println("Pominięto wiersz " + (row.getRowNum() + 1) + ": Brak nazwy zawodnika.");
                }
            }

            allWrestlers.setAll(newlyLoaded);
            updateCategoryLists();
            // --- POCZĄTEK WKLEJANEGO BLOKU ---
            // Użyj ResourceBundle i MessageFormat do stworzenia komunikatu
            if (currentBundle != null) { // Sprawdź, czy bundle został załadowany
                String title = currentBundle.getString("excel.load.success.title"); // Pobierz tytuł z properties
                String contentPattern = currentBundle.getString("excel.load.success.content"); // Pobierz wzorzec treści
                // Sformatuj treść, wstawiając liczbę i nazwę pliku
                String content = MessageFormat.format(contentPattern, allWrestlers.size(), selectedFile.getName());
                // Wywołaj alert z przetłumaczonymi tekstami
                showAlert(Alert.AlertType.INFORMATION, title, content);
            } else {
                // Komunikat awaryjny, jeśli bundle z jakiegoś powodu jest null
                showAlert(Alert.AlertType.INFORMATION, "Sukces", "Wczytano " + allWrestlers.size() + " zawodników.");
            }
            // --- KONIEC WKLEJANEGO BLOKU ---

            showWrestlerList(); // Ta linia zostaje bez zmian
        } catch (IOException e) { // Blok catch zostaje bez zmian
            // ...
            showWrestlerList();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Błąd Przetwarzania Pliku", "Wystąpił nieoczekiwany błąd: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String getCellValueAsString(org.apache.poi.ss.usermodel.Cell cell) { /* ... KOD Z POPRZEDNIEJ ODPOWIEDZI ... */
        if (cell == null) return null;
        DataFormatter formatter = new DataFormatter();
        String value = formatter.formatCellValue(cell).trim();
        return value.isEmpty() ? null : value;
    }

    public void saveDraftToExcel(Stage ownerStage) { /* ... KOD Z POPRZEDNIEJ ODPOWIEDZI ... */
        if (brandRosters.values().stream().allMatch(ObservableList::isEmpty)) {
            if (currentBundle != null) {
                showAlert(Alert.AlertType.WARNING,
                        currentBundle.getString("alert.export.nodata.title"),
                        currentBundle.getString("alert.export.nodata.content"));
            } else {
                showAlert(Alert.AlertType.WARNING, "Brak Wyników", "Brak danych do zapisania."); // Fallback
            }
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Zapisz wyniki draftu do pliku Excel");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Pliki Excel", "*.xlsx"));
        fileChooser.setInitialFileName("wyniki_draftu_" + java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(java.time.LocalDateTime.now()) + ".xlsx");
        File selectedFile = fileChooser.showSaveDialog(ownerStage);

        if (selectedFile == null) { return; }

        try (Workbook workbook = new XSSFWorkbook(); FileOutputStream fos = new FileOutputStream(selectedFile)) {

            // --- POPRAWIONA PĘTLA ---
            // Iteruj po liście WSZYSTKICH brandów (allBrands)
            for (Brand brand : allBrands) { // <-- Używamy allBrands i definiujemy 'brand'
                // Pobierz roster dla bieżącego brandu
                ObservableList<Wrestler> roster = brandRosters.getOrDefault(brand, FXCollections.observableArrayList());

                // Opcjonalnie: Pomijaj tworzenie arkusza dla brandów bez zawodników
                if (roster.isEmpty() && brandRosters.values().stream().anyMatch(list -> !list.isEmpty())) { // Nie pomijaj, jeśli cały draft jest pusty
                    continue; // Przejdź do następnego brandu, jeśli ten nie ma zawodników
                }


                Sheet sheet = workbook.createSheet(brand.getName()); // Nazwa arkusza = nazwa brandu
                Row headerRow = sheet.createRow(0);
                // Używamy pełnych nazw, aby uniknąć konfliktu
                org.apache.poi.ss.usermodel.Cell hCell1 = headerRow.createCell(0); hCell1.setCellValue("Zawodnik");
                org.apache.poi.ss.usermodel.Cell hCell2 = headerRow.createCell(1); hCell2.setCellValue("Kategoria");
                org.apache.poi.ss.usermodel.Cell hCell3 = headerRow.createCell(2); hCell3.setCellValue("Tag Team");

                int rowIndex = 1;
                for (Wrestler wrestler : roster) {
                    Row row = sheet.createRow(rowIndex++);
                    row.createCell(0).setCellValue(wrestler.getName());
                    row.createCell(1).setCellValue(wrestler.getCategory() != null ? wrestler.getCategory().getDisplayName() : "Brak");
                    row.createCell(2).setCellValue(wrestler.isTagTeam() && wrestler.getTagTeamName() != null ? wrestler.getTagTeamName() : "");
                }
                // Automatyczne dopasowanie szerokości kolumn
                sheet.autoSizeColumn(0);
                sheet.autoSizeColumn(1);
                sheet.autoSizeColumn(2);
            }
            // --- KONIEC POPRAWIONEJ PĘTLI ---

            workbook.write(fos); // Zapisz cały skoroszyt do pliku
            showAlert(Alert.AlertType.INFORMATION, "Sukces", "Wyniki draftu zapisane do pliku:\n" + selectedFile.getAbsolutePath());

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Błąd Zapisu Pliku", "Nie można zapisać pliku Excel: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Nieoczekiwany Błąd", "Wystąpił błąd podczas zapisu: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String content) { /* ... KOD Z POPRZEDNIEJ ODPOWIEDZI ... */
        Runnable alertTask = () -> {
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            if (primaryStage != null && primaryStage.isShowing()) {
                alert.initOwner(primaryStage);
            }
            alert.showAndWait();
        };
        if (Platform.isFxApplicationThread()) {
            alertTask.run();
        } else {
            Platform.runLater(alertTask);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
    }