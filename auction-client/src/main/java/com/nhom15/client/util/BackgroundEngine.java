package com.nhom15.client.util;

import javafx.animation.AnimationTimer;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BackgroundEngine {
    private static Pane sharedPane = new Pane();
    private static List<Particle> particles = new ArrayList<>();
    private static boolean isRunning = false;

    public static Pane getSharedPane() {
        if (!isRunning) {
            startEngine();
        }
        return sharedPane;
    }

    private static void startEngine() {
        int numParticles = 60;
        Random random = new Random();
        sharedPane.getChildren().clear();

        for (int i = 0; i < numParticles; i++) {
            Color color = (i % 3 == 0) ? Color.web("#4285F4", 0.4) :
                    (i % 3 == 1) ? Color.web("#9B72CB", 0.4) : Color.web("#D96570", 0.4);

            Circle circle = new Circle(random.nextDouble() * 5 + 2, color);
            sharedPane.getChildren().add(circle);
            particles.add(new Particle(circle, random));
        }

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double width = sharedPane.getWidth();
                double height = sharedPane.getHeight();
                if (width <= 0 || height <= 0) return;

                for (Particle p : particles) {
                    p.update(width, height);
                }
            }
        };
        timer.start();
        isRunning = true;
    }

    private static class Particle {
        Circle circle;
        double speedX, speedY;

        public Particle(Circle circle, Random random) {
            this.circle = circle;
            this.speedX = (random.nextDouble() - 0.5) * 1.2;
            this.speedY = (random.nextDouble() - 0.5) * 1.2;
            // Đặt vị trí ban đầu ngẫu nhiên
            circle.setCenterX(random.nextDouble() * 1000);
            circle.setCenterY(random.nextDouble() * 700);
        }

        public void update(double maxWidth, double maxHeight) {
            double x = circle.getCenterX() + speedX;
            double y = circle.getCenterY() + speedY;

            if (x <= 0 || x >= maxWidth) speedX *= -1;
            if (y <= 0 || y >= maxHeight) speedY *= -1;

            circle.setCenterX(x);
            circle.setCenterY(y);
        }
    }
}