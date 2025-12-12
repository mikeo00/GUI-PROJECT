package proj;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.Timer;

public class View {
    private Controller cont;
    
    // Grids
    public JButton[][] myGrid = new JButton[8][8];
    public JButton[][] opponentGrid = new JButton[8][8];
    
    // Main frame and panels
    private JFrame f = new JFrame("BATTLESHIP MULTIPLAYER");
    private JPanel topPanel = new JPanel();
    private JPanel centerPanel = new JPanel();
    private JPanel bottomPanel = new JPanel();
    
    // Top panel components
    private JLabel turnLabel = new JLabel("YOUR TURN", SwingConstants.CENTER);
    private JLabel gameNameLabel = new JLabel("BATTLESHIP", SwingConstants.CENTER);
    private JLabel playerNameLabel = new JLabel("PLAYER: ", SwingConstants.CENTER);
    private JLabel opponentNameLabel = new JLabel("OPPONENT: ...", SwingConstants.CENTER);
    private JButton rulesButton = new JButton("RULES");
    private JLabel timerLabel = new JLabel("TIME: 30s", SwingConstants.CENTER);
    
    // Bottom panel components
    private JLabel statusLabel = new JLabel("Place your ships", SwingConstants.CENTER);
    private JButton clearButton = new JButton("Clear Ship");
    private JButton readyButton = new JButton("Not Ready");
    private JButton leaderboardButton = new JButton("Leaderboard");
    
    // Ship selector panel
    private JPanel shipSelectorPanel = new JPanel();
    private JButton smallShipBtn = new JButton("Small (2)");
    private JButton mediumShipBtn = new JButton("Medium (3)");
    private JButton largeShipBtn = new JButton("Large (4)");
    private JLabel shipCountLabel = new JLabel("Ships: S:1 M:2 L:1");
    
    // Timer
    private Timer turnTimer;
    
    public View(Controller cont) {
        this.cont = cont;
        cont.setView(this);
        
        // Show name input dialog
        showNameDialog();
        
        // Initialize UI
        setupFrame();
        setupTopPanel();
        setupCenterPanel();
        setupBottomPanel();
        setupTimer();
        
        f.setVisible(true);
    }
    
    public void dispose() {
        f.dispose();
    }
    
    private void showNameDialog() {
        String name = JOptionPane.showInputDialog(null, "Enter your name:", "Player Name", JOptionPane.QUESTION_MESSAGE);
        if (name != null && !name.trim().isEmpty()) {
            cont.setPlayerName(name.trim());
        } else {
            cont.setPlayerName("Player");
        }
    }
    
    private void setupFrame() {
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setSize(1200, 800);
        f.setLayout(new BorderLayout(10, 10));
        f.add(topPanel, BorderLayout.NORTH);
        f.add(centerPanel, BorderLayout.CENTER);
        f.add(bottomPanel, BorderLayout.SOUTH);
    }
    
    private void setupTopPanel() {
        topPanel.setLayout(new GridLayout(1, 6, 5, 5));
        topPanel.setBackground(new Color(52, 73, 94));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Style labels
        styleLabel(turnLabel, Color.RED);
        styleLabel(gameNameLabel, Color.WHITE);
        gameNameLabel.setFont(new Font("Arial", Font.BOLD, 20));
        styleLabel(playerNameLabel, Color.CYAN);
        styleLabel(opponentNameLabel, Color.ORANGE);
        styleLabel(timerLabel, Color.YELLOW);
        
        rulesButton.setBackground(Color.WHITE);
        rulesButton.addActionListener(e -> showRules());
        
        topPanel.add(turnLabel);
        topPanel.add(gameNameLabel);
        topPanel.add(playerNameLabel);
        topPanel.add(opponentNameLabel);
        topPanel.add(rulesButton);
        topPanel.add(timerLabel);
    }
    
    private void setupCenterPanel() {
        centerPanel.setLayout(new GridLayout(1, 3, 10, 10));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        centerPanel.setBackground(new Color(44, 62, 80));
        
        // Ship selector panel (left)
        setupShipSelector();
        
        // Player's grid (center)
        JPanel myGridPanel = createGridPanel("YOUR SHIPS", myGrid, true);
        
        // Opponent's grid (right)
        JPanel opponentGridPanel = createGridPanel("OPPONENT'S GRID", opponentGrid, false);
        
        centerPanel.add(shipSelectorPanel);
        centerPanel.add(myGridPanel);
        centerPanel.add(opponentGridPanel);
    }
    
    private void setupShipSelector() {
        shipSelectorPanel.setLayout(new GridLayout(5, 1, 5, 10));
        shipSelectorPanel.setBackground(new Color(52, 73, 94));
        shipSelectorPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.WHITE, 2), 
            "Ship Selector",
            0, 0, new Font("Arial", Font.BOLD, 14), Color.WHITE));
        
        // Style ship buttons
        styleShipButton(smallShipBtn, new Color(46, 204, 113));
        styleShipButton(mediumShipBtn, new Color(52, 152, 219));
        styleShipButton(largeShipBtn, new Color(155, 89, 182));
        
        smallShipBtn.addActionListener(e -> cont.selectShipType(Model.SMALL_SHIP_SIZE));
        mediumShipBtn.addActionListener(e -> cont.selectShipType(Model.MEDIUM_SHIP_SIZE));
        largeShipBtn.addActionListener(e -> cont.selectShipType(Model.LARGE_SHIP_SIZE));
        
        shipCountLabel.setForeground(Color.WHITE);
        shipCountLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        shipCountLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        shipSelectorPanel.add(new JLabel()); // Spacer
        shipSelectorPanel.add(smallShipBtn);
        shipSelectorPanel.add(mediumShipBtn);
        shipSelectorPanel.add(largeShipBtn);
        shipSelectorPanel.add(shipCountLabel);
    }
    
    private void styleShipButton(JButton btn, Color color) {
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Arial", Font.BOLD, 14));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
    }
    
    private JPanel createGridPanel(String title, JButton[][] grid, boolean isPlayerGrid) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(52, 73, 94));
        
        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        JPanel gridPanel = new JPanel(new GridLayout(8, 8, 2, 2));
        gridPanel.setBackground(Color.BLACK);
        gridPanel.setBorder(BorderFactory.createLineBorder(Color.WHITE, 3));
        
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                JButton btn = new JButton();
                btn.setBackground(new Color(41, 128, 185)); // Ocean blue
                btn.setPreferredSize(new Dimension(50, 50));
                btn.setFocusPainted(false);
                grid[r][c] = btn;
                
                int row = r, col = c;
                if (isPlayerGrid) {
                    btn.addActionListener(e -> cont.handleMyGridClick(row, col));
                } else {
                    btn.addActionListener(e -> cont.handleOpponentGridClick(row, col));
                }
                
                gridPanel.add(btn);
            }
        }
        
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(gridPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void setupBottomPanel() {
        bottomPanel.setLayout(new GridLayout(1, 4, 5, 5));
        bottomPanel.setBackground(new Color(52, 73, 94));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        
        clearButton.setBackground(new Color(231, 76, 60));
        clearButton.setForeground(Color.WHITE);
        clearButton.setFont(new Font("Arial", Font.BOLD, 12));
        clearButton.addActionListener(e -> cont.clearAllShips());
        
        readyButton.setBackground(new Color(230, 126, 34));
        readyButton.setForeground(Color.WHITE);
        readyButton.setFont(new Font("Arial", Font.BOLD, 12));
        readyButton.addActionListener(e -> cont.toggleReady());
        
        leaderboardButton.setBackground(new Color(52, 152, 219));
        leaderboardButton.setForeground(Color.WHITE);
        leaderboardButton.setFont(new Font("Arial", Font.BOLD, 12));
        leaderboardButton.addActionListener(e -> cont.showLeaderboard());
        
        bottomPanel.add(statusLabel);
        bottomPanel.add(clearButton);
        bottomPanel.add(readyButton);
        bottomPanel.add(leaderboardButton);
    }
    
    private void setupTimer() {
        turnTimer = new Timer(1000, e -> cont.updateTimer());
    }
    
    private void styleLabel(JLabel label, Color color) {
        label.setForeground(color);
        label.setFont(new Font("Arial", Font.BOLD, 14));
    }
    
    // Public methods for controller
    public void setPlayerName(String name) {
        playerNameLabel.setText("PLAYER: " + name);
    }
    
    public void updateStatus(String status) {
        statusLabel.setText(status);
    }
    
    public void setOpponentName(String name) {
        opponentNameLabel.setText("OPPONENT: " + name);
    }
    
    public void resetForRematch() {
        // Reset grids
        Color oceanBlue = new Color(41, 128, 185);
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                myGrid[r][c].setBackground(oceanBlue);
                opponentGrid[r][c].setBackground(oceanBlue);
            }
        }
        
        // Reset UI
        setReadyButton(false);
        turnLabel.setText("YOUR TURN");
        turnLabel.setForeground(Color.RED);
        timerLabel.setText("TIME: 30s");
        timerLabel.setForeground(Color.YELLOW);
        stopTimer();
    }
    
    public void updateTimer(int seconds) {
        timerLabel.setText("TIME: " + seconds + "s");
        if (seconds <= 5) {
            timerLabel.setForeground(Color.RED);
        } else {
            timerLabel.setForeground(Color.YELLOW);
        }
    }
    
    public void updateShipCount(int small, int medium, int large) {
        shipCountLabel.setText("Ships: S:" + small + " M:" + medium + " L:" + large);
    }
    
    public void setReadyButton(boolean ready) {
        if (ready) {
            readyButton.setText("Ready");
            readyButton.setBackground(new Color(46, 204, 113));
        } else {
            readyButton.setText("Not Ready");
            readyButton.setBackground(new Color(230, 126, 34));
        }
    }
    
    public void setTurnLabel(boolean myTurn) {
        if (myTurn) {
            turnLabel.setText("YOUR TURN");
            turnLabel.setForeground(Color.GREEN);
        } else {
            turnLabel.setText("OPPONENT'S TURN");
            turnLabel.setForeground(Color.RED);
        }
    }
    
    public void startTimer() {
        turnTimer.start();
    }
    
    public void stopTimer() {
        turnTimer.stop();
    }
    
    private void showRules() {
        String rules = "BATTLESHIP GAME RULES\n\n" +
                      "Ships:\n" +
                      "  - 1 Small (2 cells)\n" +
                      "  - 2 Medium (3 cells)\n" +
                      "  - 1 Large (4 cells)\n\n" +
                      "Placement:\n" +
                      "  - Select ship type\n" +
                      "  - Click grid cells to place\n" +
                      "  - Must be horizontal or vertical\n\n" +
                      "Ready: Click 'Ready' when all ships placed\n\n" +
                      "Attack:\n" +
                      "  - Click opponent's grid during your turn\n" +
                      "  - Red = Hit, Yellow = Miss\n\n" +
                      "Turn Timer: 30 seconds per turn\n" +
                      "Extra Turn: Get another turn when you hit!\n" +
                      "Win: Sink all opponent's ships first";
        
        JTextArea textArea = new JTextArea(rules);
        textArea.setEditable(false);
        textArea.setFont(new Font("Arial", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(400, 400));
        
        JOptionPane.showMessageDialog(f, scrollPane, "Game Rules", JOptionPane.INFORMATION_MESSAGE);
    }
    
    public void showWinDialog() {
        int choice = JOptionPane.showConfirmDialog(f, 
            "Congratulations! You Won!\n\nPlay again?",
            "Victory!", 
            JOptionPane.YES_NO_OPTION,
            JOptionPane.INFORMATION_MESSAGE);
        
        if (choice == JOptionPane.YES_OPTION) {
            cont.playAgain();
        } else {
            System.exit(0);
        }
    }
    
    public void showLossDialog() {
        int choice = JOptionPane.showConfirmDialog(f,
            "You Lost! Better luck next time.\n\nPlay again?",
            "Game Over",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.INFORMATION_MESSAGE);
        
        if (choice == JOptionPane.YES_OPTION) {
            cont.playAgain();
        } else {
            System.exit(0);
        }
    }
}
