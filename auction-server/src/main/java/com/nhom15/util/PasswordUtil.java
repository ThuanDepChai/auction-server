package com.nhom15.util;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {

  // Mã hóa mật khẩu
  public static String hashPassword(String plainPassword) {
    return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12)); // độ phức tạp là const = 12
  }

  // Kiểm tra mật khẩu
  public static boolean verifyPassword(String plainPassword, String hashedPassword) {
    return BCrypt.checkpw(plainPassword, hashedPassword);
  }
}
