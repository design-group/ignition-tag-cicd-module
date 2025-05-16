---
id: installation
title: Installation
sidebar_label: Installation
---

# Installing the Tag CICD Module

This guide will walk you through the process of installing the Tag CICD Module on your Ignition gateway.

## System Requirements

Before installing, ensure your system meets these requirements:

- **Ignition Version**: 8.1.24 or higher
- **Operating System**: Any OS supported by Ignition (Windows, Linux, macOS)
- **Memory**: No additional requirements beyond Ignition's standard requirements
- **Disk Space**: At least 50MB free space for the module and exported files

## Downloading the Module

### Releases

1. Go to the [Releases page](https://github.com/design-group/ignition-tag-cicd-module/releases) on GitHub
2. Download the latest `Tag-CICD.modl` file

## Troubleshooting

If you encounter any issues during installation:

### Module Won't Install

- Ensure you have the correct Ignition version (8.1.24+)
- Check Gateway logs for specific error messages
- Verify the module file isn't corrupted (compare checksum with GitHub release)

### Module Installs But Doesn't Appear in Designer

- Check if the module is enabled in the Gateway (Config > Modules)
- Restart the Designer

### Module API Not Accessible

- Verify the module is running in the Gateway
- Check network connectivity between your client and the Gateway

## Next Steps

After successful installation:

1. Head to [Basic Setup](./basic-setup) to configure the module
2. Review the [Configuration File](./configuration-file) structure
3. Explore the [Designer Features](../designer/ui-components) to start using the module

For any installation issues not covered here, please [submit an issue](https://github.com/design-group/ignition-tag-cicd-module/issues) on GitHub.