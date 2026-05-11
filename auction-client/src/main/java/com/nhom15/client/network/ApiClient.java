package com.nhom15.client.network;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ApiClient {

  // Tạo sẵn một HttpClient dùng chung cho toàn bộ ứng dụng
  private static final HttpClient httpClient = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(10)) // Chờ tối đa 10 giây nếu mạng lag
      .build();

  /**
   * Hàm dùng để gửi yêu cầu HTTP GET đến một đường dẫn (URL) cụ thể. Thường dùng để lấy dữ liệu từ
   * các API công khai trên mạng. * @param url Đường dẫn API (Ví dụ:
   * "https://api.exchangerate-api.com/v4/latest/USD")
   *
   * @return Chuỗi dữ liệu (thường là JSON) trả về từ API, hoặc null nếu lỗi.
   */
  public static String sendGetRequest(String url) {
    try {
      // Đóng gói yêu cầu
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .GET()
          .build();

      // Gửi yêu cầu đi và lấy phản hồi dạng Chuỗi (String)
      HttpResponse<String> response = httpClient.send(request,
          HttpResponse.BodyHandlers.ofString());

      // Mã 200 nghĩa là lấy dữ liệu thành công (OK)
      if (response.statusCode() == 200) {
        return response.body();
      } else {
        System.err.println("❌ Lỗi gọi API. Mã trạng thái HTTP: " + response.statusCode());
      }
    } catch (Exception e) {
      System.err.println("❌ Lỗi ngoại lệ khi gọi API: " + e.getMessage());
      // Có thể in e.printStackTrace(); nếu muốn xem chi tiết luồng lỗi
    }

    return null;
  }
}
