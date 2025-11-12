import java.io.File;
import java.io.IOException;
import java.util.Random;
import javax.sound.sampled.*;

public class SoundManager {
    private Clip ambientSound;
    private Clip chaseMusic;
    private Clip walkingSound;
    private Clip lockerSound;
    private Clip jumpscareSound;
    private Clip[] randomAmbientSounds;
    private boolean isChaseMusicPlaying = false;
    private Random random = new Random();
    private int nextAmbientSoundTime = 0;
    private int nextRandomSoundTime = 0;
    private int gameTimer = 0;
    
    // Volume control (0.0 to 1.0)
    private float masterVolume = 0.7f;
    private float ambientVolume = 0.4f;
    private float chaseVolume = 0.6f;
    private float effectsVolume = 0.5f;
    private float randomAmbientVolume = 0.3f;
    
    public SoundManager() {
        loadSounds();
        startAmbientSound();
    }
    
    private void loadSounds() {
        try {
            // Load ambient sound (looping)
            ambientSound = loadClip("./sound/ambient.wav");
            
            // Load chase music (looping)
            chaseMusic = loadClip("./sound/chase_music.wav");
            
            // Load effect sounds
            walkingSound = loadClip("./sound/walking.wav");
            lockerSound = loadClip("./sound/locker.wav");
            
            // Load jumpscare sound
            jumpscareSound = loadClip("./sound/jumpscare.wav");
            
            // Load random ambient sounds
            randomAmbientSounds = new Clip[3];
            for (int i = 0; i < randomAmbientSounds.length; i++) {
                try {
                    randomAmbientSounds[i] = loadClip("./sound/random" + (i + 1) + ".wav");
                } catch (Exception e) {
                    System.err.println("Could not load random ambient sound " + (i + 1) + ": " + e.getMessage());
                    randomAmbientSounds[i] = null;
                }
            }
            
            setVolume(ambientSound, ambientVolume);
            setVolume(chaseMusic, chaseVolume);
            setVolume(walkingSound, effectsVolume);
            setVolume(lockerSound, effectsVolume);
            setVolume(jumpscareSound, 1.0f); // Full volume for jumpscare
            
            for (Clip clip : randomAmbientSounds) {
                if (clip != null) {
                    setVolume(clip, randomAmbientVolume);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error loading sound files: " + e.getMessage());
            System.err.println("Game will continue without sound.");
        }
    }
    
    private Clip loadClip(String filename) throws IOException, UnsupportedAudioFileException, LineUnavailableException {
        File soundFile = new File(filename);
        if (!soundFile.exists()) {
            throw new IOException("Sound file not found: " + filename);
        }
        AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);
        Clip clip = AudioSystem.getClip();
        clip.open(audioIn);
        return clip;
    }
    
    private void setVolume(Clip clip, float volume) {
        if (clip != null) {
            try {
                FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float dB = (float) (Math.log(volume) / Math.log(10.0) * 20.0);
                gainControl.setValue(dB);
            } catch (IllegalArgumentException e) {
                // Some clips might not support volume control
            }
        }
    }
    
    public void startAmbientSound() {
        if (ambientSound != null && !isChaseMusicPlaying) {
            ambientSound.loop(Clip.LOOP_CONTINUOUSLY);
            ambientSound.start();
            
            // Schedule first random ambient sound
            nextAmbientSoundTime = 120 + random.nextInt(300); // 2-7 seconds
            nextRandomSoundTime = 300 + random.nextInt(600); // 5-15 seconds for first random sound
        }
    }
    
    public void stopAmbientSound() {
        if (ambientSound != null && ambientSound.isRunning()) {
            ambientSound.stop();
        }
    }
    
    public void startChaseMusic() {
        if (chaseMusic != null && !isChaseMusicPlaying) {
            stopAmbientSound();
            chaseMusic.loop(Clip.LOOP_CONTINUOUSLY);
            chaseMusic.start();
            isChaseMusicPlaying = true;
        }
    }
    
    public void stopChaseMusic() {
        if (chaseMusic != null && chaseMusic.isRunning()) {
            chaseMusic.stop();
        }
        isChaseMusicPlaying = false;
        startAmbientSound();
    }
    
    public void playWalkingSound(boolean isMoving) {
        if (walkingSound != null) {
            if (isMoving && !walkingSound.isRunning()) {
                walkingSound.loop(Clip.LOOP_CONTINUOUSLY);
                walkingSound.start();
            } else if (!isMoving && walkingSound.isRunning()) {
                walkingSound.stop();
            }
        }
    }
    
    public void playLockerSound() {
        if (lockerSound != null) {
            // Stop if already playing and restart
            if (lockerSound.isRunning()) {
                lockerSound.stop();
            }
            lockerSound.setFramePosition(0);
            lockerSound.start();
        }
    }
    
    public void playJumpscareSound() {
        if (jumpscareSound != null) {
            // Stop all other sounds
            stopAmbientSound();
            stopChaseMusic();
            stopWalkingSound();
            
            // Play jumpscare sound
            if (jumpscareSound.isRunning()) {
                jumpscareSound.stop();
            }
            jumpscareSound.setFramePosition(0);
            jumpscareSound.start();
        }
    }
    
    public void stopWalkingSound() {
        if (walkingSound != null && walkingSound.isRunning()) {
            walkingSound.stop();
        }
    }
    
    public void playRandomAmbientSound() {
        if (randomAmbientSounds != null && !isChaseMusicPlaying) {
            int soundIndex = random.nextInt(randomAmbientSounds.length);
            Clip randomSound = randomAmbientSounds[soundIndex];
            
            if (randomSound != null) {
                // Stop if already playing and restart
                if (randomSound.isRunning()) {
                    randomSound.stop();
                }
                randomSound.setFramePosition(0);
                randomSound.start();
            }
        }
    }
    
    public void update(boolean monsterIsActive, boolean monsterIsNearPlayer, boolean playerIsMoving, int playerX, int monsterX, int screenWidth) {
        gameTimer++;
        
        // Handle chase music based on monster proximity
        if (monsterIsActive && monsterIsNearPlayer) {
            if (!isChaseMusicPlaying) {
                startChaseMusic();
            }
        } else {
            if (isChaseMusicPlaying) {
                stopChaseMusic();
            }
        }
        
        // Handle walking sounds
        playWalkingSound(playerIsMoving);
        
        // Play random ambient sounds intermittently (only when not in chase)
        if (!isChaseMusicPlaying && gameTimer >= nextRandomSoundTime) {
            playRandomAmbientSound();
            nextRandomSoundTime = gameTimer + 600 + random.nextInt(900); // 10-25 seconds until next random sound
        }
        
        // Play subtle ambient variations (only when not in chase)
        if (!isChaseMusicPlaying && ambientSound != null && gameTimer >= nextAmbientSoundTime) {
            // Slight volume variation for ambient sounds
            float variation = 0.3f + random.nextFloat() * 0.4f;
            setVolume(ambientSound, ambientVolume * variation);
            
            // Reset volume after a short time
            new Thread(() -> {
                try {
                    Thread.sleep(1000 + random.nextInt(2000));
                    setVolume(ambientSound, ambientVolume);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
            
            nextAmbientSoundTime = gameTimer + 180 + random.nextInt(420); // 3-10 seconds until next ambient variation
        }
    }
    
    public void cleanup() {
        if (ambientSound != null) ambientSound.close();
        if (chaseMusic != null) chaseMusic.close();
        if (walkingSound != null) walkingSound.close();
        if (lockerSound != null) lockerSound.close();
        if (jumpscareSound != null) jumpscareSound.close();
        if (randomAmbientSounds != null) {
            for (Clip clip : randomAmbientSounds) {
                if (clip != null) clip.close();
            }
        }
    }
}