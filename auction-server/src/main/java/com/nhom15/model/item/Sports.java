package com.nhom15.model.item;

public class Sports extends Item {
    private SportCategory sportType; // Dùng Enum
    private String condition;

    public Sports(String id, String name, double startingPrice, SportCategory sportType, String condition) {
        super(id, name, startingPrice);
        this.sportType = sportType;
        this.condition = condition;
    }

    // SỬA: Kiểu trả về phải là SportCategory
    public SportCategory getSportType() {
        return sportType;
    }

    // SỬA: Tham số truyền vào cũng phải là SportCategory
    public void setSportType(SportCategory sportType) {
        this.sportType = sportType;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    @Override
    public void displayItemInfo() {
        // Dùng sportType.name() để chuyển Enum thành chữ in ra cho đẹp
        System.out.println("Đồ thể thao: " + getName() + " - Môn: " + sportType.name() + " - Tình trạng: " + condition);
    }
}