package org.openmbee.flexo.sysmlv2.plugin.commands;

import org.openmbee.flexo.sysmlv2.plugin.config.SysMLConfigHelper;
import org.openmbee.flexo.sysmlv2.plugin.model.BranchMapping;
import org.openmbee.flexo.sysmlv2.plugin.model.ProjectMapping;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.ArrayList;
import java.util.List;

/**
 * Push command for SysML v2 - Commit model changes to a remote SysML project/branch
 * 
 * This command translates local project/branch IDs to remote IDs using mappings,
 * then delegates to the underlying Flexo CLI push command.
 * 
 * Usage: flexo sysml push <local-project-id> [options]
 */
@Command(
    name = "push",
    description = "Push model changes to remote SysML v2 project",
    mixinStandardHelpOptions = true
)
public class PushCommand extends SysMLBaseCommand {

    @Parameters(index = "0", description = "Local project ID")
    private String localProjectId;

    @Option(names = {"-b", "--branch"}, description = "Local branch ID (optional)")
    private String localBranchId;

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
            debug("Local project ID: " + localProjectId);
            if (localBranchId != null) {
                debug("Local branch ID: " + localBranchId);
            }

            // Step 1: Load configuration and mappings
            SysMLConfigHelper sysmlConfig = new SysMLConfigHelper();
            
            // Step 2: Look up project mapping
            ProjectMapping projectMapping = sysmlConfig.getProjectMapping(localProjectId);
            if (projectMapping == null) {
                error("No mapping found for local project: " + localProjectId);
                info("");
                info("Available options:");
                info("  1. Clone a remote project: flexo sysml --remote <name> clone <remote-project-id>");
                info("  2. Create mapping: flexo sysml map add " + localProjectId + " <remote-name> <remote-project-id>");
                info("  3. List mappings: flexo sysml map list");
                System.exit(1);
                return;
            }

            String remoteName = projectMapping.getRemoteName();
            String remoteProjectId = projectMapping.getRemoteProjectId();
            
            info("  Project mapping:");
            info("    Local:  " + localProjectId);
            info("    Remote: " + remoteName + "/" + remoteProjectId);

            // Step 3: Look up branch mapping (if branch specified)
            String remoteBranchId = null;
            if (localBranchId != null && !localBranchId.isEmpty()) {
                BranchMapping branchMapping = sysmlConfig.getBranchMapping(localProjectId, localBranchId);
                if (branchMapping == null) {
                    warn("No branch mapping found for: " + localBranchId);
                    warn("Using branch ID as-is (assuming it exists on remote)");
                    remoteBranchId = localBranchId;
                } else {
                    remoteBranchId = branchMapping.getRemoteBranchId();
                    info("  Branch mapping:");
                    info("    Local:  " + localBranchId);
                    info("    Remote: " + remoteBranchId);
                }
            }

            // Step 4: Get remote URL
            String remoteUrl = sysmlConfig.getRemoteUrl(remoteName);
            if (remoteUrl == null) {
                error("Remote '" + remoteName + "' not found in configuration");
                info("Add it with: flexo sysml remote add " + remoteName + " <url>");
                System.exit(1);
                return;
            }

            info("  Remote URL: " + remoteUrl);
            info("");

            // Step 5: Build flexo push command
            info("Executing: flexo push...");
            List<String> command = new ArrayList<>();
            command.add("flexo");
            command.add("push");
            command.add("--org");
            command.add(remoteProjectId);
            command.add("--repo");
            command.add("default");  // SysML v2 uses a default repo concept
            command.add("--remote");
            command.add(remoteName);
            command.add("--message");
            command.add(commitMessage);
            
            if (remoteBranchId != null && !remoteBranchId.isEmpty()) {
                command.add("--branch");
                command.add(remoteBranchId);
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
