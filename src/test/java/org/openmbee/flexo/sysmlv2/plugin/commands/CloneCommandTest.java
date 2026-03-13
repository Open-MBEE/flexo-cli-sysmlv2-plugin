package org.openmbee.flexo.sysmlv2.plugin.commands;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic tests for CloneCommand structure
 * 
 * Note: These are structural tests only. Full integration tests would require
 * running instances of SysML v2 API services.
 */
class CloneCommandTest {

    @Test
    void testCommandAnnotationPresent() {
        // Verify command has the @Command annotation
        CommandLine.Command annotation = CloneCommand.class.getAnnotation(CommandLine.Command.class);
        assertNotNull(annotation, "CloneCommand should have @Command annotation");
    }

    @Test
    void testCommandName() {
        // Verify the command name is "clone"
        CommandLine.Command annotation = CloneCommand.class.getAnnotation(CommandLine.Command.class);
        assertNotNull(annotation);
        assertEquals("clone", annotation.name(), "Command name should be 'clone'");
    }

    @Test
    void testCommandHasDescription() {
        // Verify command has a description
        CommandLine.Command annotation = CloneCommand.class.getAnnotation(CommandLine.Command.class);
        assertNotNull(annotation);
        assertTrue(annotation.description().length > 0, "Command should have a description");
        String description = annotation.description()[0].toLowerCase();
        assertTrue(description.contains("clone"), 
                "Description should mention cloning");
    }

    @Test
    void testCommandHasHelpOption() {
        // Verify standard help options are enabled
        CommandLine.Command annotation = CloneCommand.class.getAnnotation(CommandLine.Command.class);
        assertNotNull(annotation);
        assertTrue(annotation.mixinStandardHelpOptions(), 
                "Command should have standard help options enabled");
    }
}
