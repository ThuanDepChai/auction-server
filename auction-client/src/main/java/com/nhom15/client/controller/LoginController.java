package com.nhom15.client.controller;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

public class LoginController {

    @FXML private AnchorPane rootPane;
    @FXML private StackPane scaleContainer;
    @FXML private StackPane contentWrapper;

    @FXML private Circle orb1;
    @FXML private Circle orb2;

    private final double BASE_WIDTH = 420;
    private final double BASE_HEIGHT = 620;

    @FXML
    public void initialize() {

        // 🎯 Responsive scale
        rootPane.widthProperty().addListener((obs, oldVal, newVal) -> scaleUI());
        rootPane.heightProperty().addListener((obs, oldVal, newVal) -> scaleUI());

        // 🎯 Floating orbs
        animateOrb(orb1, 60, 6);
        animateOrb(orb2, -80, 8);

        // 🎯 Bind orb position theo size màn hình
        orb1.centerXProperty().bind(rootPane.widthProperty().multiply(0.2));
        orb1.centerYProperty().bind(rootPane.heightProperty().multiply(0.2));

        orb2.centerXProperty().bind(rootPane.widthProperty().multiply(0.8));
        orb2.centerYProperty().bind(rootPane.heightProperty().multiply(0.7));
    }

    private void scaleUI() {
        double scaleX = rootPane.getWidth() / BASE_WIDTH;
        double scaleY = rootPane.getHeight() / BASE_HEIGHT;

        double scale = Math.min(scaleX, scaleY);

        scaleContainer.setScaleX(scale);
        scaleContainer.setScaleY(scale);
    }

    private void animateOrb(Circle orb, double distance, int duration) {
        TranslateTransition tt = new TranslateTransition(Duration.seconds(duration), orb);
        tt.setByY(distance);
        tt.setAutoReverse(true);
        tt.setCycleCount(Animation.INDEFINITE);
        tt.play();
    }

    @FXML
    private void handleLogin() {
        System.out.println("Login...");
    }
}