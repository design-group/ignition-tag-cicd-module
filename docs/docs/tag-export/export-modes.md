---
id: export-modes
title: Export Modes
sidebar_label: Export Modes
---

# Export Modes

The Tag CICD Module supports three different export modes, each designed for specific use cases. This page explains each mode in detail to help you choose the best approach for your needs.

## Mode Comparison

Before diving into the details, here's a quick comparison of the three export modes:

| Feature | Single File | Individual Files | Structured Files |
|---------|-------------|-----------------|------------------|
| **File Structure** | One JSON file for all tags | One JSON file per tag/udt/folder | One JSON file per folder for each type |
| **Folder Hierarchy** | Preserved in JSON | Mirrored in file system | Mirrored in file system |
| **Version Control** | Good for small systems | Good for detailed versioning | Good balance |
| **Diff Readability** | Complex for large changes | Very clear, isolated changes | Clear, organized by type |
| **Export Speed** | Fastest | Slowest | Moderate |
| **Import Speed** | Fastest | Slowest | Moderate |
| **Storage Efficiency** | Most efficient | Least efficient | Moderately efficient |
| **Best For** | Small systems, quick backups | Medium systems, organized development | Large systems, team development |

## Single File Mode

The Single File mode exports all tags to a single JSON file.

### How It Works

1. All selected tags, folders, and UDT definitions are exported to a single JSON file
2. The tag hierarchy is preserved within the JSON structure
3. The file is deterministically sorted for consistent version control

### Example Output

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
        },
        {
          "name": "Tag2",
          "tagType": "AtomicTag",
          "dataType": "String",
          "value": "Hello"
        }
      ]
    },
    {
      "name": "_types_",
      "tagType": "Folder",
      "tags": [
        {
          "name": "MyUDT",
          "tagType": "UdtType",
          "parameters": {...}
        }
      ]
    }
  ]
}
```

### Advantages

- **Simple**: Easier to manage with just one file
- **Fast**: Quicker to export and import
- **Compact**: Takes up less space
- **Portable**: Easy to share and move between systems
- **Dependent**: Changes to any part affect the entire file
- **All-or-nothing**: Must import the entire file

### Use Cases

- **Small tag structures**: When the tag count is manageable
- **Quick backups**: For simple point-in-time recovery
- **Complete exports**: When you need all tags in one operation
- **Single responsibility**: When one person manages all tags

## Individual Files Mode

The Individual Files mode exports each tag, folder, and UDT as a separate JSON file, mirroring the tag hierarchy in the file system.

### How It Works

1. Each folder in the tag structure becomes a directory in the file system
2. Each tag becomes an individual JSON file
3. UDT definitions are exported to individual files in the `_types_` directory
4. The file system hierarchy mirrors the tag hierarchy

### Example Output

```
tags/
├── Folder1/
│   ├── Tag1.json
│   ├── Tag2.json
│   └── Subfolder/
│       └── Tag3.json
├── Folder2/
│   └── Tag4.json
└── _types_/
    ├── MyUDT.json
    └── AnotherUDT.json
```

Example content of `Tag1.json`:
```json
{
  "name": "Tag1",
  "tagType": "AtomicTag",
  "dataType": "Int4",
  "value": 0
}
```

### Advantages

- **Granular version control**: Changes to individual tags are clearly visible in version control systems
- **Team collaboration**: Different team members can work on different tag areas
- **Partial exports/imports**: Export or import specific sections of your tag hierarchy
- **Clear change history**: Git history shows exactly which tags changed and when

### Disadvantages

- **Slower processing**: Creates many files, which can be slower to process
- **More storage**: Generally requires more disk space due to overhead
- **More complex**: Managing many files can be more complex

### Use Cases

- **Medium to large tag structures**: When you have thousands of tags
- **Team development**: When multiple people manage different tag areas
- **Detailed change tracking**: When you need to track every tag change precisely
- **Modular systems**: When different subsystems have separate tag structures

## Structured Files Mode

The Structured Files mode creates a hybrid approach, organizing tags by type within each folder.

:::info
This export scheme is designed to be forward-compatible with Ignition 8.3's file based tag storage.
:::

### How It Works

1. Each folder in the tag structure becomes a directory in the file system
2. Within each directory, regular tags are stored in a `tags.json` file
3. UDT instances in the folder are stored in a `udts.json` file
4. UDT definitions are exported to a `udts.json` file in the `_types_` directory

### Example Output

```
tags/
├── Folder1/
│   ├── tags.json  # Contains regular tags
│   ├── udts.json  # Contains UDT instances
│   └── Subfolder/
│       ├── tags.json
│       └── udts.json
├── Folder2/
│   └── tags.json
└── _types_/
    └── udts.json  # Contains UDT definitions
```

Example content of `Folder1/tags.json`:
```json
{
  "tags": [
    {
      "name": "Tag1",
      "tagType": "AtomicTag",
      "dataType": "Int4",
      "value": 0
    },
    {
      "name": "Tag2",
      "tagType": "AtomicTag",
      "dataType": "String",
      "value": "Hello"
    }
  ]
}
```

### Advantages

- **Organized structure**: Separates regular tags from UDT instances
- **Balanced approach**: Fewer files than Individual mode, more organized than Single File mode
- **Type-specific changes**: Changes to tags or UDTs can be tracked separately
- **Moderate performance**: Reasonable export/import speed for most systems

### Disadvantages

- **Mixed granularity**: Changes to multiple tags in the same folder appear in one file
- **Moderate complexity**: Requires understanding of the structure
- **Multiple file types**: Need to understand the purpose of each file type

### Use Cases

- **Medium to large tag structures**: When you have hundreds to a few thousand tags
- **Organized development**: When you want to separate different types of tags
- **Balanced approach**: When you need a compromise between Single File and Individual Files

## How to Choose

Selecting the right export mode depends on your specific needs:

### Choose Single File Mode If:

- Your tag structure is small (dozens of tags)
- You want the simplest approach
- You need the fastest export/import performance
- You typically export/import all tags at once
- Storage space is limited

### Choose Individual Files Mode If:

- Your tag structure is medium-sized (hundreds to a few thousand tags)
- You need detailed change tracking
- Multiple team members work on different tag areas
- You frequently export/import specific tag sections
- You want the best integration with Git-based workflows

### Choose Structured Files Mode If:

- Your tag structure is medium-sized (hundreds to a few thousand tags)
- You want to organize tags by type
- You need reasonable performance with better organization
- You want a balance between simplicity and detail
- You want to prepare for future Ignition 8.3 features

## Changing Export Modes

You can change the export mode for a configuration at any time:

1. Open the [Tag Configuration Manager](../designer/ui-components#tag-configuration-manager-dialog)
2. Select the configuration you want to modify
3. Click **Edit** to open the configuration
4. Change the **Export Mode** dropdown
5. Click **Save** to apply the changes

:::warning
When changing export modes, you should perform a fresh export to ensure consistency. Different modes create different file structures, and mixing them can cause confusion.
:::

## Advanced Considerations

### Performance Impact

The export modes have different performance characteristics:

| Mode | Export Speed | Import Speed | File Count | Disk Space |
|------|--------------|--------------|------------|------------|
| Single File | Fastest | Fastest | 1 | Lowest |
| Individual Files | Slowest | Slowest | Highest | Highest |
| Structured Files | Medium | Medium | Medium | Medium |

For very large tag structures (10,000+ tags), these differences can be significant.

### Version Control Integration

The export modes integrate differently with version control systems:

- **Single File**: Changes to any tag appear as changes to one file, which can make diffs harder to read
- **Individual Files**: Changes to tags appear as individual file changes, making diffs very clear
- **Structured Files**: Changes to tags in the same folder appear together, providing moderate clarity

### Export Path Considerations

When configuring your export paths, consider:

- Using different root directories for different export modes
- Creating systematic path naming conventions
- Avoiding overlapping paths between configurations

## Examples

### Example: Plant-wide Tag Structure

For a large manufacturing plant with multiple process areas:

```
// Single File Mode
exports/plant-tags.json

// Individual Files Mode
exports/plant/
  ├── Area1/
  │   ├── Process1/
  │   └── Process2/
  ├── Area2/
  └── _types_/

// Structured Files Mode
exports/plant-structured/
  ├── Area1/
  │   ├── tags.json
  │   ├── udts.json
  │   ├── Process1/
  │   └── Process2/
  ├── Area2/
  └── _types_/
```
