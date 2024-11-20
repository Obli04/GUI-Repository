import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import io.github.cdimascio.dotenv.Dotenv;

public class DatabaseMigration {
    public static void main(String[] args) {
        // Load environment variables
        Dotenv dotenv = Dotenv.load();

        String url = dotenv.get("DB_URL");
        String user = dotenv.get("DB_USER");
        String password = dotenv.get("DB_PASSWORD");

        try {
            // Read SQL file
            String sql = new String(Files.readAllBytes(Paths.get("src/main/resources/db/migration/V1_Create_User_And_Transaction_Tables.sql")));

            // Establish connection
            Connection connection = DriverManager.getConnection(url, user, password);
            Statement statement = connection.createStatement();

            // Execute SQL
            statement.execute(sql);

            System.out.println("Migration completed successfully.");

            // Close resources
            statement.close();
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} 