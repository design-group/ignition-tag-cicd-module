---
id: key-features
title: Key Features
sidebar_label: Key Features
---

# Key Features

The Tag CICD Module provides a comprehensive set of features designed to enable version control and CI/CD practices for Ignition tags.

## Flexible Export Modes

The module supports three export modes to accommodate different workflow needs:

### Single File Mode

Exports all tags to a single JSON file, ideal for smaller tag structures or when you want to maintain a single configuration file.

```json
{
  "tags": [
    {
      "name": "Folder1",
      "tagType": "Folder",
      "tags": [
        {
          "name": "Tag1",
          "tagType": "AtomicTag",
          "dataType": "Int4",
          "value": 0
        }
      ]
    }
  ]
}
```

### Individual Files Mode

Exports each tag, folder, and UDT as an individual JSON file, maintaining the folder hierarchy. This mode provides the most granular version control and is ideal for complex tag structures where different team members might work on different sections.

```
tags/
├── Folder1/
│   ├── Tag1.json
│   └── Tag2.json
├── Folder2/
│   └── ...
└── _types_/
    ├── TankUDT.json
    └── MotorUDT.json
```

### Structured Files Mode

Creates a hybrid approach with a folder structure containing `tags.json` and `udts.json` files in each folder. This organizes tags by type while still maintaining the folder hierarchy. This version is designed to be compatible with Ignition 8.3's file based tag structure.

```
tags/
├── Folder1/
│   ├── tags.json
│   └── udts.json
├── Folder2/
│   └── ...
└── _types_/
    └── udts.json
```

## Deterministic JSON Sorting

All JSON exports are deterministically sorted to ensure consistent file output regardless of the order in which tags are processed. This is crucial for version control systems like Git, as it prevents false changes in diffs when the only difference is the order of keys in the JSON.

```json
// Always in the same order regardless of when or how the export is performed
{
  "name": "PressureTag",
  "tagType": "AtomicTag",
  "dataType": "Float8",
  "value": 0.0
}
```

## UDT Definition Handling

The module includes sophisticated handling for User Defined Types (UDTs):

- **UDT Type Export** - Properly exports UDT definitions from the `_types_` folder
- **Dependency Analysis** - Identifies dependencies between UDT types
- **Topological Sorting** - Orders UDT imports based on their dependencies
- **Configurable Inclusion** - Option to include or exclude UDT definitions during export

## Supports Standard Collision Policies

When importing tags, the [standard collision policies](https://www.docs.inductiveautomation.com/docs/8.1/platform/tags/exporting-and-importing-tags#import-tags) are supported:

- **Abort (a)** - Aborts the import if duplicate tags are found.
- **Merge (m)** - Merges properties of existing tags with those being imported. This is useful for updating tags without losing existing properties.
- **Overwrite (o)** - Overwrites any tags in the folder that have the same name as tags being imported. Note this a complete overwrite of the tag.
- **Delete and Replace (d)** - Deletes all tags in the folder before importing. This is useful for completely replacing a folder's contents.

## Designer Integration

The module seamlessly integrates with the Ignition Designer:

![Designer Interface](/img/ui-elements/tag-configuration-manager.png)

- **Custom Toolbar** - Quick access to export and import operations
- **Configuration Manager** - UI for managing and organizing export/import configurations
- **Visual Drag & Drop** - Reorder configurations to control import sequence

## Comprehensive REST API

All functionality is available through a REST API for CI/CD pipeline integration:

```bash
# Export tags example
curl -X POST "https://tag-cicd.localtest.me/data/tag-cicd/tags/export" \
  -d "provider=Example&recursive=true&baseTagPath=MyFolder&filePath=data/tags/example&exportMode=structuredByType"
  
# Import tags example
curl -X POST "https://tag-cicd.localtest.me/data/tag-cicd/tags/import" \
  -d "provider=Example&baseTagPath=MyFolder&recursive=true&sourcePath=data/tags/example&collisionPolicy=o&exportMode=structuredByType"
```

## Automated Operations

The module supports automation for seamless integration into your workflows:

- **Startup Import** - Automatically import tags when the gateway starts
- **Configuration File** - Store export/import configurations for reuse

## Gateway & Designer Components

The module includes both gateway and designer components:

- **Gateway Component**: Provides REST API endpoints and handles tag operations
- **Designer Component**: Offers a user-friendly UI for configuration and execution

## File System Integration

The module can read from and write to the gateway's file system, making it easy to:

- Save exports to any accessible location on the gateway
- Import from any file or directory the gateway can access
- Integrate with network shares or mounted repositories