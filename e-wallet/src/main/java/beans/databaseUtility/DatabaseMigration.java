package beans.databaseUtility;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * A utility class for performing database migrations.
 * This class reads SQL scripts from a file and executes them on the PostgreSQL database.
 * 
 * @author Davide Scaccia - xscaccd00
 */
public class DatabaseMigration {

    /**
     * The main method to execute the database migration.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        //Load environment variables from the correct path
        Dotenv dotenv = Dotenv.configure().directory("src/main/resources").load(); //Get the environment variables from the .env file
        String url = dotenv.get("DB_URL"); //Get the database URL from the .env file
        String user = dotenv.get("DB_USER"); //Get the database user from the .env file
        String password = dotenv.get("DB_PASSWORD"); //Get the database password from the .env file

        try {
            Class.forName("org.postgresql.Driver"); //Load the PostgreSQL JDBC Driver
            try (Connection connection = DriverManager.getConnection(url, user, password); //Establish a connection to the database
                 Statement statement = connection.createStatement()) { //Create a statement object to execute SQL commands
                String sql = new String(Files.readAllBytes(Paths.get("src/main/resources/db/migration/V6_Final_Database.sql"))); //Read the SQL script from the file
                statement.execute(sql); //Execute the SQL script
                System.out.println("Database tables updated successfully!"); //Print a message to the console
            } //Close the statement object
        } catch (IOException | ClassNotFoundException | SQLException e) {
            System.err.println("Error during database migration: " + e.getMessage()); //Catch any exceptions printing the error message
        }
    }
}   