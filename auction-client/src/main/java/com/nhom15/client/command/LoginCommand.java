package com.nhom15.client.command;

import com.google.gson.JsonObject;

/**
 * LoginCommand — đóng gói request đăng nhập. Kế thừa ServerCommand: chỉ cần buildRequest(), mọi thứ
 * còn lại do base xử lý.
 */
public class LoginCommand extends ServerCommand {

  private final String username;
  private final String email;
  private final String password;

  public LoginCommand(String username, String email, String password) {
    this.username = username;
    this.email = email;
    this.password = password;
  }

  @Override
  protected JsonObject buildRequest() {
    JsonObject data = new JsonObject();
    data.addProperty("username", username);
    data.addProperty("email", email);
    data.addProperty("password", password);

    JsonObject req = new JsonObject();
    req.addProperty("action", "LOGIN");
    req.add("data", data);
    return req;
  }
}