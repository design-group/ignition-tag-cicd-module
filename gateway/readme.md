# Gateway Scope - Example Component Library

This directory contains the gateway-specific code for the Example Component Library. The gateway scope is responsible for registering components with the Perspective system and handling any server-side logic related to these components.

## Key Files

- `ExampleComponentLibraryGatewayHook.java`: The main entry point for the gateway module.

## ExampleComponentLibraryGatewayHook

This class extends `AbstractGatewayModuleHook`, the hook provided by the Ignition SDK that acts as the entrypoint for our Java code, and is responsible for the lifecycle management of our module in the gateway scope.

### Key Methods

1. `setup(GatewayContext context)`: 
   - Called before startup.
   - We store our `GatewayContext` for later use.

2. `startup(LicenseState activationState)`:
   - Called to initialize the module.

3. `shutdown()`:
   - Called when the module is being shut down.

4. `getMountedResourceFolder()`:
   - Specifies the folder in the module's gateway jar files that should be mounted at `/res/${module-id}/foldername`.

5. `mountRouteHandlers(RouteGroup routes)`
   - Called to mount custom route handlers.
   - In this case, we mount a route handler for importing, exporting, and deleting tags.

6. `getMountPathAlias()`:
   - Provides an alternate mounting path instead of the module ID.
   - We use a constant `MODULE_URL_ALIAS` for consistency.

7. `isFreeModule()`:
   - Indicates whether this is a "free" module (not participating in the licensing system).
   - We return `true` as this is a free module.

## Resource Mounting

By implementing `getMountedResourceFolder()` and `getMountPathAlias()`, we ensure that our web resources (JavaScript and CSS files) are properly mounted and accessible to the Perspective frontend.

## Error Handling

We include error logging in case the component registry is not available, which would prevent our components from functioning correctly.

## Best Practices

1. We use a logger (`slf4j.Logger`) for proper error and info logging.
2. We check for null references before using them to avoid null pointer exceptions.
3. We use constants for component IDs and module aliases to maintain consistency across the module.

This gateway hook ensures that our custom components are properly registered with the Perspective module, making them available for use in Perspective projects. It also handles the proper mounting of resources and cleanup during shutdown.