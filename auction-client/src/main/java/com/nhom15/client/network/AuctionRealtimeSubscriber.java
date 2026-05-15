package com.nhom15.client.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.function.Consumer;
import javafx.application.Platform;

/**
 * Kết nối TCP dài hạn: SUBSCRIBE_AUCTION rồi đọc các dòng {@code AUCTION_UPDATE}.
 *
 * <p><b>Độ tin cậy:</b> đọc có timeout + ping định kỳ (tránh proxy/NAT đóng idle); parse từng dòng
 * an toàn (một dòng lỗi không giết luồng); tự kết nối lại khi mất kết nối (trừ khi ACK từ chối).
 */
public final class AuctionRealtimeSubscriber {

  private static final int READ_TIMEOUT_MS     = 30_000;
  /**
   * FIX PERF: Giảm từ 900ms → 200ms.
   * 900ms là thời gian chờ reconnect khi mất kết nối SUBSCRIBE — nếu subscriber
   * bị ngắt giữa chừng và phải reconnect, client sẽ bị miss update trong ~900ms.
   * 200ms đủ để tránh reconnect storm nhưng nhanh hơn rõ rệt.
   */
  private static final int INITIAL_RECONNECT_MS = 200;
  private static final int MAX_RECONNECT_MS     = 12_000;

  private final Object lifecycleLock = new Object();
  private volatile Socket activeSocket;
  private volatile boolean runRequested;
  private Thread workerThread;

  public void start(int auctionId, Consumer<JsonObject> onPushFx) {
    stop();
    synchronized (lifecycleLock) {
      runRequested = true;
      workerThread =
          new Thread(() -> runWithReconnect(auctionId, onPushFx), "auction-live-" + auctionId);
      workerThread.setDaemon(true);
      workerThread.start();
    }
  }

  private void runWithReconnect(int auctionId, Consumer<JsonObject> onPushFx) {
    int backoff = INITIAL_RECONNECT_MS;
    while (runRequested) {
      SessionEnd reason = runSingleSession(auctionId, onPushFx);
      if (!runRequested) {
        break;
      }
      if (reason == SessionEnd.ACK_DENIED) {
        break;
      }
      if (reason == SessionEnd.STOPPED) {
        break;
      }
      try {
        Thread.sleep(backoff);
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        break;
      }
      backoff = Math.min((int) (backoff * 1.35), MAX_RECONNECT_MS);
    }
  }

  private enum SessionEnd {
    STOPPED,
    ACK_DENIED,
    DISCONNECTED
  }

  /**
   * Một lần mở socket tới khi đóng.
   *
   * @return {@link SessionEnd#ACK_DENIED} nếu server không chấp nhận SUBSCRIBE (không retry).
   */
  private SessionEnd runSingleSession(int auctionId, Consumer<JsonObject> onPushFx) {
    Socket s = new Socket();
    synchronized (lifecycleLock) {
      activeSocket = s;
    }
    try {
      s.connect(
          new InetSocketAddress(SocketClient.getServerHost(), SocketClient.getServerPort()),
          SocketClient.getConnectTimeoutMs());
      s.setTcpNoDelay(true);
      s.setSoTimeout(READ_TIMEOUT_MS);

      try (BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
           PrintWriter out = new PrintWriter(s.getOutputStream(), true)) {

        JsonObject req = new JsonObject();
        req.addProperty("action", "SUBSCRIBE_AUCTION");
        JsonObject data = new JsonObject();
        data.addProperty("auctionId", auctionId);
        req.add("data", data);
        out.println(req);
        out.flush();

        String ackLine;
        try {
          ackLine = in.readLine();
        } catch (SocketTimeoutException e) {
          return SessionEnd.DISCONNECTED;
        }
        if (ackLine == null) {
          return SessionEnd.DISCONNECTED;
        }
        JsonObject ack;
        try {
          ack = JsonParser.parseString(ackLine.trim()).getAsJsonObject();
        } catch (JsonSyntaxException e) {
          return SessionEnd.ACK_DENIED;
        }
        if (!ack.has("status") || !"SUCCESS".equals(ack.get("status").getAsString())) {
          return SessionEnd.ACK_DENIED;
        }

        while (runRequested) {
          String line;
          try {
            line = in.readLine();
          } catch (SocketTimeoutException e) {
            JsonObject ping = new JsonObject();
            ping.addProperty("action", "SUBSCRIBE_PING");
            out.println(ping);
            out.flush();
            continue;
          }
          if (line == null) {
            return SessionEnd.DISCONNECTED;
          }
          String trimmed = line.trim();
          if (trimmed.isEmpty()) {
            continue;
          }
          JsonObject msg;
          try {
            msg = JsonParser.parseString(trimmed).getAsJsonObject();
          } catch (JsonSyntaxException e) {
            continue;
          }
          if (!msg.has("action") || !"AUCTION_UPDATE".equals(msg.get("action").getAsString())) {
            continue;
          }
          JsonObject copy = JsonParser.parseString(msg.toString()).getAsJsonObject();
          Platform.runLater(() -> onPushFx.accept(copy));
        }
        return SessionEnd.STOPPED;
      }
    } catch (SocketException e) {
      return runRequested ? SessionEnd.DISCONNECTED : SessionEnd.STOPPED;
    } catch (IOException e) {
      return runRequested ? SessionEnd.DISCONNECTED : SessionEnd.STOPPED;
    } catch (Exception e) {
      System.err.println("[AuctionRealtime] " + e.getMessage());
      return SessionEnd.DISCONNECTED;
    } finally {
      synchronized (lifecycleLock) {
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
    synchronized (lifecycleLock) {
      runRequested = false;
    }
    Socket s;
    synchronized (lifecycleLock) {
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
    Thread t;
    synchronized (lifecycleLock) {
      t = workerThread;
    }
    if (t != null) {
      t.interrupt();
      try {
        t.join(2000);
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
      }
    }
    synchronized (lifecycleLock) {
      workerThread = null;
    }
  }
}
