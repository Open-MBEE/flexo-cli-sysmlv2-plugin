package org.openmbee.flexo.sysmlv2.plugin.commands;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openmbee.flexo.sysmlv2.plugin.client.SysMLv2Client;
import org.openmbee.flexo.sysmlv2.plugin.config.SysMLConfigHelper;
import org.openmbee.flexo.sysmlv2.plugin.model.SysMLRemote;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.ArrayList;
import java.util.List;

/**
 * Push command for SysML v2 - Commit model changes to a remote SysML project/branch
 * 
 * This command delegates to the underlying Flexo CLI push command to push data
 * to the remote MMS backend.
 * 
 * Usage: flexo sysml push <project-id> [options]
 */
@Command(
    name = "push",
    description = "Push model to remote SysML v2 project",
    mixinStandardHelpOptions = true
)
public class PushCommand extends SysMLBaseCommand {

    @Parameters(index = "0", description = "Project ID")
    private String projectId;

    @Option(names = {"-b", "--branch"}, description = "Branch ID (optional)")
    private String branchId;

    @Option(names = {"-m", "--message"}, description = "Commit message", required = true)
    private String commitMessage;

    @Option(names = {"-i", "--input"}, description = "Input file (default: stdin)")
    private String inputFile;

    @Option(names = {"-f", "--format"}, description = "RDF format (turtle, jsonld, rdfxml, ntriples)")
    private String format = "turtle";

    @Override
    public void run() {
        try {
            info("Pushing to remote...");
            debug("Project ID: " + projectId);
            if (branchId != null) {
                debug("Branch ID: " + branchId);
            }

            // Step 1: Get remote name from context
            String remoteName = getRemoteName();
            if (remoteName == null) {
                try {
                    SysMLConfigHelper config = new SysMLConfigHelper();
                    remoteName = config.getDefaultRemote();
                } catch (Exception e) {
                    remoteName = "origin";
                }
            }

            // Step 2: Load configuration
            SysMLConfigHelper sysmlConfig = new SysMLConfigHelper();
            
            // Step 3: Get remote URL
            String remoteUrl = sysmlConfig.getRemoteUrl(remoteName);
            if (remoteUrl == null) {
                error("Remote '" + remoteName + "' not found in configuration");
                info("Add it with: flexo sysml remote add " + remoteName + " <url>");
                System.exit(1);
                return;
            }

            info("  Remote: " + remoteName);
            info("  Remote URL: " + remoteUrl);

            // Step 4: If no branch specified, fetch remote project's default branch
            if (branchId == null || branchId.isEmpty()) {
                info("  No branch specified, fetching remote default branch...");
                try {
                    SysMLv2Client client = new SysMLv2Client(remoteUrl, getClient());
                    String remoteProjectResponse = client.getProject(projectId);
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode remoteProject = mapper.readTree(remoteProjectResponse);
                    
                    if (remoteProject.has("defaultBranch") && remoteProject.get("defaultBranch").has("@id")) {
                        branchId = remoteProject.get("defaultBranch").get("@id").asText();
                        info("  Using default branch: " + branchId);
                    }
                } catch (Exception e) {
                    warn("Failed to fetch remote project's default branch: " + e.getMessage());
                    if (isVerbose()) {
                        e.printStackTrace();
                    }
                    // Continue without branch specification
                }
            }
            info("");

            // Step 5: Build flexo push command
            info("Executing: flexo push...");
            
            // Get Flexo organization name from remote config (defaults to "sysmlv2")
            SysMLRemote remote = sysmlConfig.getRemote(remoteName);
            String flexoOrg = (remote != null) ? remote.getOrgOrDefault() : "sysmlv2";
            
            List<String> command = new ArrayList<>();
            command.add("flexo");
            command.add("push");
            command.add("--org");
            command.add(flexoOrg);
            command.add("--repo");
            command.add(projectId);
            command.add("--remote");
            command.add(remoteName);
            command.add("--message");
            command.add(commitMessage);
            
            if (branchId != null && !branchId.isEmpty()) {
                command.add("--branch");
                command.add(branchId);
            }
            
            if (inputFile != null && !inputFile.isEmpty()) {
                command.add("--input");
                command.add(inputFile);
            }
            
            if (format != null && !format.isEmpty()) {
                command.add("--format");
                command.add(format);
            }

            if (isVerbose()) {
                debug("Command: " + String.join(" ", command));
            }

            // Step 6: Execute the command
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.inheritIO(); // Inherit stdin/stdout/stderr from parent process
            
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                success("Push completed successfully");
            } else {
                error("Push failed with exit code: " + exitCode);
                System.exit(exitCode);
            }

        } catch (Exception e) {
            error("Push failed: " + e.getMessage());
            if (isVerbose()) {
                e.printStackTrace();
            }
            System.exit(1);
        }
    }
}
