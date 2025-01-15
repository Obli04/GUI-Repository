package beans.databaseUtility;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * A utility class for printing database tables to the console.
 * This class connects to a PostgreSQL database and prints the contents of various tables.
 * 
 * @author Davide Scaccia
 */
public class DatabasePrint {

    /**
     * The main method to execute the database printing.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure().directory("src/main/resources").load(); //Get the environment variables from the .env file
        String url = dotenv.get("DB_URL"); //Get the database URL from the .env file
        String user = dotenv.get("DB_USER"); //Get the database user from the .env file
        String password = dotenv.get("DB_PASSWORD"); //Get the database password from the .env file
        try {
            Class.forName("org.postgresql.Driver"); //Load the PostgreSQL JDBC Driver
            try (Connection connection = DriverManager.getConnection(url, user, password); //Establish a connection to the database
                 Statement statement = connection.createStatement()) { //Create a statement object to execute SQL commands

                try (ResultSet usersResult = statement.executeQuery("SELECT * FROM users")) { //Execute a query to get all the users from the database
                    System.out.println("Users Table:");
                    printUserTable(usersResult);
                }

                try (ResultSet transactionsResult = statement.executeQuery("SELECT * FROM transactions")) { //Execute a query to get all the transactions from the database
                    System.out.println("\nTransactions Table:");
                    printTransactionsTable(transactionsResult);
                }

                try (ResultSet budgetsResult = statement.executeQuery("SELECT * FROM budget")) { //Execute a query to get all the budgets from the database
                    System.out.println("\nBudgets Table:");
                    printBudgetsTable(budgetsResult);
                }

                try (ResultSet requestMoneyResult = statement.executeQuery("SELECT * FROM requestMoney")) { //Execute a query to get all the request money from the database
                    System.out.println("\nRequest Money Table:");
                    printRequestMoneyTable(requestMoneyResult);
                }

            } catch (SQLException e) {
                System.err.println("Error during database printing: " + e.getMessage());
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Error during database printing: " + e.getMessage());
        }
    }

    /**
     * Prints the contents of the users table to the console.
     *
     * @param userResult the ResultSet containing the users data
     */
    public static void printUserTable(ResultSet userResult) throws SQLException {
        while (userResult.next()) {
            System.out.println("ID: " + userResult.getInt("id") +
                               ", First Name: " + userResult.getString("first_name") +
                               ", Second Name: " + userResult.getString("second_name") +
                               ", Email: " + userResult.getString("email") +
                               ", Is Verified: " + userResult.getBoolean("is_verified") +
                               ", Two Factor Enabled: " + userResult.getBoolean("two_factor_enabled") +
                               ", Balance: " + userResult.getDouble("balance") +
                               ", Budget: " + userResult.getDouble("budget") +
                               ", Piggy Bank: " + userResult.getDouble("piggy_bank") +
                               ", IBAN: " + userResult.getString("iban") +
                               ", Variable Symbol: " + userResult.getString("variable_symbol"));
        }
    }

    /**
     * Prints the contents of the transactions table to the console.
     *
     * @param transactionsResult the ResultSet containing the transactions data
     */
    public static void printTransactionsTable(ResultSet transactionsResult) throws SQLException {
        while (transactionsResult.next()) {
            System.out.println("ID: " + transactionsResult.getInt("id") +
                               ", Sender ID: " + transactionsResult.getInt("id_sender") +
                               ", Receiver ID: " + transactionsResult.getInt("id_receiver") +
                               ", Sender Name: " + transactionsResult.getString("name_of_sender") +
                               ", Value: " + transactionsResult.getDouble("value") +
                               ", Type: " + transactionsResult.getString("type"));
        }
    }

    /**
     * Prints the contents of the budgets table to the console.
     *
     * @param budgetsResult the ResultSet containing the budgets data
     */
    public static void printBudgetsTable(ResultSet budgetsResult) throws SQLException {
        while (budgetsResult.next()) {
            System.out.println("ID: " + budgetsResult.getInt("id") +
                               ", User ID: " + budgetsResult.getInt("id_user") +
                               ", Budget: " + budgetsResult.getDouble("budget") +
                               ", Budget Spent: " + budgetsResult.getDouble("budget_spent") +
                               ", Budget Category: " + budgetsResult.getString("budget_category"));
        }
    }

    /**
     * Prints the contents of the request money table to the console.
     *
     * @param requestMoneyResult the ResultSet containing the request money data
     */
    public static void printRequestMoneyTable(ResultSet requestMoneyResult) throws SQLException {
        while (requestMoneyResult.next()) {
            System.out.println("ID: " + requestMoneyResult.getInt("id") +
                               ", Sender ID: " + requestMoneyResult.getInt("id_sender") +
                               ", Receiver ID: " + requestMoneyResult.getInt("id_receiver") +
                               ", Value: " + requestMoneyResult.getDouble("value") +
                               ", Description: " + requestMoneyResult.getString("description"));
        }
    }
}