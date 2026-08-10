package ee2.smortfridge;

import javafx.application.Application;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import javafx.event.EventHandler;

public class Launcher {
    public static void main(String[] args) {
        Application.launch(UI.class, args);
    }
}
