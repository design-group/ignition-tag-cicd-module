package com.bwdesigngroup.ignition.tag_cicd.gateway.web.routes;

import static com.inductiveautomation.ignition.gateway.dataroutes.HttpMethod.POST;
import static com.inductiveautomation.ignition.gateway.dataroutes.RouteGroup.TYPE_JSON;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bwdesigngroup.ignition.tag_cicd.common.TagConfigUtilities;
import com.bwdesigngroup.ignition.tag_cicd.common.TagImportUtilities;
import com.bwdesigngroup.ignition.tag_cicd.common.WebUtilities;
import com.bwdesigngroup.ignition.tag_cicd.common.FileUtilities;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.common.tags.TagUtilities;
import com.inductiveautomation.ignition.gateway.dataroutes.RequestContext;
import com.inductiveautomation.ignition.gateway.dataroutes.RouteGroup;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.tags.model.GatewayTagManager;

import java.io.File; // Added import
import java.io.IOException;

/**
 * @author Keith Gamble
 */
public class TagImportRoutes {
    private static final Logger logger = LoggerFactory.getLogger(TagImportRoutes.class.getName());
    private final RouteGroup routes;
    private final GatewayTagManager tagManager;

    public TagImportRoutes(GatewayContext context, RouteGroup group) {
        this.routes = group;
        this.tagManager = context.getTagManager();
    }

    public void mountRoutes() {
        this.routes.newRoute("/tags/import")
                .handler(this::importTagConfiguration)
                .type(TYPE_JSON)
                .method(POST)
                .mount();
    }

    public JsonObject importTagConfiguration(RequestContext requestContext, HttpServletResponse httpServletResponse) {
        JsonObject responseObject = new JsonObject();
        try {
            // Manually handle default values since getParameter(String, String) doesn't
            // exist
            String provider = requestContext.getParameter("provider");
            if (provider == null) {
                provider = TagConfigUtilities.DEFAULT_PROVIDER;
            }

            String baseTagPath = requestContext.getParameter("baseTagPath");
            if (baseTagPath == null) {
                baseTagPath = "";
            }

            String collisionPolicy = requestContext.getParameter("collisionPolicy");
            if (collisionPolicy == null) {
                collisionPolicy = "a";
            }

            String individualFilesPerObjectStr = requestContext.getParameter("individualFilesPerObject");
            boolean individualFilesPerObject = individualFilesPerObjectStr != null
                    ? Boolean.parseBoolean(individualFilesPerObjectStr)
                    : false;

            String sourcePath = requestContext.getParameter("filePath");

            if (sourcePath == null && individualFilesPerObject) {
                throw new IllegalArgumentException("filePath is required when individualFilesPerObject is true");
            }

            // If not using individual files, read the body as the source JSON
            if (!individualFilesPerObject) {
                String jsonBody = requestContext.readBody();
                if (jsonBody == null || jsonBody.trim().isEmpty()) {
                    throw new IllegalArgumentException(
                            "Request body must contain tag configuration JSON when individualFilesPerObject is false");
                }
                sourcePath = writeTempFile(jsonBody); // Write body to a temp file for import
            }

            responseObject = TagImportUtilities.importTagsFromSource(
                    tagManager, provider, baseTagPath, sourcePath, collisionPolicy, individualFilesPerObject);

            // Clean up temp file if created
            if (!individualFilesPerObject) {
                new File(sourcePath).delete();
            }
        } catch (Exception e) {
            logger.error("Error importing tag configuration: " + e.getMessage(), e);
            responseObject = WebUtilities.getInternalServerErrorResponse(httpServletResponse, e);
        }
        return responseObject;
    }

    private String writeTempFile(String jsonContent) throws IOException {
        File tempFile = File.createTempFile("tag_import_", ".json");
        FileUtilities.saveJsonToFile(TagUtilities.stringToJson(jsonContent).getAsJsonObject(),
                tempFile.getAbsolutePath());
        return tempFile.getAbsolutePath();
    }
}