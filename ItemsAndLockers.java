import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.imageio.ImageIO;

public class ItemsAndLockers {
    // This file contains ItemManager, Locker, and related item classes
}

class ItemManager {
    private ArrayList<ArrayList<GameEntity>> floorNotes;
    private ArrayList<GameEntity> floorKeys;
    private GameEntity exit;
    private ArrayList<ArrayList<Locker>> floorLockers;
    private ArrayList<ArrayList<Obstacle>> floorObstacles;
    private ArrayList<ArrayList<Classroom>> floorClassrooms;
    private ArrayList<ArrayList<Locker>> classroomLockers;
    private BufferedImage noteImage;
    private BufferedImage keyImage;
    private BufferedImage exitImage;
    private BufferedImage lockerImage;
    private BufferedImage staircaseUpImage;
    private BufferedImage staircaseDownImage;
    private BufferedImage obstacleImage;
    private BufferedImage doorEntranceImage;
    private BufferedImage doorExitImage;
    private BufferedImage classroomBgImage;
    private BufferedImage classroomGroundImage;
    
    private int notesCollected = 0;
    private boolean hasKey = false;
    private int worldWidth;
    private int currentFloor = 0;
    private Classroom activeClassroom = null;
    private int groundHeight;
    
    public ItemManager(int worldWidth, int groundHeight) {
        this.worldWidth = worldWidth;
        this.groundHeight = groundHeight;
        
        try {
            noteImage = ImageIO.read(new File("note.png"));
            keyImage = ImageIO.read(new File("key.png"));
            exitImage = ImageIO.read(new File("exit.png"));
            lockerImage = ImageIO.read(new File("locker.png"));
            staircaseUpImage = ImageIO.read(new File("staircase_up.png"));
            staircaseDownImage = ImageIO.read(new File("staircase_down.png"));
            obstacleImage = ImageIO.read(new File("obstacle.png"));
            doorEntranceImage = ImageIO.read(new File("door_entrance.png"));
            doorExitImage = ImageIO.read(new File("door_exit.png"));
            classroomBgImage = ImageIO.read(new File("classroom_bg.png"));
            classroomGroundImage = ImageIO.read(new File("classroom_ground.png"));
        } catch (IOException e) {
            System.err.println("Error loading image files: " + e.getMessage());
            System.err.println("Make sure these files exist: note.png, key.png, exit.png, locker.png, staircase_up.png, staircase_down.png, obstacle.png, door_entrance.png, door_exit.png, classroom_bg.png, classroom_ground.png");
            System.exit(1);
        }
        
        floorNotes = new ArrayList<>();
        floorKeys = new ArrayList<>();
        floorLockers = new ArrayList<>();
        floorObstacles = new ArrayList<>();
        floorClassrooms = new ArrayList<>();
        classroomLockers = new ArrayList<>();
        
        for (int i = 0; i < 3; i++) {
            floorNotes.add(new ArrayList<>());
            floorKeys.add(null);
            floorLockers.add(new ArrayList<>());
            floorObstacles.add(new ArrayList<>());
            floorClassrooms.add(new ArrayList<>());
            classroomLockers.add(new ArrayList<>());
        }
        
        exit = new GameEntity(exitImage, 500, groundHeight - exitImage.getHeight());
    }
    
    public void generateAllFloors(int groundHeight, java.util.Random random) {
        for (int floor = 0; floor < 3; floor++) {
            generateFloorItems(floor, groundHeight, random);
        }
    }
    
    private void generateFloorItems(int floor, int groundHeight, java.util.Random random) {
        floorNotes.get(floor).clear();
        floorLockers.get(floor).clear();
        floorObstacles.get(floor).clear();
        floorClassrooms.get(floor).clear();
        classroomLockers.get(floor).clear();
        
        int noteX = 300 + random.nextInt(worldWidth - 600);
        floorNotes.get(floor).add(new GameEntity(noteImage, noteX, groundHeight - noteImage.getHeight() - 20));
        
        if (floor == 1) {
            int keyX = 300 + random.nextInt(worldWidth - 600);
            floorKeys.set(floor, new GameEntity(keyImage, keyX, groundHeight - keyImage.getHeight() - 20));
            floorKeys.get(floor).active = true;
        }
        
        int lockersInMainWorld = 1 + random.nextInt(2);
        int lockersInClassrooms = 3 - lockersInMainWorld;
        
        int mainWorldLockersPlaced = 0;
        while (mainWorldLockersPlaced < lockersInMainWorld) {
            Locker locker = new Locker(lockerImage, 0, 0);
            if (positionLocker(locker, groundHeight, random, floor)) {
                floorLockers.get(floor).add(locker);
                mainWorldLockersPlaced++;
            }
        }
        
        if (random.nextInt(100) < 60) {
            int obstacleCount = 1 + random.nextInt(2);
            for (int i = 0; i < obstacleCount; i++) {
                generateObstacleWithClassroom(floor, groundHeight, random);
            }
        }
        
if (lockersInClassrooms > 0 && !floorClassrooms.get(floor).isEmpty()) {
    int lockersPlacedInClassrooms = 0;
    int maxAttemptsPerClassroom = 30;
    int totalAttempts = 0;
    
    // Try to distribute lockers evenly across available classrooms
    ArrayList<Classroom> availableClassrooms = new ArrayList<>(floorClassrooms.get(floor));
    
    while (lockersPlacedInClassrooms < lockersInClassrooms && totalAttempts < 100 && !availableClassrooms.isEmpty()) {
        // Pick a random classroom from available ones
        Classroom classroom = availableClassrooms.get(random.nextInt(availableClassrooms.size()));
        
        Locker locker = createClassroomLocker(classroom, random, floor);
        if (locker != null) {
            classroomLockers.get(floor).add(locker);
            lockersPlacedInClassrooms++;
            totalAttempts = 0; // Reset counter on success
            
            // If this classroom is getting too crowded, remove it from available list
            if (countLockersInClassroom(classroom, floor) >= 2) {
                availableClassrooms.remove(classroom);
            }
        } else {
            totalAttempts++;
            // If we can't place in this classroom after several attempts, try another one
            if (totalAttempts > maxAttemptsPerClassroom) {
                availableClassrooms.remove(classroom);
                totalAttempts = 0;
            }
        }
    }
}


        
        int totalLockers = floorLockers.get(floor).size() + classroomLockers.get(floor).size();
        while (totalLockers < 3) {
            Locker locker = new Locker(lockerImage, 0, 0);
            if (positionLocker(locker, groundHeight, random, floor)) {
                floorLockers.get(floor).add(locker);
                totalLockers++;
            } else {
                break;
            }
        }
    }

    
    
private Locker createClassroomLocker(Classroom classroom, java.util.Random random, int floor) {
    int attempts = 0;
    int minLockerSpacing = 50; // Minimum space between lockers
    
    while (attempts < 100) {
        int lockerX = 200 + random.nextInt(1520);
        int lockerY = classroom.getClassroomGroundY() - lockerImage.getHeight();
        
        Locker locker = new Locker(lockerImage, lockerX, lockerY);
        
        // Check if locker overlaps with classroom doors (with buffer)
        Rectangle entranceWithBuffer = new Rectangle(
            classroom.getClassroomEntranceBounds().x - minLockerSpacing,
            classroom.getClassroomEntranceBounds().y - minLockerSpacing,
            classroom.getClassroomEntranceBounds().width + minLockerSpacing * 2,
            classroom.getClassroomEntranceBounds().height + minLockerSpacing * 2
        );
        
        Rectangle exitWithBuffer = new Rectangle(
            classroom.getClassroomExitBounds().x - minLockerSpacing,
            classroom.getClassroomExitBounds().y - minLockerSpacing,
            classroom.getClassroomExitBounds().width + minLockerSpacing * 2,
            classroom.getClassroomExitBounds().height + minLockerSpacing * 2
        );
        
        if (locker.getBounds().intersects(entranceWithBuffer) ||
            locker.getBounds().intersects(exitWithBuffer)) {
            attempts++;
            continue;
        }
        
        // Check if locker overlaps with other classroom lockers (with buffer)
        boolean overlapsWithOtherLockers = false;
        for (Locker existingLocker : classroomLockers.get(floor)) {
            Rectangle existingWithBuffer = new Rectangle(
                existingLocker.getX() - minLockerSpacing,
                existingLocker.getY() - minLockerSpacing,
                existingLocker.getSprite().getWidth() + minLockerSpacing * 2,
                existingLocker.getSprite().getHeight() + minLockerSpacing * 2
            );
            
            if (locker.getBounds().intersects(existingWithBuffer)) {
                overlapsWithOtherLockers = true;
                break;
            }
        }
        
        if (!overlapsWithOtherLockers) {
            return locker;
        }
        attempts++;
    }
    return null;
}

// Add this helper method to count lockers in a specific classroom
private int countLockersInClassroom(Classroom classroom, int floor) {
    int count = 0;
    for (Locker locker : classroomLockers.get(floor)) {
        // Check if locker is in this classroom (you might need to track which locker belongs to which classroom)
        // For now, we'll assume all classroom lockers on this floor are in this classroom
        // You may need to implement proper classroom-locker association if needed
        count++;
    }
    return count;
}
    
private void generateObstacleWithClassroom(int floor, int groundHeight, java.util.Random random) {
    int attempts = 0;
    boolean validPosition = false;
    int obstacleX = 0;
    
    while (attempts < 200 && !validPosition) {
        // Pick a random position for the obstacle
        obstacleX = obstacleImage.getWidth() + 300 + random.nextInt(worldWidth - (obstacleImage.getWidth() * 2) - 600);
        int obstacleY = groundHeight - obstacleImage.getHeight();
        
        Obstacle testObstacle = new Obstacle(obstacleImage, obstacleX, obstacleY);
        
        // Check if obstacle position is valid
        if (!isObstaclePositionValid(testObstacle, floor)) {
            attempts++;
            continue;
        }
        
        // CALCULATE DOOR POSITIONS USING ACTUAL IMAGE WIDTHS
int entranceX = obstacleX - doorEntranceImage.getWidth() - 200; // LEFT DOOR - 80px gap
int exitX = obstacleX + obstacleImage.getWidth() + 80;
        
        int doorY = groundHeight - doorEntranceImage.getHeight();
        
        // CHECK DOOR POSITIONS
        boolean entranceValid = isDoorPositionAbsolutelyValid(entranceX, doorY, floor);
        boolean exitValid = isDoorPositionAbsolutelyValid(exitX, doorY, floor);
        
        if (entranceValid && exitValid) {
            validPosition = true;
            
            // CREATE THE FUCKING CLASSROOM
            Obstacle obstacle = new Obstacle(obstacleImage, obstacleX, obstacleY);
            Classroom classroom = new Classroom(classroomBgImage, classroomGroundImage, 
                                               doorEntranceImage, doorExitImage, 
                                               obstacle, 1920, 1080);
            
            classroom.setEntrancePosition(entranceX, doorY);
            classroom.setExitPosition(exitX, doorY);
            
            floorObstacles.get(floor).add(obstacle);
            floorClassrooms.get(floor).add(classroom);
            break;
        }
        
        attempts++;
    }
}

private boolean isObstaclePositionValid(Obstacle obstacle, int floor) {
    Rectangle obstacleBounds = obstacle.getBounds();
    
    // Check boundaries
    if (obstacleBounds.x < 300 || obstacleBounds.x > worldWidth - 300 - obstacleBounds.width) {
        return false;
    }
    
    // Check exit on floor 0
    if (floor == 0 && exit != null && obstacleBounds.intersects(exit.getBounds())) {
        return false;
    }
    
    // Check other obstacles
    for (Obstacle existing : floorObstacles.get(floor)) {
        if (obstacleBounds.intersects(existing.getBounds())) {
            return false;
        }
    }
    
    // Check lockers with buffer
    for (Locker locker : floorLockers.get(floor)) {
        Rectangle lockerBounds = locker.getBounds();
        Rectangle expandedBounds = new Rectangle(
            lockerBounds.x - doorEntranceImage.getWidth() - 100,
            lockerBounds.y - 100,
            lockerBounds.width + (doorEntranceImage.getWidth() * 2) + 200,
            lockerBounds.height + 200
        );
        if (obstacleBounds.intersects(expandedBounds)) {
            return false;
        }
    }
    
    return true;
}

private boolean isDoorPositionAbsolutelyValid(int doorX, int doorY, int floor) {
    Rectangle doorBounds = new Rectangle(doorX, doorY, doorEntranceImage.getWidth(), doorEntranceImage.getHeight());
    
    // Check world boundaries
    if (doorX < 100 || doorX > worldWidth - 100 - doorEntranceImage.getWidth()) {
        return false;
    }
    
    // Check exit on floor 0
    if (floor == 0 && exit != null && doorBounds.intersects(exit.getBounds())) {
        return false;
    }
    
    // Check other obstacles
    for (Obstacle obstacle : floorObstacles.get(floor)) {
        if (doorBounds.intersects(obstacle.getBounds())) {
            return false;
        }
    }
    
    // Check lockers
    for (Locker locker : floorLockers.get(floor)) {
        if (doorBounds.intersects(locker.getBounds())) {
            return false;
        }
    }
    
    // Check other classroom doors
    for (Classroom classroom : floorClassrooms.get(floor)) {
        if (doorBounds.intersects(classroom.getEntranceBounds()) || 
            doorBounds.intersects(classroom.getExitBounds())) {
            return false;
        }
    }
    
    return true;
}
    
 private boolean isDoorPositionValid(int doorX, int doorY, int floor, boolean relaxed) {
    Rectangle doorBounds = new Rectangle(doorX, doorY, doorEntranceImage.getWidth(), doorEntranceImage.getHeight());
    
    // Check boundaries with more buffer
    if (doorX < 200 || doorX > worldWidth - 200 - doorEntranceImage.getWidth()) {
        return false;
    }
    
    // Check distance from exit on floor 0
    if (floor == 0 && exit != null) {
        Rectangle expandedExitBounds = new Rectangle(
            exit.x - 100, exit.y - 100,
            exit.sprite.getWidth() + 200, exit.sprite.getHeight() + 200
        );
        if (doorBounds.intersects(expandedExitBounds)) {
            return false;
        }
    }
    
    // Check distance from lockers with more buffer
    for (Locker locker : floorLockers.get(floor)) {
        Rectangle expandedBounds = new Rectangle(
            locker.getX() - 80, locker.getY() - 80, // Increased buffer
            locker.getSprite().getWidth() + 160, locker.getSprite().getHeight() + 160
        );
        if (doorBounds.intersects(expandedBounds)) {
            return false;
        }
    }
    
    // Check distance from obstacles
    for (Obstacle obstacle : floorObstacles.get(floor)) {
        Rectangle expandedObstacleBounds = new Rectangle(
            obstacle.getX() - 50, obstacle.getY() - 50,
            obstacle.getSprite().getWidth() + 100, obstacle.getSprite().getHeight() + 100
        );
        if (doorBounds.intersects(expandedObstacleBounds)) {
            return false;
        }
    }
    
    // Check distance from other classroom doors
    for (Classroom classroom : floorClassrooms.get(floor)) {
        Rectangle entranceExpanded = new Rectangle(
            classroom.getEntranceBounds().x - 100, classroom.getEntranceBounds().y - 100,
            classroom.getEntranceBounds().width + 200, classroom.getEntranceBounds().height + 200
        );
        Rectangle exitExpanded = new Rectangle(
            classroom.getExitBounds().x - 100, classroom.getExitBounds().y - 100,
            classroom.getExitBounds().width + 200, classroom.getExitBounds().height + 200
        );
        if (doorBounds.intersects(entranceExpanded) || doorBounds.intersects(exitExpanded)) {
            return false;
        }
    }
    
    return true;
}
    
    private boolean positionLocker(Locker locker, int groundHeight, java.util.Random random, int floor) {
        int attempts = 0;
        int newX = 0, newY = groundHeight - locker.getSprite().getHeight();

        while (attempts < 50) {
            newX = 100 + random.nextInt(worldWidth - 200 - locker.getSprite().getWidth());
            locker.setPosition(newX, newY);

            if (!checkOverlap(locker, floor) && isLockerPositionValid(newX, newY, floor)) {
                return true;
            }
            attempts++;
        }
        return false;
    }
    
    private boolean isLockerPositionValid(int lockerX, int lockerY, int floor) {
        Rectangle lockerBounds = new Rectangle(lockerX, lockerY, lockerImage.getWidth(), lockerImage.getHeight());
        
        if (floor == 0 && lockerX > worldWidth - 250) {
            return false;
        }
        if ((floor == 1 || floor == 2) && lockerX < 150) {
            return false;
        }
        if (floor == 1 && lockerX > worldWidth - 250) {
            return false;
        }
        
        if (floor == 0 && exit != null && lockerBounds.intersects(exit.getBounds())) {
            return false;
        }
        
        for (Classroom classroom : floorClassrooms.get(floor)) {
            Rectangle entranceExpanded = new Rectangle(
                classroom.getEntranceBounds().x - 30, classroom.getEntranceBounds().y - 30,
                classroom.getEntranceBounds().width + 60, classroom.getEntranceBounds().height + 60
            );
            Rectangle exitExpanded = new Rectangle(
                classroom.getExitBounds().x - 30, classroom.getExitBounds().y - 30,
                classroom.getExitBounds().width + 60, classroom.getExitBounds().height + 60
            );
            if (lockerBounds.intersects(entranceExpanded) || lockerBounds.intersects(exitExpanded)) {
                return false;
            }
        }
        
        return true;
    }
    
    private boolean checkOverlap(Locker newLocker, int floor) {
        if (floor == 0 && exit != null && newLocker.getBounds().intersects(exit.getBounds())) {
            return true;
        }

        for (Locker locker : floorLockers.get(floor)) {
            if (newLocker.getBounds().intersects(locker.getBounds())) {
                return true;
            }
        }
        
        for (Obstacle obstacle : floorObstacles.get(floor)) {
            if (newLocker.getBounds().intersects(obstacle.getBounds())) {
                return true;
            }
        }

        return false;
    }
    
    public void checkPlayerInteractions(Player player) {
        if (activeClassroom == null) {
            ArrayList<GameEntity> currentNotes = floorNotes.get(currentFloor);
            for (int i = 0; i < currentNotes.size(); i++) {
                if (player.getBounds().intersects(currentNotes.get(i).getBounds())) {
                    currentNotes.remove(i);
                    notesCollected++;
                    break;
                }
            }
            
            GameEntity currentKey = floorKeys.get(currentFloor);
            if (!hasKey && currentKey != null && currentKey.active && player.getBounds().intersects(currentKey.getBounds())) {
                hasKey = true;
                currentKey.active = false;
            }
        }
    }
    
    public boolean checkPlayerObstacleCollision(Player player) {
        if (activeClassroom != null) return false;
        
        for (Obstacle obstacle : floorObstacles.get(currentFloor)) {
            if (player.getBounds().intersects(obstacle.getBounds())) {
                return true;
            }
        }
        return false;
    }
    
public boolean checkClassroomEnter(Player player) {
    if (isInClassroom()) return false;
    
    // CHECK BOTH ENTRANCE AND EXIT DOORS FOR ENTERING
    for (Classroom classroom : getCurrentFloorClassrooms()) {
        if (player.getBounds().intersects(classroom.getEntranceBounds()) || 
            player.getBounds().intersects(classroom.getExitBounds())) {
            return true;
        }
    }
    return false;
}

public boolean checkClassroomExit(Player player) {
    if (!isInClassroom()) return false;
    
    // IN CLASSROOM - BOTH DOORS CAN BE USED TO EXIT
    if (player.getBounds().intersects(activeClassroom.getClassroomEntranceBounds()) || 
        player.getBounds().intersects(activeClassroom.getClassroomExitBounds())) {
        return true;
    }
    return false;
}

public void enterClassroom(Player player) {
    if (isInClassroom()) return;
    
    for (Classroom classroom : getCurrentFloorClassrooms()) {
        // CHECK WHICH DOOR THE PLAYER IS ENTERING FROM
        if (player.getBounds().intersects(classroom.getEntranceBounds())) {
            activeClassroom = classroom;
            // Position player at classroom entrance door (left side)
            Rectangle entranceBounds = classroom.getClassroomEntranceBounds();
            player.setPosition((int)entranceBounds.getX() + 20, classroom.getClassroomGroundY() - player.getBounds().height);
            break;
        } else if (player.getBounds().intersects(classroom.getExitBounds())) {
            activeClassroom = classroom;
            // Position player at classroom exit door (right side)  
            Rectangle exitBounds = classroom.getClassroomExitBounds();
            player.setPosition((int)exitBounds.getX() + 20, classroom.getClassroomGroundY() - player.getBounds().height);
            break;
        }
    }
}

public void exitClassroom(Player player) {
    if (!isInClassroom()) return;
    
    // Store the player's bounds for collision detection
    Rectangle playerBounds = player.getBounds();
    
    // CHECK WHICH DOOR THE PLAYER IS EXITING FROM
    if (playerBounds.intersects(activeClassroom.getClassroomEntranceBounds())) {
        // Exiting through classroom entrance door - go back to main world ENTRANCE position
        Rectangle mainWorldEntranceBounds = activeClassroom.getEntranceBounds();
        
        // Calculate safe spawn position - ensure player doesn't spawn inside obstacle
        int spawnX = (int)mainWorldEntranceBounds.getX() + 20;
        int spawnY = groundHeight - playerBounds.height;
        
        // Check if spawn position would be inside an obstacle
        Rectangle testBounds = new Rectangle(spawnX, spawnY, playerBounds.width, playerBounds.height);
        boolean safeSpawn = true;
        
        for (Obstacle obstacle : floorObstacles.get(currentFloor)) {
            if (testBounds.intersects(obstacle.getBounds())) {
                safeSpawn = false;
                // Try alternative spawn positions
                if (!tryAlternativeSpawnPositions(player, testBounds, mainWorldEntranceBounds, true)) {
                    // If no safe position found, force spawn and let player move away
                    spawnX = (int)mainWorldEntranceBounds.getX() + 100; // Force move right
                }
                break;
            }
        }
        
        player.setPosition(spawnX, spawnY);
        
    } else if (playerBounds.intersects(activeClassroom.getClassroomExitBounds())) {
        // Exiting through classroom exit door - go back to main world EXIT position  
        Rectangle mainWorldExitBounds = activeClassroom.getExitBounds();
        
        // Calculate safe spawn position
        int spawnX = (int)mainWorldExitBounds.getX() + 20;
        int spawnY = groundHeight - playerBounds.height;
        
        // Check if spawn position would be inside an obstacle
        Rectangle testBounds = new Rectangle(spawnX, spawnY, playerBounds.width, playerBounds.height);
        boolean safeSpawn = true;
        
        for (Obstacle obstacle : floorObstacles.get(currentFloor)) {
            if (testBounds.intersects(obstacle.getBounds())) {
                safeSpawn = false;
                // Try alternative spawn positions
                if (!tryAlternativeSpawnPositions(player, testBounds, mainWorldExitBounds, false)) {
                    // If no safe position found, force spawn and let player move away
                    spawnX = (int)mainWorldExitBounds.getX() - 100; // Force move left
                }
                break;
            }
        }
        
        player.setPosition(spawnX, spawnY);
    }
    
    activeClassroom = null;
}

// Helper method to find safe spawn positions
private boolean tryAlternativeSpawnPositions(Player player, Rectangle testBounds, Rectangle doorBounds, boolean isEntrance) {
    int[] xOffsets = {50, 100, -50, -100, 150, -150}; // Try different horizontal offsets
    
    for (int offset : xOffsets) {
        int testX = (int)doorBounds.getX() + offset;
        Rectangle newTestBounds = new Rectangle(testX, testBounds.y, testBounds.width, testBounds.height);
        
        boolean collision = false;
        for (Obstacle obstacle : floorObstacles.get(currentFloor)) {
            if (newTestBounds.intersects(obstacle.getBounds())) {
                collision = true;
                break;
            }
        }
        
        if (!collision) {
            player.setPosition(testX, testBounds.y);
            return true;
        }
    }
    return false;
}
    
    public boolean canExit(Player player, int currentFloor) {
        return currentFloor == 0 && exit != null && player.getBounds().intersects(exit.getBounds()) && notesCollected >= 3 && hasKey;
    }
    
    public void draw(Graphics g, int cameraX, int screenWidth, int groundHeight) {
        if (activeClassroom != null) {
            activeClassroom.draw(g);
            
            for (Locker locker : classroomLockers.get(currentFloor)) {
                g.drawImage(locker.getSprite(), locker.getX(), locker.getY(), null);
            }
        } else {
            if (currentFloor == 0 && exit != null) {
                int drawX = exit.x - cameraX;
                if (drawX > -exit.sprite.getWidth() && drawX < screenWidth) {
                    g.drawImage(exit.sprite, drawX, exit.y, null);
                }
            }
            
            for (GameEntity note : floorNotes.get(currentFloor)) {
                int drawX = note.x - cameraX;
                if (drawX > -note.sprite.getWidth() && drawX < screenWidth) {
                    g.drawImage(note.sprite, drawX, note.y, null);
                }
            }
            
            GameEntity currentKey = floorKeys.get(currentFloor);
            if (currentKey != null && currentKey.active) {
                int drawX = currentKey.x - cameraX;
                if (drawX > -currentKey.sprite.getWidth() && drawX < screenWidth) {
                    g.drawImage(currentKey.sprite, drawX, currentKey.y, null);
                }
            }
            
            for (Locker locker : floorLockers.get(currentFloor)) {
                int drawX = locker.getX() - cameraX;
                if (drawX > -locker.getSprite().getWidth() && drawX < screenWidth) {
                    g.drawImage(locker.getSprite(), drawX, locker.getY(), null);
                }
            }
            
            for (Obstacle obstacle : floorObstacles.get(currentFloor)) {
                int drawX = obstacle.getX() - cameraX;
                if (drawX > -obstacle.getSprite().getWidth() && drawX < screenWidth) {
                    g.drawImage(obstacle.getSprite(), drawX, obstacle.getY(), null);
                }
            }
            
            for (Classroom classroom : floorClassrooms.get(currentFloor)) {
                int entranceX = classroom.getEntranceBounds().x - cameraX;
                if (entranceX > -doorEntranceImage.getWidth() && entranceX < screenWidth) {
                    g.drawImage(doorEntranceImage, entranceX, classroom.getEntranceBounds().y, null);
                }
                
                int exitX = classroom.getExitBounds().x - cameraX;
                if (exitX > -doorExitImage.getWidth() && exitX < screenWidth) {
                    g.drawImage(doorExitImage, exitX, classroom.getExitBounds().y, null);
                }
            }
            
            if (currentFloor == 0) {
                int stairX = worldWidth - 200;
                g.drawImage(staircaseUpImage, stairX - cameraX, groundHeight - staircaseUpImage.getHeight(), null);
            } else if (currentFloor == 1) {
                int rightStairX = worldWidth - 200;
                int leftStairX = 100;
                g.drawImage(staircaseDownImage, rightStairX - cameraX, groundHeight - staircaseDownImage.getHeight(), null);
                g.drawImage(staircaseUpImage, leftStairX - cameraX, groundHeight - staircaseUpImage.getHeight(), null);
            } else if (currentFloor == 2) {
                int leftStairX = 100;
                g.drawImage(staircaseDownImage, leftStairX - cameraX, groundHeight - staircaseDownImage.getHeight(), null);
            }
        }
    }
    
    public boolean checkStaircaseUp(Player player, int groundHeight) {
        if (activeClassroom != null) return false;
        if (currentFloor >= 2) return false;
        
        int stairX = (currentFloor == 0) ? worldWidth - 200 : 100;
        Rectangle staircaseBounds = new Rectangle(stairX, groundHeight - staircaseUpImage.getHeight(), 
                                                 staircaseUpImage.getWidth(), staircaseUpImage.getHeight());
        
        return player.getBounds().intersects(staircaseBounds);
    }
    
    public boolean checkStaircaseDown(Player player, int groundHeight) {
        if (activeClassroom != null) return false;
        if (currentFloor <= 0) return false;
        
        int stairX = (currentFloor == 1) ? worldWidth - 200 : 100;
        Rectangle staircaseBounds = new Rectangle(stairX, groundHeight - staircaseDownImage.getHeight(), 
                                                 staircaseDownImage.getWidth(), staircaseDownImage.getHeight());
        
        return player.getBounds().intersects(staircaseBounds);
    }
    
    public void changeFloor(int newFloor) {
        currentFloor = newFloor;
        activeClassroom = null;
    }
    
    public ArrayList<Locker> getCurrentFloorLockers() {
        if (activeClassroom != null) {
            return classroomLockers.get(currentFloor);
        } else {
            return floorLockers.get(currentFloor);
        }
    }
    
    public int getNotesCollected() { return notesCollected; }
    public boolean hasKey() { return hasKey; }
    public int getCurrentFloor() { return currentFloor; }
    public boolean isInClassroom() { return activeClassroom != null; }
    public Classroom getActiveClassroom() { return activeClassroom; }
    public ArrayList<Classroom> getCurrentFloorClassrooms() {
        return floorClassrooms.get(currentFloor);
    }
    public int getGroundHeight() { return groundHeight; }
}

class Locker {
    private BufferedImage sprite;
    private int x, y;
    private boolean playerInside = false;
    
    public Locker(BufferedImage sprite, int x, int y) {
        this.sprite = sprite;
        this.x = x;
        this.y = y;
    }
    
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    public Rectangle getBounds() {
        return new Rectangle(x, y, sprite.getWidth(), sprite.getHeight());
    }
    
    public BufferedImage getSprite() { return sprite; }
    public int getX() { return x; }
    public int getY() { return y; }
    public boolean isPlayerInside() { return playerInside; }
    public void setPlayerInside(boolean inside) { playerInside = inside; }
}

class GameEntity {
    public BufferedImage sprite;
    public int x, y;
    public boolean active = true;
    
    public GameEntity(BufferedImage sprite, int x, int y) {
        this.sprite = sprite;
        this.x = x;
        this.y = y;
    }
    
    public Rectangle getBounds() {
        return new Rectangle(x, y, sprite.getWidth(), sprite.getHeight());
    }
}

class Obstacle {
    private BufferedImage sprite;
    private int x, y;
    
    public Obstacle(BufferedImage sprite, int x, int y) {
        this.sprite = sprite;
        this.x = x;
        this.y = y;
    }
    
    public Rectangle getBounds() {
        return new Rectangle(x, y, sprite.getWidth(), sprite.getHeight());
    }
    
    public BufferedImage getSprite() { return sprite; }
    public int getX() { return x; }
    public int getY() { return y; }
}

class Classroom {
    private BufferedImage background;
    private BufferedImage ground;
    private BufferedImage entranceDoor;
    private BufferedImage exitDoor;
    private Obstacle connectedObstacle;
    private int width, height;
    private int entranceX, entranceY;
    private int exitX, exitY;
    private int classroomGroundY;
    
    public Classroom(BufferedImage bg, BufferedImage ground, BufferedImage entrance, 
                    BufferedImage exit, Obstacle obstacle, int width, int height) {
        this.background = bg;
        this.ground = ground;
        this.entranceDoor = entrance;
        this.exitDoor = exit;
        this.connectedObstacle = obstacle;
        this.width = width;
        this.height = height;
        this.classroomGroundY = height - ground.getHeight();
    }
    
    public void setEntrancePosition(int x, int y) {
        this.entranceX = x;
        this.entranceY = y;
    }
    
    public void setExitPosition(int x, int y) {
        this.exitX = x;
        this.exitY = y;
    }
    
    public void draw(Graphics g) {
        g.drawImage(background, 0, 0, width, height, null);
        g.drawImage(ground, 0, classroomGroundY, width, ground.getHeight(), null);
        
        int classroomEntranceX = 100;
        int classroomExitX = width - 200;
        int doorY = classroomGroundY - entranceDoor.getHeight();
        
        g.drawImage(entranceDoor, classroomEntranceX, doorY, null);
        g.drawImage(exitDoor, classroomExitX, doorY, null);
    }
    
    public Rectangle getEntranceBounds() {
        return new Rectangle(entranceX, entranceY, entranceDoor.getWidth(), entranceDoor.getHeight());
    }
    
    public Rectangle getExitBounds() {
        return new Rectangle(exitX, exitY, exitDoor.getWidth(), exitDoor.getHeight());
    }
    
    public Rectangle getClassroomEntranceBounds() {
        int doorY = classroomGroundY - entranceDoor.getHeight();
        return new Rectangle(100, doorY, entranceDoor.getWidth(), entranceDoor.getHeight());
    }
    
    public Rectangle getClassroomExitBounds() {
        int doorY = classroomGroundY - exitDoor.getHeight();
        return new Rectangle(width - 200, doorY, exitDoor.getWidth(), exitDoor.getHeight());
    }
    
    public int getClassroomWidth() { return width; }
    public int getClassroomHeight() { return height; }
    public int getClassroomGroundY() { return classroomGroundY; }
    
    public Obstacle getConnectedObstacle() { return connectedObstacle; }
}