import java.util.Random;

public class WorldGenerator {
    private int worldWidth;
    private int groundHeight;
    private Random random;
    
    public WorldGenerator(int worldWidth, int groundHeight) {
        this.worldWidth = worldWidth;
        this.groundHeight = groundHeight;
        this.random = new Random();
    }
    
    public void generateWorld(ItemManager itemManager) {
        // Generate items for all floors
        itemManager.generateAllFloors(groundHeight, random);
    }
}