package proj;

import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class Database {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/";
    private static final String DB_NAME = "battleship";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";
    
    private Connection connection;
    private boolean isConnected = false;
    
    public Database() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            
            // First connect without database to create it if needed
            Connection tempConn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            Statement stmt = tempConn.createStatement();
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
            stmt.close();
            tempConn.close();
            
            // Now connect to the battleship database
            connection = DriverManager.getConnection(DB_URL + DB_NAME, DB_USER, DB_PASSWORD);
            System.out.println("Database connected successfully!");
            isConnected = true;
            createTables();
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found. Leaderboard will not be available.");
            System.err.println("To enable database: Add mysql-connector-java JAR to classpath");
        } catch (SQLException e) {
            System.err.println("Database connection failed. Leaderboard will not be available.");
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    private void createTables() {
        String createPlayersTable = "CREATE TABLE IF NOT EXISTS players (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "name VARCHAR(100) NOT NULL," +
                "wins INT DEFAULT 0," +
                "losses INT DEFAULT 0," +
                "total_hits INT DEFAULT 0," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";
        
        String createGamesTable = "CREATE TABLE IF NOT EXISTS games (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "player1_name VARCHAR(100) NOT NULL," +
                "player2_name VARCHAR(100) NOT NULL," +
                "winner VARCHAR(100)," +
                "player1_hits INT," +
                "player2_hits INT," +
                "game_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createPlayersTable);
            stmt.execute(createGamesTable);
            System.out.println("Tables created/verified successfully!");
        } catch (SQLException e) {
            System.err.println("Error creating tables.");
            e.printStackTrace();
        }
    }
    
    public void saveGameResult(String player1, String player2, String winner, int player1Hits, int player2Hits) {
        if (!isConnected) {
            System.out.println("Database not connected. Game result not saved.");
            return;
        }
        
        String insertGame = "INSERT INTO games (player1_name, player2_name, winner, player1_hits, player2_hits) VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(insertGame)) {
            pstmt.setString(1, player1);
            pstmt.setString(2, player2);
            pstmt.setString(3, winner);
            pstmt.setInt(4, player1Hits);
            pstmt.setInt(5, player2Hits);
            pstmt.executeUpdate();
            
            // Update player stats
            updatePlayerStats(player1, winner.equals(player1), player1Hits);
            updatePlayerStats(player2, winner.equals(player2), player2Hits);
            
            System.out.println("Game result saved successfully!");
        } catch (SQLException e) {
            System.err.println("Error saving game result.");
            e.printStackTrace();
        }
    }
    
    private void updatePlayerStats(String playerName, boolean won, int hits) {
        // Check if player exists
        String checkPlayer = "SELECT * FROM players WHERE name = ?";
        String insertPlayer = "INSERT INTO players (name, wins, losses, total_hits) VALUES (?, ?, ?, ?)";
        String updatePlayer = "UPDATE players SET wins = wins + ?, losses = losses + ?, total_hits = total_hits + ? WHERE name = ?";
        
        try {
            PreparedStatement checkStmt = connection.prepareStatement(checkPlayer);
            checkStmt.setString(1, playerName);
            ResultSet rs = checkStmt.executeQuery(); // FIX: Execute query before accessing results
            
            if (!rs.next()) {
                // Player doesn't exist, insert
                PreparedStatement insertStmt = connection.prepareStatement(insertPlayer);
                insertStmt.setString(1, playerName);
                insertStmt.setInt(2, won ? 1 : 0);
                insertStmt.setInt(3, won ? 0 : 1);
                insertStmt.setInt(4, hits);
                insertStmt.executeUpdate();
            } else {
                // Player exists, update
                PreparedStatement updateStmt = connection.prepareStatement(updatePlayer);
                updateStmt.setInt(1, won ? 1 : 0);
                updateStmt.setInt(2, won ? 0 : 1);
                updateStmt.setInt(3, hits);
                updateStmt.setString(4, playerName);
                updateStmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Error updating player stats.");
            e.printStackTrace();
        }
    }
    
    public JTable getLeaderboardTable() {
        DefaultTableModel model = new DefaultTableModel(
                new String[]{"Rank", "Player", "Wins", "Losses", "Total Hits", "Win Rate %"}, 0);
        
        if (!isConnected) {
            model.addRow(new Object[]{"--", "Database not connected", "--", "--", "--", "--"});
            JTable table = new JTable(model);
            table.setEnabled(false);
            return table;
        }
        
        String query = "SELECT name, wins, losses, total_hits, " +
                      "(wins * 100.0 / (wins + losses)) AS win_rate " +
                      "FROM players " +
                      "WHERE (wins + losses) > 0 " +
                      "ORDER BY wins DESC, win_rate DESC " +
                      "LIMIT 20";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            int rank = 1;
            while (rs.next()) {
                String name = rs.getString("name");
                int wins = rs.getInt("wins");
                int losses = rs.getInt("losses");
                int totalHits = rs.getInt("total_hits");
                double winRate = rs.getDouble("win_rate");
                
                model.addRow(new Object[]{rank++, name, wins, losses, totalHits, String.format("%.1f", winRate)});
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving leaderboard.");
            e.printStackTrace();
        }
        
        JTable table = new JTable(model);
        table.setEnabled(false); // Make it read-only
        return table;
    }
    
    public JTable getGamesTable() {
        DefaultTableModel model = new DefaultTableModel(
                new String[]{"Game #", "Player 1", "Player 2", "Winner", "P1 Hits", "P2 Hits", "Date"}, 0);
        
        if (!isConnected) {
            model.addRow(new Object[]{"--", "Database not connected", "--", "--", "--", "--", "--"});
            JTable table = new JTable(model);
            table.setEnabled(false);
            return table;
        }
        
        String query = "SELECT id, player1_name, player2_name, winner, player1_hits, player2_hits, game_date " +
                      "FROM games " +
                      "ORDER BY game_date DESC " +
                      "LIMIT 50";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                int id = rs.getInt("id");
                String player1 = rs.getString("player1_name");
                String player2 = rs.getString("player2_name");
                String winner = rs.getString("winner");
                int p1Hits = rs.getInt("player1_hits");
                int p2Hits = rs.getInt("player2_hits");
                Timestamp gameDate = rs.getTimestamp("game_date");
                
                model.addRow(new Object[]{id, player1, player2, winner, p1Hits, p2Hits, gameDate});
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving games.");
            e.printStackTrace();
        }
        
        JTable table = new JTable(model);
        table.setEnabled(false);
        return table;
    }
    
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
