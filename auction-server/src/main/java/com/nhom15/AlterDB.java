package com.nhom15;

import com.nhom15.util.DBConnection;
import java.sql.Connection;
import java.sql.Statement;

public class AlterDB {
    public static void main(String[] args) {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("ALTER TABLE item ADD COLUMN extra_info TEXT DEFAULT NULL");
            System.out.println("ALTER TABLE SUCCESS");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DBConnection.shutdown();
        }
    }
}
