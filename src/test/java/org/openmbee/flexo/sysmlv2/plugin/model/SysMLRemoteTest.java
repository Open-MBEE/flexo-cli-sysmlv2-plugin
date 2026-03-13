package org.openmbee.flexo.sysmlv2.plugin.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SysMLRemote model class
 */
class SysMLRemoteTest {

    @Test
    void testConstructorWithParameters() {
        SysMLRemote remote = new SysMLRemote("origin", "http://localhost:9000");
        
        assertEquals("origin", remote.getName());
        assertEquals("http://localhost:9000", remote.getUrl());
    }

    @Test
    void testDefaultConstructor() {
        SysMLRemote remote = new SysMLRemote();
        
        assertNull(remote.getName());
        assertNull(remote.getUrl());
    }

    @Test
    void testSetters() {
        SysMLRemote remote = new SysMLRemote();
        
        remote.setName("production");
        remote.setUrl("https://sysml.example.com");
        
        assertEquals("production", remote.getName());
        assertEquals("https://sysml.example.com", remote.getUrl());
    }

    @Test
    void testEquals() {
        SysMLRemote remote1 = new SysMLRemote("origin", "http://localhost:9000");
        SysMLRemote remote2 = new SysMLRemote("origin", "http://different:9000");
        SysMLRemote remote3 = new SysMLRemote("other", "http://localhost:9000");
        
        // Same name = equal (URL doesn't matter for equality)
        assertEquals(remote1, remote2);
        
        // Different name = not equal
        assertNotEquals(remote1, remote3);
    }

    @Test
    void testHashCode() {
        SysMLRemote remote1 = new SysMLRemote("origin", "http://localhost:9000");
        SysMLRemote remote2 = new SysMLRemote("origin", "http://different:9000");
        
        // Same name = same hash code
        assertEquals(remote1.hashCode(), remote2.hashCode());
    }

    @Test
    void testToString() {
        SysMLRemote remote = new SysMLRemote("origin", "http://localhost:9000");
        String str = remote.toString();
        
        assertTrue(str.contains("origin"));
        assertTrue(str.contains("http://localhost:9000"));
    }
}
