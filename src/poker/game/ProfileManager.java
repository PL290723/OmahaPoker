package poker.game;

import java.io.*;
import java.util.*;

/**
 * Gestiona múltiples perfiles de jugador con progreso independiente
 */
public class ProfileManager {
    
    public static class Profile {
        public String name;
        public int totalWins;
        public int totalLosses;
        public int currentLevel;
        public int totalChips;
        public long createdDate;
        public long lastPlayed;
        
        public Profile(String name) {
            this.name = name;
            this.totalWins = 0;
            this.totalLosses = 0;
            this.currentLevel = 1;
            this.totalChips = 5000;
            this.createdDate = System.currentTimeMillis();
            this.lastPlayed = System.currentTimeMillis();
        }
        
        public String getFormattedDate(long timestamp) {
            Date date = new Date(timestamp);
            return String.format("%td/%<tm/%<tY", date);
        }
        
        @Override
        public String toString() {
            double winRate = (totalWins + totalLosses) > 0 
                ? (double) totalWins / (totalWins + totalLosses) * 100 
                : 0;
            
            return String.format("%-15s | Nivel: %d | V/D: %d/%d (%.1f%%) | Fichas: $%d | Jugado: %s",
                name, currentLevel, totalWins, totalLosses, winRate, totalChips, 
                getFormattedDate(lastPlayed));
        }
    }
    
    private static final String PROFILES_DIR = "profiles";
    private static final int MAX_PROFILES = 3;
    
    public ProfileManager() {
        File dir = new File(PROFILES_DIR);
        if (!dir.exists()) {
            dir.mkdir();
        }
    }
    
    /**
     * Carga todos los perfiles disponibles
     */
    public List<Profile> loadProfiles() {
        List<Profile> profiles = new ArrayList<>();
        File dir = new File(PROFILES_DIR);
        
        File[] files = dir.listFiles((d, name) -> name.endsWith(".profile"));
        if (files != null) {
            for (File file : files) {
                Profile profile = loadProfile(file);
                if (profile != null) {
                    profiles.add(profile);
                }
            }
        }
        
        // Ordenar por fecha de último uso
        profiles.sort((a, b) -> Long.compare(b.lastPlayed, a.lastPlayed));
        
        return profiles;
    }
    
    /**
     * Carga un perfil específico desde archivo
     */
    private Profile loadProfile(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            Profile profile = null;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                
                String[] parts = line.split("=");
                if (parts.length != 2) continue;
                
                String key = parts[0].trim();
                String value = parts[1].trim();
                
                if (key.equals("NAME")) {
                    profile = new Profile(value);
                } else if (profile != null) {
                    switch (key) {
                        case "TOTAL_WINS":
                            profile.totalWins = Integer.parseInt(value);
                            break;
                        case "TOTAL_LOSSES":
                            profile.totalLosses = Integer.parseInt(value);
                            break;
                        case "CURRENT_LEVEL":
                            profile.currentLevel = Integer.parseInt(value);
                            break;
                        case "TOTAL_CHIPS":
                            profile.totalChips = Integer.parseInt(value);
                            break;
                        case "CREATED_DATE":
                            profile.createdDate = Long.parseLong(value);
                            break;
                        case "LAST_PLAYED":
                            profile.lastPlayed = Long.parseLong(value);
                            break;
                    }
                }
            }
            
            return profile;
        } catch (IOException | NumberFormatException e) {
            System.err.println("[X] Error cargando perfil: " + file.getName());
            return null;
        }
    }
    
    /**
     * Guarda un perfil en archivo
     */
    public void saveProfile(Profile profile) {
        String filename = PROFILES_DIR + "/" + sanitizeFilename(profile.name) + ".profile";
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("# PROFILE DATA");
            writer.println("NAME=" + profile.name);
            writer.println("TOTAL_WINS=" + profile.totalWins);
            writer.println("TOTAL_LOSSES=" + profile.totalLosses);
            writer.println("CURRENT_LEVEL=" + profile.currentLevel);
            writer.println("TOTAL_CHIPS=" + profile.totalChips);
            writer.println("CREATED_DATE=" + profile.createdDate);
            writer.println("LAST_PLAYED=" + profile.lastPlayed);
            
            System.out.println("[OK] Perfil guardado: " + profile.name);
        } catch (IOException e) {
            System.err.println("[X] Error guardando perfil: " + e.getMessage());
        }
    }
    
    /**
     * Crea un nuevo perfil
     */
    public Profile createProfile(String name) {
        List<Profile> existing = loadProfiles();
        
        if (existing.size() >= MAX_PROFILES) {
            System.err.println("[X] Límite de perfiles alcanzado (" + MAX_PROFILES + ")");
            return null;
        }
        
        // Verificar que no exista
        for (Profile p : existing) {
            if (p.name.equalsIgnoreCase(name)) {
                System.err.println("[X] Ya existe un perfil con ese nombre");
                return null;
            }
        }
        
        Profile profile = new Profile(name);
        saveProfile(profile);
        return profile;
    }
    
    /**
     * Borra un perfil
     */
    public boolean deleteProfile(String name) {
        String filename = PROFILES_DIR + "/" + sanitizeFilename(name) + ".profile";
        File file = new File(filename);
        
        if (file.exists() && file.delete()) {
            System.out.println("[OK] Perfil borrado: " + name);
            
            // También borrar los datos de IA asociados
            deleteProfileBossData(name);
            return true;
        }
        
        return false;
    }
    
    /**
     * Borra datos de IA asociados a un perfil
     */
    private void deleteProfileBossData(String profileName) {
        String prefix = "ia_" + sanitizeFilename(profileName) + "_boss_";
        File dir = new File(".");
        
        File[] files = dir.listFiles((d, filename) -> filename.startsWith(prefix));
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
    }
    
    /**
     * Limpia el nombre para usarlo como nombre de archivo
     */
    private String sanitizeFilename(String name) {
        return name.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
    }
    
    /**
     * Obtiene el nombre de archivo para datos de IA específicos de perfil
     */
    public static String getBossDataFilename(String profileName, int bossNumber, String bossName) {
        String sanitized = profileName.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
        String bossNameSanitized = bossName.replace(" ", "_");
        return "data/ia_" + sanitized + "_boss_" + bossNumber + "_" + bossNameSanitized + ".dat";
    }
}
