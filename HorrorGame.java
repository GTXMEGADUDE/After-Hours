import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;

public class HorrorGame extends JPanel implements ActionListener, KeyListener {
    private static final int WIDTH = 1920;
    private static final int HEIGHT = 1080;
    private static final int GROUND_HEIGHT = 900;
    private static final int WORLD_WIDTH = 3840;

    private Player player;
    private Monster monster;
    private ItemManager itemManager;
    private WorldGenerator worldGenerator;
    private SoundManager soundManager;
    
    // Background images
    private BufferedImage backgroundFar;
    private BufferedImage backgroundMid;
    private BufferedImage backgroundNear;
    private BufferedImage ground;
    private BufferedImage jumpscareImage;
    private BufferedImage titleScreenImage;
    
    // Game states
    private enum GameState { TITLE, PLAYING, GAME_OVER, GAME_WON }
    private GameState gameState = GameState.TITLE;
    
    private boolean showingJumpscare = false;
    private int jumpscareTimer = 0;
    private static final int JUMPSACRE_DURATION = 300; // 5 seconds at 60fps
    private Timer timer;
    private int cameraX = 0;
    private boolean playerMoving = false;
    private boolean leftPressed = false;
    private boolean rightPressed = false;

    // Scoring system variables
    private int gameStartTime;
    private int totalHidingTime = 0;
    private int score = 0;
    private boolean scoreCalculated = false;
    private int finalGameTime = 0;

    // Title screen animation
    private float titleAlpha = 0.0f;
    private boolean titleFadingIn = true;
    private int titleTimer = 0;

    public HorrorGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        addKeyListener(this);
        setFocusable(true);
        
        loadImageFiles();
        initializeGame();
        
        timer = new Timer(1000 / 60, this);
        timer.start();
    }
    
    private void loadImageFiles() {
        try {
            backgroundFar = ImageIO.read(new File("background_far.png"));
            backgroundMid = ImageIO.read(new File("background_mid.png"));
            backgroundNear = ImageIO.read(new File("background_near.png"));
            ground = ImageIO.read(new File("ground.png"));
            jumpscareImage = ImageIO.read(new File("jumpscare.png"));
            
            // Try to load title screen image, use fallback if not found
            try {
                titleScreenImage = ImageIO.read(new File("title_screen.png"));
                titleScreenImage = scaleImage(titleScreenImage, WIDTH, HEIGHT);
            } catch (IOException e) {
                System.err.println("Title screen image not found, using generated title screen");
                titleScreenImage = createFallbackTitleScreen();
            }
            
            // REMOVED: Background scaling - use original image sizes
            // backgroundFar = scaleImage(backgroundFar, WORLD_WIDTH, HEIGHT);
            // backgroundMid = scaleImage(backgroundMid, WORLD_WIDTH, HEIGHT);
            // backgroundNear = scaleImage(backgroundNear, WORLD_WIDTH, HEIGHT);
            // ground = scaleImage(ground, WORLD_WIDTH, HEIGHT - GROUND_HEIGHT);
            
        } catch (IOException e) {
            showImageErrorDialog(e);
            System.exit(1);
        }
    }
    
    private void drawTiledBackground(Graphics g, BufferedImage bgImage, int offsetX) {
        if (bgImage == null) return;
        
        int bgWidth = bgImage.getWidth();
        int bgHeight = bgImage.getHeight();
        
        // Calculate how many times to tile the image horizontally
        int tilesNeeded = (int) Math.ceil((double) WIDTH / bgWidth) + 1;
        
        // Draw tiled background
        for (int i = 0; i < tilesNeeded; i++) {
            int x = offsetX + (i * bgWidth);
            // Only draw if visible on screen
            if (x + bgWidth > 0 && x < WIDTH) {
                g.drawImage(bgImage, x, 0, null);
            }
        }
    }
    
    private BufferedImage createFallbackTitleScreen() {
        BufferedImage title = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = title.createGraphics();
        
        // Dark background
        GradientPaint gradient = new GradientPaint(0, 0, new Color(10, 10, 30), 0, HEIGHT, new Color(0, 0, 0));
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);
        
        // Add some creepy elements
        g2d.setColor(new Color(30, 0, 0, 100));
        for (int i = 0; i < 50; i++) {
            int x = (int)(Math.random() * WIDTH);
            int y = (int)(Math.random() * HEIGHT);
            int size = 2 + (int)(Math.random() * 8);
            g2d.fillOval(x, y, size, size);
        }
        
        // Main title
        g2d.setColor(Color.RED);
        g2d.setFont(new Font("Arial", Font.BOLD, 96));
        String mainTitle = "After Hours";
        int titleWidth = g2d.getFontMetrics().stringWidth(mainTitle);
        g2d.drawString(mainTitle, WIDTH/2 - titleWidth/2, HEIGHT/2 - 50);
        
        // Subtitle
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 36));
        String subtitle = "3 Floors of Horror";
        int subtitleWidth = g2d.getFontMetrics().stringWidth(subtitle);
        g2d.drawString(subtitle, WIDTH/2 - subtitleWidth/2, HEIGHT/2 + 30);
        
        // Controls hint
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.setFont(new Font("Arial", Font.PLAIN, 18));
        String controlsText = "Controls: Arrow Keys/WASD to move, E to interact, W/S for stairs";
        int controlsWidth = g2d.getFontMetrics().stringWidth(controlsText);
        g2d.drawString(controlsText, WIDTH/2 - controlsWidth/2, HEIGHT - 100);
        
        g2d.dispose();
        return title;
    }
    
    private void showImageErrorDialog(IOException e) {
        JOptionPane.showMessageDialog(this, 
            "Error loading image files!\n" +
            "Please make sure you have these PNG files in the same directory:\n" +
            "- player.png\n- monster.png\n- note.png\n- key.png\n- exit.png\n" +
            "- background_far.png\n- background_mid.png\n- background_near.png\n" +
            "- locker.png\n- ground.png\n- staircase_up.png\n- staircase_down.png\n" +
            "- obstacle.png\n- door_entrance.png\n- door_exit.png\n" +
            "- classroom_bg.png\n- classroom_ground.png\n" +
            "- jumpscare.png\n- title_screen.png (optional)\n\n" +
            "Error: " + e.getMessage(),
            "Image Loading Error",
            JOptionPane.ERROR_MESSAGE);
    }
    
    private BufferedImage scaleImage(BufferedImage original, int newWidth, int newHeight) {
        BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, original.getType());
        Graphics2D g2d = scaledImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(original, 0, 0, newWidth, newHeight, null);
        g2d.dispose();
        return scaledImage;
    }
    
    private void initializeGame() {
        soundManager = new SoundManager();
        worldGenerator = new WorldGenerator(WORLD_WIDTH, GROUND_HEIGHT);
        itemManager = new ItemManager(WORLD_WIDTH, GROUND_HEIGHT);
        player = new Player(200, GROUND_HEIGHT, soundManager);
        monster = new Monster(WIDTH + 300, GROUND_HEIGHT, WORLD_WIDTH);
        
        worldGenerator.generateWorld(itemManager);
        
        cameraX = 0;
        gameState = GameState.TITLE;
        showingJumpscare = false;
        jumpscareTimer = 0;
        playerMoving = false;
        leftPressed = false;
        rightPressed = false;
        
        // Initialize title screen animation
        titleAlpha = 0.0f;
        titleFadingIn = true;
        titleTimer = 0;
        
        // Initialize scoring system
        gameStartTime = (int)(System.currentTimeMillis() / 1000);
        totalHidingTime = 0;
        score = 0;
        scoreCalculated = false;
        finalGameTime = 0;
        
        soundManager.startAmbientSound();
    }
    
    private void startGame() {
        gameState = GameState.PLAYING;
        soundManager.startAmbientSound();
    }
    
    private void triggerJumpscare() {
        showingJumpscare = true;
        jumpscareTimer = JUMPSACRE_DURATION;
        soundManager.playJumpscareSound();
        
        // Set final game time when caught
        finalGameTime = (int)(System.currentTimeMillis() / 1000) - gameStartTime;
    }
    
    private void updateCamera() {
        int targetX = player.getX() - WIDTH / 2;
        cameraX = Math.max(0, Math.min(targetX, WORLD_WIDTH - WIDTH));
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        Graphics2D g2d = (Graphics2D) g;
        
        switch (gameState) {
            case TITLE:
                drawTitleScreen(g2d);
                break;
            case PLAYING:
                if (showingJumpscare) {
                    // Draw jumpscare screen
                    g.drawImage(jumpscareImage, 0, 0, WIDTH, HEIGHT, null);
                    
                    // Show game over text OVERLAY on top of jumpscare image after 3 seconds
                    if (jumpscareTimer <= JUMPSACRE_DURATION - 180) {
                        drawGameOverTextOverlay(g);
                    }
                } else {
                    // Draw normal game
                    drawGameWorld(g);
                }
                break;
            case GAME_WON:
                // Draw win screen
                drawGameWorld(g);
                drawGameEndScreen(g);
                break;
            case GAME_OVER:
                // Already handled in jumpscare
                break;
        }
    }
    
    private void drawTitleScreen(Graphics2D g2d) {
        // Draw background
        g2d.drawImage(titleScreenImage, 0, 0, null);
        
        // Apply pulsing effect to start text
        float pulse = (float)(0.7f + 0.3f * Math.sin(titleTimer * 0.1f));
        
        // Draw "Press SPACE to Start" with pulsing effect
        g2d.setColor(new Color(1.0f, 1.0f, 0.0f, pulse));
        g2d.setFont(new Font("Arial", Font.BOLD, 36));
        String startText = "Press SPACE to Start";
        int startWidth = g2d.getFontMetrics().stringWidth(startText);
        g2d.drawString(startText, WIDTH/2 - startWidth/2, HEIGHT/2 + 150);
        
        // Draw version info
        g2d.setColor(new Color(1.0f, 1.0f, 1.0f, 0.7f));
        g2d.setFont(new Font("Arial", Font.PLAIN, 16));
        g2d.drawString("Horror Escape v1.0 - Find all notes and the key to escape!", WIDTH/2 - 250, HEIGHT - 50);
        
        // Draw creepy subtitle that fades in
        if (titleTimer > 60) {
            float subtitleAlpha = Math.min(1.0f, (titleTimer - 60) / 60.0f);
            g2d.setColor(new Color(1.0f, 0.2f, 0.2f, subtitleAlpha));
            g2d.setFont(new Font("Arial", Font.ITALIC, 24));
            String subtitle = "Can you survive all 3 floors?";
            int subtitleWidth = g2d.getFontMetrics().stringWidth(subtitle);
            g2d.drawString(subtitle, WIDTH/2 - subtitleWidth/2, HEIGHT/2 + 200);
        }
    }
    
    // Draw just the game over text on top of jumpscare image
    private void drawGameOverTextOverlay(Graphics g) {
        // Semi-transparent dark overlay (but not full coverage)
        g.setColor(new Color(0, 0, 0, 120)); // Less opaque so jumpscare is still visible
        g.fillRect(0, HEIGHT/2 - 100, WIDTH, 300); // Only cover middle section
        
        // Game over text
        g.setColor(Color.RED);
        g.setFont(new Font("Arial", Font.BOLD, 72));
        String gameOverText = "GAME OVER - MONSTER CAUGHT YOU!";
        int textWidth = g.getFontMetrics().stringWidth(gameOverText);
        g.drawString(gameOverText, WIDTH/2 - textWidth/2, HEIGHT/2 - 30);
        
        // Score text (always 0 when caught)
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 36));
        String scoreText = "Score: 0 (Failed Escape)";
        textWidth = g.getFontMetrics().stringWidth(scoreText);
        g.drawString(scoreText, WIDTH/2 - textWidth/2, HEIGHT/2 + 30);
        
        // Show time played when caught
        int minutes = finalGameTime / 60;
        int seconds = finalGameTime % 60;
        String timeText = "Time: " + String.format("%02d:%02d", minutes, seconds);
        textWidth = g.getFontMetrics().stringWidth(timeText);
        g.drawString(timeText, WIDTH/2 - textWidth/2, HEIGHT/2 + 80);
        
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 24));
        String restartText = "Press R to restart or ESC for title screen";
        textWidth = g.getFontMetrics().stringWidth(restartText);
        g.drawString(restartText, WIDTH/2 - textWidth/2, HEIGHT/2 + 130);
    }
    
    private void drawGameWorld(Graphics g) {
        if (itemManager.isInClassroom()) {
            // DRAW CLASSROOM
            itemManager.draw(g, cameraX, WIDTH, GROUND_HEIGHT);
            
            // Draw player in classroom
            player.drawInClassroom(g);
            
            // Draw monster in classroom if active
            if (monster.isActive() && monster.isInClassroom()) {
                monster.draw(g, cameraX, WIDTH, itemManager.getCurrentFloor(), itemManager);
            }
        } else {
            // DRAW MAIN FLOOR
            // Draw parallax backgrounds without scaling
            drawTiledBackground(g, backgroundFar, -cameraX / 4);
            drawTiledBackground(g, backgroundMid, -cameraX / 2);
            drawTiledBackground(g, backgroundNear, -cameraX);
            
            // Draw ground (tiled if needed)
            drawTiledBackground(g, ground, -cameraX);
            // Position ground at the bottom
            g.drawImage(ground, -cameraX, GROUND_HEIGHT, null);
            
            // Draw game objects for CURRENT FLOOR
            itemManager.draw(g, cameraX, WIDTH, GROUND_HEIGHT);
            
            if (monster.isActive() && !monster.isInClassroom()) {
                monster.draw(g, cameraX, WIDTH, itemManager.getCurrentFloor(), itemManager);
            }
            
            if (!player.isHiding()) {
                player.draw(g, cameraX);
            }
        }
        
        drawUI(g);
    }
    
    private void drawUI(Graphics g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.drawString("Notes: " + itemManager.getNotesCollected() + "/3", 50, 50);
        g.drawString("Key: " + (itemManager.hasKey() ? "YES" : "NO"), 50, 80);
        g.drawString("Floor: " + (itemManager.getCurrentFloor() + 1) + "/3", 50, 110);
        
        // Real-time score display
        if (gameState == GameState.PLAYING && !showingJumpscare) {
            updateRealTimeScore();
        }
        
        if (gameState == GameState.GAME_WON) {
            g.setColor(Color.GREEN);
            g.drawString("Final Score: " + score, 50, 140);
        } else if (gameState == GameState.PLAYING) {
            g.drawString("Score: " + score, 50, 140);
        }
        
        // Display time played (always updating)
        int currentTime = (int)(System.currentTimeMillis() / 1000);
        int timePlayed = currentTime - gameStartTime;
        int minutes = timePlayed / 60;
        int seconds = timePlayed % 60;
        g.drawString("Time: " + String.format("%02d:%02d", minutes, seconds), 50, 170);
        
        // Show item location hints
        if (gameState == GameState.PLAYING && !showingJumpscare) {
            drawItemLocationHints(g, 50, 200);
        }
        
        // Show real-time score breakdown during gameplay
        if (gameState == GameState.PLAYING && !showingJumpscare) {
            drawRealTimeScoreBreakdown(g, 50, 250);
        }
        
        if (itemManager.isInClassroom()) {
            g.setColor(Color.YELLOW);
            g.drawString("CLASSROOM - GO TO EXIT DOOR TO CONTINUE", WIDTH / 2 - 250, 100);
            g.drawString("MONSTER CAN FOLLOW YOU IN HERE!", WIDTH / 2 - 200, 130);
            
            // Classroom specific hints
            g.setColor(Color.CYAN);
            g.drawString("Search this classroom for notes and key!", WIDTH / 2 - 200, 160);
        } else if (player.isHiding()) {
            g.setColor(Color.GREEN);
            g.drawString("HIDING IN LOCKER - PRESS E TO EXIT", WIDTH / 2 - 200, 100);
            g.drawString("MONSTER ACTIVE: " + monster.isActive(), WIDTH / 2 - 150, 130);
        } else if (monster.isActive()) {
            g.setColor(Color.RED);
            g.drawString("MONSTER IS HUNTING! FIND A LOCKER!", WIDTH / 2 - 200, 100);
        }
        
        // ESC to return to title (only in gameplay)
        if (gameState == GameState.PLAYING && !showingJumpscare) {
            g.setColor(Color.LIGHT_GRAY);
            g.setFont(new Font("Arial", Font.PLAIN, 16));
            g.drawString("Press ESC to return to Title Screen", WIDTH - 300, HEIGHT - 30);
        }
    }

    private void drawItemLocationHints(Graphics g, int x, int y) {
        g.setColor(Color.CYAN);
        g.setFont(new Font("Arial", Font.PLAIN, 16));
        
        g.drawString("Item Locations:", x, y);
        y += 20;
        
        // Notes hint
        int notesRemaining = 3 - itemManager.getNotesCollected();
        g.drawString("Notes remaining: " + notesRemaining, x, y);
        y += 20;
        
        // Key hint
        if (!itemManager.hasKey() && itemManager.getCurrentFloor() == 1) {
            g.drawString("Key is on this floor!", x, y);
            y += 20;
        }
        
        // Classroom hint
        if (!itemManager.isInClassroom() && !itemManager.getCurrentFloorClassrooms().isEmpty()) {
            g.drawString("Classrooms available: " + itemManager.getCurrentFloorClassrooms().size(), x, y);
            y += 20;
            g.drawString("Some items may be inside classrooms", x, y);
        }
    }
    
    private void updateRealTimeScore() {
        int currentTime = (int)(System.currentTimeMillis() / 1000);
        int totalGameTime = currentTime - gameStartTime;
        
        // Base score for escaping (prorated)
        int baseScore = 10000;
        
        // Time multiplier - faster escape = higher multiplier
        double timeMultiplier = calculateTimeMultiplier(totalGameTime);
        
        // Deductions for hiding
        int hidingPenalty = totalHidingTime * 2; // 2 points per frame hiding
        int hideCountPenalty = player.getHideCount() * 100; // 100 points per hide instance
        
        // Objective completion bonus (notes and key)
        int objectiveBonus = itemManager.getNotesCollected() * 500;
        if (itemManager.hasKey()) {
            objectiveBonus += 1000;
        }
        
        // Calculate real-time score (prorated base score)
        int proratedBaseScore = (int)(baseScore * (1.0 - (totalGameTime / 1800.0))); // Lose base score over time
        proratedBaseScore = Math.max(0, proratedBaseScore);
        
        score = (int)((proratedBaseScore + objectiveBonus - hidingPenalty - hideCountPenalty) * timeMultiplier);
        
        // Ensure score doesn't go negative
        score = Math.max(0, score);
    }
    
    private double calculateTimeMultiplier(int totalGameTime) {
        if (totalGameTime < 300) { // Under 5 minutes
            return 3.0;
        } else if (totalGameTime < 600) { // Under 10 minutes
            return 2.0;
        } else if (totalGameTime < 900) { // Under 15 minutes
            return 1.5;
        } else {
            return 1.0;
        }
    }
    
    private void drawRealTimeScoreBreakdown(Graphics g, int x, int y) {
        int currentTime = (int)(System.currentTimeMillis() / 1000);
        int totalGameTime = currentTime - gameStartTime;
        
        int baseScore = 10000;
        int proratedBaseScore = (int)(baseScore * (1.0 - (totalGameTime / 1800.0)));
        proratedBaseScore = Math.max(0, proratedBaseScore);
        int objectiveBonus = itemManager.getNotesCollected() * 500 + (itemManager.hasKey() ? 1000 : 0);
        int hidingPenalty = totalHidingTime * 2;
        int hideCountPenalty = player.getHideCount() * 100;
        double timeMultiplier = calculateTimeMultiplier(totalGameTime);
        
        g.setColor(Color.YELLOW);
        g.setFont(new Font("Arial", Font.PLAIN, 16));
        
        g.drawString("Real-time Score Breakdown:", x, y);
        y += 20;
        g.drawString("Base Score: " + proratedBaseScore + " (decaying)", x, y);
        y += 20;
        g.drawString("Objectives: +" + objectiveBonus, x, y);
        y += 20;
        g.drawString("Hiding Time: -" + hidingPenalty, x, y);
        y += 20;
        g.drawString("Hide Count: -" + hideCountPenalty, x, y);
        y += 20;
        g.drawString("Time Multiplier: " + String.format("%.1fx", timeMultiplier), x, y);
    }
    
    private void drawGameEndScreen(Graphics g) {
        // Only used for win screen now
        g.setColor(new Color(0, 255, 0, 150)); // Semi-transparent green
        g.fillRect(0, 0, WIDTH, HEIGHT);
        
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 72));
        String winText = "YOU ESCAPED!";
        int textWidth = g.getFontMetrics().stringWidth(winText);
        g.drawString(winText, WIDTH/2 - textWidth/2, HEIGHT/2 - 100);
        
        // Show score details
        g.setFont(new Font("Arial", Font.BOLD, 36));
        String scoreText = "Final Score: " + score;
        textWidth = g.getFontMetrics().stringWidth(scoreText);
        g.drawString(scoreText, WIDTH/2 - textWidth/2, HEIGHT/2);
        
        // Show detailed score breakdown
        drawScoreBreakdown(g);
        
        g.setFont(new Font("Arial", Font.PLAIN, 24));
        String restartText = "Press R to play again or ESC for title screen";
        textWidth = g.getFontMetrics().stringWidth(restartText);
        g.drawString(restartText, WIDTH/2 - textWidth/2, HEIGHT/2 + 250);
    }
    
    private void drawScoreBreakdown(Graphics g) {
        if (gameState != GameState.GAME_WON || !scoreCalculated) return;
        
        int currentTime = (int)(System.currentTimeMillis() / 1000);
        int totalGameTime = currentTime - gameStartTime;
        int minutes = totalGameTime / 60;
        int seconds = totalGameTime % 60;
        
        int baseScore = 10000;
        int objectiveBonus = itemManager.getNotesCollected() * 500 + (itemManager.hasKey() ? 1000 : 0);
        int hidingPenalty = totalHidingTime * 2;
        int hideCountPenalty = player.getHideCount() * 100;
        
        // Calculate time multiplier
        double timeMultiplier = calculateTimeMultiplier(finalGameTime);
        String timeMultiplierText = String.format("%.1fx", timeMultiplier);
        if (finalGameTime < 300) {
            timeMultiplierText += " (Excellent Time!)";
        } else if (finalGameTime < 600) {
            timeMultiplierText += " (Great Time!)";
        } else if (finalGameTime < 900) {
            timeMultiplierText += " (Good Time)";
        }
        
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 20));
        
        int yPos = HEIGHT/2 + 50;
        g.drawString("Score Breakdown:", WIDTH/2 - 100, yPos);
        yPos += 25;
        g.drawString("Escape Time: " + String.format("%02d:%02d", minutes, seconds), WIDTH/2 - 100, yPos);
        yPos += 25;
        g.drawString("Time Multiplier: " + timeMultiplierText, WIDTH/2 - 100, yPos);
        yPos += 25;
        g.drawString("Base Score: " + baseScore, WIDTH/2 - 100, yPos);
        yPos += 25;
        g.drawString("Objectives Bonus: +" + objectiveBonus, WIDTH/2 - 100, yPos);
        yPos += 25;
        g.drawString("Hiding Time Penalty: -" + hidingPenalty, WIDTH/2 - 100, yPos);
        yPos += 25;
        g.drawString("Hide Count Penalty: -" + hideCountPenalty, WIDTH/2 - 100, yPos);
    }
    
    private void calculateFinalScore() {
        int currentTime = (int)(System.currentTimeMillis() / 1000);
        finalGameTime = currentTime - gameStartTime;
        
        // Base score for escaping
        int baseScore = 10000;
        
        // Time multiplier - faster escape = higher multiplier
        double timeMultiplier = calculateTimeMultiplier(finalGameTime);
        
        // Deductions for hiding
        int hidingPenalty = totalHidingTime * 2; // 2 points per frame hiding
        int hideCountPenalty = player.getHideCount() * 100; // 100 points per hide instance
        
        // Objective completion bonus (notes and key)
        int objectiveBonus = itemManager.getNotesCollected() * 500;
        if (itemManager.hasKey()) {
            objectiveBonus += 1000;
        }
        
        // Calculate final score
        score = (int)((baseScore + objectiveBonus - hidingPenalty - hideCountPenalty) * timeMultiplier);
        
        // Ensure score doesn't go negative
        score = Math.max(0, score);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        titleTimer++;
        
        switch (gameState) {
            case TITLE:
                // Update title screen animations
                if (titleFadingIn) {
                    titleAlpha += 0.02f;
                    if (titleAlpha >= 1.0f) {
                        titleAlpha = 1.0f;
                        titleFadingIn = false;
                    }
                }
                break;
                
            case PLAYING:
                if (showingJumpscare) {
                    jumpscareTimer--;
                    if (jumpscareTimer <= 0) {
                        gameState = GameState.GAME_OVER;
                    }
                } else {
                    updateCamera();
                    
                    // Track hiding time for scoring
                    if (player.isHiding()) {
                        totalHidingTime++;
                    }
                    
                    // Update real-time score during gameplay
                    updateRealTimeScore();
                    
                    // FIXED: Pass proper ground height to monster update
                    monster.update(player.getX(), player.isHiding(), WIDTH, cameraX, GROUND_HEIGHT, 
                                  itemManager.getCurrentFloor(), itemManager, itemManager.isInClassroom());
                    
                    if (!player.isHiding() && player.collidesWith(monster, itemManager.getCurrentFloor(), itemManager)) {
                        triggerJumpscare();
                        soundManager.stopChaseMusic();
                        soundManager.stopAmbientSound();
                        
                        // Invalidate score when caught
                        score = 0;
                        scoreCalculated = true;
                    }
                    
                    player.update();
                    
                    itemManager.checkPlayerInteractions(player);
                    
                    if (!itemManager.isInClassroom() && itemManager.canExit(player, itemManager.getCurrentFloor())) {
                        gameState = GameState.GAME_WON;
                        soundManager.stopChaseMusic();
                        soundManager.stopAmbientSound();
                        
                        // Calculate final score when winning
                        if (!scoreCalculated) {
                            calculateFinalScore();
                            scoreCalculated = true;
                        }
                    }
                    
                    // Update sound manager
                    boolean monsterIsNear = monster.isActive() && 
                                           Math.abs(player.getX() - monster.getX()) < 600 &&
                                           ((!itemManager.isInClassroom() && !monster.isInClassroom() && 
                                             monster.getCurrentFloor() == itemManager.getCurrentFloor()) ||
                                            (itemManager.isInClassroom() && monster.isInClassroom()));
                    
                    soundManager.update(monster.isActive(), monsterIsNear, playerMoving, 
                                       player.getX(), monster.getX(), WIDTH);
                }
                break;
                
            case GAME_WON:
            case GAME_OVER:
                // End game states - just wait for input
                break;
        }
        
        repaint();
    }
    
    @Override
    public void keyPressed(KeyEvent e) {
        switch (gameState) {
            case TITLE:
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    startGame();
                }
                break;
                
            case PLAYING:
                if (showingJumpscare) {
                    // FIXED: Restart game directly from jumpscare screen (after text appears)
                    if (jumpscareTimer <= JUMPSACRE_DURATION - 180) {
                        if (e.getKeyCode() == KeyEvent.VK_R) {
                            soundManager.cleanup();
                            initializeGame();
                        } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                            soundManager.cleanup();
                            initializeGame(); // Returns to title
                        }
                    }
                    return;
                }
                
                // Handle ESC to return to title screen
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    gameState = GameState.TITLE;
                    soundManager.stopChaseMusic();
                    soundManager.stopAmbientSound();
                    return;
                }
                
                // Handle movement keys for sound
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT:
                    case KeyEvent.VK_A:
                        leftPressed = true;
                        break;
                    case KeyEvent.VK_RIGHT:
                    case KeyEvent.VK_D:
                        rightPressed = true;
                        break;
                }
                playerMoving = leftPressed || rightPressed;
                
                player.handleKeyPress(e, itemManager.getCurrentFloorLockers(), itemManager, GROUND_HEIGHT);
                break;
                
            case GAME_WON:
            case GAME_OVER:
                if (e.getKeyCode() == KeyEvent.VK_R) {
                    soundManager.cleanup();
                    initializeGame();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    soundManager.cleanup();
                    initializeGame(); // Returns to title
                }
                break;
        }
    }
    
    @Override
    public void keyReleased(KeyEvent e) {
        if (gameState == GameState.PLAYING && !showingJumpscare) {
            // Handle movement keys for sound
            switch (e.getKeyCode()) {
                case KeyEvent.VK_LEFT:
                case KeyEvent.VK_A:
                    leftPressed = false;
                    break;
                case KeyEvent.VK_RIGHT:
                case KeyEvent.VK_D:
                    rightPressed = false;
                    break;
            }
            playerMoving = leftPressed || rightPressed;
        }
    }
    
    @Override
    public void keyTyped(KeyEvent e) {}
    
    public static void main(String[] args) {
        JFrame frame = new JFrame("Horror Escape - 3 Floors with Obstacles");
        HorrorGame game = new HorrorGame();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        
        // Add window listener to clean up sounds
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                game.soundManager.cleanup();
            }
        });
    }
}