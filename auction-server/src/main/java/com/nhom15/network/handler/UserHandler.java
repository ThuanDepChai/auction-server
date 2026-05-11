package com.nhom15.network.handler;

import com.google.gson.JsonObject;
import com.nhom15.controller.UserController;

public class UserHandler {
  private final UserController userController = new UserController();

  public JsonObject handle(JsonObject request) {
    String action = request.get("action").getAsString();
    JsonObject d = request.has("data") ? request.getAsJsonObject("data") : new JsonObject();

    return switch (action) {
      case "CHECK_USERNAME"    -> userController.handleCheckUsername(d);
      case "REGISTER"          -> userController.handleRegister(d);
      case "LOGIN"             -> userController.handleLogin(d);
      case "GET_PROFILE"       -> userController.handleGetProfile(d);
      case "UPDATE_PROFILE"    -> userController.handleUpdateProfile(d);
      case "CHANGE_PASSWORD"   -> userController.handleChangePassword(d);
      case "UPGRADE_TO_SELLER" -> userController.handleUpgradeToSeller(d);
      case "UPDATE_AVATAR"     -> userController.handleUpdateAvatar(d);
      case "GET_AVATAR"        -> userController.handleGetAvatar(d);
      default -> {
        JsonObject err = new JsonObject();
        err.addProperty("status", "ERROR");
        err.addProperty("message", "Action không hợp lệ");
        yield err;
      }
    };
  }
}