package org.openmbee.flexo.sysmlv2.plugin.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.openmbee.flexo.sysmlv2.plugin.model.SysMLRemote;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SysMLConfigHelper
 * Note: These tests use temporary directories and manipulate user.home system property
 * @ResourceLock ensures tests run sequentially to prevent interference
 */
@ResourceLock("user.home")
class SysMLConfigHelperTest {

    private String originalHome;

    @BeforeEach
    void setUp() {
        originalHome = System.getProperty("user.home");
    }

    private SysMLConfigHelper createConfigWithTempDir(Path tempDir) throws IOException {
        // Set temporary home directory
        System.setProperty("user.home", tempDir.toString());
        
        // Create .flexo directory
        File flexoDir = tempDir.resolve(".flexo").toFile();
        flexoDir.mkdirs();
        
        // Restore after creating config
        SysMLConfigHelper config = new SysMLConfigHelper();
        System.setProperty("user.home", originalHome);
        
        return config;
    }

    @Test
    void testAddRemote(@TempDir Path tempDir) throws IOException {
        System.setProperty("user.home", tempDir.toString());
        try {
            new File(tempDir.toFile(), ".flexo").mkdirs();
            
            SysMLConfigHelper config = new SysMLConfigHelper();
            
            SysMLRemote remote = new SysMLRemote("origin", "http://localhost:9000");
            config.setRemote(remote);
            
            SysMLRemote retrieved = config.getRemote("origin");
            
            assertNotNull(retrieved);
            assertEquals("origin", retrieved.getName());
            assertEquals("http://localhost:9000", retrieved.getUrl());
        } finally {
            System.setProperty("user.home", originalHome);
        }
    }

    @Test
    void testGetNonExistentRemote(@TempDir Path tempDir) throws IOException {
        System.setProperty("user.home", tempDir.toString());
        try {
            new File(tempDir.toFile(), ".flexo").mkdirs();
            SysMLConfigHelper config = new SysMLConfigHelper();
            SysMLRemote remote = config.getRemote("nonexistent");
            
            assertNull(remote);
        } finally {
            System.setProperty("user.home", originalHome);
        }
    }

    @Test
    void testHasRemote(@TempDir Path tempDir) throws IOException {
        System.setProperty("user.home", tempDir.toString());
        try {
            new File(tempDir.toFile(), ".flexo").mkdirs();
            SysMLConfigHelper config = new SysMLConfigHelper();
            
            SysMLRemote remote = new SysMLRemote("test", "http://test.com");
            config.setRemote(remote);
            
            assertTrue(config.hasRemote("test"));
            assertFalse(config.hasRemote("nonexistent"));
        } finally {
            System.setProperty("user.home", originalHome);
        }
    }

    @Test
    void testGetAllRemotes(@TempDir Path tempDir) throws IOException {
        System.setProperty("user.home", tempDir.toString());
        try {
            new File(tempDir.toFile(), ".flexo").mkdirs();
            SysMLConfigHelper config = new SysMLConfigHelper();
            
            SysMLRemote remote1 = new SysMLRemote("origin", "http://localhost:9000");
            SysMLRemote remote2 = new SysMLRemote("production", "https://sysml.example.com");
            
            config.setRemote(remote1);
            config.setRemote(remote2);
            
            Map<String, SysMLRemote> remotes = config.getRemotes();
            
            assertEquals(2, remotes.size());
            assertTrue(remotes.containsKey("origin"));
            assertTrue(remotes.containsKey("production"));
        } finally {
            System.setProperty("user.home", originalHome);
        }
    }

    @Test
    void testRemoveRemote(@TempDir Path tempDir) throws IOException {
        System.setProperty("user.home", tempDir.toString());
        try {
            new File(tempDir.toFile(), ".flexo").mkdirs();
            SysMLConfigHelper config = new SysMLConfigHelper();
            
            SysMLRemote remote = new SysMLRemote("test", "http://test.com");
            config.setRemote(remote);
            
            assertTrue(config.hasRemote("test"));
            
            config.removeRemote("test");
            
            assertFalse(config.hasRemote("test"));
            assertNull(config.getRemote("test"));
        } finally {
            System.setProperty("user.home", originalHome);
        }
    }

    @Test
    void testUpdateRemoteUrl(@TempDir Path tempDir) throws IOException {
        System.setProperty("user.home", tempDir.toString());
        try {
            new File(tempDir.toFile(), ".flexo").mkdirs();
            SysMLConfigHelper config = new SysMLConfigHelper();
            
            SysMLRemote remote = new SysMLRemote("origin", "http://localhost:9000");
            config.setRemote(remote);
            
            remote.setUrl("http://newhost:8000");
            config.setRemote(remote);
            
            SysMLRemote retrieved = config.getRemote("origin");
            assertEquals("http://newhost:8000", retrieved.getUrl());
        } finally {
            System.setProperty("user.home", originalHome);
        }
    }

    @Test
    void testDefaultRemote(@TempDir Path tempDir) throws IOException {
        System.setProperty("user.home", tempDir.toString());
        try {
            new File(tempDir.toFile(), ".flexo").mkdirs();
            SysMLConfigHelper config = new SysMLConfigHelper();
            
            // Default should be "origin"
            assertEquals("origin", config.getDefaultRemote());
            
            // Set custom default
            config.setDefaultRemote("production");
            assertEquals("production", config.getDefaultRemote());
        } finally {
            System.setProperty("user.home", originalHome);
        }
    }

    @Test
    void testRenameRemote(@TempDir Path tempDir) throws IOException {
        System.setProperty("user.home", tempDir.toString());
        try {
            new File(tempDir.toFile(), ".flexo").mkdirs();
            SysMLConfigHelper config = new SysMLConfigHelper();
            
            SysMLRemote remote = new SysMLRemote("old", "http://test.com");
            config.setRemote(remote);
            config.setDefaultRemote("old");
            
            config.renameRemote("old", "new");
            
            assertFalse(config.hasRemote("old"));
            assertTrue(config.hasRemote("new"));
            
            // Default should be updated too
            assertEquals("new", config.getDefaultRemote());
        } finally {
            System.setProperty("user.home", originalHome);
        }
    }

    @Test
    void testRenameNonExistentRemote(@TempDir Path tempDir) throws IOException {
        System.setProperty("user.home", tempDir.toString());
        try {
            new File(tempDir.toFile(), ".flexo").mkdirs();
            SysMLConfigHelper config = new SysMLConfigHelper();
            
            assertThrows(IllegalArgumentException.class, () -> {
                config.renameRemote("nonexistent", "newname");
            });
        } finally {
            System.setProperty("user.home", originalHome);
        }
    }

    @Test
    void testRenameToExistingName(@TempDir Path tempDir) throws IOException {
        System.setProperty("user.home", tempDir.toString());
        try {
            new File(tempDir.toFile(), ".flexo").mkdirs();
            SysMLConfigHelper config = new SysMLConfigHelper();
            
            config.setRemote(new SysMLRemote("remote1", "http://url1.com"));
            config.setRemote(new SysMLRemote("remote2", "http://url2.com"));
            
            assertThrows(IllegalArgumentException.class, () -> {
                config.renameRemote("remote1", "remote2");
            });
        } finally {
            System.setProperty("user.home", originalHome);
        }
    }

    @Test
    void testGetRemoteUrl(@TempDir Path tempDir) throws IOException {
        System.setProperty("user.home", tempDir.toString());
        try {
            new File(tempDir.toFile(), ".flexo").mkdirs();
            SysMLConfigHelper config = new SysMLConfigHelper();
            
            config.setRemote(new SysMLRemote("origin", "http://localhost:9000"));
            config.setRemote(new SysMLRemote("staging", "https://staging.example.com"));
            config.setDefaultRemote("origin");
            
            // Get default remote URL
            assertEquals("http://localhost:9000", config.getRemoteUrl(null));
            
            // Get specific remote URL
            assertEquals("https://staging.example.com", config.getRemoteUrl("staging"));
        } finally {
            System.setProperty("user.home", originalHome);
        }
    }

    @Test
    void testGetRemoteUrlFallback(@TempDir Path tempDir) throws IOException {
        System.setProperty("user.home", tempDir.toString());
        try {
            new File(tempDir.toFile(), ".flexo").mkdirs();
            SysMLConfigHelper config = new SysMLConfigHelper();
            
            // No remotes configured, should return default
            String url = config.getRemoteUrl(null);
            assertEquals("http://localhost:9000", url);
        } finally {
            System.setProperty("user.home", originalHome);
        }
    }

    @Test
    void testPersistence(@TempDir Path tempDir) throws IOException {
        System.setProperty("user.home", tempDir.toString());
        try {
            new File(tempDir.toFile(), ".flexo").mkdirs();
            
            SysMLConfigHelper config1 = new SysMLConfigHelper();
            
            config1.setRemote(new SysMLRemote("origin", "http://localhost:9000"));
            config1.setRemote(new SysMLRemote("prod", "https://prod.example.com"));
            config1.setDefaultRemote("prod");
            config1.save();
            
            // Create new instance (should load from file)
            SysMLConfigHelper config2 = new SysMLConfigHelper();
            
            assertTrue(config2.hasRemote("origin"));
            assertTrue(config2.hasRemote("prod"));
            assertEquals("prod", config2.getDefaultRemote());
            assertEquals("http://localhost:9000", config2.getRemote("origin").getUrl());
            assertEquals("https://prod.example.com", config2.getRemote("prod").getUrl());
        } finally {
            System.setProperty("user.home", originalHome);
        }
    }

    @Test
    void testEmptyConfig(@TempDir Path tempDir) throws IOException {
        System.setProperty("user.home", tempDir.toString());
        try {
            new File(tempDir.toFile(), ".flexo").mkdirs();
            SysMLConfigHelper config = new SysMLConfigHelper();
            
            Map<String, SysMLRemote> remotes = config.getRemotes();
            assertTrue(remotes.isEmpty());
        } finally {
            System.setProperty("user.home", originalHome);
        }
    }

    @Test
    void testMultipleRemotesRetrieval(@TempDir Path tempDir) throws IOException {
        System.setProperty("user.home", tempDir.toString());
        try {
            new File(tempDir.toFile(), ".flexo").mkdirs();
            SysMLConfigHelper config = new SysMLConfigHelper();
            
            config.setRemote(new SysMLRemote("local", "http://localhost:9000"));
            config.setRemote(new SysMLRemote("dev", "https://dev.example.com"));
            config.setRemote(new SysMLRemote("staging", "https://staging.example.com"));
            config.setRemote(new SysMLRemote("prod", "https://prod.example.com"));
            
            Map<String, SysMLRemote> remotes = config.getRemotes();
            
            assertEquals(4, remotes.size());
            assertNotNull(remotes.get("local"));
            assertNotNull(remotes.get("dev"));
            assertNotNull(remotes.get("staging"));
            assertNotNull(remotes.get("prod"));
        } finally {
            System.setProperty("user.home", originalHome);
        }
    }
}
