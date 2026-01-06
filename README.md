# Flexo SysML v2 Plugin

A plugin for Flexo CLI that provides commands for interacting with SysML v2 API services.

## Features

This plugin provides comprehensive access to SysML v2 API operations:

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

## Usage

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

All commands support standard Flexo CLI options:

```bash
# Verbose output
flexo -v sysml project list

# Custom configuration
flexo --config ~/.flexo/custom-config sysml element list

# Specify org and repo
flexo --org myorg --repo myrepo sysml project list
```

## Configuration

The plugin uses the same configuration as the core Flexo CLI. Ensure your `~/.flexo/config` file has the correct MMS URL:

```
mms.url=http://localhost:8080
```

## API Mapping

The plugin maps to the following SysML v2 API endpoints:

| Command | HTTP Method | Endpoint |
|---------|-------------|----------|
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
│   │   │   ├── SysMLCommand.java        # Root command
│   │   │   ├── ProjectCommand.java      # Project operations
│   │   │   ├── ElementCommand.java      # Element operations
│   │   │   ├── BranchCommand.java       # Branch operations
│   │   │   ├── CommitCommand.java       # Commit operations
│   │   │   ├── QueryCommand.java        # Query operations
│   │   │   ├── TagCommand.java          # Tag operations
│   │   │   └── RelationshipCommand.java # Relationship operations
│   │   └── client/
│   │       └── SysMLv2Client.java       # HTTP client wrapper
│   └── resources/
│       └── META-INF/services/
│           └── org.openmbee.flexo.cli.plugin.FlexoPlugin
```

## Requirements

- Java 17 or higher
- Flexo CLI 0.1.0 or higher
- SysML v2 API service running

## License

Same as Flexo CLI

## Support

For issues or questions:
- Check the Flexo CLI documentation
- Review the SysML v2 API specification
- Create an issue on GitHub
