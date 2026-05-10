package com.nhom15.client.model;

/**
 * DTO cho sản phẩm + phiên đấu giá — dùng để hiển thị danh sách và chi tiết.
 * Server trả về camelCase nên không cần @SerializedName.
 */
public class ItemDTO {

    private int    auctionId;
    private int    itemId;
    private int    sellerId;
    private String sellerName;
    private String name;
    private String description;
    private String category;
    private String imagePath;
    private String imageBase64;   // server đính kèm để hiển thị không cần load file
    private double startPrice;
    private double currentPrice;
    private double minStep;
    private String endTime;
    private String status;        // ACTIVE | ENDED | CANCELLED

    public ItemDTO() {}

    // ── Getters & Setters ────────────────────────────────────────────────────

    public int    getAuctionId()   { return auctionId; }
    public void   setAuctionId(int v)    { auctionId = v; }

    public int    getItemId()      { return itemId; }
    public void   setItemId(int v)       { itemId = v; }

    public int    getSellerId()    { return sellerId; }
    public void   setSellerId(int v)     { sellerId = v; }

    public String getSellerName()  { return sellerName; }
    public void   setSellerName(String v){ sellerName = v; }

    public String getName()        { return name; }
    public void   setName(String v)      { name = v; }

    public String getDescription() { return description; }
    public void   setDescription(String v){ description = v; }

    public String getCategory()    { return category; }
    public void   setCategory(String v)  { category = v; }

    public String getImagePath()   { return imagePath; }
    public void   setImagePath(String v) { imagePath = v; }

    public String getImageBase64() { return imageBase64; }
    public void   setImageBase64(String v){ imageBase64 = v; }

    public double getStartPrice()  { return startPrice; }
    public void   setStartPrice(double v){ startPrice = v; }

    public double getCurrentPrice(){ return currentPrice; }
    public void   setCurrentPrice(double v){ currentPrice = v; }

    public double getMinStep()     { return minStep; }
    public void   setMinStep(double v)   { minStep = v; }

    public String getEndTime()     { return endTime; }
    public void   setEndTime(String v)   { endTime = v; }

    public String getStatus()      { return status; }
    public void   setStatus(String v)    { status = v; }

    @Override
    public String toString() {
        return "ItemDTO{auctionId=" + auctionId + ", name='" + name
                + "', currentPrice=" + currentPrice + ", status='" + status + "'}";
    }
}