package beans;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import io.github.cdimascio.dotenv.Dotenv;

public class DatabaseMigration {
    public static void main(String[] args) {
        // Load environment variables from the correct path
        Dotenv dotenv = Dotenv.configure()
            .directory("src/main/resources")  // Update this path
            .load();
        String url = dotenv.get("DB_URL");
        String user = dotenv.get("DB_USER");
        String password = dotenv.get("DB_PASSWORD");

        try {
            // Load PostgreSQL JDBC Driver
            Class.forName("org.postgresql.Driver");

            // Establish connection
            Connection connection = DriverManager.getConnection(url, user, password);
            Statement statement = connection.createStatement();

            // Clear existing tables
            statement.execute("DROP TABLE IF EXISTS transactions CASCADE;");
            statement.execute("DROP TABLE IF EXISTS users CASCADE;");

            // Read SQL file
            String sql = new String(Files.readAllBytes(Paths.get("src/main/resources/db/migration/V1_Create_User_And_Transaction_Tables.sql")));

            // Execute SQL
            statement.execute(sql);
            System.out.println("Database tables created successfully!");

            // Close resources
            statement.close();
            connection.close();
        } catch (Exception e) {
            System.err.println("Error during database migration: " + e.getMessage());
            e.printStackTrace();
        }
    }
}   