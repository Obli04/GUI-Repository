package beans;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import io.github.cdimascio.dotenv.Dotenv;

public class DatabasePrint {
    public static void main(String[] args) {
        // Load environment variables
        Dotenv dotenv = Dotenv.configure()
            .directory("src/main/resources")
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

            // Execute SQL to print all data
            ResultSet usersResult = statement.executeQuery("SELECT * FROM users");
            System.out.println("Users Table:");
            while (usersResult.next()) {
                System.out.println("ID: " + usersResult.getInt("id") +
                                   ", First Name: " + usersResult.getString("first_name") +
                                   ", Second Name: " + usersResult.getString("second_name") +
                                   ", Email: " + usersResult.getString("email") +
                                   ", Is Verified: " + usersResult.getBoolean("is_verified") +
                                   ", Two Factor Enabled: " + usersResult.getBoolean("two_factor_enabled") +
                                   ", Balance: " + usersResult.getDouble("balance") +
                                   ", Budget: " + usersResult.getDouble("budget") +
                                   ", Piggy Bank: " + usersResult.getDouble("piggy_bank") +
                                   ", IBAN: " + usersResult.getString("iban") +
                                   ", Variable Symbol: " + usersResult.getString("variable_symbol"));
            }

            ResultSet transactionsResult = statement.executeQuery("SELECT * FROM transactions");
            System.out.println("\nTransactions Table:");
            while (transactionsResult.next()) {
                System.out.println("ID: " + transactionsResult.getInt("id") +
                                   ", Sender ID: " + transactionsResult.getInt("id_sender") +
                                   ", Receiver ID: " + transactionsResult.getInt("id_receiver") +
                                   ", Value: " + transactionsResult.getDouble("value") +
                                   ", Type: " + transactionsResult.getString("type"));
            }
            ResultSet maxConnectionsResult = statement.executeQuery("SELECT * FROM pg_settings WHERE name IN ('max_connections', 'superuser_reserved_connections');");
            System.out.println("\nMax Connections:");
            while (maxConnectionsResult.next()) {
                System.out.println("Max Connections: " + maxConnectionsResult.getString("setting") +
                                   ", Value: " + maxConnectionsResult.getString("unit"));
            }
            
            ResultSet budgetsResult = statement.executeQuery("SELECT * FROM budgets");
            System.out.println("\nBudgets Table:");
            while (budgetsResult.next()) {
                System.out.println("ID: " + budgetsResult.getInt("id") +
                                   ", User ID: " + budgetsResult.getInt("id_user") +
                                   ", Budget: " + budgetsResult.getDouble("budget") +
                                   ", Budget Spent: " + budgetsResult.getDouble("budget_spent") +
                                   ", Budget Category: " + budgetsResult.getString("budget_category"));
            }

            ResultSet requestMoneyResult = statement.executeQuery("SELECT * FROM request_money");
            System.out.println("\nRequest Money Table:");
            while (requestMoneyResult.next()) {
                System.out.println("ID: " + requestMoneyResult.getInt("id") +
                                   ", Sender ID: " + requestMoneyResult.getInt("id_sender") +
                                   ", Receiver ID: " + requestMoneyResult.getInt("id_receiver") +
                                   ", Value: " + requestMoneyResult.getDouble("value") +
                                   ", Description: " + requestMoneyResult.getString("description"));
            }
            // Close resources  
            usersResult.close();
            transactionsResult.close();
            statement.close();
            connection.close();
        } catch (Exception e) {
            System.err.println("Error during database printing: " + e.getMessage());
            e.printStackTrace();
        }
    }
}