package com.nhom15.client.controller;

import com.nhom15.client.command.CheckUsernameCommand;
import com.nhom15.client.command.RegisterCommand;
import com.nhom15.client.command.ServerCommand;
import com.nhom15.client.util.FormValidator;
import com.nhom15.client.util.ViewManager;
import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

/**
 * RegisterController — SRP: chỉ xử lý UI đăng ký.
 *
 * Không tự build JsonObject, không tự FXMLLoader, không tự lấy Stage.
 * Dùng: CheckUsernameCommand + RegisterCommand (gửi server),
 *        ViewManager (điều hướng), FormValidator (validate realtime).
 */
public class RegisterController {

    @FXML private Pane          bgAnimationPane;

    @FXML private TextField     txtUsername;
    @FXML private TextField     txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;

    @FXML private Label lblUsernameError;
    @FXML private Label lblEmailError;
    @FXML private Label lblPasswordError;
    @FXML private Label lblConfirmError;

    @FXML private Button btnRegister;

    // ── Trạng thái nội bộ ────────────────────────────────────────────────────
    private PauseTransition usernameCheckDelay;
    private boolean         isUsernameAvailable = true;

    // ── Khởi tạo ────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        bindManagedToVisible(lblUsernameError, lblEmailError,
                lblPasswordError, lblConfirmError);

        // Validate realtime bằng FormValidator (giống LoginController)
        FormValidator.bindRegex(txtEmail, lblEmailError,
                "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$",
                "Email không hợp lệ!");
        FormValidator.bindRegex(txtPassword, lblPasswordError,
                "^(?=.*[A-Za-z])(?=.*\\d).{6,}$",
                "Mật khẩu phải từ 6 ký tự, bao gồm cả chữ lẫn số!");

        setupUsernameDebounce();
        loadBackground();
    }

    /** Debounce 600ms sau khi ngừng gõ → gửi CheckUsernameCommand. */
    private void setupUsernameDebounce() {
        usernameCheckDelay = new PauseTransition(Duration.millis(600));
        usernameCheckDelay.setOnFinished(e -> checkUsernameAsync());

        txtUsername.textProperty().addListener((obs, oldVal, newVal) -> {
            lblUsernameError.setVisible(false);
            isUsernameAvailable = true;
            usernameCheckDelay.stop();
            if (!newVal.trim().isEmpty()) {
                usernameCheckDelay.playFromStart();
            }
        });
    }

    // ── Xử lý sự kiện ───────────────────────────────────────────────────────

    @FXML
    private void handleRegister(ActionEvent event) {
        if (!validateAll()) return;

        setLoading(true);

        new RegisterCommand(
                txtUsername.getText().trim(),
                txtEmail.getText().trim(),
                txtPassword.getText()
        ).executeAsync(
                response -> onRegisterResponse(response),
                ()       -> {
                    setLoading(false);
                    showAlert(Alert.AlertType.ERROR,
                            "Lỗi kết nối", "Không thể kết nối đến Server!");
                }
        );
    }

    @FXML
    private void handleBackToLogin(ActionEvent event) {
        ViewManager.navigateTo(ViewManager.Views.LOGIN);
    }

    // ── Callbacks ────────────────────────────────────────────────────────────

    private void checkUsernameAsync() {
        String username = txtUsername.getText().trim();
        if (username.isEmpty()) return;

        new CheckUsernameCommand(username).executeAsync(
                response -> {
                    // Bỏ qua nếu người dùng đã gõ tiếp trong lúc chờ
                    if (!txtUsername.getText().trim().equals(username)) return;

                    if (CheckUsernameCommand.isAvailable(response)) {
                        isUsernameAvailable = true;
                        lblUsernameError.setVisible(false);
                    } else {
                        isUsernameAvailable = false;
                        lblUsernameError.setText("Tên đăng nhập đã tồn tại!");
                        lblUsernameError.setVisible(true);
                    }
                },
                () -> { /* Lỗi kết nối khi check — im lặng, không chặn UX */ }
        );
    }

    private void onRegisterResponse(com.google.gson.JsonObject response) {
        setLoading(false);
        if (ServerCommand.isSuccess(response)) {
            showAlert(Alert.AlertType.INFORMATION,
                    "Thành công", "Đăng ký thành công! Vui lòng đăng nhập.");
            ViewManager.navigateTo(ViewManager.Views.LOGIN);
        } else {
            // Server báo username đã tồn tại (race condition sau check debounce)
            isUsernameAvailable = false;
            lblUsernameError.setText("Tên đăng nhập đã tồn tại!");
            lblUsernameError.setVisible(true);
        }
    }

    // ── Validate ─────────────────────────────────────────────────────────────

    /**
     * Kiểm tra toàn bộ form trước khi submit.
     * Trả về false nếu bất kỳ điều kiện nào không thoả.
     */
    private boolean validateAll() {
        boolean ok = true;

        if (txtUsername.getText().trim().isEmpty()) {
            lblUsernameError.setText("Vui lòng nhập tên đăng nhập!");
            lblUsernameError.setVisible(true);
            ok = false;
        }
        if (txtEmail.getText().trim().isEmpty()) {
            lblEmailError.setText("Vui lòng nhập email!");
            lblEmailError.setVisible(true);
            ok = false;
        }
        if (txtPassword.getText().isEmpty()) {
            lblPasswordError.setText("Vui lòng nhập mật khẩu!");
            lblPasswordError.setVisible(true);
            ok = false;
        }
        if (txtConfirmPassword.getText().isEmpty()) {
            lblConfirmError.setText("Vui lòng xác nhận mật khẩu!");
            lblConfirmError.setVisible(true);
            ok = false;
        }

        // Dừng sớm nếu có trường trống
        if (!ok) return false;

        // Kiểm tra các lỗi realtime từ FormValidator còn hiển thị không
        if (lblEmailError.isVisible() || lblPasswordError.isVisible()) return false;

        // Username chưa available (debounce chưa xong hoặc đã tồn tại)
        if (!isUsernameAvailable) return false;

        // Confirm password
        if (!txtPassword.getText().equals(txtConfirmPassword.getText())) {
            lblConfirmError.setText("Mật khẩu không khớp!");
            lblConfirmError.setVisible(true);
            return false;
        }

        return true;
    }

    // ── Tiện ích UI ──────────────────────────────────────────────────────────

    private void setLoading(boolean loading) {
        btnRegister.setDisable(loading);
        btnRegister.setText(loading ? "Đang xử lý..." : "XÁC NHẬN ĐĂNG KÝ");
    }

    private void loadBackground() {
        try {
            Pane sharedBg = com.nhom15.client.util.BackgroundEngine.getSharedPane();
            if (sharedBg.getParent() instanceof Pane p) p.getChildren().remove(sharedBg);
            if (bgAnimationPane != null) {
                bgAnimationPane.getChildren().add(0, sharedBg);
                sharedBg.prefWidthProperty().bind(bgAnimationPane.widthProperty());
                sharedBg.prefHeightProperty().bind(bgAnimationPane.heightProperty());
            }
        } catch (Exception e) {
            System.err.println("[RegisterController] loadBackground: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }

    /** Binding managedProperty vào visibleProperty cho nhiều label cùng lúc. */
    private void bindManagedToVisible(Label... labels) {
        for (Label lbl : labels) lbl.managedProperty().bind(lbl.visibleProperty());
    }
}