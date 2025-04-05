# Ignition Tag CICD Module

## Overview

The Tag CICD Module is an extension for [Ignition](https://inductiveautomation.com/) that enables sophisticated version control and deployment workflows for tag configurations. It provides a bridge between traditional software development practices and industrial automation, allowing you to track, manage, and deploy tag configurations across environments using modern DevOps approaches.

This module lets you:

- **Export** tag configurations to files that can be stored in version control systems
- **Import** tag configurations from files into Ignition gateways
- **Track changes** to tags with proper version history
- **Automate deployments** of tag configurations across environments
- **Implement CI/CD practices** for consistent and reliable tag management

## Key Features

- **Multiple Export Formats**: Export as individual files, structured directories, or a single file
- **Deterministic Output**: Creates consistently ordered JSON for meaningful version control diffs
- **UDT Dependency Resolution**: Automatically handles UDT dependencies during import
- **Configurable Collision Handling**: Multiple strategies for handling tag conflicts
- **Complete REST API**: Full API for integration with CI/CD pipelines
- **Designer Integration**: Seamless UI integration with Ignition Designer
- **Automation Support**: Configurable for automated operations like startup imports

## System Requirements

- **Ignition Version**: 8.1.24 or higher
- **Operating System**: Any OS supported by Ignition (Windows, Linux, macOS)
- **Java Version**: 17 or higher

## Installation

1. Download the latest `Tag-CICD.modl` file from the [Releases page](https://github.com/design-group/ignition-tag-cicd-module/releases)
2. In your Ignition Gateway, go to Config > Modules
3. Click "Install or Upgrade a Module..."
4. Choose the downloaded .modl file and follow the installation wizard
5. Restart the Designer to enable the module's features

## Getting Started

### Basic Configuration Setup

1. In the Ignition Designer, go to Tools > Tag Export Manager
2. Click "Add" to create a new configuration
3. Select the tag provider and base tag path
4. Choose the export mode and collision policy
5. Specify the export path
6. Click "Add" to save the configuration

### Export Tags

1. Open the Tag Export Manager from Tools menu
2. Select the configuration you want to use
3. Click "Export Selected"

### Import Tags

1. Open the Tag Export Manager from Tools menu
2. Select the configuration you want to use
3. Click "Import Selected"
4. Confirm the import operation

## Export Modes

The module supports three export modes:

- **Single File Mode**: All tags in one JSON file, ideal for smaller tag structures
- **Individual Files Mode**: Each tag/folder/UDT as an individual file, providing granular version control
- **Structured Files Mode**: Folder structure with tags.json and udts.json files in each folder

## REST API Usage

The module provides a comprehensive REST API for integration with CI/CD pipelines:

```bash
# Export tags example
curl -X POST "https://gateway-url/data/tag-cicd/tags/export" \
  -d "provider=default&recursive=true&baseTagPath=MyFolder&filePath=data/tags/example&exportMode=structuredByType"
  
# Import tags example
curl -X POST "https://gateway-url/data/tag-cicd/tags/import" \
  -d "provider=default&baseTagPath=MyFolder&recursive=true&sourcePath=data/tags/example&collisionPolicy=o&exportMode=structuredByType"
```

## Common Use Cases

- **Version control** for tag configurations using Git or other VCS
- Maintaining **consistency across multiple environments** (dev, test, prod)
- **Collaborative development** of tag configurations by multiple engineers
- **Automated deployment** of tag configurations as part of a CI/CD pipeline
- **System backup and disaster recovery**
- **Template-based deployments** across similar systems

## Architecture

The module consists of three main components:

1. **Common**: Shared utilities, interfaces, and core tag operations
2. **Gateway**: REST endpoints, RPC handlers, and gateway startup logic
3. **Designer**: UI components including toolbar buttons and configuration dialogs

## Building from Source

```bash
# Clone the repository
git clone https://github.com/design-group/ignition-tag-cicd-module.git
cd ignition-tag-cicd-module

# Build the module
./gradlew build

# Deploy to a local gateway (configured in gradle.properties)
./gradlew deployModl
```

## Configuration File

The module stores its configuration in a JSON file on the Ignition gateway at:

```
data/modules/tag-cicd/export-config.json
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the [MIT License](LICENSE).

## Documentation

For full documentation, visit the [Tag CICD Module Documentation](https://design-group.github.io/ignition-tag-cicd-module/).