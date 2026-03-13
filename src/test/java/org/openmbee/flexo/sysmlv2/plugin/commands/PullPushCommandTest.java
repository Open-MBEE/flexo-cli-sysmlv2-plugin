package org.openmbee.flexo.sysmlv2.plugin.commands;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PullCommand and PushCommand
 */
class PullPushCommandTest {

    @Test
    void testPullCommandAnnotation() {
        CommandLine.Command annotation = PullCommand.class.getAnnotation(CommandLine.Command.class);
        assertNotNull(annotation, "PullCommand should have @Command annotation");
        assertEquals("pull", annotation.name(), "Command name should be 'pull'");
    }

    @Test
    void testPushCommandAnnotation() {
        CommandLine.Command annotation = PushCommand.class.getAnnotation(CommandLine.Command.class);
        assertNotNull(annotation, "PushCommand should have @Command annotation");
        assertEquals("push", annotation.name(), "Command name should be 'push'");
    }

    @Test
    void testPullCommandRegistered() {
        CommandLine.Command sysmlAnnotation = SysMLCommand.class.getAnnotation(CommandLine.Command.class);
        assertNotNull(sysmlAnnotation);
        
        Class<?>[] subcommands = sysmlAnnotation.subcommands();
        boolean found = false;
        for (Class<?> subcommand : subcommands) {
            if (subcommand.equals(PullCommand.class)) {
                found = true;
                break;
            }
        }
        
        assertTrue(found, "PullCommand should be registered in SysMLCommand subcommands");
    }

    @Test
    void testPushCommandRegistered() {
        CommandLine.Command sysmlAnnotation = SysMLCommand.class.getAnnotation(CommandLine.Command.class);
        assertNotNull(sysmlAnnotation);
        
        Class<?>[] subcommands = sysmlAnnotation.subcommands();
        boolean found = false;
        for (Class<?> subcommand : subcommands) {
            if (subcommand.equals(PushCommand.class)) {
                found = true;
                break;
            }
        }
        
        assertTrue(found, "PushCommand should be registered in SysMLCommand subcommands");
    }

    @Test
    void testPullCommandHasDescription() {
        CommandLine.Command annotation = PullCommand.class.getAnnotation(CommandLine.Command.class);
        assertNotNull(annotation);
        assertTrue(annotation.description().length > 0, "Command should have a description");
        String description = annotation.description()[0].toLowerCase();
        assertTrue(description.contains("pull") || description.contains("fetch"), 
                "Description should mention pull or fetch");
    }

    @Test
    void testPushCommandHasDescription() {
        CommandLine.Command annotation = PushCommand.class.getAnnotation(CommandLine.Command.class);
        assertNotNull(annotation);
        assertTrue(annotation.description().length > 0, "Command should have a description");
        String description = annotation.description()[0].toLowerCase();
        assertTrue(description.contains("push") || description.contains("commit"), 
                "Description should mention push or commit");
    }

    @Test
    void testPullCommandExtendsBaseCommand() {
        assertTrue(SysMLBaseCommand.class.isAssignableFrom(PullCommand.class),
                "PullCommand should extend SysMLBaseCommand");
    }

    @Test
    void testPushCommandExtendsBaseCommand() {
        assertTrue(SysMLBaseCommand.class.isAssignableFrom(PushCommand.class),
                "PushCommand should extend SysMLBaseCommand");
    }
}
