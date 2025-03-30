package org.macko.wwedraft; //Dodaj deklarację pakietu

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class TestPOI {
    public static void main(String[] args) {
        try {
            XSSFWorkbook workbook = new XSSFWorkbook(); // Utwórz pusty skoroszyt
            System.out.println("Apache POI działa poprawnie!");
            workbook.close(); // Zamknij (nawet pusty)
        } catch (Exception e) {
            System.err.println("Błąd: " + e.getMessage());
            e.printStackTrace(); // Wyświetl szczegółowy błąd
        }
    }
}