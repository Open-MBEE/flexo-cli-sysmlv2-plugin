package org.openmbee.flexo.sysmlv2.plugin.commands;

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
import java.nio.file.*;
import java.util.Properties;

/**
 * Init command - Initialize a local SysML v2 API service
 * Automatically starts Docker service and configures the plugin
 */
@Command(
        name = "init",
        description = "Initialize local SysML v2 API service (starts Docker container on port 9000)",
        mixinStandardHelpOptions = true
)
public class InitCommand extends PluginCommand {

    private static final String DOCKER_COMPOSE_FILE = "sysmlv2-docker-compose.yml";
    private static final String DEFAULT_SYSMLV2_URL = "http://localhost:9000";
    private static final int SYSMLV2_PORT = 9000;
    private static final String SERVICE_NAME = "sysmlv2-service";
    
    // Trusted paths where docker executable is expected to be found
    private static final String[] TRUSTED_DOCKER_PATHS = {
        "/usr/bin/docker",           // Linux standard location
        "/usr/local/bin/docker",     // macOS Homebrew/standard location
        "/opt/homebrew/bin/docker",  // macOS Apple Silicon Homebrew
        "C:\\Program Files\\Docker\\Docker\\resources\\bin\\docker.exe",  // Windows Docker Desktop
        "/Applications/Docker.app/Contents/Resources/bin/docker"  // macOS Docker Desktop
    };
    
    private static final String[] TRUSTED_DOCKER_COMPOSE_PATHS = {
        "/usr/bin/docker-compose",
        "/usr/local/bin/docker-compose",
        "/opt/homebrew/bin/docker-compose",
        "C:\\Program Files\\Docker\\Docker\\resources\\bin\\docker-compose.exe"
    };

    @Option(names = {"--skip-docker"}, description = "Skip Docker service startup (assumes service already running)")
    private boolean skipDocker = false;

    @Option(names = {"--url"}, description = "SysML v2 API URL (default: http://localhost:9000)")
    private String sysmlv2Url = DEFAULT_SYSMLV2_URL;

    @Option(names = {"--remote-name"}, description = "Remote name to create (default: origin)")
    private String remoteName = "origin";

    @Option(names = {"--set-default"}, description = "Set as default remote (default: true)")
    private boolean setDefault = true;

    @Override
    public void run() {
        info("Initializing SysML v2 API service...");
        info("This will:");
        if (!skipDocker) {
            info("  1. Start Docker service (SysML v2 API on port 9000)");
        }
        info("  2. Create remote '" + remoteName + "' with URL: " + sysmlv2Url);

        try {
            if (!skipDocker) {
                startSysMLv2Service();
            } else {
                info("Skipping Docker startup...");
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

        File composeFile = extractDockerComposeFromClasspath();
        if (composeFile == null) {
            throw new Exception("Docker compose file not found in plugin resources. " +
                    "Please ensure the plugin is properly packaged.");
        }

        info("  Using docker-compose file: " + composeFile.getAbsolutePath());

        if (!isDockerAvailable()) {
            throw new Exception("Docker is not available. Please install Docker and ensure it's running.");
        }

        boolean success = runDockerComposeService(composeFile, SERVICE_NAME);

        if (!success) {
            throw new Exception("Failed to start " + SERVICE_NAME + ". Please check Docker logs:\n" +
                    "  docker logs " + SERVICE_NAME);
        }

        success("  " + SERVICE_NAME + " started");
        info("  Waiting for " + SERVICE_NAME + " to be ready...");

        waitForSysMLv2Service();
        verifySysMLv2ServiceHealth();
    }

    private void waitForSysMLv2Service() throws Exception {
        int maxAttempts = 30;
        int attempt = 0;
        info("  Waiting for service on port " + SYSMLV2_PORT + "...");
        
        while (attempt < maxAttempts) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress("localhost", SYSMLV2_PORT), 1000);
                success("  " + SERVICE_NAME + " is ready");
                return;
            } catch (Exception e) {
                attempt++;
                if (attempt >= maxAttempts) {
                    throw new Exception(SERVICE_NAME + " did not become ready within timeout. " +
                            "Please check Docker logs: docker logs " + SERVICE_NAME);
                }
                Thread.sleep(2000);
                if (isVerbose()) {
                    debug("  Waiting for " + SERVICE_NAME + "... (attempt " + attempt + "/" + maxAttempts + ")");
                }
            }
        }
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

    private File extractDockerComposeFromClasspath() throws Exception {
        InputStream resourceStream = getClass().getClassLoader()
                .getResourceAsStream(DOCKER_COMPOSE_FILE);
        
        if (resourceStream == null) {
            return null;
        }

        // Create temporary file with secure permissions
        File tempFile;
        String os = System.getProperty("os.name").toLowerCase();
        
        if (os.contains("unix") || os.contains("linux") || os.contains("mac")) {
            // On Unix-like systems, create file with restrictive permissions
            tempFile = Files.createTempFile(
                "sysmlv2-docker-compose-",
                ".yml",
                java.nio.file.attribute.PosixFilePermissions.asFileAttribute(
                    java.nio.file.attribute.PosixFilePermissions.fromString("rw-------")
                )
            ).toFile();
        } else {
            // On Windows, create file then set restrictive permissions
            tempFile = Files.createTempFile(
                "sysmlv2-docker-compose-",
                ".yml"
            ).toFile();
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

        if (isVerbose()) {
            debug("  Extracted docker-compose file to: " + tempFile.getAbsolutePath());
        }

        return tempFile;
    }

    private String findDockerExecutable() {
        for (String path : TRUSTED_DOCKER_PATHS) {
            File dockerFile = new File(path);
            if (dockerFile.exists() && dockerFile.canExecute()) {
                return path;
            }
        }
        return null;
    }

    private String[] findDockerComposeCommand() {
        // First try docker compose (preferred, newer approach)
        String dockerPath = findDockerExecutable();
        if (dockerPath != null) {
            try {
                ProcessBuilder pb = new ProcessBuilder(dockerPath, "compose", "version");
                pb.redirectErrorStream(true);
                Process process = pb.start();
                if (process.waitFor() == 0) {
                    return new String[] { dockerPath, "compose" };
                }
            } catch (Exception e) {
                // Fall through to try standalone docker-compose
            }
        }
        
        // Try standalone docker-compose
        for (String path : TRUSTED_DOCKER_COMPOSE_PATHS) {
            File composeFile = new File(path);
            if (composeFile.exists() && composeFile.canExecute()) {
                return new String[] { path };
            }
        }
        
        return null;
    }

    private boolean isDockerAvailable() {
        String dockerPath = findDockerExecutable();
        if (dockerPath == null) {
            return false;
        }
        
        try {
            ProcessBuilder pb = new ProcessBuilder(dockerPath, "--version");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean runDockerComposeService(File composeFile, String serviceName) throws Exception {
        String[] composeCommand = findDockerComposeCommand();
        if (composeCommand == null) {
            throw new Exception("Docker Compose is not available. Please install Docker Compose.");
        }

        ProcessBuilder pb = new ProcessBuilder(composeCommand);
        pb.command().add("-f");
        pb.command().add(composeFile.getAbsolutePath());
        pb.command().add("up");
        pb.command().add("-d");
        pb.command().add(serviceName);
        pb.redirectErrorStream(true);

        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (isVerbose()) {
                    debug("  " + line);
                }
            }
        }

        int exitCode = process.waitFor();
        return exitCode == 0;
    }

    private void createOrUpdateRemote() throws Exception {
        SysMLConfigHelper config = new SysMLConfigHelper();
        
        // Check if remote exists
        if (config.hasRemote(remoteName)) {
            info("  Remote '" + remoteName + "' already exists, updating URL...");
            SysMLRemote remote = config.getRemote(remoteName);
            remote.setUrl(sysmlv2Url);
            config.setRemote(remote);
        } else {
            info("  Creating remote '" + remoteName + "'...");
            SysMLRemote remote = new SysMLRemote(remoteName, sysmlv2Url);
            config.setRemote(remote);
        }
        
        // Set as default if requested
        if (setDefault) {
            config.setDefaultRemote(remoteName);
        }
        
        config.save();
        success("  Remote '" + remoteName + "' configured");
    }
}
