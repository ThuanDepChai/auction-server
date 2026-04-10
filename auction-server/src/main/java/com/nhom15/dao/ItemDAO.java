package com.nhom15.dao;

import com.nhom15.database.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO {

    public List<String> getAllItems() {
        List<String> items = new ArrayList<>();
        String sql = "SELECT item_name FROM item";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                items.add(rs.getString("item_name"));
            }
            System.out.println("Đã lấy dữ liệu từ DB thành công!");

        } catch (SQLException e) {
            System.out.println("Lỗi kết nối DB " + e.getMessage());
        }
        return items;
    }

    //Test:
    public static void main(String[] args) {
        ItemDAO dao = new ItemDAO();
        List<String> items = dao.getAllItems();
        System.out.println("Danh sách món hàng: " + items);
    }
}