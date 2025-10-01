import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Program for searching strings in files with extensions .class, .properties and .jar
 * Search is performed recursively, JAR archives are unpacked in memory
 */
public class SearchString {
    
    // Class for storing search results
    static class SearchResult {
        String filePath;
        String entryPath; // path inside JAR archive (if applicable)
        boolean isJarEntry;
        
        SearchResult(String filePath, String entryPath, boolean isJarEntry) {
            this.filePath = filePath;
            this.entryPath = entryPath;
            this.isJarEntry = isJarEntry;
        }
        
        @Override
        public String toString() {
            if (isJarEntry) {
                return filePath + " -> " + entryPath;
            }
            return filePath;
        }
    }
    
    private static final Set<String> TARGET_EXTENSIONS = Set.of(".class", ".properties", ".jar");
    private static List<SearchResult> results = new ArrayList<>();
    
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java SearchString <search_path> <search_string>");
            System.out.println("Example: java SearchString C:\\myproject \"myString\"");
            return;
        }
        
        String searchPath = args[0];
        String searchString = args[1];
        
        System.out.println("Searching for string: \"" + searchString + "\"");
        System.out.println("In directory: " + searchPath);
        System.out.println("Looking for files with extensions: .class, .properties, .jar");
        System.out.println("==========================================");
        
        try {
            searchInDirectory(Paths.get(searchPath), searchString);
            
            if (results.isEmpty()) {
                System.out.println("String not found in specified files.");
            } else {
                System.out.println("Found matches: " + results.size());
                System.out.println("Results:");
                for (SearchResult result : results) {
                    System.out.println("  " + result);
                }
            }
        } catch (Exception e) {
            System.err.println("Error during search: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Recursive search in directory
     */
    private static void searchInDirectory(Path directory, String searchString) throws IOException {
        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            System.err.println("Directory does not exist or is not a directory: " + directory);
            return;
        }
        
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path path : stream) {
                if (Files.isDirectory(path)) {
                    // Recursive search in subdirectories
                    searchInDirectory(path, searchString);
                } else if (Files.isRegularFile(path)) {
                    String fileName = path.getFileName().toString().toLowerCase();
                    String extension = getFileExtension(fileName);
                    
                    if (TARGET_EXTENSIONS.contains(extension)) {
                        if (".jar".equals(extension)) {
                            // JAR archive processing
                            searchInJarFile(path, searchString);
                        } else {
                            // Regular file processing
                            searchInFile(path, searchString);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Get file extension
     */
    private static String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex);
        }
        return "";
    }
    
    /**
     * Search in regular file
     */
    private static void searchInFile(Path filePath, String searchString) {
        try {
            // Try different encodings for better compatibility
            String content = null;
            try {
                content = Files.readString(filePath, java.nio.charset.StandardCharsets.UTF_8);
            } catch (Exception e) {
                // Try with ISO-8859-1 if UTF-8 fails
                try {
                    content = Files.readString(filePath, java.nio.charset.StandardCharsets.ISO_8859_1);
                } catch (Exception e2) {
                    // Try with default encoding
                    content = Files.readString(filePath);
                }
            }
            
            if (content != null && content.toLowerCase().contains(searchString.toLowerCase())) {
                results.add(new SearchResult(filePath.toString(), null, false));
                System.out.println("Found in file: " + filePath);
            }
        } catch (IOException e) {
            // Skip files that cannot be read (don't print error for every failed file)
            if (e.getMessage().contains("Input length")) {
                // Skip files with encoding issues silently
            } else {
                System.err.println("Failed to read file: " + filePath + " - " + e.getMessage());
            }
        }
    }
    
    /**
     * Search in JAR archive
     */
    private static void searchInJarFile(Path jarPath, String searchString) {
        try {
            // Check if JAR file is empty or corrupted
            if (Files.size(jarPath) == 0) {
                System.err.println("Skipping empty JAR file: " + jarPath);
                return;
            }
            
            try (JarFile jarFile = new JarFile(jarPath.toFile())) {
                Enumeration<JarEntry> entries = jarFile.entries();
                
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String entryName = entry.getName().toLowerCase();
                    
                    // Check if file is target by extension
                    if (isTargetFile(entryName)) {
                        try (InputStream inputStream = jarFile.getInputStream(entry)) {
                            String content = new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                            
                            if (content.toLowerCase().contains(searchString.toLowerCase())) {
                                results.add(new SearchResult(jarPath.toString(), entry.getName(), true));
                                System.out.println("Found in JAR: " + jarPath + " -> " + entry.getName());
                            }
                        } catch (IOException e) {
                            // Skip files that cannot be read (don't print error for every failed file)
                            if (!e.getMessage().contains("Input length")) {
                                System.err.println("Failed to read file in JAR: " + jarPath + " -> " + entry.getName());
                            }
                        }
                    }
                }
            }
        } catch (SecurityException e) {
            // Handle signed JAR files that cause security exceptions
            System.err.println("Skipping signed JAR file (security restriction): " + jarPath);
        } catch (IOException e) {
            if (e.getMessage().contains("zip file is empty")) {
                System.err.println("Skipping empty JAR file: " + jarPath);
            } else {
                System.err.println("Failed to open JAR file: " + jarPath + " - " + e.getMessage());
            }
        }
    }
    
    /**
     * Check if file is target by extension
     */
    private static boolean isTargetFile(String fileName) {
        String extension = getFileExtension(fileName);
        return TARGET_EXTENSIONS.contains(extension);
    }
}
