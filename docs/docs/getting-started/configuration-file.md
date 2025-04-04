---
id: configuration-file
title: Configuration File
sidebar_label: Configuration File
---

# Configuration File Structure

The Tag CICD Module stores its configuration in a JSON file on the Ignition gateway. This file contains all your export/import configurations and is used for both manual operations and automated tasks like startup imports.

## File Location

The configuration file is stored at the following location on your Ignition gateway:

```
data/modules/tag-cicd/export-config.json`
```

This path is relative to your Ignition installation directory. For example, on a typical Windows installation, the full path might be:

```
C:\Program Files\Inductive Automation\Ignition\data\modules\tag-cicd\export-config.json
```

## File Format

The configuration file is a JSON array where each item represents a tag export/import configuration:

```json
[
  {
    "provider": "default",
    "baseTagPath": "MyTags",
    "sourcePath": "data/tags/mytags",
    "exportMode": "individualFiles",
    "collisionPolicy": "o",
    "excludeUdtDefinitions": false
  },
  {
    "provider": "Device",
    "baseTagPath": "",
    "sourcePath": "data/tags/device-tags.json",
    "exportMode": "singleFile",
    "collisionPolicy": "d"
  }
]
```

## Configuration Properties

Each configuration object can contain the following properties:

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `provider` | String | Yes | The name of the tag provider (e.g., "default") |
| `baseTagPath` | String | Yes | The tag path to export from or import to (empty string for provider root, or `_types_` for UDT Definitions) |
| `sourcePath` | String | Yes | The gateway file system path for export/import operations |
| `exportMode` | String | Yes | The export/import format: `singleFile`, `individualFiles`, or `structuredByType` |
| `collisionPolicy` | String | Yes | How to handle tag conflicts: `a` (abort), `m` (merge), `o` (overwrite), or `d` (delete and replace) |
| `excludeUdtDefinitions` | Boolean | No | Whether to exclude UDT definitions when exporting (defaults to `false`) |

### Provider

The name of the tag provider to export from or import to. This corresponds to the provider names visible in the Ignition Designer tag browser, such as `default` in a fresh Ignition installation.

### Base Tag Path

The starting point for export/import operations within the specified provider:

- Empty string (`""`) - Export/import from the provider root
- `"MyFolder"` - Export/import from a top-level folder named "MyFolder"
- `"MyFolder/SubFolder"` - Export/import from a nested folder
- `"_types_"` - Export/import UDT definitions (UDT instances are not included)
- `"_types_/MyFolder"` - Export/import UDT definitions from a specific folder

### Source Path

The gateway file system path where tags will be exported to or imported from:

- For file paths, include the file extension (e.g., `data/tags/mytags.json`)
- For directory paths, no trailing slash is needed (e.g., `data/tags/mytags`)

The path is relative to the Ignition installation directory, but absolute paths are also supported.

### Export Mode

The format used for export/import operations:

- `singleFile` - All tags in a single JSON file
- `individualFiles` - Separate JSON file for each tag and folder, maintaining the hierarchy
- `structuredByType` - Folder structure with `tags.json` and `udts.json` files in each folder

### Collision Policy

How tag conflicts are handled during import:

- `a` (abort) - Abort the import if any tags already exist at the target path
- `m` (merge) - Update properties of existing tags, preserving other properties
- `o` (overwrite) - Replace existing tags completely with imported ones
- `d` (delete and replace) - Delete all existing tags at the target path before importing

### Exclude UDT Definitions

Whether to exclude UDT definitions from export operations:

- `true` - Do not export UDT definitions (the `_types_` folder)
- `false` or omitted - Include UDT definitions in exports

## JSON Schema

The module includes a JSON schema that defines the structure of the configuration file. This schema can be used for validation and editor auto-completion. The schema is available at:

```
docs/tag-cicd-config-schema.json
```

Here's the schema specification:

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "Tag CICD Configuration",
  "description": "Configuration for tag export/import with CI/CD support",
  "type": "array",
  "items": {
    "type": "object",
    "required": [
      "provider",
      "baseTagPath",
      "sourcePath",
      "collisionPolicy"
    ],
    "properties": {
      "provider": {
        "type": "string",
        "description": "The Ignition tag provider name"
      },
      "baseTagPath": {
        "type": "string",
        "description": "The base tag path to export from or import to"
      },
      "sourcePath": {
        "type": "string",
        "description": "The file or directory path to save tags to or import tags from"
      },
      "collisionPolicy": {
        "type": "string",
        "enum": [
          "a",
          "m",
          "o",
          "d"
        ],
        "description": "The collision policy to use for imports (a=abort, m=merge, o=overwrite, d=delete-and-replace)"
      },
      "exportMode": {
        "type": "string",
        "enum": [
          "singleFile",
          "individualFiles",
          "structuredByType"
        ],
        "description": "The export mode to use for exporting and importing tags",
        "default": "individual"
      },
      "excludeUdtDefinitions": {
        "type": "boolean",
        "description": "Whether to exclude UDT definitions (_types_ folder) when exporting",
        "default": false
      }
    }
  }
}
```

## Managing the Configuration File

While the configuration file is typically managed through the Designer interface, you can also edit it directly for advanced use cases:

1. **Manual Editing**: Edit the JSON file directly using a text editor
2. **Version Control**: Store the configuration file in your version control system
3. **Deployment Automation**: Deploy the same configuration to multiple gateways
4. **Backup**: Back up your configurations alongside your tag exports

**Note**: After manually editing the configuration file, you may need to restart the gateway, reload the module, or run the import function in the Designer for changes to take effect.

## Export/Import Configuration Example

Here's a comprehensive example showing different configuration scenarios:

```json
[
  {
    "provider": "default",
    "baseTagPath": "",
    "sourcePath": "data/tags/all_tags",
    "exportMode": "individualFiles",
    "collisionPolicy": "o"
  },
  {
    "provider": "default",
    "baseTagPath": "ProcessTags",
    "sourcePath": "data/tags/process_tags.json",
    "exportMode": "singleFile",
    "collisionPolicy": "m"
  },
  {
    "provider": "default",
    "baseTagPath": "UDTInstances",
    "sourcePath": "data/tags/udt_instances",
    "exportMode": "structuredByType",
    "collisionPolicy": "d"
  },
  {
    "provider": "default",
    "baseTagPath": "_types_",
    "sourcePath": "data/tags/udt_definitions.json",
    "exportMode": "singleFile",
    "collisionPolicy": "o"
  }
]
```

This example configuration includes:

1. Export/import of all tags using individual files
2. Export/import of just the ProcessTags folder as a single file
3. Export/import of UDT instances using the structured format
4. Export/import of UDT definitions as a single file

## Next Steps

Now that you understand the configuration file structure, you can:

1. Explore the [Designer Features](../designer/ui-components) to create and manage configurations through the UI
2. Learn about [Export Modes](../tag-export/export-modes) to choose the best format for your needs
