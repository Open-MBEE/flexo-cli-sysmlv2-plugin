package org.openmbee.flexo.sysmlv2.plugin.commands;

import org.openmbee.flexo.cli.container.ContainerException;
import org.openmbee.flexo.cli.container.ContainerServiceHelper;
import org.openmbee.flexo.cli.plugin.PluginCommand;
import org.openmbee.flexo.sysmlv2.plugin.config.SysMLConfigHelper;
import org.openmbee.flexo.sysmlv2.plugin.model.SysMLRemote;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;

/**
 * Init command - Initialize a local SysML v2 API service
 * Automatically starts container service and configures the plugin
 * Supports Docker, Podman, and Colima container runtimes
 */
@Command(
        name = "init",
        description = "Initialize local SysML v2 API service (starts container on port 9000)",
        mixinStandardHelpOptions = true
)
public class InitCommand extends PluginCommand {

    private static final String DOCKER_COMPOSE_FILE = "sysmlv2-docker-compose.yml";
    private static final String DEFAULT_SYSMLV2_URL = "http://localhost:9000";
    private static final int SYSMLV2_PORT = 9000;
    private static final String SERVICE_NAME = "sysmlv2-service";
    
    private ContainerServiceHelper containerHelper;

    @Option(names = {"--skip-docker"}, description = "Skip container startup (assumes SysML v2 service already running on port 9000)")
    private boolean skipDocker = false;

    @Option(names = {"--url"}, description = "SysML v2 API URL (default: http://localhost:9000)")
    private String sysmlv2Url = DEFAULT_SYSMLV2_URL;

    @Option(names = {"--remote-name"}, description = "Remote name to create (default: origin)")
    private String remoteName = "origin";

    @Option(names = {"--set-default"}, description = "Set as default remote (default: true)")
    private boolean setDefault = true;

    @Option(names = {"--flexo-remote"}, description = "Name of Flexo backend remote to use for authentication")
    private String flexoRemote;

    @Override
    public void run() {
        info("Initializing SysML v2 API service...");
        
        // Initialize container helper if not skipping Docker
        if (!skipDocker) {
            try {
                containerHelper = new ContainerServiceHelper(getConfig(), isVerbose());
                info("Using container runtime: " + containerHelper.getRuntimeInfo());
            } catch (ContainerException e) {
                error("Failed to initialize container runtime: " + e.getMessage());
                error("Install Docker/Podman/Colima or use --skip-docker if service is already running");
                if (isVerbose()) {
                    e.printStackTrace();
                }
                throw new RuntimeException("Container runtime not available", e);
            }
        }
        
        info("This will:");
        if (!skipDocker) {
            info("  1. Start container service (SysML v2 API on port 9000)");
        }
        info("  2. Create remote '" + remoteName + "' with URL: " + sysmlv2Url);

        try {
            if (!skipDocker) {
                startSysMLv2Service();
            } else {
                info("Skipping container startup...");
                verifySysMLv2ServiceAvailable();
            }

            createOrUpdateRemote();

            success("Initialization complete!");
            info("");
            info("Remote '" + remoteName + "' configured with URL: " + sysmlv2Url);
            if (setDefault) {
                info("Set as default remote");
            }
            info("");
            info("You can now use the SysML v2 commands:");
            info("  flexo sysml project list");
            info("  flexo sysml project create --name \"My Project\"");
            info("");
            info("To use a different remote: flexo sysml --remote <name> <command>");

        } catch (Exception e) {
            error("Initialization failed: " + e.getMessage());
            if (isVerbose()) {
                e.printStackTrace();
            }
            throw new RuntimeException("Initialization failed: " + e.getMessage(), e);
        }
    }

    private void startSysMLv2Service() throws Exception {
        info("Starting SysML v2 API service...");

        // Extract compose file from plugin resources (not parent classloader)
        File composeFile = extractComposeFileFromPlugin(DOCKER_COMPOSE_FILE);

        info("  Using compose file: " + composeFile.getAbsolutePath());

        boolean success = containerHelper.startService(composeFile, SERVICE_NAME);

        if (!success) {
            throw new ContainerException("Failed to start " + SERVICE_NAME + ". Please check container logs:\n" +
                    "  " + containerHelper.getRuntime().getName() + " logs " + SERVICE_NAME);
        }

        success("  " + SERVICE_NAME + " started");
        info("  Waiting for " + SERVICE_NAME + " to be ready...");

        waitForSysMLv2Service();
        verifySysMLv2ServiceHealth();
    }

    private void waitForSysMLv2Service() throws Exception {
        containerHelper.waitForServicePort(SERVICE_NAME, SYSMLV2_PORT, 30, 2000);
    }

    private void verifySysMLv2ServiceAvailable() throws Exception {
        info("  Verifying SysML v2 service at " + sysmlv2Url + "...");
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", SYSMLV2_PORT), 5000);
            success("  SysML v2 service is available");
        } catch (Exception e) {
            throw new Exception("SysML v2 service is not available at " + sysmlv2Url + ". " +
                    "Please ensure the service is running or use 'flexo sysml init' without --skip-docker");
        }
    }

    private void verifySysMLv2ServiceHealth() throws Exception {
        String healthUrl = sysmlv2Url + "/";
        int maxAttempts = 15;
        int attempt = 0;

        info("  Verifying service health...");
        while (attempt < maxAttempts) {
            try {
                URL url = new URL(healthUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                int responseCode = conn.getResponseCode();
                
                if (responseCode >= 200 && responseCode < 500) {
                    success("  " + SERVICE_NAME + " is healthy");
                    return;
                }
            } catch (Exception e) {
                // Ignore and retry
            }

            attempt++;
            if (attempt < maxAttempts) {
                Thread.sleep(2000);
                if (isVerbose()) {
                    debug("  Checking service health... (attempt " + attempt + "/" + maxAttempts + ")");
                }
            }
        }

        warn("  Service health check timed out, proceeding anyway...");
    }

    private void createOrUpdateRemote() throws Exception {
        SysMLConfigHelper config = new SysMLConfigHelper();
        
        // Validate Flexo remote if specified
        if (flexoRemote != null) {
            validateFlexoRemote(flexoRemote);
        }
        
        // Check if remote exists
        if (config.hasRemote(remoteName)) {
            info("  Remote '" + remoteName + "' already exists, updating...");
            SysMLRemote remote = config.getRemote(remoteName);
            remote.setUrl(sysmlv2Url);
            if (flexoRemote != null) {
                remote.setFlexoRemote(flexoRemote);
            }
            config.setRemote(remote);
        } else {
            info("  Creating remote '" + remoteName + "'...");
            SysMLRemote remote = new SysMLRemote(remoteName, sysmlv2Url, flexoRemote);
            config.setRemote(remote);
        }
        
        // Set as default if requested
        if (setDefault) {
            config.setDefaultRemote(remoteName);
        }
        
        config.save();
        success("  Remote '" + remoteName + "' configured");
    }

    /**
     * Extract compose file from plugin resources
     * Needed because ContainerServiceHelper uses parent project's classloader
     */
    private File extractComposeFileFromPlugin(String resourceName) throws IOException {
        InputStream resourceStream = getClass().getClassLoader()
                .getResourceAsStream(resourceName);
        
        if (resourceStream == null) {
            throw new FileNotFoundException("Resource not found in plugin: " + resourceName);
        }
        
        // Create temporary file with secure permissions
        File tempFile;
        String os = System.getProperty("os.name").toLowerCase();
        
        if (os.contains("unix") || os.contains("linux") || os.contains("mac")) {
            tempFile = java.nio.file.Files.createTempFile(
                "sysmlv2-docker-compose-",
                ".yml",
                java.nio.file.attribute.PosixFilePermissions.asFileAttribute(
                    java.nio.file.attribute.PosixFilePermissions.fromString("rw-------")
                )
            ).toFile();
        } else {
            tempFile = java.nio.file.Files.createTempFile("sysmlv2-docker-compose-", ".yml").toFile();
            tempFile.setReadable(false, false);
            tempFile.setReadable(true, true);
            tempFile.setWritable(false, false);
            tempFile.setWritable(true, true);
            tempFile.setExecutable(false, false);
        }
        tempFile.deleteOnExit();
        
        // Copy resource to temporary file
        try (FileOutputStream fos = new FileOutputStream(tempFile);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = resourceStream.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }
            bos.flush();
        } finally {
            resourceStream.close();
        }
        
        return tempFile;
    }

    /**
     * Validate that a Flexo backend remote exists in the configuration
     */
    private void validateFlexoRemote(String flexoRemoteName) {
        try {
            org.openmbee.flexo.cli.config.FlexoConfig flexoConfig = getConfig();
            String remoteUrl = flexoConfig.getRemoteUrl(flexoRemoteName);
            
            if (remoteUrl == null) {
                throw new IllegalArgumentException(
                    "Flexo backend remote '" + flexoRemoteName + "' does not exist.\n" +
                    "Add it first with: flexo remote add " + flexoRemoteName + " <url>"
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to validate Flexo backend remote: " + e.getMessage(), e);
        }
    }
}
