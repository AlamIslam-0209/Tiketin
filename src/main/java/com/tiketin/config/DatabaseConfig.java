package com.tiketin.config;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConfig {
    private static String host = "127.0.0.1";
    private static String port = "3306";
    private static String dbName = "tiketin_db2";
    private static String user = "root";
    private static String password = "";

    static {
        loadProperties();
    }

    private static void loadProperties() {
        try (InputStream input = DatabaseConfig.class.getClassLoader().getResourceAsStream("database.properties")) {
            if (input != null) {
                Properties prop = new Properties();
                prop.load(input);
                host = prop.getProperty("db.host", host);
                port = prop.getProperty("db.port", port);
                dbName = prop.getProperty("db.name", dbName);
                user = prop.getProperty("db.user", user);
                password = prop.getProperty("db.password", password);
            }
        } catch (Exception e) {
            System.err.println("Gagal memuat database.properties, menggunakan default: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        String url = String.format("jdbc:mysql://%s:%s/%s?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8",
                host, port, dbName);
        return DriverManager.getConnection(url, user, password);
    }

    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}
