module ee2.smortfridge {
    // Standard JavaFX modules
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.swing; // Added because it's in your POM
    requires javafx.media; // Added because it's in your POM

    // Third-party UI Libraries
    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    //requires org.kordamp.ikonli.javafx;
    //requires eu.hansolo.tilesfx;

    // Networking and Data
    requires java.net.http;
    requires org.json;
    requires java.desktop;
    requires google.genai;

    // Reflection for FXML to work
    opens ee2.smortfridge to javafx.fxml;

    // Exporting the package so JavaFX can launch it
    exports ee2.smortfridge;
}