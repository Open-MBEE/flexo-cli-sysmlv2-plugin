# Flexo SysML v2 Plugin

A plugin for Flexo CLI that provides commands for interacting with SysML v2 API services.

## Architecture

This plugin provides a SysML v2 API interface that translates between the SysML v2 API standard and the Flexo MMS backend:

```
Client → SysML v2 API (port 9000) → Flexo MMS Layer 1 (port 8080) → Triple Store
```

**Key Points:**
- The SysML v2 API service is a **translator/adapter** between SysML v2 and Flexo MMS
- All project data is stored in the Flexo MMS backend
- SysML v2 API uses UUID-based project IDs (per SysML v2 spec)
- Flexo MMS uses string-based repo IDs (flexible naming)

## Requirements

1. **Flexo MMS Backend** - The SysML v2 service requires a running Flexo MMS Layer 1 service
2. **Docker** - Required for running the SysML v2 API service locally
3. **Java 17+** - For building the plugin

## Features

This plugin provides comprehensive access to SysML v2 API operations:

- **Remote Management** - Manage multiple SysML v2 servers (similar to git remotes)
- **Authentication Mapping** - Link each SysML v2 remote to a Flexo backend remote for environment-specific authentication
- **Local Deployment** - Automatically deploy a local SysML v2 API service via Docker
- **Project Cloning** - Clone complete projects from remote to local with automatic mapping
- **Pull & Push** - Sync models between local and remote using mapped project/branch IDs
- **Project Management** - Create, list, update, and delete projects
- **Element Operations** - Query and retrieve elements, get root elements
- **Branch Management** - List project branches (automatic "Initial" branch creation)
- **Commit Operations** - View commit history
- **Query Execution** - Execute and manage queries
- **Tag Management** - List and manage tags
- **Relationship Navigation** - Query element relationships
- **Project & Branch Mapping** - Map local project/branch IDs to remote IDs for seamless workflows


## Installation

1. Build the plugin:
```bash
cd flexo-cli-sysmlv2-plugin
./gradlew jar
```

2. Copy the JAR to the Flexo plugins directory:
```bash
mkdir -p ~/.flexo/plugins
cp build/libs/flexo-cli-sysmlv2-plugin-1.0.0.jar ~/.flexo/plugins/
```

3. Verify installation:
```bash
flexo --help  # Should show 'sysml' command
```

## Quick Start

### Prerequisites

Ensure you have a Flexo MMS backend running:

```bash
# Initialize Flexo MMS (if not already running)
flexo init --org sysmlv2 --repo default
```

This creates:
- Flexo MMS Layer 1 service on port 8080
- Triple store (Fuseki) backend
- Organization "sysmlv2" for SysML v2 projects

### Initialize Local SysML v2 Service

Deploy a local SysML v2 API service that connects to your Flexo MMS backend:

```bash
# Start local SysML v2 API service (requires Docker)
flexo sysml init
```

This will:
1. Start a Docker container running SysML v2 API service on port 9000
2. Configure it to connect to Flexo MMS backend at localhost:8080
3. Create a remote named 'origin' with the service URL
4. Set 'origin' as the default remote
5. Verify the service is healthy and ready

You can now start using SysML v2 commands immediately.

### Working with Multiple Remotes

Similar to Git, you can configure multiple SysML v2 servers and switch between them:

```bash
# Add remotes for different environments
flexo sysml remote add local http://localhost:9000 --set-default
flexo sysml remote add staging https://sysml-staging.example.com
flexo sysml remote add production https://sysml.example.com

# List all remotes (* indicates default)
flexo sysml remote list

# Use a specific remote for a command
flexo --remote production project list

# Switch default remote
flexo sysml remote add staging https://sysml-staging.example.com --set-default

# Show remote details
flexo sysml remote show production
```

### Using an Existing Service

If you already have a SysML v2 API service running, you can add it as a remote:

```bash
# Add existing service as a remote
flexo sysml remote add myserver http://your-sysml-service:9000 --set-default

# Or use init with --skip-docker to create default 'origin' remote
flexo sysml init --skip-docker --url http://your-sysml-service:9000
```

## Usage

### Init Command

```bash
# Initialize local SysML v2 service (recommended)
flexo sysml init

# Create with custom remote name
flexo sysml init --remote-name local

# Skip Docker startup if service is already running
flexo sysml init --skip-docker --url http://existing-service:9000

# Use custom service URL
flexo sysml init --url http://custom-host:9000

# Get help
flexo sysml init --help
```

### Clone Command

Clone a complete SysML v2 project from a remote server to your local instance. This automatically creates project and branch mappings.

```bash
# Clone a project from the default remote
flexo sysml clone <remote-project-id>

# Clone from a specific remote
flexo --remote production clone proj-abc-123

# Clone with a custom local project name
flexo sysml clone proj-abc-123 --name "My Local Project"

# Clone with custom description
flexo sysml clone proj-abc-123 --name "Local Copy" --description "Development copy"

# Clone to a specific local SysML v2 API URL
flexo sysml clone proj-abc-123 --to-local http://localhost:8080

# Clone only a specific branch
flexo sysml clone proj-abc-123 --branch main

# Preview what would be cloned without making changes
flexo sysml clone proj-abc-123 --dry-run

# Get help
flexo sysml clone --help
```

**What the clone command does:**

1. Fetches project metadata from the remote server
2. Creates a new local project with the same name (or custom name)
3. Fetches all branches (or specific branch with `--branch`)
4. Clones all elements from each branch to the local project
5. Automatically creates project mapping (local project ID ↔ remote project ID)
6. Automatically creates branch mappings (local branch IDs ↔ remote branch IDs)

**Example workflow:**

```bash
# Step 1: Initialize local service
flexo sysml init

# Step 2: Add production remote
flexo sysml remote add production https://sysml.example.com

# Step 3: Clone a project from production to local
flexo --remote production clone proj-remote-abc123 --name "Dev Copy"

# The command output will show:
# - Local project ID (e.g., proj-local-xyz789)
# - Branches cloned
# - Elements copied
# - Mappings created

# Step 4: Work with the local copy
flexo sysml project list
flexo sysml element list --project proj-local-xyz789

# Step 5: Query using mapped IDs
flexo --remote production --map-from project get --project proj-remote-abc123
# This automatically translates to the local project ID
```

### Pull and Push Commands

After cloning a project, you can sync changes between local and remote using pull and push commands. These commands leverage the Flexo CLI underneath, using your project and branch mappings to translate IDs automatically.

**Pull Command - Fetch model from remote:**

```bash
# Pull from a mapped project (uses default branch)
flexo sysml pull <local-project-id>

# Pull from specific branch
flexo sysml pull <local-project-id> --branch <local-branch-id>

# Pull and save to file
flexo sysml pull proj-local-xyz789 --output model.ttl

# Pull with specific RDF format
flexo sysml pull proj-local-xyz789 --format jsonld --output model.jsonld

# Get help
flexo sysml pull --help
```

**Push Command - Commit model changes to remote:**

```bash
# Push from file to mapped project
flexo sysml push <local-project-id> --message "Update model" --input model.ttl

# Push from stdin
cat model.ttl | flexo sysml push proj-local-xyz789 --message "Update from script"

# Push to specific branch
flexo sysml push proj-local-xyz789 --branch <local-branch-id> --message "Branch update" --input model.ttl

# Push with specific RDF format
flexo sysml push proj-local-xyz789 --message "Update" --input model.jsonld --format jsonld

# Get help
flexo sysml push --help
```

**How Pull/Push Work:**

1. **ID Translation**: Commands look up your project/branch mappings automatically
2. **Flexo CLI Delegation**: Under the hood, they call `flexo pull` and `flexo push` with the remote IDs
3. **Seamless Integration**: You work with local IDs, the plugin handles the remote translation

**Example Workflow:**

```bash
# 1. Clone a project (creates mappings)
flexo --remote production clone proj-remote-abc123 --name "Dev Copy"
# Output: Created local project proj-local-xyz789

# 2. Pull latest changes from remote
flexo sysml pull proj-local-xyz789 --output current-model.ttl
# Translates to: flexo pull --org proj-remote-abc123 --repo default --remote production

# 3. Make changes locally (edit current-model.ttl)

# 4. Push changes back to remote
flexo sysml push proj-local-xyz789 --message "Added new components" --input current-model.ttl
# Translates to: flexo push --org proj-remote-abc123 --repo default --remote production

# 5. Pull again to verify
flexo sysml pull proj-local-xyz789 --output verified-model.ttl
```

**Requirements:**

- Project must have a mapping (created via `clone` or `map add`)
- Branch mapping is optional (commands will use mapped branch if available)
- Flexo CLI must be installed and accessible in PATH
- Remote must be configured in SysML config

### Remote Management Commands

Similar to Git remotes, manage multiple SysML v2 servers:

```bash
# List all remotes (* indicates default)
flexo sysml remote list
flexo sysml remote ls        # Alias

# Add a new remote
flexo sysml remote add <name> <url>
flexo sysml remote add staging https://sysml-staging.example.com
flexo sysml remote add production https://sysml.example.com --set-default

# Add remote with Flexo backend authentication mapping
flexo sysml remote add production https://sysml.example.com \
    --flexo-remote flexo-prod \
    --set-default

# Show remote details (including Flexo backend mapping)
flexo sysml remote show <name>
flexo sysml remote show production
# Output:
# Remote: production (default)
#   URL: https://sysml.example.com
#   Flexo Backend: flexo-prod

# Change remote URL
flexo sysml remote set-url <name> <new-url>
flexo sysml remote set-url staging https://new-staging.example.com

# Set or update Flexo backend remote for authentication
flexo sysml remote set-flexo-remote <name> <flexo-remote-name>
flexo sysml remote set-flexo-remote production flexo-prod

# Remove Flexo backend mapping
flexo sysml remote set-flexo-remote production none

# Rename a remote
flexo sysml remote rename <old-name> <new-name>
flexo sysml remote rename staging dev

# Remove a remote
flexo sysml remote remove <name>
flexo sysml remote rm <name>          # Alias

# Get help
flexo sysml remote --help
```

### Flexo Backend Authentication Mapping

Each SysML v2 remote can be linked to a specific Flexo backend remote for authentication. This enables multi-environment workflows where each environment uses different credentials:

```bash
# 1. First, configure your Flexo backend remotes
flexo remote add flexo-local http://localhost:8080 --local-mode=true
flexo remote add flexo-prod https://flexo-backend.example.com \
    --local-mode=false \
    --auth-enabled=true \
    --ssh-key-path=~/.ssh/prod_rsa

# 2. Link SysML v2 remotes to Flexo backend remotes
flexo sysml remote add origin http://localhost:9000 --flexo-remote flexo-local
flexo sysml remote add production https://sysml-api.example.com --flexo-remote flexo-prod

# 3. Commands now use the correct authentication automatically
flexo sysml project list                    # Uses flexo-local auth
flexo --remote production project list      # Uses flexo-prod auth

# 4. Verify with verbose output
flexo sysml -v project list
# Shows: "Using Flexo backend remote 'flexo-local' for authentication"
```

**Configuration in `~/.flexo/config`:**
```properties
# Flexo backend remotes
remote.flexo-local.url=http://localhost:8080
remote.flexo-local.localMode=true

remote.flexo-prod.url=https://flexo-backend.example.com
remote.flexo-prod.localMode=false
remote.flexo-prod.authEnabled=true
remote.flexo-prod.sshKeyPath=~/.ssh/prod_rsa

# SysML v2 remotes with Flexo backend mappings
sysmlv2.remote.origin.url=http://localhost:9000
sysmlv2.remote.origin.flexoRemote=flexo-local

sysmlv2.remote.production.url=https://sysml-api.example.com
sysmlv2.remote.production.flexoRemote=flexo-prod
```

**Benefits:**
- Different authentication per environment (local dev vs staging vs production)
- Automatic credential selection based on which SysML v2 remote you're using
- No need to manually switch authentication settings
- Supports both local development mode and SSH key authentication



### Using Remotes with Commands

All SysML commands automatically use the default remote, or you can specify a different one:

```bash
# Use default remote
flexo sysml project list

# Use specific remote
flexo --remote production project list
flexo --remote staging project create --name "Test"

# The --remote flag works with any command
flexo --remote local element list --project PROJECT_ID --commit COMMIT_ID
```

### Project Mapping Commands

Due to the SysML v2 API requiring random project IDs, use mappings to transparently redirect queries:

```bash
# List all project mappings
flexo sysml map list
flexo sysml map ls           # Alias

# Create a mapping (maps local project to remote project)
flexo sysml map add <local-project-id> <remote-name> <remote-project-id>
flexo sysml map add my-local-proj staging proj-abc123

# Show mapping details by local project ID
flexo sysml map show <local-project-id>
flexo sysml map show my-local-proj

# Look up local project ID from remote project ID (reverse lookup)
flexo sysml map lookup <remote-project-id>
flexo sysml map lookup proj-remote-xyz789

# Look up with specific remote
flexo sysml map lookup proj-remote-xyz789 --remote-name production

# Remove a mapping
flexo sysml map remove <local-project-id>
flexo sysml map rm my-local-proj     # Alias

# Get help
flexo sysml map --help
```

**Lookup Command Details:**

The `lookup` command is useful when you only have a remote project ID and need to find the corresponding local project ID:

```bash
# Find local project ID from remote project ID
$ flexo sysml map lookup proj-remote-abc123

Found mapping:
  Remote Project ID: proj-remote-abc123
  Remote Name:       production
  Local Project ID:  proj-local-xyz789

You can use this local project ID with commands like:
  flexo sysml project get --project proj-local-xyz789
  flexo sysml element list --project proj-local-xyz789
```

If no mapping exists, the command provides helpful suggestions:

```bash
$ flexo sysml map lookup proj-unknown-123

No mapping found for remote project: proj-unknown-123
Remote: production

Available options:
  1. Clone the project: flexo --remote production clone proj-unknown-123
  2. Create mapping: flexo sysml map add <local-project-id> production proj-unknown-123
  3. List mappings: flexo sysml map list
```

### Branch Mapping Commands

Branches also get random GUIDs. Map branch IDs under their project mappings:

```bash
# List branch mappings for a project
flexo sysml map list-branches <local-project-id>
flexo sysml map ls-branches my-local-proj

# Add a branch mapping
flexo sysml map add-branch <local-project-id> <local-branch-id> <remote-branch-id>
flexo sysml map add-branch my-local-proj local-master-guid remote-master-guid

# Remove a branch mapping
flexo sysml map remove-branch <local-project-id> <local-branch-id>
flexo sysml map rm-branch my-local-proj local-master-guid
```

### Using Mappings with Commands

Once configured, mappings allow you to query local projects using remote project IDs:

```bash
# Query with remote project ID - transparently maps to local
flexo sysml --map-from project get --project remote-proj-xyz789
# Automatically queries local project "my-local-proj"

# Works with all project-based commands
flexo sysml --map-from element list --project remote-proj-xyz789 --commit HEAD
flexo sysml --map-from branch list --project remote-proj-xyz789

# View branches with their IDs (for creating branch mappings)
flexo sysml branch list --project my-local-proj
# Output shows: master (ID: local-master-guid-123)

# Without --map-from, uses the ID as-is
flexo sysml project get --project my-local-proj
```

### Project Commands

```bash
# List all projects
flexo sysml project list

# Get project details
flexo sysml project get --project PROJECT_ID

# Create a new project
flexo sysml project create --name "My Project" --description "Test project"

# Update a project
flexo sysml project update --project PROJECT_ID --name "New Name"

# Delete a project
flexo sysml project delete --project PROJECT_ID --confirm
```

### Element Commands

```bash
# List elements in a project/commit
flexo sysml element list --project PROJECT_ID --commit COMMIT_ID

# Get element by ID
flexo sysml element get --project PROJECT_ID --commit COMMIT_ID ELEMENT_ID

# Get root elements
flexo sysml element roots --project PROJECT_ID --commit COMMIT_ID
```

### Branch Commands

The SysML v2 API automatically creates an "Initial" branch when a project is created. Branch operations are primarily read-only through the API.

```bash
# List branches in a project
flexo sysml branch list --project PROJECT_ID

# List branches on a specific remote
flexo --remote production branch list --project PROJECT_ID

# List branches with verbose output (shows authentication details)
flexo sysml -v branch list --project PROJECT_ID

# Example output:
# Branches:
#   Initial (ID: 88299563-581f-45e0-978a-99b5a70b5d2b)
```

**Note**: Branch creation via the SysML v2 API is not currently supported in the standard. Branches are managed through the underlying Flexo MMS backend and commit operations. When you create a project, an "Initial" branch is automatically created.

**Working with Branch IDs**: Branch IDs are UUIDs and are needed for:
- Pull/push operations with `--branch` flag
- Creating branch mappings for synchronized workflows
- Querying commits on specific branches

**Example**: Get branch ID for mapping:
```bash
# List branches to get their IDs
flexo sysml branch list --project proj-local-abc123
# Output: Initial (ID: 88299563-581f-45e0-978a-99b5a70b5d2b)

# Use this ID in branch mappings
flexo sysml map add-branch proj-local-abc123 \
    88299563-581f-45e0-978a-99b5a70b5d2b \
    remote-branch-guid-xyz

# Use in pull/push operations
flexo sysml pull proj-local-abc123 \
    --branch 88299563-581f-45e0-978a-99b5a70b5d2b \
    --output model.ttl
```

### Commit Commands

```bash
# List commits in a project
flexo sysml commit list --project PROJECT_ID --branch BRANCH_NAME
```

### Query Commands

```bash
# List queries in a project
flexo sysml query list --project PROJECT_ID
```

### Tag Commands

```bash
# List tags in a project
flexo sysml tag list --project PROJECT_ID
```

### Relationship Commands

```bash
# List relationships for an element
flexo sysml relationship list --project PROJECT_ID --commit COMMIT_ID ELEMENT_ID
```

## Global Options

All commands support standard Flexo CLI options plus plugin-specific options:

```bash
# Use a specific remote
flexo --remote production project list

# Use project ID mapping (maps remote IDs to local IDs)
flexo sysml --map-from project get --project remote-proj-id-123

# Combine remote and mapping
flexo --remote staging --map-from element list --project remote-proj-id

# Verbose output
flexo -v sysml project list

# Combine multiple options
flexo -v sysml --remote staging --map-from project create --name "Test"

# Custom configuration
flexo --config ~/.flexo/custom-config sysml element list

# Specify org and repo (for parent CLI integration)
flexo --org myorg --repo myrepo sysml project list
```

## Configuration

The plugin uses a remote-based configuration system in `~/.flexo/config`:

```properties
# SysML v2 remotes (similar to git remotes)
sysmlv2.remote.origin.url=http://localhost:9000
sysmlv2.remote.staging.url=https://sysml-staging.example.com
sysmlv2.remote.production.url=https://sysml.example.com

# Default remote (used when --remote not specified)
sysmlv2.default.remote=origin

# Project mappings (local project -> remote project)
sysmlv2.mapping.proj-local-abc.remote=staging
sysmlv2.mapping.proj-local-abc.remoteProjectId=proj-remote-xyz

# Branch mappings (scoped under project mappings)
sysmlv2.mapping.proj-local-abc.branch.local-master-guid.remoteBranchId=remote-master-guid
sysmlv2.mapping.proj-local-abc.branch.local-dev-guid.remoteBranchId=remote-dev-guid

# Another project mapping
sysmlv2.mapping.my-model.remote=production
sysmlv2.mapping.my-model.remoteProjectId=prod-model-123
sysmlv2.mapping.my-model.branch.local-main-guid.remoteBranchId=remote-main-guid

# Standard Flexo CLI configuration
mms.url=http://localhost:8080
```

### Configuration Management

Remotes and mappings are managed via commands (recommended):

```bash
# Automated setup (creates 'origin' remote)
flexo sysml init

# Manual remote management
flexo sysml remote add <name> <url>
flexo sysml remote remove <name>
flexo sysml remote set-url <name> <url>

# Project mapping management
flexo sysml map add <local-project-id> <remote-name> <remote-project-id>
flexo sysml map remove <local-project-id>
flexo sysml map list

# Branch mapping management
flexo sysml map add-branch <local-project-id> <local-branch-id> <remote-branch-id>
flexo sysml map remove-branch <local-project-id> <local-branch-id>
flexo sysml map list-branches <local-project-id>
```

### Backward Compatibility

For backward compatibility, the plugin also supports the legacy `sysmlv2.url` property, but remotes are the recommended approach.

## API Mapping

The plugin maps to the following SysML v2 API endpoints:

| Command | HTTP Method | Endpoint | Notes |
|---------|-------------|----------|-------|
| `init` | - | Starts local Docker service & creates remote | - |
| `remote list/add/remove/...` | - | Manages remote configurations | Config only |
| `remote set-flexo-remote` | - | Maps SysML v2 remote to Flexo backend remote | Config only |
| `map list/add/remove/show` | - | Manages project mappings | Config only |
| `map list-branches/add-branch/...` | - | Manages branch mappings | Config only |
| `project list` | GET | `/projects` | May fail if backend has non-UUID repos |
| `project get` | GET | `/projects/{id}` | - |
| `project create` | POST | `/projects` | Creates with UUID ID |
| `project update` | PUT | `/projects/{id}` | - |
| `project delete` | DELETE | `/projects/{id}` | - |
| `element list` | GET | `/projects/{id}/commits/{commit}/elements` | - |
| `element get` | GET | `/projects/{id}/commits/{commit}/elements/{elementId}` | - |
| `element roots` | GET | `/projects/{id}/commits/{commit}/elements/roots` | - |
| `branch list` | GET | `/projects/{id}/branches` | Shows "Initial" branch auto-created |
| `commit list` | GET | `/projects/{id}/commits` | Optional `?branch=` parameter |
| `query list` | GET | `/projects/{id}/queries` | - |
| `tag list` | GET | `/projects/{id}/tags` | - |
| `relationship list` | GET | `/projects/{id}/commits/{commit}/elements/{elementId}/relationships` | - |
| `clone` | Multiple | Multiple API calls | Clones project + branches + elements |
| `pull` | - | Delegates to `flexo pull` | Uses project/branch mappings |
| `push` | - | Delegates to `flexo push` | Uses project/branch mappings |


## Example Workflows

### Local Development Workflow

```bash
# 1. Initialize local SysML v2 service
flexo sysml init

# 2. Create a project
flexo sysml project create --name "My Model" --description "System model"

# 3. Work with the project
flexo sysml project list
flexo sysml branch list --project <project-id>
flexo sysml element list --project <project-id> --commit <commit-id>
```

### Multi-Environment Workflow

```bash
# 1. Set up remotes for different environments
flexo sysml init --remote-name local                    # Local development
flexo sysml remote add dev https://dev.example.com
flexo sysml remote add staging https://staging.example.com
flexo sysml remote add prod https://prod.example.com

# 2. Develop locally (uses 'local' as default)
flexo sysml project create --name "New Feature"

# 3. Test in staging
flexo --remote staging project list

# 4. Deploy to production
flexo --remote prod project list

# 5. Switch default to staging for extended testing
flexo sysml remote add staging https://staging.example.com --set-default
flexo sysml project list  # Now uses staging by default
```

### Team Collaboration Workflow

```bash
# Team member 1: Set up shared development server
flexo sysml remote add team-dev https://team-dev.example.com --set-default

# Team member 2: Use same configuration
flexo sysml remote add team-dev https://team-dev.example.com --set-default

# Both can work on same server
flexo sysml project list
flexo sysml project create --name "Collaborative Model"

# Individual testing with local instance
flexo --remote local project list
```

### Project Synchronization Workflow

Handle the SysML v2 API's random project and branch ID limitation using transparent mappings:

```bash
# 1. Set up local and remote environments
flexo sysml init --remote-name local
flexo sysml remote add staging https://staging.example.com

# 2. Create project locally
flexo sysml project create --name "Aircraft Model"
# Note the generated project ID, e.g., "proj-local-abc123"

# 3. On the remote (staging), a project already exists
# Remote project ID: "proj-remote-xyz789"

# 4. Create project mapping
flexo sysml map add proj-local-abc123 staging proj-remote-xyz789

# 5. Get branch IDs from both local and remote
flexo sysml branch list --project proj-local-abc123
# Output: master (ID: local-master-guid-aaa)

flexo --remote staging branch list --project proj-remote-xyz789
# Output: master (ID: remote-master-guid-bbb)

# 6. Create branch mapping
flexo sysml map add-branch proj-local-abc123 local-master-guid-aaa remote-master-guid-bbb

# 7. View all mappings
flexo sysml map show proj-local-abc123
flexo sysml map list-branches proj-local-abc123

# 8. Query local project using remote IDs
# The mapping transparently redirects to the local project
flexo sysml --map-from project get --project proj-remote-xyz789
# Automatically queries proj-local-abc123 locally

# 9. Works with all project-based commands
flexo sysml --map-from element list --project proj-remote-xyz789 --commit HEAD
flexo sysml --map-from branch list --project proj-remote-xyz789
```

**How it works**: When you use `--map-from`, the plugin:
1. Looks up the provided project ID in your project mappings
2. Translates it to the local project ID
3. Looks up branch IDs in your branch mappings (if applicable)
4. Translates those to local branch IDs
5. Queries the local API with the translated IDs

This allows you to use remote project and branch IDs in your queries while working with local projects.

## Development

### Building

```bash
./gradlew jar
```

### Testing

```bash
# Install to plugins directory
mkdir -p ~/.flexo/plugins
cp build/libs/flexo-cli-sysmlv2-plugin-1.0.0.jar ~/.flexo/plugins/

# Test loading
flexo --help

# Test commands
flexo sysml --help
flexo sysml remote list
flexo sysml project --help
```

### Project Structure

```
flexo-cli-sysmlv2-plugin/
├── build.gradle                          # Build configuration
├── src/main/
│   ├── java/org/openmbee/flexo/sysmlv2/plugin/
│   │   ├── SysMLv2Plugin.java           # Main plugin class
│   │   ├── commands/                     # Command implementations
│   │   │   ├── SysMLCommand.java        # Root command (with --remote flag)
│   │   │   ├── SysMLBaseCommand.java    # Base for remote-aware commands
│   │   │   ├── InitCommand.java         # Init/deployment command
│   │   │   ├── RemoteCommand.java       # Remote management commands
│   │   │   ├── MapCommand.java          # Project mapping commands
│   │   │   ├── ProjectCommand.java      # Project operations
│   │   │   ├── ElementCommand.java      # Element operations
│   │   │   ├── BranchCommand.java       # Branch operations
│   │   │   ├── CommitCommand.java       # Commit operations
│   │   │   ├── QueryCommand.java        # Query operations
│   │   │   ├── TagCommand.java          # Tag operations
│   │   │   └── RelationshipCommand.java # Relationship operations
│   │   ├── client/
│   │   │   └── SysMLv2Client.java       # HTTP client wrapper
│   │   ├── model/
│   │   │   ├── SysMLRemote.java         # Remote model
│   │   │   └── ProjectMapping.java      # Project mapping model
│   │   └── config/
│   │       └── SysMLConfigHelper.java   # Config management for remotes & mappings
│   └── resources/
│       ├── sysmlv2-docker-compose.yml   # Docker compose for local deployment
│       └── META-INF/services/
│           └── org.openmbee.flexo.cli.plugin.FlexoPlugin
```

## Requirements

- Java 17 or higher
- Flexo CLI 0.1.0 or higher  
- **Flexo MMS Layer 1 service** running on port 8080 (required backend)
- Docker (required for `flexo sysml init` command)

## Troubleshooting

### "HTTP 500" error on project list

**Issue**: `flexo sysml project list` returns HTTP 500 error

**Cause**: The backend organization contains repositories with non-UUID IDs (e.g., "default", "localrepo"). The SysML v2 API requires all project IDs to be UUIDs per the standard specification.

**Solution**: 
- Use individual project operations (`get`, `update`, `delete`) which work correctly
- Create a dedicated organization for SysML v2: `flexo init --org sysmlv2`
- Projects created via `flexo sysml project create` will have UUID IDs and work correctly

### Service fails to start

**Issue**: SysML v2 service container fails to start or shows health check errors

**Cause**: Missing or incorrect Flexo MMS backend connection

**Solution**:
1. Ensure Flexo MMS is running: `docker ps | grep layer1-service`
2. If not running, initialize: `flexo init --org sysmlv2`
3. Restart SysML v2 service: `docker restart sysmlv2-service`

### "Connection refused" errors

**Issue**: Commands fail with connection errors

**Cause**: Either SysML v2 service or Flexo MMS backend is not running

**Solution**:
1. Check SysML v2 service: `docker ps | grep sysmlv2`
2. Check Flexo MMS backend: `curl http://localhost:8080`
3. Restart services if needed

## License

Same as Flexo CLI

## Support

For issues or questions:
- Check the Flexo CLI documentation
- Review the SysML v2 API specification
- Create an issue on GitHub
