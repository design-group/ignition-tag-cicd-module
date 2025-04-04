---
id: index
title: Ignition Tag CICD Module
sidebar_label: Home
slug: /
---

# Ignition Tag CICD Module

Welcome to the official documentation for the Ignition Tag CICD Module - a powerful tool for implementing version control and continuous integration/continuous deployment (CI/CD) practices for Ignition tags.

## What is the Tag CICD Module?

The Tag CICD Module is an extension for Ignition that enables more sophisticated version control and deployment workflows for tag configurations. It allows you to:

- **Export** tag configurations to files that can be stored in version control systems
- **Import** tag configurations from files into Ignition gateways
- **Track changes** to tags with proper version history
- **Automate deployments** of tag configurations across environments
- **Implement CI/CD practices** for consistent and reliable tag management

This module bridges the gap between traditional software development practices and industrial automation systems, bringing modern DevOps approaches to Ignition tag management.

## Key Features

- **Multiple Export Formats**: Export as individual files, structured directories, or a single file
- **Deterministic Output**: Creates consistently ordered JSON for meaningful version control diffs
- **UDT Dependency Resolution**: Automatically handles UDT dependencies during import
- **Configurable Collision Handling**: Multiple strategies for handling tag conflicts
- **Complete REST API**: Full API for integration with CI/CD pipelines
- **Designer Integration**: Seamless UI integration with Ignition Designer
- **Automation Support**: Configurable for automated operations like startup imports

## Getting Started

New to the Tag CICD Module? Start here:

<div class="grid">
  <a href="getting-started/installation" class="card">
    <h3>Installation</h3>
    <p>Install the module and get ready to use it</p>
    <span>Learn more →</span>
  </a>
  <a href="getting-started/basic-setup" class="card">
    <h3>Basic Setup</h3>
    <p>Create your first export/import configuration</p>
    <span>Learn more →</span>
  </a>
  <a href="getting-started/configuration-file" class="card">
    <h3>Configuration File</h3>
    <p>Understand the module's configuration structure</p>
    <span>Learn more →</span>
  </a>
  <a href="getting-started/configuration-schema" class="card">
    <h3>Configuration Schema</h3>
    <p>Explore the JSON schema for export-config.json</p>
    <span>Learn more →</span>
  </a>
</div>

## Core Documentation

Explore the module's main functionality:

<div class="grid">
  <a href="designer/ui-components" class="card">
    <h3>Designer Features</h3>
    <p>Use the Designer UI components</p>
    <span>Learn more →</span>
  </a>
  <a href="tag-export/export-modes" class="card">
    <h3>Tag Export</h3>
    <p>Export tag configurations to files</p>
    <span>Learn more →</span>
  </a>
</div>

## Support and Community

- **GitHub Issues**: [Report bugs or request features](https://github.com/keith-gamble/ignition-tag-cicd-module/issues)
- **GitHub Repository**: [Access the source code](https://github.com/keith-gamble/ignition-tag-cicd-module)

## License

The Tag CICD Module is licensed under the [MIT License](https://github.com/keith-gamble/ignition-tag-cicd-module/blob/master/LICENSE.txt).