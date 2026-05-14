package com.nhom15.client.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.function.Consumer;
import javafx.application.Platform;

/**
 * Một kết nối TCP dài hạn tới server: SUBSCRIBE_AUCTION rồi đọc các dòng {@code AUCTION_UPDATE}.
 * Phải {@link #stop()} khi rời phòng đấu giá.
 */
public final class AuctionRealtimeSubscriber {

  private final Object socketLock = new Object();
  private volatile Socket activeSocket;

  public void start(int auctionId, Consumer<JsonObject> onPushFx) {
    stop();
    Thread t = new Thread(() -> runLoop(auctionId, onPushFx), "auction-live-" + auctionId);
    t.setDaemon(true);
    t.start();
  }

  private void runLoop(int auctionId, Consumer<JsonObject> onPushFx) {
    Socket s = new Socket();
    synchronized (socketLock) {
      activeSocket = s;
    }
    try {
      s.connect(
          new InetSocketAddress(SocketClient.getServerHost(), SocketClient.getServerPort()),
          SocketClient.getConnectTimeoutMs());
      s.setSoTimeout(0);

      try (BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
           PrintWriter out = new PrintWriter(s.getOutputStream(), true)) {

        JsonObject req = new JsonObject();
        req.addProperty("action", "SUBSCRIBE_AUCTION");
        JsonObject data = new JsonObject();
        data.addProperty("auctionId", auctionId);
        req.add("data", data);
        out.println(req);

        String ackLine = in.readLine();
        if (ackLine == null) {
          return;
        }
        JsonObject ack = JsonParser.parseString(ackLine).getAsJsonObject();
        if (!ack.has("status") || !"SUCCESS".equals(ack.get("status").getAsString())) {
          return;
        }

        String line;
        while ((line = in.readLine()) != null) {
          JsonObject msg = JsonParser.parseString(line).getAsJsonObject();
          if (!msg.has("action") || !"AUCTION_UPDATE".equals(msg.get("action").getAsString())) {
            continue;
          }
          JsonObject copy = JsonParser.parseString(msg.toString()).getAsJsonObject();
          Platform.runLater(() -> onPushFx.accept(copy));
        }
      }
    } catch (IOException ignored) {
      // ngắt kết nối / đóng socket
    } catch (Exception e) {
      System.err.println("[AuctionRealtime] " + e.getMessage());
    } finally {
      synchronized (socketLock) {
        if (activeSocket == s) {
          activeSocket = null;
        }
      }
      try {
        s.close();
      } catch (IOException ignored) {
        // ignore
      }
    }
  }

  public void stop() {
    Socket s;
    synchronized (socketLock) {
      s = activeSocket;
      activeSocket = null;
    }
    if (s != null) {
      try {
        s.close();
      } catch (IOException ignored) {
        // ignore
      }
    }
  }
}
