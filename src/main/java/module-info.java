module com.mycompany.exemplojogobeatemup {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.mycompany.exemplojogobeatemup to javafx.fxml;
    exports com.mycompany.exemplojogobeatemup;
}
