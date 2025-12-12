package proj;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

public class Controller {
    private View view;
    private Model model = new Model();
    private GameSocket gameSocket;
    private Database database;
    
    public Controller() {
        gameSocket = new GameSocket();
        gameSocket.setController(this);
        try {
            database = new Database();
        } catch (Exception e) {
            System.err.println("Database not available: " + e.getMessage());
        }
    }

    public void setView(View view) {
        this.view = view;
        showConnectionDialog();
    }
    
    private void showConnectionDialog() {
        String[] options = {"Host Game", "Join Game"};
        int choice = JOptionPane.showOptionDialog(null,
            "Choose connection type:",
            "Multiplayer Setup",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]);
        
        if (choice == 0) {
            // Host game
            String portStr = JOptionPane.showInputDialog(null, "Enter port (default 12345):", "12345");
            int port = portStr != null && !portStr.isEmpty() ? Integer.parseInt(portStr) : 12345;
            gameSocket.startServer(port);
            view.updateStatus("Hosting on port " + port + ". Waiting for opponent...");
        } else if (choice == 1) {
            // Join game
            String host = JOptionPane.showInputDialog(null, "Enter host IP address:", "localhost");
            String portStr = JOptionPane.showInputDialog(null, "Enter port:", "12345");
            int port = portStr != null && !portStr.isEmpty() ? Integer.parseInt(portStr) : 12345;
            gameSocket.connectToServer(host != null ? host : "localhost", port);
            view.updateStatus("Connecting to " + host + ":" + port + "...");
        }
    }

    public void setPlayerName(String name) {
        model.playerName = name;
        view.setPlayerName(name);
    }

    // Ship selection
    public void selectShipType(int shipSize) {
        // Check if we can still place this ship type
        if (shipSize == Model.SMALL_SHIP_SIZE && model.smallShipsPlaced >= Model.SMALL_SHIP_COUNT) {
            view.updateStatus("All small ships already placed!");
            return;
        }
        if (shipSize == Model.MEDIUM_SHIP_SIZE && model.mediumShipsPlaced >= Model.MEDIUM_SHIP_COUNT) {
            view.updateStatus("All medium ships already placed!");
            return;
        }
        if (shipSize == Model.LARGE_SHIP_SIZE && model.largeShipsPlaced >= Model.LARGE_SHIP_COUNT) {
            view.updateStatus("All large ships already placed!");
            return;
        }
        
        // Only allow if we haven't already selected a ship being placed
        if (model.currentShipSize != 0 && model.placedCount > 0) {
            view.updateStatus("Finish placing current ship first!");
            return;
        }

        // Reset ONLY current incomplete ship placement (not all ships!)
        resetCurrentShipOnly();
        model.currentShipSize = shipSize;
        view.updateStatus("Selected " + getShipName(shipSize) + " ship - Place " + shipSize + " cells");
    }
    
    private void resetCurrentShipOnly() {
        // Clear only the current ship being placed
        for (Point p : model.currentShipCells) {
            view.myGrid[p.x][p.y].setBackground(new Color(41, 128, 185));
            model.myGrid[p.x][p.y] = "EMPTY";
        }
        model.reset();
    }

    private String getShipName(int size) {
        if (size == Model.SMALL_SHIP_SIZE) return "Small";
        if (size == Model.MEDIUM_SHIP_SIZE) return "Medium";
        return "Large";
    }

    // Handle clicks on player's grid (ship placement)
    public void handleMyGridClick(int r, int c) {
        // Only allow placement if not ready and ship type selected
        if (model.isReady) {
            view.updateStatus("You're already ready! Clear ships to edit");
            return;
        }

        if (model.currentShipSize == 0) {
            view.updateStatus("Select a ship type first!");
            return;
        }

        // Already placed the whole ship?
        if (model.placedCount >= model.currentShipSize) return;

        // Check if cell already has a ship
        if (!model.myGrid[r][c].equals("EMPTY")) {
            view.updateStatus("Cell already occupied!");
            return;
        }

        // Prevent clicking same cell twice
        for (Point p : model.currentShipCells) {
            if (p.x == r && p.y == c) return;
        }

        // First click
        if (model.placedCount == 0) {
            addCell(r, c);
            return;
        }

        // Second click → decide direction
        if (model.placedCount == 1) {
            Point first = model.currentShipCells.get(0);

            if (isAdjacent(first.x, first.y, r, c)) {
                if (r == first.x) model.direction = "H";
                else model.direction = "V";

                model.directionLocked = true;
                addCell(r, c);
            } else {
                view.updateStatus("Ship must be horizontal or vertical (adjacent cells only)");
            }
            return;
        }

        // Other clicks → must follow direction
        Point last = model.currentShipCells.get(model.currentShipCells.size() - 1);

        if (model.direction.equals("H")) {
            if (r == last.x && Math.abs(c - last.y) == 1) {
                addCell(r, c);
            } else {
                view.updateStatus("Continue placing horizontally!");
            }
        } else {
            if (c == last.y && Math.abs(r - last.x) == 1) {
                addCell(r, c);
            } else {
                view.updateStatus("Continue placing vertically!");
            }
        }
    }

    private boolean isAdjacent(int r1, int c1, int r2, int c2) {
        return (Math.abs(r1 - r2) == 1 && c1 == c2) ||
               (Math.abs(c1 - c2) == 1 && r1 == r2);
    }

    private void addCell(int r, int c) {
        view.myGrid[r][c].setBackground(Color.BLUE);
        model.myGrid[r][c] = "SHIP";
        model.currentShipCells.add(new Point(r, c));
        model.placedCount++;

        if (model.placedCount == model.currentShipSize) {
            // Ship fully placed
            completeShipPlacement();
        } else {
            view.updateStatus("Placed " + model.placedCount + "/" + model.currentShipSize + " cells");
        }
    }

    private void completeShipPlacement() {
        // Save the ship
        model.placedShips.add(new ArrayList<>(model.currentShipCells));

        // Update ship count
        if (model.currentShipSize == Model.SMALL_SHIP_SIZE) {
            model.smallShipsPlaced++;
        } else if (model.currentShipSize == Model.MEDIUM_SHIP_SIZE) {
            model.mediumShipsPlaced++;
        } else if (model.currentShipSize == Model.LARGE_SHIP_SIZE) {
            model.largeShipsPlaced++;
        }

        updateShipCounts();

        // Reset for next ship
        model.reset();

        if (model.allShipsPlaced()) {
            view.updateStatus("All ships placed! Click Ready when done");
        } else {
            view.updateStatus("Ship placed! Select next ship type");
        }
    }

    private void updateShipCounts() {
        int smallLeft = Model.SMALL_SHIP_COUNT - model.smallShipsPlaced;
        int mediumLeft = Model.MEDIUM_SHIP_COUNT - model.mediumShipsPlaced;
        int largeLeft = Model.LARGE_SHIP_COUNT - model.largeShipsPlaced;
        view.updateShipCount(smallLeft, mediumLeft, largeLeft);
    }

    public void resetShipPlacement() {
        // Clear only current incomplete ship
        resetCurrentShipOnly();
        view.updateStatus("Current ship placement cleared");
    }

    public void clearAllShips() {
        // Clear all ships
        for (int r = 0; r < Model.GRID_SIZE; r++) {
            for (int c = 0; c < Model.GRID_SIZE; c++) {
                if (model.myGrid[r][c].equals("SHIP")) {
                    view.myGrid[r][c].setBackground(new Color(41, 128, 185));
                    model.myGrid[r][c] = "EMPTY";
                }
            }
        }

        model.resetAll();
        updateShipCounts();
        view.setReadyButton(false);
        view.updateStatus("All ships cleared");
    }

    public void toggleReady() {
        if (!model.allShipsPlaced()) {
            view.updateStatus("Place all ships before clicking Ready!");
            return;
        }

        model.isReady = !model.isReady;
        view.setReadyButton(model.isReady);

        if (model.isReady) {
            view.updateStatus("Ready! Waiting for opponent...");
            model.gameStatus = "Ready - Waiting for opponent";
            
            // Send player name when becoming ready (both players are listening now)
            gameSocket.sendPlayerName(model.playerName);
            gameSocket.sendReady();
            
            // Check if opponent was already ready
            if (model.opponentReady && gameSocket.isServer()) {
                boolean serverStarts = Math.random() < 0.5;
                gameSocket.sendStartGame(serverStarts);
                onGameStart(serverStarts);
            }
        } else {
            view.updateStatus("Not ready");
            model.gameStatus = "Placing ships";
        }
    }

    // Handle clicks on opponent's grid (attacks)
    public void handleOpponentGridClick(int r, int c) {
        if (!model.isReady) {
            view.updateStatus("You must be ready first!");
            return;
        }

        if (!model.isMyTurn) {
            view.updateStatus("Wait for your turn!");
            return;
        }
        
        if (model.waitingForResult) {
            return; // Ignore clicks while waiting for result
        }
        
        // Check if already attacked
        if (!model.opponentGrid[r][c].equals("EMPTY")) {
            view.updateStatus("Already attacked this cell!");
            return;
        }

        // Store attack coordinates for visualization when result comes back
        model.lastAttackRow = r;
        model.lastAttackCol = c;
        model.waitingForResult = true; // Block input
        
        // Send attack to opponent
        gameSocket.sendAttack(r, c);
        view.updateStatus("Attacking...");
    }

    public void updateTimer() {
        model.timeLeft--;
        view.updateTimer(model.timeLeft);

        if (model.timeLeft <= 0) {
            // Time's up - auto switch turns
            view.stopTimer();
            if (model.isMyTurn) {
                view.updateStatus("Time's up! Opponent's turn");
                model.isMyTurn = false;
                model.waitingForResult = false; // Reset if time runs out
                view.setTurnLabel(false);
                
                // Send a miss to a random unattacked cell to switch turns
                // This simulates "passing" the turn
                gameSocket.sendAttack(-1, -1); // Special code for timeout
            }
            model.timeLeft = 30;
        }
    }

    public void showLeaderboard() {
        if (database == null) {
            JOptionPane.showMessageDialog(null, "Database not connected!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        JTable leaderboardTable = database.getLeaderboardTable();
        JScrollPane scrollPane = new JScrollPane(leaderboardTable);
        scrollPane.setPreferredSize(new Dimension(600, 400));
        
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        JButton viewGamesButton = new JButton("View Game History");
        viewGamesButton.addActionListener(e -> showGameHistory());
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(viewGamesButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        JOptionPane.showMessageDialog(null, panel, "Leaderboard - Top Players", JOptionPane.PLAIN_MESSAGE);
    }
    
    private void showGameHistory() {
        if (database == null) {
            JOptionPane.showMessageDialog(null, "Database not connected!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        JTable gamesTable = database.getGamesTable();
        JScrollPane scrollPane = new JScrollPane(gamesTable);
        scrollPane.setPreferredSize(new Dimension(800, 400));
        
        JOptionPane.showMessageDialog(null, scrollPane, "Game History - Recent 50 Games", JOptionPane.PLAIN_MESSAGE);
    }

    public void playAgain() {
        model.rematchRequested = true;
        gameSocket.sendRematchRequest();
        view.updateStatus("Waiting for opponent to accept rematch...");
        
        if (model.opponentRematchRequested) {
            gameSocket.sendRematchAccept();
            resetGameForRematch();
        }
    }
    
    private void resetGameForRematch() {
        model.resetForRematch();
        view.resetForRematch();
        updateShipCounts();
        view.updateStatus("Rematch! Place your ships");
    }
    
    // Socket callback methods
    public void onOpponentConnected() {
        view.updateStatus("Opponent connected! Place your ships");
    }
    
    public void sendPlayerName() {
        System.out.println("Sending player name: " + model.playerName);
        gameSocket.sendPlayerName(model.playerName);
    }
    
    public void onConnectionFailed() {
        view.updateStatus("Connection failed! Retrying...");
    }
    
    public void onConnectionLost() {
        view.updateStatus("Connection lost! Game ended");
        JOptionPane.showMessageDialog(null, "Connection to opponent lost!", "Connection Error", JOptionPane.ERROR_MESSAGE);
    }
    
    public void onOpponentReady() {
        view.updateStatus("Opponent is ready!");
        model.opponentReady = true;
        
        // If both players ready, start game
        if (model.isReady && gameSocket.isServer()) {
            boolean serverStarts = Math.random() < 0.5;
            gameSocket.sendStartGame(serverStarts);
            onGameStart(serverStarts);
        }
    }
    
    public void onGameStart(boolean myTurnFirst) {
        model.gameStatus = "Game Started! " + (myTurnFirst ? "Your Turn" : "Opponent's Turn");
        view.updateStatus(model.gameStatus);
        model.isMyTurn = myTurnFirst;
        view.setTurnLabel(myTurnFirst);
        
        // Reset hits for new game
        model.myHits = 0;
        model.opponentHits = 0;
        
        // Disable ship placement
        // ... (rest of logic handled by checking isReady in click handlers)
        
        if (myTurnFirst) {
            model.timeLeft = 30;
            view.updateTimer(30);
            view.startTimer();
        }
    }
    
    // Handle opponent's attack on our grid
    public void onOpponentAttack(int row, int col) {
        // Check if this is a timeout (automatic turn switch)
        if (row == -1 && col == -1) {
            // Opponent ran out of time, now it's our turn
            model.isMyTurn = true;
            view.setTurnLabel(true);
            model.timeLeft = 30;
            view.updateTimer(30);
            view.startTimer();
            view.updateStatus("Opponent's time ran out! Your turn");
            gameSocket.sendAttackResult(false); // Send miss
            return;
        }
        
        boolean isHit = model.myGrid[row][col].equals("SHIP");
        
        if (isHit) {
            view.myGrid[row][col].setBackground(Color.RED);
            model.myGrid[row][col] = "HIT";
            model.opponentHits++;
            gameSocket.sendAttackResult(true);
            view.updateStatus("Opponent HIT your ship! Their turn continues");
            
            // Check if all our ships are sunk
            if (checkAllShipsSunk()) {
                onOpponentWins();
            }
        } else {
            view.myGrid[row][col].setBackground(Color.YELLOW);
            model.myGrid[row][col] = "MISS";
            gameSocket.sendAttackResult(false);
            
            // Our turn now
            model.isMyTurn = true;
            view.setTurnLabel(true);
            model.timeLeft = 30;
            view.updateTimer(30);
            view.startTimer();
            view.updateStatus("Opponent missed! Your turn");
        }
    }
    
    // Handle result of our attack
    public void onAttackResult(boolean isHit) {
        // Update the opponent grid with the attack result
        SwingUtilities.invokeLater(() -> {
            if (model.lastAttackRow >= 0 && model.lastAttackCol >= 0) {
            if (isHit) {
                view.opponentGrid[model.lastAttackRow][model.lastAttackCol].setBackground(Color.RED);
                model.opponentGrid[model.lastAttackRow][model.lastAttackCol] = "HIT";
                model.myHits++;
                
                int totalHitsNeeded = Model.SMALL_SHIP_SIZE * Model.SMALL_SHIP_COUNT +
                                      Model.MEDIUM_SHIP_SIZE * Model.MEDIUM_SHIP_COUNT +
                                      Model.LARGE_SHIP_SIZE * Model.LARGE_SHIP_COUNT;
                                      
                view.updateStatus("HIT! You get another turn (Hits: " + model.myHits + "/" + totalHitsNeeded + ")");
                
                // Keep our turn
                model.timeLeft = 30;
                view.updateTimer(30);
                
                // Check if we won
                if (model.myHits >= totalHitsNeeded) {
                    onWeWin();
                }
            } else {
                view.opponentGrid[model.lastAttackRow][model.lastAttackCol].setBackground(Color.YELLOW);
                model.opponentGrid[model.lastAttackRow][model.lastAttackCol] = "MISS";
                view.updateStatus("MISS! Opponent's turn");
                model.isMyTurn = false;
                view.setTurnLabel(false);
                view.stopTimer();
            }
            }
            
            // Reset last attack coordinates
            model.lastAttackRow = -1;
            model.lastAttackCol = -1;
            model.waitingForResult = false; // Unblock input
        });
    }
    
    private boolean checkAllShipsSunk() {
        int requiredHits = Model.SMALL_SHIP_SIZE * Model.SMALL_SHIP_COUNT +
                          Model.MEDIUM_SHIP_SIZE * Model.MEDIUM_SHIP_COUNT +
                          Model.LARGE_SHIP_SIZE * Model.LARGE_SHIP_COUNT;
        return model.opponentHits >= requiredHits;
    }
    
    private void onWeWin() {
        gameSocket.sendWin();
        
        // Save to database (in background thread)
        try {
            if (database != null) {
                String opponentName = model.opponentName.isEmpty() ? "Opponent" : model.opponentName;
                database.saveGameResult(model.playerName, opponentName, model.playerName, 
                                       model.myHits, model.opponentHits);
            }
        } catch (Exception e) {
            System.err.println("Error saving result: " + e.getMessage());
        }
        
        // Update UI on EDT
        SwingUtilities.invokeLater(() -> {
            view.stopTimer();
            view.showWinDialog();
        });
    }
    
    public void onOpponentWins() {
        // Save to database
        try {
            if (database != null) {
                String opponentName = model.opponentName.isEmpty() ? "Opponent" : model.opponentName;
                database.saveGameResult(model.playerName, opponentName, opponentName,
                                       model.myHits, model.opponentHits);
            }
        } catch (Exception e) {
            System.err.println("Error saving result: " + e.getMessage());
        }
        
        // Update UI on EDT
        SwingUtilities.invokeLater(() -> {
            view.stopTimer();
            view.showLossDialog();
        });
    }
    
    public void onPlayerNameReceived(String name) {
        System.out.println("Received opponent name: " + name + " (isServer: " + gameSocket.isServer() + ")");
        model.opponentName = name;
        SwingUtilities.invokeLater(() -> {
            view.setOpponentName(name);
            view.updateStatus("Playing against " + name + " - Place your ships");
        });
    }
    
    public void onRematchRequest() {
        model.opponentRematchRequested = true;
        view.updateStatus(model.opponentName + " wants a rematch!");
        
        if (model.rematchRequested) {
            gameSocket.sendRematchAccept();
            resetGameForRematch();
        }
    }
    
    public void onRematchAccept() {
        resetGameForRematch();
    }
}

