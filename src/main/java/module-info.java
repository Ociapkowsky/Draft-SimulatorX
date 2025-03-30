module org.macko.wwedraft {
    requires javafx.controls;
    requires javafx.fxml; // WAŻNE DLA FXML
    requires org.apache.poi.ooxml;
    requires org.apache.poi.poi;
    requires javafx.graphics; // Dodane dla Image itp.

    opens org.macko.wwedraft to javafx.fxml; // Otwiera pakiet dla FXML
    exports org.macko.wwedraft; // Eksportuje pakiet
}