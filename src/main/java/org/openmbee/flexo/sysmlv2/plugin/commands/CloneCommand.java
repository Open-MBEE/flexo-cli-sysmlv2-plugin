package org.openmbee.flexo.sysmlv2.plugin.commands;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openmbee.flexo.sysmlv2.plugin.client.SysMLv2Client;
import org.openmbee.flexo.sysmlv2.plugin.config.SysMLConfigHelper;
import org.openmbee.flexo.sysmlv2.plugin.model.BranchMapping;
import org.openmbee.flexo.sysmlv2.plugin.model.ProjectMapping;
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
 * 1. Creates a new local SysML v2 project
 * 2. Creates a project mapping (remote project ID -> local project ID)
 * 3. Delegates to the parent Flexo CLI pull command to fetch the data
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

            // Step 2: Create a new local project (always on local/origin SysML)
            info("");
            info("Step 1: Creating new local project...");
            debug("Using local SysML v2 API at: " + localSysmlUrl + " (remote=" + localSysmlRemoteName + ")");
            
            SysMLv2Client localClient = new SysMLv2Client(localSysmlUrl, getClientForSysmlRemote(localSysmlRemoteName));
            
            // Use provided name or default to remote project ID
            String localProjectName = (projectName != null && !projectName.isEmpty()) 
                ? projectName 
                : "clone-of-" + remoteProjectId;
            
            String response = localClient.createProject(localProjectName, projectDescription);
            
            // Parse response to get the new project ID and default branch ID
            ObjectMapper mapper = new ObjectMapper();
            JsonNode project = mapper.readTree(response);
            String localProjectId = project.has("@id") ? project.get("@id").asText() : null;
            
            if (localProjectId == null || localProjectId.isEmpty()) {
                error("Failed to create local project: No project ID returned");
                System.exit(1);
                return;
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

            // Fetch remote project's default branch ID (from the remote SysML instance)
            info("");
            info("Step 2: Fetching remote project details...");
            debug("Using remote SysML v2 API at: " + remoteSysmlUrl + " (remote=" + remoteName + ")");
            String remoteDefaultBranchId = null;
            try {
                SysMLv2Client remoteClient = new SysMLv2Client(remoteSysmlUrl, getClientForSysmlRemote(remoteName));
                String remoteProjectResponse = remoteClient.getProject(remoteProjectId);
                JsonNode remoteProject = mapper.readTree(remoteProjectResponse);
                if (remoteProject.has("defaultBranch") && remoteProject.get("defaultBranch").has("@id")) {
                    remoteDefaultBranchId = remoteProject.get("defaultBranch").get("@id").asText();
                    info("  Remote default branch ID: " + remoteDefaultBranchId);
                }
            } catch (Exception e) {
                warn("Failed to fetch remote project details: " + e.getMessage());
                if (isVerbose()) {
                    e.printStackTrace();
                }
            }

            // Step 3: Create project mapping
            info("");
            info("Step 3: Creating project mapping...");
            
            ProjectMapping projectMapping = new ProjectMapping();
            projectMapping.setLocalProjectId(localProjectId);
            projectMapping.setRemoteName(remoteName);
            projectMapping.setRemoteProjectId(remoteProjectId);
            config.setProjectMapping(projectMapping);
            config.save();
            
            success("  Created project mapping: " + localProjectId + " <-> " + remoteName + "/" + remoteProjectId);

            // Step 4: Create branch mapping for default branches (if both exist)
            if (localDefaultBranchId != null && remoteDefaultBranchId != null) {
                info("");
                info("Step 4: Creating default branch mapping...");
                BranchMapping branchMapping = new BranchMapping(localProjectId, localDefaultBranchId, remoteDefaultBranchId);
                config.setBranchMapping(branchMapping);
                config.save();
                success("  Created branch mapping: " + localDefaultBranchId + " <-> " + remoteDefaultBranchId);
            } else {
                info("");
                info("Step 4: Skipping branch mapping (default branch not available on both local and remote)");
                if (localDefaultBranchId == null) {
                    debug("  Local project has no default branch");
                }
                if (remoteDefaultBranchId == null) {
                    debug("  Remote project has no default branch");
                }
            }

            // Step 5: Build flexo pull command to fetch the data from remote MMS (dev)
            // Write to a file so Step 6 can push it into local MMS (one pull, no second fetch).
            Path pullOutputPath;
            boolean usedTempFile = false;
            if (outputFile != null && !outputFile.isEmpty()) {
                pullOutputPath = Path.of(outputFile);
            } else {
                pullOutputPath = Files.createTempFile("flexo-clone-", ".ttl");
                usedTempFile = true;
            }

            info("");
            info("Step 5: Pulling project data...");
            
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
            
            // Always write to file so Step 6 can push the same data into local MMS
            command.add("--output");
            command.add(pullOutputPath.toString());
            
            if (format != null && !format.isEmpty()) {
                command.add("--format");
                command.add(format);
            }

            if (isVerbose()) {
                debug("Step 5 flexo pull command: " + String.join(" ", command));
            }

            // Step 5: Execute the pull command
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

            // Step 6: Push the same data (from Step 5) into local MMS via sysml push so commit list works
            info("");
            info("Step 6: Pushing cloned model into local MMS...");
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
                    debug("Step 6 sysml push(local) command: " + String.join(" ", pushCmd));
                }

                ProcessBuilder pbPush = new ProcessBuilder(pushCmd);
                pbPush.inheritIO();
                Process pushProcess = pbPush.start();
                int pushExit = pushProcess.waitFor();

                if (pushExit == 0) {
                    success("Step 6: Pushed cloned model into local MMS for SysML project '" + localProjectId
                            + (localDefaultBranchId != null ? "', branch '" + localDefaultBranchId + "'" : "'")
                            + ".");
                } else {
                    warn("Step 6: sysml push into local MMS failed with exit code " + pushExit + ".");
                }

                if (usedTempFile) {
                    try {
                        Files.deleteIfExists(pullOutputPath);
                    } catch (Exception ignore) {
                        // best-effort cleanup
                    }
                }
            } catch (Exception e) {
                warn("Step 6: Failed to push cloned model into local MMS: " + e.getMessage());
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
            info("Local project ID: " + localProjectId);
            info("Remote project ID: " + remoteProjectId);
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
