package com.bwdesigngroup.ignition.tag_cicd.gateway.web.routes;

import static com.inductiveautomation.ignition.gateway.dataroutes.HttpMethod.POST;
import static com.inductiveautomation.ignition.gateway.dataroutes.RouteGroup.TYPE_JSON;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bwdesigngroup.ignition.tag_cicd.common.TagExportUtilities;
import com.bwdesigngroup.ignition.tag_cicd.common.WebUtilities;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.gateway.dataroutes.RequestContext;
import com.inductiveautomation.ignition.gateway.dataroutes.RouteGroup;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.tags.model.GatewayTagManager;

/**
 * @author Keith Gamble
 */
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
			boolean individualFilesPerObject = Boolean
					.parseBoolean(requestContext.getParameter("individualFilesPerObject"));
			boolean deleteExisting = Boolean.parseBoolean(requestContext.getParameter("deleteExisting"));

			TagExportUtilities.exportTagsToDisk(tagManager, provider, baseTagPath, recursive, localPropsOnly, filePath,
					individualFilesPerObject, deleteExisting);
			responseObject.addProperty("status", "success");
			responseObject.addProperty("filePath", filePath);
		} catch (Exception e) {
			logger.error("Error exporting tags to disk: " + e.getMessage(), e);
			responseObject = WebUtilities.getInternalServerErrorResponse(httpServletResponse, e);
		}
		return responseObject;
	}
}