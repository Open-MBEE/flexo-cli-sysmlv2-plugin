package org.openmbee.flexo.sysmlv2.plugin.commands;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openmbee.flexo.sysmlv2.plugin.client.SysMLv2Client;
import org.openmbee.flexo.sysmlv2.plugin.config.SysMLConfigHelper;
import org.openmbee.flexo.sysmlv2.plugin.model.SysMLRemote;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Clone command - Clone a SysML v2 project from a remote
 * 
 * This command:
 * 1. Creates a new local SysML v2 project with the same ID as the remote project
 * 2. Delegates to the parent Flexo CLI pull command to fetch the data
 * 
 * Usage: flexo sysml clone <remote-project-id> [options]
 */
@Command(
    name = "clone",
    description = "Clone a SysML v2 project from remote",
    mixinStandardHelpOptions = true
)
public class CloneCommand extends SysMLBaseCommand {

    @Parameters(index = "0", description = "Remote project ID to clone")
    private String remoteProjectId;

    @Option(names = {"--name", "-n"}, description = "Name for the new local project (default: same as remote project)")
    private String projectName;

    @Option(names = {"--description", "-d"}, description = "Description for the new local project")
    private String projectDescription;

    @Option(names = {"--branch", "-b"}, description = "Clone specific branch only (default: clone all branches)")
    private String specificBranch;

    @Option(names = {"-o", "--output"}, description = "Output file (default: stdout)")
    private String outputFile;

    @Option(names = {"-f", "--format"}, description = "RDF format (turtle, jsonld, rdfxml, ntriples)")
    private String format = "turtle";

    @Override
    public void run() {
        try {
            info("Cloning project from remote...");
            info("  Remote project ID: " + remoteProjectId);
            info("");

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
            info("  Remote: " + remoteName);

            SysMLConfigHelper config = new SysMLConfigHelper();
            String localSysmlRemoteName = config.getDefaultRemote();
            String localSysmlUrl = getSysMLUrl(localSysmlRemoteName);
            String remoteSysmlUrl = getSysMLUrl(remoteName);

            // Step 2: Fetch remote project details to get default branch
            info("");
            info("Step 1: Fetching remote project details...");
            debug("Using remote SysML v2 API at: " + remoteSysmlUrl + " (remote=" + remoteName + ")");
            
            String remoteDefaultBranchId = null;
            String remoteProjectName = null;
            try {
                SysMLv2Client remoteClient = new SysMLv2Client(remoteSysmlUrl, getClientForSysmlRemote(remoteName));
                String remoteProjectResponse = remoteClient.getProject(remoteProjectId);
                ObjectMapper mapper = new ObjectMapper();
                JsonNode remoteProject = mapper.readTree(remoteProjectResponse);
                
                if (remoteProject.has("defaultBranch") && remoteProject.get("defaultBranch").has("@id")) {
                    remoteDefaultBranchId = remoteProject.get("defaultBranch").get("@id").asText();
                    info("  Remote default branch ID: " + remoteDefaultBranchId);
                }
                
                if (remoteProject.has("name")) {
                    remoteProjectName = remoteProject.get("name").asText();
                    info("  Remote project name: " + remoteProjectName);
                }
            } catch (Exception e) {
                warn("Failed to fetch remote project details: " + e.getMessage());
                if (isVerbose()) {
                    e.printStackTrace();
                }
            }

            // Step 3: Create a new local project with the SAME ID as the remote project
            info("");
            info("Step 2: Creating new local project with same ID...");
            debug("Using local SysML v2 API at: " + localSysmlUrl + " (remote=" + localSysmlRemoteName + ")");
            
            SysMLv2Client localClient = new SysMLv2Client(localSysmlUrl, getClientForSysmlRemote(localSysmlRemoteName));
            
            // Use provided name or remote project name or generate a default
            String localProjectName = (projectName != null && !projectName.isEmpty()) 
                ? projectName 
                : (remoteProjectName != null ? remoteProjectName : "clone-of-" + remoteProjectId);
            
            // Create project with the specific ID (same as remote)
            String response = localClient.createProjectWithId(remoteProjectId, localProjectName, projectDescription, remoteDefaultBranchId);
            
            // Parse response to verify creation
            ObjectMapper mapper = new ObjectMapper();
            JsonNode project = mapper.readTree(response);
            String localProjectId = project.has("@id") ? project.get("@id").asText() : null;
            
            if (localProjectId == null || localProjectId.isEmpty()) {
                error("Failed to create local project: No project ID returned");
                System.exit(1);
                return;
            }
            
            if (!localProjectId.equals(remoteProjectId)) {
                warn("Warning: Local project ID (" + localProjectId + ") differs from remote project ID (" + remoteProjectId + ")");
            }
            
            // Extract local default branch ID from the response
            String localDefaultBranchId = null;
            if (project.has("defaultBranch") && project.get("defaultBranch").has("@id")) {
                localDefaultBranchId = project.get("defaultBranch").get("@id").asText();
            }
            
            success("  Created local project: " + localProjectId);
            info("  Project name: " + localProjectName);
            if (localDefaultBranchId != null) {
                info("  Local default branch ID: " + localDefaultBranchId);
            }

            // Step 4: Build flexo pull command to fetch the data from remote MMS
            Path pullOutputPath;
            boolean usedTempFile = false;
            if (outputFile != null && !outputFile.isEmpty()) {
                pullOutputPath = Path.of(outputFile);
            } else {
                pullOutputPath = Files.createTempFile("flexo-clone-", ".ttl");
                usedTempFile = true;
            }

            info("");
            info("Step 3: Pulling project data...");
            
            // Get Flexo organization name from remote config (defaults to "sysmlv2")
            SysMLRemote remote = config.getRemote(remoteName);
            String flexoOrg = (remote != null) ? remote.getOrgOrDefault() : "sysmlv2";
            
            List<String> command = new ArrayList<>();
            command.add("flexo");
            command.add("pull");
            command.add("--org");
            command.add(flexoOrg);  // Get org from remote config (configurable per remote)
            command.add("--repo");
            command.add(remoteProjectId);  // Each project is a repo in the org
            command.add("--remote");
            command.add(remoteName);
            
            // Use specified branch or remote default branch ID
            if (specificBranch != null && !specificBranch.isEmpty()) {
                command.add("--branch");
                command.add(specificBranch);
            } else if (remoteDefaultBranchId != null) {
                command.add("--branch");
                command.add(remoteDefaultBranchId);
                debug("Using remote default branch ID: " + remoteDefaultBranchId);
            }
            
            // Always write to file so Step 4 can push the same data into local MMS
            command.add("--output");
            command.add(pullOutputPath.toString());
            
            if (format != null && !format.isEmpty()) {
                command.add("--format");
                command.add(format);
            }

            if (isVerbose()) {
                debug("Step 3 flexo pull command: " + String.join(" ", command));
            }

            // Execute the pull command
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.inheritIO(); // Inherit stdin/stdout/stderr from parent process
            
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                error("Clone failed with exit code: " + exitCode);
                if (usedTempFile) {
                    try { Files.deleteIfExists(pullOutputPath); } catch (Exception ignore) { }
                }
                System.exit(exitCode);
            }

            // Step 5: Push the same data (from Step 3) into local MMS via sysml push
            info("");
            info("Step 4: Pushing cloned model into local MMS...");
            try {
                List<String> pushCmd = new ArrayList<>();
                pushCmd.add("flexo");
                pushCmd.add("sysml");
                pushCmd.add("push");
                pushCmd.add(localProjectId);
                if (localDefaultBranchId != null && !localDefaultBranchId.isEmpty()) {
                    pushCmd.add("--branch");
                    pushCmd.add(localDefaultBranchId);
                }
                pushCmd.add("--message");
                pushCmd.add("clone from " + remoteName + "/" + remoteProjectId);
                pushCmd.add("--input");
                pushCmd.add(pullOutputPath.toString());
                if (format != null && !format.isEmpty()) {
                    pushCmd.add("--format");
                    pushCmd.add(format);
                }

                if (isVerbose()) {
                    debug("Step 4 sysml push command: " + String.join(" ", pushCmd));
                }

                ProcessBuilder pbPush = new ProcessBuilder(pushCmd);
                pbPush.inheritIO();
                Process pushProcess = pbPush.start();
                int pushExit = pushProcess.waitFor();

                if (pushExit == 0) {
                    success("Step 4: Pushed cloned model into local MMS for project '" + localProjectId
                            + (localDefaultBranchId != null ? "', branch '" + localDefaultBranchId + "'" : "'")
                            + ".");
                } else {
                    warn("Step 4: sysml push into local MMS failed with exit code " + pushExit + ".");
                }

                if (usedTempFile) {
                    try {
                        Files.deleteIfExists(pullOutputPath);
                    } catch (Exception ignore) {
                        // best-effort cleanup
                    }
                }
            } catch (Exception e) {
                warn("Step 4: Failed to push cloned model into local MMS: " + e.getMessage());
                if (isVerbose()) {
                    e.printStackTrace();
                }
                if (usedTempFile) {
                    try { Files.deleteIfExists(pullOutputPath); } catch (Exception ignore) { }
                }
            }

            info("");
            success("Clone complete!");
            info("");
            info("Project ID: " + localProjectId);
            info("Remote: " + remoteName);
            info("");
            info("You can now work with this project using:");
            info("  flexo sysml pull " + remoteProjectId);
            info("  flexo sysml push " + remoteProjectId + " -m \"commit message\"");

        } catch (Exception e) {
            error("Clone failed: " + e.getMessage());
            if (isVerbose()) {
                e.printStackTrace();
            }
            System.exit(1);
        }
    }
}
