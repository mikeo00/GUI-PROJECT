package proj;

import java.awt.Point;
import java.util.List;

public class Ship {
    private String type;
    private int size;
    private List<Point> cells;
    private int hits;
    
    public Ship(String type, int size, List<Point> cells) {
        this.type = type;
        this.size = size;
        this.cells = cells;
        this.hits = 0;
    }
    
    public String getType() {
        return type;
    }
    
    public int getSize() {
        return size;
    }
    
    public List<Point> getCells() {
        return cells;
    }
    
    public void hit() {
        hits++;
    }
    
    public boolean isSunk() {
        return hits >= size;
    }
    
    public int getHits() {
        return hits;
    }
}