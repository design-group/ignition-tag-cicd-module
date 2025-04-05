package dev.bwdesigngroup.ignition.tag_cicd.gateway.web.routes;

import static com.inductiveautomation.ignition.gateway.dataroutes.HttpMethod.GET;
import static com.inductiveautomation.ignition.gateway.dataroutes.RouteGroup.TYPE_JSON;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.bwdesigngroup.ignition.tag_cicd.common.constants.TagCICDConstants;
import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.gson.JsonParser;
import com.inductiveautomation.ignition.gateway.dataroutes.RequestContext;
import com.inductiveautomation.ignition.gateway.dataroutes.RouteGroup;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Routes for accessing tag configuration data.
 */
public class TagConfigRoutes {
    private static final Logger logger = LoggerFactory.getLogger(TagConfigRoutes.class.getName());
    private final RouteGroup routes;

    public TagConfigRoutes(GatewayContext context, RouteGroup group) {
        this.routes = group;
    }

    public void mountRoutes() {
        this.routes.newRoute("/tags/config")
                .handler(this::getTagConfig)
                .type(TYPE_JSON)
                .method(GET)
                .mount();
    }

    public JsonArray getTagConfig(RequestContext requestContext, HttpServletResponse httpServletResponse) {
        try {
            Path configPath = Paths.get(TagCICDConstants.CONFIG_FILE_PATH);
            if (!Files.exists(configPath)) {
                logger.warn("No export-config.json found at {}", configPath.toAbsolutePath());
                httpServletResponse.setStatus(404);
                return new JsonArray();
            }

            String configContent = new String(Files.readAllBytes(configPath));
            return new JsonParser().parse(configContent).getAsJsonArray();
        } catch (Exception e) {
            logger.error("Error retrieving tag config: {}", e.getMessage(), e);
            httpServletResponse.setStatus(500);
            return new JsonArray();
        }
    }
}