package com.nhom15.network.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nhom15.service.AuctionService;
import com.nhom15.service.AutoBidService;
import com.nhom15.service.ItemService;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * AuctionHandler — xử lý tất cả request liên quan đến đấu giá. Không chứa routing — chỉ chứa logic
 * của từng action.
 */
public class AuctionHandler {

  private final AuctionService auctionService = new AuctionService();
  private final ItemService itemService = new ItemService();
  private final AutoBidService autoBidService = new AutoBidService();

  public JsonObject handle(JsonObject request) {
    String action = request.get("action").getAsString();
    JsonObject d = request.has("data") ? request.getAsJsonObject("data") : new JsonObject();

    return switch (action) {
      case "GET_AUCTIONS",
           "GET_ACTIVE_AUCTIONS" -> handleGetAuctions();
      case "GET_AUCTION_DETAIL"  -> handleGetAuctionDetail(d);
      case "CREATE_AUCTION"      -> handleCreateAuction(d);
      case "PLACE_BID"           -> handlePlaceBid(d);
      case "GET_BID_HISTORY"     -> handleGetBidHistory(d);
      case "END_AUCTION"         -> handleEndAuction(d);
      case "GET_MY_AUCTIONS"     -> handleGetMyAuctions(d);
      case "CREATE_ITEM"         -> handleCreateItem(d);
      case "GET_MY_ITEMS"        -> handleGetMyItems(d);
      case "GET_FEATURED_PRODUCTS" -> handleGetFeaturedProducts();
      case "SEARCH_PRODUCTS"     -> handleSearchProducts(d);
      case "DELETE_ITEM"         -> handleDeleteItem(d);
      case "GET_ITEM_IMAGE"      -> handleGetItemImage(d);
      // FIX: Thêm routing cho Auto-Bid (trước đây bị thiếu hoàn toàn)
      case "SET_AUTO_BID"        -> handleSetAutoBid(d);
      case "CANCEL_AUTO_BID"     -> handleCancelAutoBid(d);
      case "GET_AUTO_BID_STATUS" -> handleGetAutoBidStatus(d);
      default -> error("AuctionHandler không hỗ trợ action: " + action);
    };
  }

  // ── Auction Handlers ─────────────────────────────────────────────────────

  private JsonObject handleGetAuctions() {
    JsonArray auctions = auctionService.getActiveAuctions();
    JsonObject res = new JsonObject();
    res.addProperty("status", "SUCCESS");
    res.add("auctions", auctions);
    return res;
  }

  private JsonObject handleGetAuctionDetail(JsonObject d) {
    int auctionId = d.get("auctionId").getAsInt();
    JsonObject auction = auctionService.getAuctionDetail(auctionId);
    JsonObject res = new JsonObject();
    if (auction != null) {
      res.addProperty("status", "SUCCESS");
      res.add("auction", auction);
    } else {
      res.addProperty("status", "FAIL");
      res.addProperty("message", "Không tìm thấy phiên đấu giá!");
    }
    return res;
  }

  /** Client gửi lên: itemId, sellerId, startPrice, minStep, endTime */
  private JsonObject handleCreateAuction(JsonObject d) {
    try {
      int itemId       = d.get("itemId").getAsInt();
      int sellerId     = d.get("sellerId").getAsInt();
      double startPrice = d.get("startPrice").getAsDouble();
      double minStep   = d.get("minStep").getAsDouble();
      String endTime   = d.get("endTime").getAsString();
      return auctionService.createAuction(itemId, sellerId, startPrice, minStep, endTime);
    } catch (Exception e) {
      return error("Lỗi tạo phiên đấu giá: " + e.getMessage());
    }
  }

  private JsonObject handlePlaceBid(JsonObject d) {
    int auctionId = d.get("auctionId").getAsInt();
    int bidderId  = d.get("bidderId").getAsInt();
    double amount = d.get("amount").getAsDouble();
    return auctionService.placeBid(auctionId, bidderId, amount);
  }

  private JsonObject handleGetBidHistory(JsonObject d) {
    int auctionId = d.get("auctionId").getAsInt();
    JsonArray history = auctionService.getBidHistory(auctionId);
    JsonObject res = new JsonObject();
    res.addProperty("status", "SUCCESS");
    res.add("history", history);
    return res;
  }

  private JsonObject handleEndAuction(JsonObject d) {
    int auctionId = d.get("auctionId").getAsInt();
    boolean ok = auctionService.endAuction(auctionId);
    JsonObject res = new JsonObject();
    res.addProperty("status", ok ? "SUCCESS" : "FAIL");
    res.addProperty("message", ok ? "Kết thúc phiên đấu giá thành công!" : "Thất bại!");
    return res;
  }

  private JsonObject handleGetMyAuctions(JsonObject d) {
    int sellerId = d.get("sellerId").getAsInt();
    JsonArray auctions = auctionService.getAuctionsBySeller(sellerId);
    JsonObject res = new JsonObject();
    res.addProperty("status", "SUCCESS");
    res.add("auctions", auctions);
    return res;
  }

  // ── Auto-Bid Handlers ────────────────────────────────────────────────────

  /** Đăng ký hoặc cập nhật cấu hình auto-bid */
  private JsonObject handleSetAutoBid(JsonObject d) {
    try {
      int auctionId    = d.get("auctionId").getAsInt();
      int bidderId     = d.get("bidderId").getAsInt();
      double maxBid    = d.get("maxBid").getAsDouble();
      double increment = d.get("increment").getAsDouble();
      return autoBidService.setAutoBid(auctionId, bidderId, maxBid, increment);
    } catch (Exception e) {
      return error("Lỗi kích hoạt auto-bid: " + e.getMessage());
    }
  }

  /** Huỷ auto-bid */
  private JsonObject handleCancelAutoBid(JsonObject d) {
    try {
      int auctionId = d.get("auctionId").getAsInt();
      int bidderId  = d.get("bidderId").getAsInt();
      return autoBidService.cancelAutoBid(auctionId, bidderId);
    } catch (Exception e) {
      return error("Lỗi huỷ auto-bid: " + e.getMessage());
    }
  }

  /** Lấy trạng thái auto-bid hiện tại của 1 bidder trong 1 phiên */
  private JsonObject handleGetAutoBidStatus(JsonObject d) {
    try {
      int auctionId = d.get("auctionId").getAsInt();
      int bidderId  = d.get("bidderId").getAsInt();
      return autoBidService.getAutoBidStatus(auctionId, bidderId);
    } catch (Exception e) {
      return error("Lỗi lấy trạng thái auto-bid: " + e.getMessage());
    }
  }

  // ── Item Handlers (Seller Dashboard) ────────────────────────────────────

  private JsonObject handleCreateItem(JsonObject d) {
    try {
      int sellerId      = d.get("sellerId").getAsInt();
      String name       = d.get("name").getAsString();
      String desc       = d.has("description") ? d.get("description").getAsString() : "";
      String category   = d.has("category") ? d.get("category").getAsString() : "";
      double startPrice = d.get("startPrice").getAsDouble();
      String extension  = d.has("extension") ? d.get("extension").getAsString() : "jpg";
      String extraInfo  = d.has("extraInfo") ? d.get("extraInfo").getAsString() : "";

      //Xử lý danh sách ảnh
      List<String> imageList = new ArrayList<>();

      if (d.has("images") && d.get("images").isJsonArray()) {
        JsonArray imagesJson = d.getAsJsonArray("images");
        for (int i = 0; i < imagesJson.size(); i++) {
          imageList.add(imagesJson.get(i).getAsString());
        }
      }
      // Nếu Client vẫn gửi 1 chuỗi "imageBase64"
      else if (d.has("imageBase64") && !d.get("imageBase64").getAsString().isEmpty()) {
        imageList.add(d.get("imageBase64").getAsString());
      }

      // Truyền imageList (List) thay vì imageBase64 (String)
      return itemService.createItem(sellerId, name, desc, category, startPrice, imageList, extension, extraInfo);

    } catch (Exception e) {
      return error("Lỗi tạo sản phẩm: " + e.getMessage());
    }
  }

  private JsonObject handleGetMyItems(JsonObject d) {
    try {
      int sellerId = d.get("sellerId").getAsInt();
      JsonArray items = itemService.getItemsBySeller(sellerId);
      JsonObject res = new JsonObject();
      res.addProperty("status", "SUCCESS");
      res.add("items", items);
      return res;
    } catch (Exception e) {
      return error("Lỗi lấy danh sách sản phẩm: " + e.getMessage());
    }
  }

  private JsonObject handleDeleteItem(JsonObject d) {
    try {
      int itemId = d.get("itemId").getAsInt();
      boolean ok = itemService.deleteItem(itemId);
      JsonObject res = new JsonObject();
      res.addProperty("status", ok ? "SUCCESS" : "FAIL");
      res.addProperty("message", ok ? "Xóa sản phẩm thành công!" : "Xóa thất bại!");
      return res;
    } catch (Exception e) {
      return error("Lỗi xóa sản phẩm: " + e.getMessage());
    }
  }

  // ── Sản phẩm trang chủ ───────────────────────────────────────────────────

  private JsonObject handleGetFeaturedProducts() {
    try {
      JsonArray items = itemService.getFeaturedItems();
      JsonObject res = new JsonObject();
      res.addProperty("status", "SUCCESS");
      res.add("items", items);
      return res;
    } catch (Exception e) {
      return error("Lỗi lấy sản phẩm nổi bật: " + e.getMessage());
    }
  }

  private JsonObject handleSearchProducts(JsonObject d) {
    try {
      String keyword  = d.has("keyword")  ? d.get("keyword").getAsString()  : "";
      String category = d.has("category") ? d.get("category").getAsString() : "";
      JsonArray items = itemService.searchItems(keyword, category);
      JsonObject res = new JsonObject();
      res.addProperty("status", "SUCCESS");
      res.add("items", items);
      return res;
    } catch (Exception e) {
      return error("Lỗi tìm kiếm sản phẩm: " + e.getMessage());
    }
  }

  // ── Ảnh sản phẩm ─────────────────────────────────────────────────────────

  /**
   * Trả Base64 của ảnh sản phẩm theo imagePath.
   *
   * FIX PERF — Thumbnail resize:
   *  - Ảnh gốc có thể lên đến vài MB (3000×3000px). Gửi nguyên qua mạng
   *    vừa tốn bandwidth vừa làm chận decode/render phía client.
   *  - Nếu chiều dài/rộng đều không vượt MAX_DIM (720px) thì giữ nguyên.
   *  - Nếu vượt: scale down proportionally, encode JPEG quality 0.85.
   *    Kết quả: 3000px → 720px ≈ giảm 17x số pixel → file giảm khoảng 8–15x.
   *
   * imagePath trong DB là đường dẫn TƯƠNG ĐỐI (ví dụ "item_images/item_123.jpg").
   * Server ghép với working directory để tìm file thực tế.
   */
  private static final int MAX_DIM = 720; // px — đủ rõ cho card UI, giảm bandwidth rõ rệt

  private JsonObject handleGetItemImage(JsonObject d) {
    JsonObject res = new JsonObject();
    try {
      String imagePath = d.has("imagePath") ? d.get("imagePath").getAsString() : "";
      if (imagePath.isEmpty()) {
        res.addProperty("status", "FAIL");
        res.addProperty("message", "Không có đường dẫn ảnh!");
        return res;
      }

      File f = new File(imagePath);

      // Nếu không phải absolute path (hoặc absolute path không tồn tại)
      // → ghép với working directory của server
      if (!f.isAbsolute() || !f.exists()) {
        String baseDir = System.getProperty("user.dir");
        f = new File(baseDir, imagePath);
      }

      if (!f.exists()) {
        res.addProperty("status", "FAIL");
        res.addProperty("message", "Không tìm thấy file ảnh: " + imagePath);
        System.err.println("⚠️ [GET_ITEM_IMAGE] File không tồn tại: " + f.getAbsolutePath());
        return res;
      }

      // FIX PERF: đọc ảnh, resize nếu quá lớn, encode JPEG
      byte[] imageBytes = resizeIfNeeded(f);
      res.addProperty("status", "SUCCESS");
      res.addProperty("imageBase64", Base64.getEncoder().encodeToString(imageBytes));

    } catch (Exception e) {
      res.addProperty("status", "ERROR");
      res.addProperty("message", "Lỗi đọc ảnh: " + e.getMessage());
    }
    return res;
  }

  /**
   * Nếu ảnh đủ nhỏ (≤ MAX_DIM cả 2 chiều) → trả bytes gốc (không decode/re-encode).
   * Nếu vượt → scale down rồi encode JPEG 0.85 quality.
   */
  private static byte[] resizeIfNeeded(File f) throws Exception {
    BufferedImage orig = ImageIO.read(f);
    if (orig == null) {
      // Không parse được (file không phải ảnh chuNmM) → trả bytes gốc
      return Files.readAllBytes(f.toPath());
    }

    int w = orig.getWidth();
    int h = orig.getHeight();
    if (w <= MAX_DIM && h <= MAX_DIM) {
      // Ảnh đã nhỏ — không cần re-encode, giữ bytes gốc để bảo toàn chất lượng
      return Files.readAllBytes(f.toPath());
    }

    // Tính tỉ lệ scale
    double scale = (double) MAX_DIM / Math.max(w, h);
    int nw = (int) (w * scale);
    int nh = (int) (h * scale);

    BufferedImage scaled = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = scaled.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    g.setRenderingHint(RenderingHints.KEY_RENDERING,     RenderingHints.VALUE_RENDER_QUALITY);
    g.drawImage(orig, 0, 0, nw, nh, null);
    g.dispose();

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    // JPEG chất lượng 0.85 — không mất rõ đi trên card nhỏ nhưng nhỏ hơn nhiều
    javax.imageio.ImageWriter writer = javax.imageio.ImageIO.getImageWritersByFormatName("jpeg").next();
    javax.imageio.ImageWriteParam param = writer.getDefaultWriteParam();
    param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
    param.setCompressionQuality(0.85f);
    writer.setOutput(javax.imageio.ImageIO.createImageOutputStream(baos));
    writer.write(null, new javax.imageio.IIOImage(scaled, null, null), param);
    writer.dispose();

    System.out.printf("[🖼️ Resize] %s: %dx%d → %dx%d, %.1fKB%n",
        f.getName(), w, h, nw, nh, baos.size() / 1024.0);
    return baos.toByteArray();
  }

  // ── Util ─────────────────────────────────────────────────────────────────

  private JsonObject error(String message) {
    JsonObject r = new JsonObject();
    r.addProperty("status", "ERROR");
    r.addProperty("message", message);
    return r;
  }
}