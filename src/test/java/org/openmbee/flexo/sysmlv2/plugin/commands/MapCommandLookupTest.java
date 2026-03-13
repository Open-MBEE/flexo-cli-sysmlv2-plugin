package org.openmbee.flexo.sysmlv2.plugin.commands;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MapCommand.LookupCommand
 */
class MapCommandLookupTest {

    @Test
    void testLookupCommandAnnotation() {
        // Verify lookup command has the @Command annotation
        CommandLine.Command annotation = MapCommand.LookupCommand.class.getAnnotation(CommandLine.Command.class);
        assertNotNull(annotation, "LookupCommand should have @Command annotation");
        assertEquals("lookup", annotation.name(), "Command name should be 'lookup'");
    }

    @Test
    void testLookupCommandRegisteredInMapCommand() {
        // Verify lookup command is registered in MapCommand subcommands
        CommandLine.Command mapAnnotation = MapCommand.class.getAnnotation(CommandLine.Command.class);
        assertNotNull(mapAnnotation);
        
        Class<?>[] subcommands = mapAnnotation.subcommands();
        boolean found = false;
        for (Class<?> subcommand : subcommands) {
            if (subcommand.equals(MapCommand.LookupCommand.class)) {
                found = true;
                break;
            }
        }
        
        assertTrue(found, "LookupCommand should be registered in MapCommand subcommands");
    }

    @Test
    void testLookupCommandHasDescription() {
        // Verify command has a description
        CommandLine.Command annotation = MapCommand.LookupCommand.class.getAnnotation(CommandLine.Command.class);
        assertNotNull(annotation);
        assertTrue(annotation.description().length > 0, "Command should have a description");
        String description = annotation.description()[0].toLowerCase();
        assertTrue(description.contains("local") || description.contains("remote"), 
                "Description should mention local or remote");
    }

    @Test
    void testLookupCommandHasHelpOption() {
        // Verify standard help options are enabled
        CommandLine.Command annotation = MapCommand.LookupCommand.class.getAnnotation(CommandLine.Command.class);
        assertNotNull(annotation);
        assertTrue(annotation.mixinStandardHelpOptions(), 
                "Command should have standard help options enabled");
    }
}
