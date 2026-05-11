package com.nhom15.service;

import com.nhom15.dao.UserDAO;
import com.nhom15.model.user.User;
import com.nhom15.util.PasswordUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho {@link UserService} – nghiệp vụ đăng nhập / đăng ký.
 *
 * <p>Chiến lược mock:
 * <ul>
 *   <li>{@code @Mock UserDAO} – giả lập tầng DAO, không chạm database.</li>
 *   <li>{@code MockedStatic<PasswordUtil>} – mock static method hash/verify.</li>
 *   <li>Inject qua package-private constructor {@code new UserService(userDAO)}.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService – Đăng nhập / Đăng ký")
class UserServiceTest {

  // ── Mock tầng DAO ─────────────────────────────────────────────────────
  @Mock
  private UserDAO userDAO;

  // ── Instance cần test ─────────────────────────────────────────────────
  private UserService userService;

  // ── Dữ liệu dùng chung ───────────────────────────────────────────────
  private static final String USERNAME      = "nguyenvana";
  private static final String PASSWORD_RAW  = "Abcd@1234";
  private static final String PASSWORD_HASH = "$2a$10$hashedValueExample";
  private static final String EMAIL         = "vana@email.com";

  @BeforeEach
  void setUp() {
    // Dùng package-private constructor để inject mock UserDAO
    userService = new UserService(userDAO);
  }

  // ======================================================================
  //  NHÓM 1: register()
  // ======================================================================
  @Nested
  @DisplayName("register() – Đăng ký tài khoản")
  class RegisterTests {

    @Test
    @DisplayName("✅ Đăng ký thành công khi username chưa tồn tại")
    void register_whenUsernameNotExists_returnsTrue() {
      // ARRANGE
      when(userDAO.isUsernameExists(USERNAME)).thenReturn(false);
      when(userDAO.registerUser(eq(USERNAME), anyString(), eq(EMAIL)))
        .thenReturn(true);

      // Mock static PasswordUtil.hashPassword()
      try (MockedStatic<PasswordUtil> mockedUtil = mockStatic(PasswordUtil.class)) {
        mockedUtil.when(() -> PasswordUtil.hashPassword(PASSWORD_RAW))
          .thenReturn(PASSWORD_HASH);

        // ACT
        boolean result = userService.register(USERNAME, PASSWORD_RAW, EMAIL);

        // ASSERT
        assertTrue(result, "Đăng ký phải trả về true khi thành công");

        // Đảm bảo password được hash trước khi lưu (KHÔNG lưu plain text)
        verify(userDAO).registerUser(USERNAME, PASSWORD_HASH, EMAIL);
      }
    }

    @Test
    @DisplayName("❌ Đăng ký thất bại khi username đã tồn tại")
    void register_whenUsernameAlreadyExists_returnsFalse() {
      // ARRANGE – username đã có trong DB
      when(userDAO.isUsernameExists(USERNAME)).thenReturn(true);

      // ACT
      boolean result = userService.register(USERNAME, PASSWORD_RAW, EMAIL);

      // ASSERT
      assertFalse(result, "Đăng ký phải trả về false khi username trùng");

      // Đảm bảo KHÔNG gọi registerUser khi username đã tồn tại
      verify(userDAO, never()).registerUser(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("❌ Đăng ký thất bại khi DAO lỗi (DB exception)")
    void register_whenDAOFails_returnsFalse() {
      // ARRANGE
      when(userDAO.isUsernameExists(USERNAME)).thenReturn(false);
      when(userDAO.registerUser(eq(USERNAME), anyString(), eq(EMAIL)))
        .thenReturn(false); // DB insert thất bại

      try (MockedStatic<PasswordUtil> mockedUtil = mockStatic(PasswordUtil.class)) {
        mockedUtil.when(() -> PasswordUtil.hashPassword(PASSWORD_RAW))
          .thenReturn(PASSWORD_HASH);

        // ACT
        boolean result = userService.register(USERNAME, PASSWORD_RAW, EMAIL);

        // ASSERT
        assertFalse(result, "Phải trả về false khi DAO thất bại");
      }
    }
  }

  // ======================================================================
  //  NHÓM 2: login()
  // ======================================================================
  @Nested
  @DisplayName("login() – Đăng nhập")
  class LoginTests {

    private User mockUser;

    @BeforeEach
    void setUpUser() {
      // User là abstract class → dùng mock thay vì new User()
      mockUser = mock(User.class);
      when(mockUser.getUsername()).thenReturn(USERNAME);
      when(mockUser.getPasswordHash()).thenReturn(PASSWORD_HASH);
    }

    @Test
    @DisplayName("✅ Đăng nhập thành công với đúng username và password")
    void login_withCorrectCredentials_returnsUser() {
      // ARRANGE
      when(userDAO.findByUsername(USERNAME)).thenReturn(mockUser);

      try (MockedStatic<PasswordUtil> mockedUtil = mockStatic(PasswordUtil.class)) {
        mockedUtil.when(() -> PasswordUtil.verifyPassword(PASSWORD_RAW, PASSWORD_HASH))
          .thenReturn(true);

        // ACT
        User result = userService.login(USERNAME, PASSWORD_RAW);

        // ASSERT
        assertNotNull(result, "Đăng nhập thành công phải trả về User, không phải null");
        assertEquals(USERNAME, result.getUsername());
      }
    }

    @Test
    @DisplayName("❌ Đăng nhập thất bại khi sai mật khẩu")
    void login_withWrongPassword_returnsNull() {
      // ARRANGE
      when(userDAO.findByUsername(USERNAME)).thenReturn(mockUser);

      try (MockedStatic<PasswordUtil> mockedUtil = mockStatic(PasswordUtil.class)) {
        mockedUtil.when(() -> PasswordUtil.verifyPassword("SaiMatKhau", PASSWORD_HASH))
          .thenReturn(false); // password không khớp

        // ACT
        User result = userService.login(USERNAME, "SaiMatKhau");

        // ASSERT
        assertNull(result, "Đăng nhập sai mật khẩu phải trả về null");
      }
    }

    @Test
    @DisplayName("❌ Đăng nhập thất bại khi username không tồn tại")
    void login_withNonExistentUsername_returnsNull() {
      // ARRANGE – DAO trả về null khi không tìm thấy user
      when(userDAO.findByUsername("khongtontai")).thenReturn(null);

      // ACT
      User result = userService.login("khongtontai", PASSWORD_RAW);

      // ASSERT
      assertNull(result, "Username không tồn tại phải trả về null");

      // Đảm bảo KHÔNG gọi verifyPassword khi user không tồn tại
      // (tránh NullPointerException)
      try (MockedStatic<PasswordUtil> mockedUtil = mockStatic(PasswordUtil.class)) {
        mockedUtil.verify(
          () -> PasswordUtil.verifyPassword(anyString(), anyString()),
          never()
        );
      }
    }
  }

  // ======================================================================
  //  NHÓM 3: changePassword()
  // ======================================================================
  @Nested
  @DisplayName("changePassword() – Đổi mật khẩu")
  class ChangePasswordTests {

    private User mockUser;

    @BeforeEach
    void setUpUser() {
      // User là abstract class → dùng mock thay vì new User()
      mockUser = mock(User.class);
      when(mockUser.getPasswordHash()).thenReturn(PASSWORD_HASH);
    }

    @Test
    @DisplayName("✅ Đổi mật khẩu thành công khi mật khẩu cũ đúng")
    void changePassword_withCorrectOldPassword_returnsTrue() {
      // ARRANGE
      String newPassword     = "NewPass@5678";
      String newPasswordHash = "$2a$10$newHashedValue";

      when(userDAO.findById(1)).thenReturn(mockUser);
      when(userDAO.updatePassword(eq(1), eq(newPasswordHash))).thenReturn(true);

      try (MockedStatic<PasswordUtil> mockedUtil = mockStatic(PasswordUtil.class)) {
        mockedUtil.when(() -> PasswordUtil.verifyPassword(PASSWORD_RAW, PASSWORD_HASH))
          .thenReturn(true);
        mockedUtil.when(() -> PasswordUtil.hashPassword(newPassword))
          .thenReturn(newPasswordHash);

        // ACT
        boolean result = userService.changePassword(1, PASSWORD_RAW, newPassword);

        // ASSERT
        assertTrue(result);
        verify(userDAO).updatePassword(1, newPasswordHash);
      }
    }

    @Test
    @DisplayName("❌ Đổi mật khẩu thất bại khi mật khẩu cũ sai")
    void changePassword_withWrongOldPassword_returnsFalse() {
      // ARRANGE
      when(userDAO.findById(1)).thenReturn(mockUser);

      try (MockedStatic<PasswordUtil> mockedUtil = mockStatic(PasswordUtil.class)) {
        mockedUtil.when(() -> PasswordUtil.verifyPassword("SaiPass", PASSWORD_HASH))
          .thenReturn(false);

        // ACT
        boolean result = userService.changePassword(1, "SaiPass", "NewPass@5678");

        // ASSERT
        assertFalse(result);
        // Đảm bảo KHÔNG cập nhật password khi xác thực thất bại
        verify(userDAO, never()).updatePassword(anyInt(), anyString());
      }
    }

    @Test
    @DisplayName("❌ Đổi mật khẩu thất bại khi userId không tồn tại")
    void changePassword_whenUserNotFound_returnsFalse() {
      // ARRANGE
      when(userDAO.findById(999)).thenReturn(null);

      // ACT
      boolean result = userService.changePassword(999, PASSWORD_RAW, "NewPass@5678");

      // ASSERT
      assertFalse(result);
      verify(userDAO, never()).updatePassword(anyInt(), anyString());
    }
  }
}