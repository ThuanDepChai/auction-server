package com.nhom15.client.util;

import javafx.animation.PauseTransition;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.util.Duration;

public class FormValidator {

    // Hàm 1: Kiểm tra định dạng bằng Regex (Dùng cho Email, Password...)
    public static void bindRegex(TextField textField, Label errorLabel, String regex, String errorMsg) {
        PauseTransition pause = new PauseTransition(Duration.seconds(1.2));
        errorLabel.setVisible(false); // Ban đầu ẩn nhãn lỗi
        errorLabel.setText(errorMsg);

        textField.textProperty().addListener((obs, oldVal, newVal) -> {
            errorLabel.setVisible(false); // Người dùng đang gõ -> Tạm cất lỗi đi
            pause.setOnFinished(e -> {
                // Đợi 1.2s, nếu ô có chữ VÀ chữ đó không khớp định dạng -> Báo lỗi
                if (!newVal.trim().isEmpty() && !newVal.matches(regex)) {
                    errorLabel.setVisible(true);
                }
            });
            pause.playFromStart();
        });
    }

    // Hàm 2: Kiểm tra 2 ô có giống nhau không (Dùng cho Xác nhận mật khẩu)
    public static void bindMatch(TextField txtSource, TextField txtTarget, Label errorLabel, String errorMsg) {
        PauseTransition pause = new PauseTransition(Duration.seconds(1.0));
        errorLabel.setVisible(false);
        errorLabel.setText(errorMsg);

        txtTarget.textProperty().addListener((obs, oldVal, newVal) -> {
            errorLabel.setVisible(false);
            pause.setOnFinished(e -> {
                if (!newVal.isEmpty() && !newVal.equals(txtSource.getText())) {
                    errorLabel.setVisible(true);
                }
            });
            pause.playFromStart();
        });
    }
}
