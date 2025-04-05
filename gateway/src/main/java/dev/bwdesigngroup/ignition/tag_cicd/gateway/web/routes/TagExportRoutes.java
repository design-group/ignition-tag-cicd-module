package dev.bwdesigngroup.ignition.tag_cicd.gateway.web.routes;

import static com.inductiveautomation.ignition.gateway.dataroutes.HttpMethod.POST;
import static com.inductiveautomation.ignition.gateway.dataroutes.RouteGroup.TYPE_JSON;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.bwdesigngroup.ignition.tag_cicd.common.model.ExportMode;
import dev.bwdesigngroup.ignition.tag_cicd.common.util.TagExportUtilities;
import dev.bwdesigngroup.ignition.tag_cicd.common.util.WebUtilities;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.gateway.dataroutes.RequestContext;
import com.inductiveautomation.ignition.gateway.dataroutes.RouteGroup;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.tags.model.GatewayTagManager;

public class TagExportRoutes {
	private static final Logger logger = LoggerFactory.getLogger(TagExportRoutes.class.getName());
	private final RouteGroup routes;
	private final GatewayTagManager tagManager;

	public TagExportRoutes(GatewayContext context, RouteGroup group) {
		this.routes = group;
		this.tagManager = context.getTagManager();
	}

	public void mountRoutes() {
		this.routes.newRoute("/tags/export")
				.handler(this::exportTagsToJson)
				.type(TYPE_JSON)
				.mount();

		this.routes.newRoute("/tags/export")
				.handler(this::exportTagsToDisk)
				.type(TYPE_JSON)
				.method(POST)
				.mount();

		this.routes.newRoute("/tags/export/modes")
				.handler(this::getExportModes)
				.type(TYPE_JSON)
				.mount();
	}

	public JsonObject exportTagsToJson(RequestContext requestContext, HttpServletResponse httpServletResponse) {
		JsonObject responseObject = new JsonObject();
		try {
			String provider = requestContext.getParameter("provider");
			String baseTagPath = requestContext.getParameter("baseTagPath");
			boolean recursive = Boolean.parseBoolean(requestContext.getParameter("recursive"));
			boolean localPropsOnly = Boolean.parseBoolean(requestContext.getParameter("localPropsOnly"));

			responseObject = TagExportUtilities.exportTagsToJson(tagManager, provider, baseTagPath, recursive,
					localPropsOnly);
		} catch (Exception e) {
			logger.error("Error exporting tags to JSON: " + e.getMessage(), e);
			responseObject = WebUtilities.getInternalServerErrorResponse(httpServletResponse, e);
		}
		return responseObject;
	}

	public JsonObject exportTagsToDisk(RequestContext requestContext, HttpServletResponse httpServletResponse) {
		JsonObject responseObject = new JsonObject();
		try {
			String provider = requestContext.getParameter("provider");
			String baseTagPath = requestContext.getParameter("baseTagPath");
			boolean recursive = Boolean.parseBoolean(requestContext.getParameter("recursive"));
			boolean localPropsOnly = Boolean.parseBoolean(requestContext.getParameter("localPropsOnly"));
			String filePath = requestContext.getParameter("filePath");
			if (filePath == null) {
				throw new IllegalArgumentException("filePath parameter is required");
			}

			String exportMode = requestContext.getParameter("exportMode");
			if (exportMode == null) {
				throw new IllegalArgumentException("exportMode parameter is required");
			}

			boolean deleteExisting = Boolean.parseBoolean(requestContext.getParameter("deleteExisting"));
			boolean excludeUdtDefinitions = Boolean.parseBoolean(requestContext.getParameter("excludeUdtDefinitions"));

			TagExportUtilities.exportTagsToDisk(tagManager, provider, baseTagPath, recursive, localPropsOnly, filePath,
					exportMode, deleteExisting, excludeUdtDefinitions);

			responseObject.addProperty("status", "success");
			responseObject.addProperty("filePath", filePath);
			responseObject.addProperty("exportMode", exportMode);
			if (excludeUdtDefinitions) {
				responseObject.addProperty("excludedUdtDefinitions", true);
			}
		} catch (Exception e) {
			logger.error("Error exporting tags to disk: " + e.getMessage(), e);
			responseObject = WebUtilities.getInternalServerErrorResponse(httpServletResponse, e);
		}
		return responseObject;
	}

	public JsonObject getExportModes(RequestContext requestContext, HttpServletResponse httpServletResponse) {
		JsonObject responseObject = new JsonObject();
		try {
			com.inductiveautomation.ignition.common.gson.JsonArray modesArray = new com.inductiveautomation.ignition.common.gson.JsonArray();

			for (ExportMode mode : ExportMode.values()) {
				JsonObject modeObject = new JsonObject();
				modeObject.addProperty("code", mode.getCode());
				modeObject.addProperty("name", mode.getDisplayName());
				modesArray.add(modeObject);
			}

			responseObject.add("modes", modesArray);
			responseObject.addProperty("status", "success");
		} catch (Exception e) {
			logger.error("Error getting export modes: " + e.getMessage(), e);
			responseObject = WebUtilities.getInternalServerErrorResponse(httpServletResponse, e);
		}
		return responseObject;
	}
}