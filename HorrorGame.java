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
    
    private boolean gameOver = false;
    private boolean gameWon = false;
    private boolean showingJumpscare = false;
    private int jumpscareTimer = 0;
    private static final int JUMPSACRE_DURATION = 300; // 5 seconds at 60fps
    private Timer timer;
    private int cameraX = 0;
    private boolean playerMoving = false;
    private boolean leftPressed = false;
    private boolean rightPressed = false;

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
            
            backgroundFar = scaleImage(backgroundFar, WORLD_WIDTH, HEIGHT);
            backgroundMid = scaleImage(backgroundMid, WORLD_WIDTH, HEIGHT);
            backgroundNear = scaleImage(backgroundNear, WORLD_WIDTH, HEIGHT);
            ground = scaleImage(ground, WORLD_WIDTH, HEIGHT - GROUND_HEIGHT);
            
        } catch (IOException e) {
            showImageErrorDialog(e);
            System.exit(1);
        }
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
            "- jumpscare.png\n\n" +
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
        gameOver = false;
        gameWon = false;
        showingJumpscare = false;
        jumpscareTimer = 0;
        playerMoving = false;
        leftPressed = false;
        rightPressed = false;
        
        soundManager.startAmbientSound();
    }
    
    private void triggerJumpscare() {
        showingJumpscare = true;
        jumpscareTimer = JUMPSACRE_DURATION;
        soundManager.playJumpscareSound();
    }
    
    private void updateCamera() {
        int targetX = player.getX() - WIDTH / 2;
        cameraX = Math.max(0, Math.min(targetX, WORLD_WIDTH - WIDTH));
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        if (showingJumpscare) {
            // Draw jumpscare screen
            g.drawImage(jumpscareImage, 0, 0, WIDTH, HEIGHT, null);
            
            // Show game over text OVERLAY on top of jumpscare image after 3 seconds
            if (jumpscareTimer <= JUMPSACRE_DURATION - 180) {
                drawGameOverTextOverlay(g);
            }
        } else if (gameWon) {
            // Draw win screen
            drawGameWorld(g);
            drawGameEndScreen(g);
        } else if (!gameOver) {
            // Draw normal game
            drawGameWorld(g);
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
        g.drawString(gameOverText, WIDTH/2 - textWidth/2, HEIGHT/2);
        
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 36));
        String restartText = "Press any key to restart";
        textWidth = g.getFontMetrics().stringWidth(restartText);
        g.drawString(restartText, WIDTH/2 - textWidth/2, HEIGHT/2 + 60);
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
            // Draw parallax backgrounds
            int farOffset = cameraX / 4;
            g.drawImage(backgroundFar, -farOffset, 0, null);
            
            int midOffset = cameraX / 2;
            g.drawImage(backgroundMid, -midOffset, 0, null);
            
            int nearOffset = cameraX;
            g.drawImage(backgroundNear, -nearOffset, 0, null);
            
            // Draw ground
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
        
        if (itemManager.isInClassroom()) {
            g.setColor(Color.YELLOW);
            g.drawString("CLASSROOM - GO TO EXIT DOOR TO CONTINUE", WIDTH / 2 - 250, 100);
            g.drawString("MONSTER CAN FOLLOW YOU IN HERE!", WIDTH / 2 - 200, 130);
        } else if (player.isHiding()) {
            g.setColor(Color.GREEN);
            g.drawString("HIDING IN LOCKER - PRESS E TO EXIT", WIDTH / 2 - 200, 100);
            g.drawString("MONSTER ACTIVE: " + monster.isActive(), WIDTH / 2 - 150, 130);
        } else if (monster.isActive()) {
            g.setColor(Color.RED);
            g.drawString("MONSTER IS HUNTING! FIND A LOCKER!", WIDTH / 2 - 200, 100);
        }
        
        // Floor navigation instructions
        if (!itemManager.isInClassroom()) {
            g.setColor(Color.YELLOW);
            if (itemManager.getCurrentFloor() < 2) {
                g.drawString("Press W at right staircase to go UP", WIDTH - 400, 50);
            }
            if (itemManager.getCurrentFloor() > 0) {
                g.drawString("Press S at left staircase to go DOWN", WIDTH - 400, 80);
            }
            
            // Obstacle instructions
            g.setColor(Color.ORANGE);
            g.drawString("Obstacles block your path - Use classrooms to go around", WIDTH / 2 - 300, HEIGHT - 50);
        }
    }
    
    private void drawGameEndScreen(Graphics g) {
        // Only used for win screen now
        g.setColor(new Color(0, 255, 0, 150)); // Semi-transparent green
        g.fillRect(0, 0, WIDTH, HEIGHT);
        
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 72));
        String winText = "YOU ESCAPED!";
        int textWidth = g.getFontMetrics().stringWidth(winText);
        g.drawString(winText, WIDTH/2 - textWidth/2, HEIGHT/2);
        
        g.setFont(new Font("Arial", Font.PLAIN, 36));
        String restartText = "Press R to play again";
        textWidth = g.getFontMetrics().stringWidth(restartText);
        g.drawString(restartText, WIDTH/2 - textWidth/2, HEIGHT/2 + 60);
    }
    
@Override
public void actionPerformed(ActionEvent e) {
    if (showingJumpscare) {
        jumpscareTimer--;
    } else if (!gameOver && !gameWon) {
        updateCamera();
        
        // FIXED: Pass proper ground height to monster update
        monster.update(player.getX(), player.isHiding(), WIDTH, cameraX, GROUND_HEIGHT, 
                      itemManager.getCurrentFloor(), itemManager, itemManager.isInClassroom());
        
        if (!player.isHiding() && player.collidesWith(monster, itemManager.getCurrentFloor(), itemManager)) {
            triggerJumpscare();
            soundManager.stopChaseMusic();
            soundManager.stopAmbientSound();
        }
        
        player.update();
        
        itemManager.checkPlayerInteractions(player);
        
        if (!itemManager.isInClassroom() && itemManager.canExit(player, itemManager.getCurrentFloor())) {
            gameWon = true;
            soundManager.stopChaseMusic();
            soundManager.stopAmbientSound();
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
    
    repaint();
}
    
    @Override
    public void keyPressed(KeyEvent e) {
        if (gameWon) {
            if (e.getKeyCode() == KeyEvent.VK_R) {
                soundManager.cleanup();
                initializeGame();
            }
            return;
        }
        
        if (showingJumpscare) {
            // FIXED: Restart game directly from jumpscare screen (after text appears)
            if (jumpscareTimer <= JUMPSACRE_DURATION - 180) {
                soundManager.cleanup();
                initializeGame();
            }
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
    }
    
    @Override
    public void keyReleased(KeyEvent e) {
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
