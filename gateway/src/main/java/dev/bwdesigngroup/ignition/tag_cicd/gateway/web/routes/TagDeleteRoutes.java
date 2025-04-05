package dev.bwdesigngroup.ignition.tag_cicd.gateway.web.routes;

import static com.inductiveautomation.ignition.gateway.dataroutes.HttpMethod.DELETE;
import static com.inductiveautomation.ignition.gateway.dataroutes.RouteGroup.TYPE_JSON;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.bwdesigngroup.ignition.tag_cicd.common.util.TagDeleteUtilities;
import dev.bwdesigngroup.ignition.tag_cicd.common.util.WebUtilities;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.gateway.dataroutes.RequestContext;
import com.inductiveautomation.ignition.gateway.dataroutes.RouteGroup;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.tags.model.GatewayTagManager;

/**
 * @author Keith Gamble
 */
public class TagDeleteRoutes {
	private static final Logger logger = LoggerFactory.getLogger(TagDeleteRoutes.class.getName());
	private final RouteGroup routes;
	private final GatewayTagManager tagManager;

	public TagDeleteRoutes(GatewayContext context, RouteGroup group) {
		this.routes = group;
		this.tagManager = context.getTagManager();
	}

	public void mountRoutes() {
		this.routes.newRoute("/tags/delete")
				.handler(this::deleteTags)
				.type(TYPE_JSON)
				.method(DELETE)
				.mount();
	}

	public JsonObject deleteTags(RequestContext requestContext, HttpServletResponse httpServletResponse) {
		JsonObject responseObject = new JsonObject();
		try {
			String provider = requestContext.getParameter("provider");
			String baseTagPath = requestContext.getParameter("tagPath");
			boolean recursive = Boolean.parseBoolean(requestContext.getParameter("recursive"));

			responseObject = TagDeleteUtilities.deleteTags(tagManager, provider, baseTagPath, recursive);
		} catch (Exception e) {
			logger.error("Error deleting tags: " + e.getMessage(), e);
			responseObject = WebUtilities.getInternalServerErrorResponse(httpServletResponse, e);
		}
		return responseObject;
	}
}