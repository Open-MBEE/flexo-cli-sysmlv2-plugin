# Flexo SysML v2 Plugin

A plugin for Flexo CLI that provides commands for interacting with SysML v2 API services.

## Features

This plugin provides comprehensive access to SysML v2 API operations:

- **Remote Management** - Manage multiple SysML v2 servers (similar to git remotes)
- **Local Deployment** - Automatically deploy a local SysML v2 API service via Docker
- **Project Management** - Create, list, update, and delete projects
- **Element Operations** - Query and retrieve elements, get root elements
- **Branch Management** - List and manage project branches
- **Commit Operations** - View commit history
- **Query Execution** - Execute and manage queries
- **Tag Management** - List and manage tags
- **Relationship Navigation** - Query element relationships

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

### Initialize Local SysML v2 Service

The easiest way to get started is to use the built-in init command to deploy a local SysML v2 API service:

```bash
# Start local SysML v2 API service (requires Docker)
flexo sysml init
```

This will:
1. Start a Docker container running SysML v2 API service on port 9000
2. Create a remote named 'origin' with the service URL
3. Set 'origin' as the default remote
4. Verify the service is healthy and ready

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
flexo sysml --remote production project list

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

# Show remote details
flexo sysml remote show <name>
flexo sysml remote show production

# Change remote URL
flexo sysml remote set-url <name> <new-url>
flexo sysml remote set-url staging https://new-staging.example.com

# Rename a remote
flexo sysml remote rename <old-name> <new-name>
flexo sysml remote rename staging dev

# Remove a remote
flexo sysml remote remove <name>
flexo sysml remote rm <name>          # Alias

# Get help
flexo sysml remote --help
```

### Using Remotes with Commands

All SysML commands automatically use the default remote, or you can specify a different one:

```bash
# Use default remote
flexo sysml project list

# Use specific remote
flexo sysml --remote production project list
flexo sysml --remote staging project create --name "Test"

# The --remote flag works with any command
flexo sysml --remote local element list --project PROJECT_ID --commit COMMIT_ID
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

```bash
# List branches in a project
flexo sysml branch list --project PROJECT_ID
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
flexo sysml --remote production project list

# Verbose output
flexo -v sysml project list

# Combine remote with other options
flexo -v sysml --remote staging project create --name "Test"

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

# Standard Flexo CLI configuration
mms.url=http://localhost:8080
```

### Configuration Management

Remotes are managed via commands (recommended):

```bash
# Automated setup (creates 'origin' remote)
flexo sysml init

# Manual remote management
flexo sysml remote add <name> <url>
flexo sysml remote remove <name>
flexo sysml remote set-url <name> <url>
```

### Backward Compatibility

For backward compatibility, the plugin also supports the legacy `sysmlv2.url` property, but remotes are the recommended approach.

## API Mapping

The plugin maps to the following SysML v2 API endpoints:

| Command | HTTP Method | Endpoint |
|---------|-------------|----------|
| `init` | - | Starts local Docker service & creates remote |
| `remote list/add/remove/...` | - | Manages remote configurations |
| `project list` | GET | `/projects` |
| `project get` | GET | `/projects/{id}` |
| `project create` | POST | `/projects` |
| `project update` | PUT | `/projects/{id}` |
| `project delete` | DELETE | `/projects/{id}` |
| `element list` | GET | `/projects/{id}/commits/{commit}/elements` |
| `element get` | GET | `/projects/{id}/commits/{commit}/elements/{elementId}` |
| `element roots` | GET | `/projects/{id}/commits/{commit}/elements/roots` |
| `branch list` | GET | `/projects/{id}/branches` |
| `commit list` | GET | `/projects/{id}/commits` |
| `query list` | GET | `/projects/{id}/queries` |
| `tag list` | GET | `/projects/{id}/tags` |
| `relationship list` | GET | `/projects/{id}/commits/{commit}/elements/{elementId}/relationships` |

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
flexo sysml --remote staging project list

# 4. Deploy to production
flexo sysml --remote prod project list

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
flexo sysml --remote local project list
```

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
│   │   │   └── SysMLRemote.java         # Remote model
│   │   └── config/
│   │       └── SysMLConfigHelper.java   # Config management for remotes
│   └── resources/
│       ├── sysmlv2-docker-compose.yml   # Docker compose for local deployment
│       └── META-INF/services/
│           └── org.openmbee.flexo.cli.plugin.FlexoPlugin
```

## Requirements

- Java 17 or higher
- Flexo CLI 0.1.0 or higher
- Docker (required for `flexo sysml init` command)
- SysML v2 API service (can be deployed automatically via `flexo sysml init`)

## License

Same as Flexo CLI

## Support

For issues or questions:
- Check the Flexo CLI documentation
- Review the SysML v2 API specification
- Create an issue on GitHub
