package com.nhom15.client.util;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * FormValidator — validate realtime khi người dùng rời khỏi field. Controller chỉ gọi bindRegex()
 * trong initialize(), không tự viết listener.
 */
public final class FormValidator {

  private FormValidator() {
  }

  /**
   * Gắn listener vào TextField: khi mất focus, kiểm tra regex. Nếu không khớp → hiện label lỗi với
   * message cho trước. Nếu khớp → ẩn label lỗi.
   *
   * @param field    TextField cần validate
   * @param errLabel Label hiển thị lỗi (cần bind managed = visible trước)
   * @param regex    Regex hợp lệ
   * @param message  Thông báo lỗi khi không khớp
   */
  public static void bindRegex(TextField field, Label errLabel,
      String regex, String message) {
    field.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
      if (!isFocused) {
        String val = field.getText().trim();
        boolean valid = !val.isEmpty() && val.matches(regex);
        errLabel.setText(valid ? "" : message);
        errLabel.setVisible(!val.isEmpty() && !valid);
      }
    });
  }

  /**
   * Validate thủ công không cần focus event. Trả về true nếu hợp lệ.
   */
  public static boolean validate(TextField field, Label errLabel,
      String regex, String message) {
    String val = field.getText().trim();
    boolean valid = !val.isEmpty() && val.matches(regex);
    if (!valid) {
      errLabel.setText(message);
      errLabel.setVisible(true);
    } else {
      errLabel.setVisible(false);
    }
    return valid;
  }
}