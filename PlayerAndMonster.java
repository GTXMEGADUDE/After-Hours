import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.imageio.ImageIO;

public class PlayerAndMonster {
    // This file contains both Player and Monster classes
}

class Player {
    private static final int PLAYER_SPEED = 8;
    private BufferedImage sprite;
    private int x, y;
    private boolean isHiding = false;
    private int hideCooldown = 0;
    private SoundManager soundManager;
    private int hideCount = 0;
    
    public Player(int startX, int groundHeight, SoundManager soundManager) {
        this.soundManager = soundManager;
        try {
            sprite = ImageIO.read(new File("player.png"));
            this.x = startX;
            this.y = groundHeight - sprite.getHeight();
        } catch (IOException e) {
            System.err.println("Error loading player.png: " + e.getMessage());
            System.exit(1);
        }
    }
    
    public void update() {
        if (hideCooldown > 0) {
            hideCooldown--;
        }
    }
    
    public void handleKeyPress(KeyEvent e, ArrayList<Locker> lockers, ItemManager itemManager, int groundHeight) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                if (!isHiding && !itemManager.checkPlayerObstacleCollision(this)) {
                    int newX = Math.max(0, x - PLAYER_SPEED);
                    // Check if new position would collide with obstacle
                    int tempX = x;
                    x = newX;
                    if (itemManager.checkPlayerObstacleCollision(this)) {
                        x = tempX; // Revert if collision
                    }
                }
                break;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                if (!isHiding && !itemManager.checkPlayerObstacleCollision(this)) {
                    int newX = Math.min(3840 - sprite.getWidth(), x + PLAYER_SPEED);
                    // Check if new position would collide with obstacle
                    int tempX = x;
                    x = newX;
                    if (itemManager.checkPlayerObstacleCollision(this)) {
                        x = tempX; // Revert if collision
                    }
                }
                break;
case KeyEvent.VK_E:
    if (itemManager.isInClassroom()) {
        // In classroom - check for BOTH doors to exit AND lockers to hide
        if (itemManager.checkClassroomExit(this)) {
            itemManager.exitClassroom(this);
        } else {
            // TRY TO USE LOCKERS IN CLASSROOM
            handleLockerInteraction(itemManager.getCurrentFloorLockers());
        }
    } else {
        // In main world - check for BOTH doors to enter classroom
        if (itemManager.checkClassroomEnter(this)) {
            itemManager.enterClassroom(this);
        } else {
            handleLockerInteraction(lockers);
        }
    }
    break;
            case KeyEvent.VK_W:
                if (itemManager.checkStaircaseUp(this, groundHeight)) {
                    itemManager.changeFloor(itemManager.getCurrentFloor() + 1);
                }
                break;
            case KeyEvent.VK_S:
                if (itemManager.checkStaircaseDown(this, groundHeight)) {
                    itemManager.changeFloor(itemManager.getCurrentFloor() - 1);
                }
                break;
        }
    }
    
private void handleLockerInteraction(ArrayList<Locker> lockers) {
    if (isHiding) {
        // Exit locker - WORKS IN BOTH MAIN WORLD AND CLASSROOM
        isHiding = false;
        hideCooldown = 60;
        if (soundManager != null) {
            soundManager.playLockerSound();
        }
    } else if (hideCooldown == 0) {
        // Try to enter locker - WORKS IN BOTH MAIN WORLD AND CLASSROOM
        for (Locker locker : lockers) {
            if (getBounds().intersects(locker.getBounds())) {
                isHiding = true;
                hideCount++; // Track hide count for scoring
                if (soundManager != null) {
                    soundManager.playLockerSound();
                }
                break;
            }
        }
    }
}
    
    public void draw(Graphics g, int cameraX) {
        if (!isHiding) {
            int drawX = x - cameraX;
            g.drawImage(sprite, drawX, y, null);
        }
    }
    
    public void drawInClassroom(Graphics g) {
        if (!isHiding) {
            // Draw player in classroom - full screen positioning
            g.drawImage(sprite, x, y, null);
        }
    }
    
    public Rectangle getBounds() {
        return new Rectangle(x, y, sprite.getWidth(), sprite.getHeight());
    }
    
    public boolean collidesWith(Monster monster, int currentFloor, ItemManager itemManager) {
        if (!monster.isActive() || monster.getX() < -1000) {
            return false;
        }
        
        // If in classroom, only check collision if monster is also in same classroom
        if (itemManager.isInClassroom()) {
            return monster.isInClassroom() && getBounds().intersects(monster.getBounds());
        } else {
            return !monster.isInClassroom() && monster.getCurrentFloor() == currentFloor && getBounds().intersects(monster.getBounds());
        }
    }
    
    // Getters
    public int getX() { return x; }
    public int getY() { return y; }
    public boolean isHiding() { return isHiding; }
    public int getHideCount() { return hideCount; }
    
    // Setters for classroom positioning
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
}

class Monster {
    private static final int MONSTER_SPEED = 5;
    private static final int MONSTER_APPEAR_TIME = 180;
    
    private BufferedImage sprite;
    private int x, y;
    private boolean active = false;
    private int monsterTimer = 0;
    private int monsterStateTimer = 0;
    private int worldWidth;
    private java.util.Random random = new java.util.Random();
    private int lastKnownPlayerX;
    private int searchTimer = 0;
    private int spawnSide;
    private int pauseTimer = 0;
    private boolean isPaused = false;
    private boolean hasReachedLastKnownPosition = false;
    private int walkAwayDirection;
    private int currentFloor = 0;
    private boolean inClassroom = false;
    private Classroom currentClassroom = null;
    
    // CLIMBING SYSTEM
    private boolean isMovingToStairs = false;
    private boolean isClimbingStairs = false;
    private int targetStairX = 0;
    private int targetFloorAfterClimb = 0;
    private int lastPlayerFloor = 0;
    
    public Monster(int startX, int groundHeight, int worldWidth) {
        try {
            sprite = ImageIO.read(new File("monster.png"));
            this.x = startX;
            this.y = groundHeight - sprite.getHeight();
            this.worldWidth = worldWidth;
            this.lastKnownPlayerX = startX;
            this.lastPlayerFloor = 0;
        } catch (IOException e) {
            System.err.println("Error loading monster.png: " + e.getMessage());
            System.exit(1);
        }
    }
    
    public void update(int playerX, boolean playerHiding, int screenWidth, int cameraX, int groundHeight, int playerFloor, ItemManager itemManager, boolean playerInClassroom) {
        monsterTimer++;
        
        // MONSTER CAN MANUALLY ENTER CLASSROOMS - NO AUTO TELEPORT
if (active && !inClassroom && playerInClassroom) {
    // Check if monster is near ANY classroom door that player might have entered
    for (Classroom classroom : itemManager.getCurrentFloorClassrooms()) {
        boolean nearEntrance = getBounds().intersects(classroom.getEntranceBounds());
        boolean nearExit = getBounds().intersects(classroom.getExitBounds());
        
        if ((nearEntrance || nearExit) && random.nextInt(120) == 0) {
            inClassroom = true;
            currentClassroom = classroom;
            
            // Position monster at the correct classroom door based on which main world door it came from
            if (nearEntrance) {
                // Entered through entrance - appear at classroom entrance (left side)
                x = 100;
            } else {
                // Entered through exit - appear at classroom exit (right side)
                x = 1920 - 200 - sprite.getWidth(); // Right side position
            }
            y = classroom.getClassroomGroundY() - sprite.getHeight();
            break;
        }
    }
}
        
if (active && inClassroom && !playerInClassroom && random.nextInt(180) == 0) {
    // 1 in 180 chance per frame to exit classroom if player left
    if (currentClassroom != null) {
        // Determine which classroom door to exit from based on current position
        Rectangle classroomEntrance = currentClassroom.getClassroomEntranceBounds();
        Rectangle classroomExit = currentClassroom.getClassroomExitBounds();
        
        // Calculate distance to each door
        int distToEntrance = Math.abs(x - (classroomEntrance.x + classroomEntrance.width/2));
        int distToExit = Math.abs(x - (classroomExit.x + classroomExit.width/2));
        
        // Exit through the closest door
        if (distToEntrance < distToExit) {
            // Exit through classroom entrance (left side)
            Rectangle mainWorldEntrance = currentClassroom.getEntranceBounds();
            x = (int)mainWorldEntrance.getX() + 20;
        } else {
            // Exit through classroom exit (right side)  
            Rectangle mainWorldExit = currentClassroom.getExitBounds();
            x = (int)mainWorldExit.getX() + 20;
        }
        
        y = groundHeight - sprite.getHeight();
    }
    
    inClassroom = false;
    currentClassroom = null;
}
        
        // FIXED: Check if monster needs to climb to reach player's floor WITH VALID STAIRCASE CHECK
        if (active && !isMovingToStairs && !isClimbingStairs && !inClassroom && playerFloor != currentFloor && !playerInClassroom) {
            // Check if there's a valid staircase connection between these floors
            boolean canClimb = false;
            
            if (playerFloor > currentFloor) {
                // Need to go UP - check if current floor has UP staircase
                canClimb = (currentFloor == 0) || (currentFloor == 1); // Floors 0 and 1 have UP staircases
            } else if (playerFloor < currentFloor) {
                // Need to go DOWN - check if current floor has DOWN staircase  
                canClimb = (currentFloor == 1) || (currentFloor == 2); // Floors 1 and 2 have DOWN staircases
            }
            
            if (canClimb) {
                findAndMoveToStairs(playerFloor);
            }
        }
        lastPlayerFloor = playerFloor;
        
        // Handle moving to stairs first - NO DESPAWN DURING CLIMB
        if (isMovingToStairs) {
            int direction = (x > targetStairX) ? -1 : 1;
            x += direction * MONSTER_SPEED;
            
            // Check if reached staircase
            if (Math.abs(x - targetStairX) <= MONSTER_SPEED) {
                isMovingToStairs = false;
                isClimbingStairs = true;
                currentFloor = targetFloorAfterClimb; // Change floor immediately
            }
            return; // NO DESPAWN CHECKS WHILE MOVING TO STAIRS
        }
        
        // Handle climbing stairs (brief visual) - NO DESPAWN DURING CLIMB
        if (isClimbingStairs) {
            if (monsterTimer % 30 == 0) {
                isClimbingStairs = false;
            }
            return; // NO DESPAWN CHECKS WHILE CLIMBING
        }
        
        // CLASSROOM BEHAVIOR
// In the update method, replace the classroom behavior section:
if (active && inClassroom) {
    // If player is hiding in locker, search around the classroom
    if (playerHiding) {
        searchTimer++;
        
        if (!hasReachedLastKnownPosition) {
            // Move to last known player position
            int direction = (x > lastKnownPlayerX) ? -1 : 1;
            x += direction * MONSTER_SPEED;
            
            if (Math.abs(x - lastKnownPlayerX) <= MONSTER_SPEED * 2) {
                hasReachedLastKnownPosition = true;
                pauseTimer = 120 + random.nextInt(120);
                walkAwayDirection = random.nextBoolean() ? -1 : 1;
            }
        } else if (pauseTimer > 0) {
            pauseTimer--;
        } else {
            // Wander around after searching
            x += walkAwayDirection * MONSTER_SPEED;
            
            // Change direction if hitting classroom walls
            if (x <= 0 || x >= 1920 - sprite.getWidth()) {
                walkAwayDirection *= -1;
            }
            
            // Despawn chance after wandering
            if (searchTimer > 300 && random.nextInt(200) == 0) {
                inClassroom = false;
                currentClassroom = null;
                active = false;
                monsterTimer = 0;
            }
        }
    } else {
        // Chase visible player in classroom
        searchTimer = 0;
        hasReachedLastKnownPosition = false;
        
        int direction = (x > playerX) ? -1 : 1;
        x += direction * MONSTER_SPEED;
        
        lastKnownPlayerX = playerX; // Update last known position
    }
    
    // Keep monster in classroom bounds
    x = Math.max(0, Math.min(x, 1920 - sprite.getWidth()));
    
    // Can despawn from classroom if player leaves
    if (!playerInClassroom && random.nextInt(300) == 0) {
        inClassroom = false;
        currentClassroom = null;
        active = false;
        monsterTimer = 0;
    }
    return;
}
        
        // NORMAL SPAWNING LOGIC (only in main world)
        if (!active && !inClassroom) {
            if (!playerHiding && !playerInClassroom && monsterTimer > 180 && random.nextInt(300) == 0) {
                active = true;
                monsterStateTimer = 0;
                searchTimer = 0;
                isPaused = false;
                pauseTimer = 0;
                hasReachedLastKnownPosition = false;
                
                // SPAWN ON RANDOM FLOOR (can be different from player)
                currentFloor = random.nextInt(3);
                lastPlayerFloor = playerFloor;
                
                spawnSide = random.nextInt(2);
                int minSpawnDistance = 800;
                int spawnBuffer = 200;
                
                if (spawnSide == 0) {
                    x = playerX - minSpawnDistance - random.nextInt(spawnBuffer);
                } else {
                    x = playerX + minSpawnDistance + random.nextInt(spawnBuffer);
                }
                
                x = Math.max(0, Math.min(x, worldWidth - sprite.getWidth()));
                
                if (Math.abs(x - playerX) < 600) {
                    active = false;
                    monsterTimer = 0;
                    return;
                }
            }
        } else if (active && !inClassroom) {
            monsterStateTimer++;
            
            int leftEdge = cameraX;
            int rightEdge = cameraX + screenWidth;
            
            // ONLY CHASE IF ON SAME FLOOR
            if (currentFloor == playerFloor) {
                if (playerHiding || playerInClassroom) {
                    searchTimer++;
                    
                    if (!hasReachedLastKnownPosition) {
                        int direction = (x > lastKnownPlayerX) ? -1 : 1;
                        x += direction * MONSTER_SPEED;
                        
                        if (Math.abs(x - lastKnownPlayerX) <= MONSTER_SPEED * 2) {
                            hasReachedLastKnownPosition = true;
                            pauseTimer = 120 + random.nextInt(120);
                            walkAwayDirection = random.nextBoolean() ? -1 : 1;
                        }
                    } else if (pauseTimer > 0) {
                        pauseTimer--;
                    } else {
                        x += walkAwayDirection * MONSTER_SPEED;
                        
                        // DESPAWN ONLY WHEN: off-screen + lost player + not climbing
                        boolean isWayOffScreen = (x + sprite.getWidth() < leftEdge - 500) || (x > rightEdge + 500);
                        if (isWayOffScreen) {
                            x = -2000;
                            active = false;
                            monsterTimer = 0;
                            hasReachedLastKnownPosition = false;
                        }
                    }
                } else {
                    // CHASE PLAYER
                    searchTimer = 0;
                    isPaused = false;
                    hasReachedLastKnownPosition = false;
                    
                    int direction = (x > playerX) ? -1 : 1;
                    x += direction * MONSTER_SPEED;
                    
                    // DESPAWN CHECKS - ONLY WHEN NOT CLIMBING AND ON SAME FLOOR
                    boolean isWayOffScreen = (x + sprite.getWidth() < leftEdge - 500) || (x > rightEdge + 500);
                    
                    if (monsterStateTimer > MONSTER_APPEAR_TIME && isWayOffScreen && random.nextInt(200) == 0) {
                        x = -2000;
                        active = false;
                        monsterTimer = 0;
                    }
                    
                    if (monsterStateTimer > 240 && isWayOffScreen && Math.abs(x - playerX) > 1200) {
                        x = -2000;
                        active = false;
                        monsterTimer = 0;
                    }
                }
            } else {
                // MONSTER IS ON DIFFERENT FLOOR - NO DESPAWN, JUST WAIT TO CLIMB
                if (monsterStateTimer % 120 == 0 && random.nextInt(5) == 0) {
                    // Check if valid staircase exists before attempting to climb
                    boolean canClimb = false;
                    if (playerFloor > currentFloor) {
                        canClimb = (currentFloor == 0) || (currentFloor == 1);
                    } else if (playerFloor < currentFloor) {
                        canClimb = (currentFloor == 1) || (currentFloor == 2);
                    }
                    
                    if (canClimb) {
                        findAndMoveToStairs(playerFloor);
                    }
                }
            }
        }
        
        // UPDATE LAST KNOWN POSITION
        if (!playerHiding && !playerInClassroom && currentFloor == playerFloor) {
            lastKnownPlayerX = playerX;
        }
    }
    
    // FIXED: Only allow climbing where staircases actually exist based on floor layout
    private void findAndMoveToStairs(int targetFloor) {
        int stairX = -1;
        
        // FIXED: Only allow climbing where staircases actually exist based on floor layout
        if (targetFloor > currentFloor) {
            // Need to go UP
            if (currentFloor == 0) {
                // Floor 0: Only UP staircase on RIGHT side
                stairX = worldWidth - 200;
            } else if (currentFloor == 1) {
                // Floor 1: Only UP staircase on LEFT side
                stairX = 100;
            }
            // Floor 2 has NO UP staircase
        } else if (targetFloor < currentFloor) {
            // Need to go DOWN
            if (currentFloor == 1) {
                // Floor 1: Only DOWN staircase on RIGHT side  
                stairX = worldWidth - 200;
            } else if (currentFloor == 2) {
                // Floor 2: Only DOWN staircase on LEFT side
                stairX = 100;
            }
            // Floor 0 has NO DOWN staircase
        }
        
        if (stairX != -1) {
            isMovingToStairs = true;
            targetStairX = stairX;
            targetFloorAfterClimb = targetFloor;
        }
    }
    
public void draw(Graphics g, int cameraX, int screenWidth, int currentPlayerFloor, ItemManager itemManager) {
    // DRAW MONSTER IN CLASSROOM
    if (active && inClassroom && currentClassroom != null) {
        // Draw monster in classroom - full screen positioning
        g.drawImage(sprite, x, y, null);
        
        if (isMovingToStairs) {
            g.setColor(Color.ORANGE);
            g.setFont(new Font("Arial", Font.BOLD, 12));
            g.drawString("MOVING TO STAIRS", x, y - 10);
        } else if (isClimbingStairs) {
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 12));
            g.drawString("CLIMBING", x, y - 10);
        }
    }
    // DRAW MONSTER IN MAIN WORLD
    else if (active && !inClassroom && currentFloor == currentPlayerFloor) {
        int drawX = x - cameraX;
        if (drawX > -sprite.getWidth() && drawX < screenWidth) {
            g.drawImage(sprite, drawX, y, null);
            
            if (isMovingToStairs) {
                g.setColor(Color.ORANGE);
                g.setFont(new Font("Arial", Font.BOLD, 12));
                g.drawString("MOVING TO STAIRS", drawX, y - 10);
            } else if (isClimbingStairs) {
                g.setColor(Color.RED);
                g.setFont(new Font("Arial", Font.BOLD, 12));
                g.drawString("CLIMBING", drawX, y - 10);
            }
        }
    }
}
    
    public Rectangle getBounds() {
        return new Rectangle(x, y, sprite.getWidth(), sprite.getHeight());
    }
    
    // Getters
    public boolean isActive() { return active; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getCurrentFloor() { return currentFloor; }
    public boolean isClimbingStairs() { return isClimbingStairs || isMovingToStairs; }
    public boolean isInClassroom() { return inClassroom; }
}