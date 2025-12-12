package proj;

import java.io.*;
import java.net.*;

public class GameSocket {
    private java.net.Socket socket;
    private ServerSocket serverSocket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean isServer;
    private Controller controller;
    
    public GameSocket() {
    }
    
    public void setController(Controller controller) {
        this.controller = controller;
    }
    
    // Start as server (host game)
    public void startServer(int port) {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                System.out.println("Server started on port " + port + ". Waiting for opponent...");
                isServer = true;
                
                socket = serverSocket.accept();
                System.out.println("Opponent connected!");
                
                setupStreams();
                if (controller != null) {
                    controller.onOpponentConnected();
                }
                listenForMessages();
            } catch (IOException e) {
                System.err.println("Server error: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }
    
    // Connect to server (join game)
    public void connectToServer(String host, int port) {
        new Thread(() -> {
            try {
                System.out.println("Connecting to " + host + ":" + port + "...");
                socket = new Socket(host, port);
                System.out.println("Connected to server!");
                isServer = false;
                
                setupStreams();
                if (controller != null) {
                    controller.onOpponentConnected();
                }
                listenForMessages();
            } catch (IOException e) {
                System.err.println("Connection error: " + e.getMessage());
                if (controller != null) {
                    controller.onConnectionFailed();
                }
                e.printStackTrace();
            }
        }).start();
    }
    
    private void setupStreams() throws IOException {
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
    }
    
    private void listenForMessages() {
        new Thread(() -> {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    handleMessage(message);
                }
            } catch (IOException e) {
                System.err.println("Connection lost: " + e.getMessage());
                if (controller != null) {
                    controller.onConnectionLost();
                }
            }
        }).start();
    }
    
    private void handleMessage(String message) {
        if (controller == null) return;
        
        String[] parts = message.split(":");
        String command = parts[0];
        
        switch (command) {
            case "PLAYER_NAME":
                String opponentName = parts.length > 1 ? parts[1] : "Opponent";
                controller.onPlayerNameReceived(opponentName);
                break;
                
            case "READY":
                controller.onOpponentReady();
                break;
                
            case "START":
                boolean serverStarts = Boolean.parseBoolean(parts[1]);
                controller.onGameStart(isServer ? serverStarts : !serverStarts);
                break;
                
            case "ATTACK":
                int row = Integer.parseInt(parts[1]);
                int col = Integer.parseInt(parts[2]);
                controller.onOpponentAttack(row, col);
                break;
                
            case "RESULT":
                boolean isHit = Boolean.parseBoolean(parts[1]);
                controller.onAttackResult(isHit);
                break;
                
            case "WIN":
                controller.onOpponentWins();
                break;
                
            case "REMATCH_REQUEST":
                controller.onRematchRequest();
                break;
                
            case "REMATCH_ACCEPT":
                controller.onRematchAccept();
                break;
                
            case "CHAT":
                String chatMessage = parts[1];
                System.out.println("Opponent: " + chatMessage);
                break;
                
            default:
                System.err.println("Unknown command: " + command);
        }
    }
    
    public void sendReady() {
        sendMessage("READY");
    }
    
    public void sendStartGame(boolean serverStarts) {
        sendMessage("START:" + serverStarts);
    }
    
    public void sendAttack(int row, int col) {
        sendMessage("ATTACK:" + row + ":" + col);
    }
    
    public void sendAttackResult(boolean isHit) {
        sendMessage("RESULT:" + isHit);
    }
    
    public void sendWin() {
        sendMessage("WIN");
    }
    
    public void sendPlayerName(String name) {
        sendMessage("PLAYER_NAME:" + name);
    }
    
    public void sendRematchRequest() {
        sendMessage("REMATCH_REQUEST");
    }
    
    public void sendRematchAccept() {
        sendMessage("REMATCH_ACCEPT");
    }
    
    public void sendChat(String message) {
        sendMessage("CHAT:" + message);
    }
    
    private void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }
    
    public boolean isServer() {
        return isServer;
    }
    
    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }
    
    public void close() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
            if (serverSocket != null) serverSocket.close();
            System.out.println("Socket closed.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
