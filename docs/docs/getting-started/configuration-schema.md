---
id: configuration-schema
title: Configuration Schema
sidebar_label: Configuration Schema
---

# Configuration Schema

The `export-config.json` file defines the export and import configurations for the Tag CICD Module. This page provides the JSON schema for the configuration file, which you can use to validate your setup or understand the available options.

## Schema Definition

Below is the JSON schema for `export-config.json`:

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

## Usage

You can use this schema to validate your `export-config.json` file using a JSON schema validator, such as [Ajv](https://ajv.js.org/) or an online tool like [JSON Schema Validator](https://www.jsonschemavalidator.net/). This ensures your configuration adheres to the expected structure.

### Example Configuration

Here’s an example `export-config.json` that conforms to the schema:

```json
[
    {
        "provider": "default",
        "baseTagPath": "Tags/Folder1",
        "sourcePath": "/path/to/export/folder",
        "collisionPolicy": "o",
        "exportMode": "individualFiles",
        "excludeUdtDefinitions": false
    },
    {
        "provider": "default",
        "baseTagPath": "Tags/Folder2",
        "sourcePath": "/path/to/export/folder2",
        "collisionPolicy": "m",
        "exportMode": "singleFile",
        "excludeUdtDefinitions": true
    }
]
```

## Field Descriptions

- **`provider`**: The name of the Ignition tag provider (e.g., `default`). Required.
- **`baseTagPath`**: The tag path to export from or import to (e.g., `Tags/Folder1`). Required.
- **`sourcePath`**: The file or directory path where tags are saved or loaded from (e.g., `/path/to/export`). Required.
- **`collisionPolicy`**: Defines how to handle tag conflicts during import:
  - `a` (abort): Abort the import if conflicts are found.
  - `m` (merge): Merge new tags with existing ones, preserving unchanged properties.
  - `o` (overwrite): Overwrite existing tags with new ones.
  - `d` (delete-and-replace): Delete existing tags and replace with new ones.
- **`exportMode`**: Specifies the export format:
  - `singleFile`: Export all tags to a single JSON file.
  - `individualFiles`: Export each tag to its own file.
  - `structuredByType`: Export tags into a directory structure based on type.
- **`excludeUdtDefinitions`**: If `true`, excludes UDT definitions (`_types_` folder) from exports. Defaults to `false`.

## Next Steps

- Learn how to set up your configuration file in [Basic Setup](./basic-setup).
- Explore export options in [Tag Export](../tag-export/export-modes).
