package dev.bwdesigngroup.ignition.tag_cicd.gateway.web.routes;

import static com.inductiveautomation.ignition.gateway.dataroutes.HttpMethod.POST;
import static com.inductiveautomation.ignition.gateway.dataroutes.RouteGroup.TYPE_JSON;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.bwdesigngroup.ignition.tag_cicd.common.model.ExportMode;
import dev.bwdesigngroup.ignition.tag_cicd.common.util.FileUtilities;
import dev.bwdesigngroup.ignition.tag_cicd.common.util.TagConfigUtilities;
import dev.bwdesigngroup.ignition.tag_cicd.common.util.TagImportUtilities;
import dev.bwdesigngroup.ignition.tag_cicd.common.util.WebUtilities;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.common.tags.TagUtilities;
import com.inductiveautomation.ignition.gateway.dataroutes.RequestContext;
import com.inductiveautomation.ignition.gateway.dataroutes.RouteGroup;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.tags.model.GatewayTagManager;

import java.io.File;
import java.io.IOException;

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

            String exportMode = requestContext.getParameter("exportMode");
            if (exportMode == null) {
                throw new IllegalArgumentException("exportMode parameter is required");
            }

            String sourcePath = requestContext.getParameter("filePath");

            if (sourcePath == null && !ExportMode.SINGLE_FILE.getCode().equals(exportMode)) {
                throw new IllegalArgumentException("filePath is required for selected export mode");
            }

            if (ExportMode.SINGLE_FILE.getCode().equals(exportMode) && sourcePath == null) {
                String jsonBody = requestContext.readBody();
                if (jsonBody == null || jsonBody.trim().isEmpty()) {
                    throw new IllegalArgumentException(
                            "Request body must contain tag configuration JSON when using single file mode without filePath");
                }
                sourcePath = writeTempFile(jsonBody);
            }

            responseObject = TagImportUtilities.importTagsFromSource(
                    tagManager, provider, baseTagPath, sourcePath, collisionPolicy, exportMode);

            if (ExportMode.SINGLE_FILE.getCode().equals(exportMode)
                    && !sourcePath.equals(requestContext.getParameter("filePath"))) {
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