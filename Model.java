package proj;
import java.awt.*;
import java.util.*;
import java.util.List;

public class Model {
    // Ship types and sizes
    public static final int SMALL_SHIP_SIZE = 2;
    public static final int MEDIUM_SHIP_SIZE = 3;
    public static final int LARGE_SHIP_SIZE = 4;
    
    // Ship counts per player
    public static final int SMALL_SHIP_COUNT = 1;
    public static final int MEDIUM_SHIP_COUNT = 2;
    public static final int LARGE_SHIP_COUNT = 1;
    
    // Grid size
    public static final int GRID_SIZE = 8;
    
    // Game state
    public String playerName = "";
    public String opponentName = "";
    public boolean isReady = false;
    public boolean opponentReady = false; // Track opponent readiness
    public boolean isMyTurn = false;
    public boolean rematchRequested = false;
    public boolean opponentRematchRequested = false;
    public int timeLeft = 30;
    
    // Current ship placement
    public int currentShipSize = 0;
    public int placedCount = 0;
    public List<Point> currentShipCells = new ArrayList<>();
    public boolean directionLocked = false;
    public String direction = "";
    
    // Ships tracking
    public int smallShipsPlaced = 0;
    public int mediumShipsPlaced = 0;
    public int largeShipsPlaced = 0;
    public List<List<Point>> placedShips = new ArrayList<>();
    
    // Player's own grid (ship placements)
    public String[][] myGrid = new String[GRID_SIZE][GRID_SIZE];
    
    // Opponent's grid (attack results)
    public String[][] opponentGrid = new String[GRID_SIZE][GRID_SIZE];
    
    // Game status
    public String gameStatus = "Waiting for connection...";
    public int myScore = 0;
    public int opponentScore = 0;
    public int myHits = 0;
    public int opponentHits = 0;
    
    // Track last attack for visualization
    public int lastAttackRow = -1;
    public int lastAttackCol = -1;
    public boolean waitingForResult = false;
    
    public Model() {
        initializeGrids();
    }
    
    private void initializeGrids() {
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                myGrid[i][j] = "EMPTY";
                opponentGrid[i][j] = "EMPTY";
            }
        }
    }
    
    public boolean allShipsPlaced() {
        return smallShipsPlaced >= SMALL_SHIP_COUNT &&
               mediumShipsPlaced >= MEDIUM_SHIP_COUNT &&
               largeShipsPlaced >= LARGE_SHIP_COUNT;
    }
    
    public void reset() {
        placedCount = 0;
        currentShipSize = 0; // Reset selection so user must select again
        currentShipCells.clear();
        directionLocked = false;
        direction = "";
    }
    
    public void resetAll() {
        smallShipsPlaced = 0;
        mediumShipsPlaced = 0;
        largeShipsPlaced = 0;
        placedShips.clear();
        initializeGrids();
        reset();
        isReady = false;
        myHits = 0;
        opponentHits = 0;
    }
    
    public void resetForRematch() {
        smallShipsPlaced = 0;
        mediumShipsPlaced = 0;
        largeShipsPlaced = 0;
        placedShips.clear();
        initializeGrids();
        reset();
        isReady = false;
        opponentReady = false;
        myHits = 0;
        opponentHits = 0;
        isMyTurn = false;
        timeLeft = 30;
        rematchRequested = false;
        opponentRematchRequested = false;
        lastAttackRow = -1;
        lastAttackCol = -1;
        waitingForResult = false;
    }
}