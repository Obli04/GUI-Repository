package beans.databaseUtility;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * A temporary class to manage friend relationships in the database until the friend system is implemented.
 * This class provides methods to add and remove friendships between users.
 * 
 * @author Danilo Spera
 */
public class DatabaseFriends {
    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure().directory("src/main/resources").load();
        String url = dotenv.get("DB_URL");
        String user = dotenv.get("DB_USER");
        String password = dotenv.get("DB_PASSWORD");
        Scanner scanner = new Scanner(System.in);
        
        try {
            Class.forName("org.postgresql.Driver");
            while (true) {
                System.out.println("\n=== Friend Management Utility ===");
                System.out.println("1. Add Friend Relationship");
                System.out.println("2. Remove Friend Relationship");
                System.out.println("3. Exit");
                System.out.print("Choose an option (1-3): ");
                
                String choice = scanner.nextLine();
                
                switch (choice) {
                    case "1":
                        addFriendship(scanner, url, user, password);
                        break;
                    case "2":
                        removeFriendship(scanner, url, user, password);
                        break;
                    case "3":
                        System.out.println("Exiting...");
                        scanner.close();
                        return;
                    default:
                        System.out.println("Invalid option. Please try again.");
                }
            }
        } catch (ClassNotFoundException e) {
            System.err.println("PostgreSQL JDBC Driver not found: " + e.getMessage());
        }
    }
    
    private static void addFriendship(Scanner scanner, String url, String user, String password) {
        try {
            System.out.println("\n=== Add Friend Relationship ===");
            System.out.print("Enter first user's email or ID: ");
            String user1 = scanner.nextLine();
            System.out.print("Enter second user's email or ID: ");
            String user2 = scanner.nextLine();
            
            // Get user IDs if emails were provided
            Long user1Id = getUserId(user1, url, user, password);
            Long user2Id = getUserId(user2, url, user, password);
            
            if (user1Id == null || user2Id == null) {
                System.out.println("Error: One or both users not found!");
                return;
            }
            
            if (user1Id.equals(user2Id)) {
                System.out.println("Error: Cannot add friendship with self!");
                return;
            }
            
            // Check if friendship already exists
            if (checkFriendshipExists(user1Id, user2Id, url, user, password)) {
                System.out.println("Friendship already exists!");
                return;
            }
            
            // Add friendship
            try (Connection conn = DriverManager.getConnection(url, user, password)) {
                String sql = "INSERT INTO friends (id_user_1, id_user_2) VALUES (?, ?) RETURNING id";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setLong(1, user1Id);
                pstmt.setLong(2, user2Id);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    System.out.println("Friendship added successfully with ID: " + rs.getLong("id"));
                }
            }
            
        } catch (SQLException | IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
    
    private static void removeFriendship(Scanner scanner, String url, String user, String password) {
        try {
            System.out.println("\n=== Remove Friend Relationship ===");
            System.out.print("Enter first user's email or ID: ");
            String user1 = scanner.nextLine();
            System.out.print("Enter second user's email or ID: ");
            String user2 = scanner.nextLine();
            
            // Get user IDs if emails were provided
            Long user1Id = getUserId(user1, url, user, password);
            Long user2Id = getUserId(user2, url, user, password);
            
            if (user1Id == null || user2Id == null) {
                System.out.println("Error: One or both users not found!");
                return;
            }
            
            // Remove friendship
            try (Connection conn = DriverManager.getConnection(url, user, password)) {
                String sql = "DELETE FROM friends WHERE (id_user_1 = ? AND id_user_2 = ?) OR (id_user_1 = ? AND id_user_2 = ?)";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setLong(1, user1Id);
                pstmt.setLong(2, user2Id);
                pstmt.setLong(3, user2Id);
                pstmt.setLong(4, user1Id);
                
                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected > 0) {
                    System.out.println("Friendship removed successfully!");
                } else {
                    System.out.println("No friendship found between these users!");
                }
            }
            
        } catch (SQLException | IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
    
    private static Long getUserId(String identifier, String url, String user, String password) {
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            String sql = "SELECT id FROM users WHERE email = ? OR id::text = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, identifier);
            pstmt.setString(2, identifier);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() ? rs.getLong("id") : null;
        } catch (SQLException e) {
            System.out.println("Error looking up user: " + e.getMessage());
        }
        return null;
    }
    
    private static boolean checkFriendshipExists(Long user1Id, Long user2Id, String url, String user, String password) {
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            String sql = "SELECT 1 FROM friends WHERE (id_user_1 = ? AND id_user_2 = ?) OR (id_user_1 = ? AND id_user_2 = ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, user1Id);
            pstmt.setLong(2, user2Id);
            pstmt.setLong(3, user2Id);
            pstmt.setLong(4, user1Id);
            
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.out.println("Error checking friendship: " + e.getMessage());
            return false;
        }
    }
} 