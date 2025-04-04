---
id: ui-components
title: UI Components
sidebar_label: UI Components
---

# UI Components

The Tag CICD Module integrates with the Ignition Designer interface to provide a seamless user experience. This page details the various UI components added by the module.

## Tag CICD Toolbar

The module adds a dedicated toolbar to the Ignition Designer interface:

![Tag CICD Toolbar](/img/ui-elements/tag-cicd-toolbar.png)

The toolbar contains an Import and Export button that allows the user to quickly select the configurations to import/export based off the current configuration `export-config.json`

## Tools Menu Integration

In addition to the toolbar, the module adds entries to the Designer's Tools menu:

![Tools Menu](/img/ui-elements/tool-menu.png)

## Tag Configuration Manager Dialog

The Tag Configuration Manager is the central UI for managing your export/import configurations:

![Configuration Manager](/img/ui-elements/tag-configuration-manager.png)

### Main Components

The dialog contains the following elements:

1. **Configuration Table**: Displays all saved configurations with their properties
2. **Export Config Button**: Exports the current configurations to a JSON file
3. **Import Config Button**: Imports configurations from a JSON file
4. **Add Button**: Creates a new configuration
5. **Edit Button**: Modifies the selected configuration
6. **Delete Button**: Removes the selected configuration
7. **Export Selected Button**: Executes an export operation using the selected configuration
8. **Import Selected Button**: Executes an import operation using the selected configuration
9. **Edit Order Button**: Enters edit mode to reorder configurations
10. **Close Button**: Closes the dialog
11. **Status Label**: Displays operation status and results

### Configuration Table Columns

The table displays the following information for each configuration:

- **Provider**: The tag provider name
- **Base Tag Path**: The starting path for export/import
- **Export Path**: The file path for the operation
- **Export Mode**: The selected export format
- **Collision Policy**: How conflicts are handled
- **Include UDT Defs**: Whether UDT definitions are included

### Path Overlap Warning

The Configuration Manager shows warning icons for export paths that might overlap:

![Path Overlap Warning](/img/ui-elements/path-overlap-warning.png)

This helps prevent issues where one export might overwrite files from another export.

## Tag Configuration Editor Dialog

When adding or editing a configuration, the Tag Configuration Editor dialog appears:

![Configuration Editor](/img/ui-elements/tag-configuration-editor.png)

### Editor Components

The dialog contains the following elements:

1. **Provider Dropdown**: Select the tag provider
2. **Tag Selection Tree**: Browse and select tags and folders
3. **Export Mode Dropdown**: Choose the export/import format
4. **Collision Policy Dropdown**: Select the conflict handling policy
5. **Export Path Field**: Specify the gateway file system path
6. **Full Path Preview**: Shows the complete path on the gateway
7. **Export Provider Root Checkbox**: Option to export from provider root
8. **Include UDT Definitions Checkbox**: Option to include UDT definitions
9. **Cancel Button**: Discard changes and close the dialog
10. **Save/Add Button**: Save the configuration and close the dialog

## Tag Export/Import Dialog

When using the toolbar buttons, a dialog appears to select configurations for immediate export/import:

![Export/Import Buttons](/img/ui-elements/tag-configuration-export.png)

### Dialog Components

The dialog contains:

1. **Configuration Tree**: Hierarchical view of configurations grouped by provider
2. **Select All/None Links**: Quickly select or deselect all configurations
4. **Cancel Button**: Closes the dialog without performing operations
5. **Export/Import Button**: Executes the selected operations

## Export/Import Order Management

When working with multiple configurations, the order of operations is important. The Edit Order mode allows you to arrange configurations:

![Edit Order Mode](/img/ui-elements/tag-configuration-order-mode.png)

### Order Management Features

- **Drag and Drop**: Reorder configurations by dragging them
- **Visual Feedback**: Highlights indicate the drop position
- **Order Impact**: During imports, configurations are processed in the displayed order
