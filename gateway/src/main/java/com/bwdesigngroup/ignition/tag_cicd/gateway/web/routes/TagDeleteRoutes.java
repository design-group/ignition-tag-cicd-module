/*
 * Copyright 2023 Barry-Wehmiller Design Group
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
package com.bwdesigngroup.ignition.tag_cicd.gateway.web.routes;

import static com.inductiveautomation.ignition.gateway.dataroutes.HttpMethod.DELETE;
import static com.inductiveautomation.ignition.gateway.dataroutes.RouteGroup.TYPE_JSON;

import java.io.IOException;
import java.util.List;
import java.io.File;

import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bwdesigngroup.ignition.tag_cicd.common.TagConfigUtilities;
import com.bwdesigngroup.ignition.tag_cicd.common.TagImportUtilities;
import com.bwdesigngroup.ignition.tag_cicd.common.WebUtilities;
import com.bwdesigngroup.ignition.tag_cicd.common.FileUtilities;
import com.inductiveautomation.ignition.common.gson.JsonElement;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.model.values.QualityCode;
import com.inductiveautomation.ignition.common.tags.TagUtilities;
import com.inductiveautomation.ignition.common.tags.config.CollisionPolicy;
import com.inductiveautomation.ignition.common.tags.config.TagConfigurationModel;
import com.inductiveautomation.ignition.common.tags.model.TagPath;
import com.inductiveautomation.ignition.common.tags.paths.BasicTagPath;
import com.inductiveautomation.ignition.gateway.dataroutes.RequestContext;
import com.inductiveautomation.ignition.gateway.dataroutes.RouteGroup;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.tags.model.GatewayTagManager;

/**
 *
 * @author Keith Gamble
 */
public class TagDeleteRoutes {
	private static final Logger logger = LoggerFactory.getLogger(TagDeleteRoutes.class.getName());
	private final RouteGroup routes;
	private final GatewayTagManager tagManager;

	/**
	 * Creates a new instance of the TagDeleteRoutes class with the given
	 * GatewayContext and RouteGroup.
	 *
	 * @param context the GatewayContext to use
	 * @param group   the RouteGroup to use
	 */
	public TagDeleteRoutes(GatewayContext context, RouteGroup group) {
		this.routes = group;
		this.tagManager = context.getTagManager();
	}

	/**
	 * Mounts the routes for the TagCICD module.
	 */
	public void mountRoutes() {
		/*
		* Delete tag configuration
		* This will be a DELETE request, with a tag path and recursive flag in the query parameters
		* 
		* Example Usage: curl -X DELETE http://tag-cicd.localtest.me/data/tag-cicd/tags/delete?provider=default&tagPath=Folder/NestedFolder&recursive=true
		*/ 
		this.routes.newRoute("/tags/delete")
			.handler(this::deleteAllTagsInProvider)
			.type(TYPE_JSON)
			.method(DELETE) // Use the DELETE method to indicate resource removal
			.mount();
	}

	/**
	 * Deletes all tags within a specified path.
	 *
	 * @param requestContext      the context of the HTTP request
	 * @param httpServletResponse the HTTP response to send
	 * @return a JsonObject indicating the status of the deletion process
	 */
	public JsonObject deleteAllTagsInProvider(RequestContext requestContext, HttpServletResponse httpServletResponse) {
		JsonObject responseObject = new JsonObject();
		try {
			String provider = requestContext.getParameter("provider"); 
			String basePath = requestContext.getParameter("tagPath");

			logger.info("Deleting all tags in " + basePath);
			TagConfigurationModel baseTagsConfigurationModel = TagConfigUtilities.getTagConfigurationModel(tagManager, provider, basePath, true, false);
			List<QualityCode> deletedUdtQualityCodes = TagConfigUtilities.deleteTagsInConfigurationModel(tagManager, provider, new BasicTagPath(basePath), baseTagsConfigurationModel);
			responseObject.add(basePath, TagConfigUtilities.convertQualityCodesToArray(deletedUdtQualityCodes));

		} catch (Exception e) {
			logger.error("Error deleting tags: " + e.toString(), e);
			responseObject = WebUtilities.getInternalServerErrorResponse(httpServletResponse, e);
			httpServletResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		return responseObject;
	}
}  