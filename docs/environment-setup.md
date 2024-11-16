# Environment Setup Guide

This guide will walk you through setting up a development environment for Java and Node.js. We'll be using the following tools:

- VS Code
- SDKMAN! (for Java version management)
- Gradle
- NVM (Node Version Manager)
- Node.js
- Docker and Docker Compose

## Steps

### 1. Install SDKMAN! and Java

1. In your terminal, install SDKMAN! by running:
   ```
   curl -s "https://get.sdkman.io" | bash
   ```
2. Restart your terminal or run:
   ```
   source "$HOME/.sdkman/bin/sdkman-init.sh"
   ```
3. Install Java using SDKMAN!:
   ```
   sdk install java 11.0.24-zulu
   ```

### 2. Install Gradle

With SDKMAN! installed, you can easily install Gradle:

```
sdk install gradle 7.6
```

### 3. Install NVM and Node.js

1. Install NVM by running:
   ```
   curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.3/install.sh | bash
   ```
2. Restart your terminal or run:
   ```
   source ~/.bashrc
   ```
3. Install Node.js using NVM:
   ```
   nvm install --lts
   ```

### 4. Configure VS Code

1. Open VS Code.
2. Install the following VS Code extensions:
   - [Java Extension Pack](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack)
   - [Gradle for Java](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-gradle)

### 5. Setup Gradle Properties

1. Copy the `gradle.properties.template` file to `gradle.properties` in the root of the project.
2. Update the `gradle.properties` file with your own values.
3. If you do not have the signing files, you can set `signModule=false`.

### 6. Unsecure Designer Login

It is possible to auto-login to the designer for testing purposes, however it is noted that this is not secure and should not be used in a production environment. To enable this, add the following JVM args to your designer launcher config for this gateway:

```sh
-Dautologin.username=some_username;
-Dautologin.password=some_password;
-Djavaws.ignition.loglevel=INFO;
-Djavaws.ignition.debug=true;
-Dproject.name=some_project_name;
```

## Verification

To verify your setup:

1. Open a new WSL terminal and run:
   ```
   java -version
   gradle -version
   node -v
   npm -v
   docker --version
   docker-compose --version
   ```
2. All commands should return version information without errors.
3. Build the module to test the Gradle setup:
   ```
   ./gradlew clean build
   ```

## Next Steps

Now that your environment is set up, you're ready to start developing! Check out our [Getting Started Guide](getting-started.md) for next steps on how to begin working with our project.

If you encounter any issues during setup, please refer to the official documentation for each tool or reach out to our support team.