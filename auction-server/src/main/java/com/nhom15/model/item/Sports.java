package com.nhom15.model.item;

/**
 * Sản phẩm loại thể thao (dụng cụ, trang phục thi đấu, …).
 *
 * <p><b>FIX:</b> Cập nhật constructor nhận {@code int id} thay {@code String id}.
 */
public class Sports extends Item {

  private SportCategory sportType;
  private String        condition;

  /**
   * @param id           khoá chính từ DB
   * @param name         tên sản phẩm
   * @param startingPrice giá khởi điểm
   * @param sportType    loại thể thao (enum {@link SportCategory})
   * @param condition    tình trạng sản phẩm (mới / đã dùng …)
   */
  public Sports(int id, String name, double startingPrice,
      SportCategory sportType, String condition) {
    super(id, name, startingPrice);
    this.sportType = sportType;
    this.condition = condition;
  }

  public SportCategory getSportType() { return sportType; }
  public String        getCondition() { return condition; }

  public void setSportType(SportCategory sportType) { this.sportType = sportType; }
  public void setCondition(String condition)        { this.condition = condition; }

  @Override
  public void displayItemInfo() {
    System.out.println("Đồ thể thao: " + getName()
        + " - Môn: " + sportType.name()
        + " - Tình trạng: " + condition
        + " - Giá khởi điểm: " + getStartingPrice());
  }
}
