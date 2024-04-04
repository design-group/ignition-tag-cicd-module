/*
 * Copyright 2023 Barry-Wehmiller Design Group
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
package com.bwdesigngroup.ignition.tag_cicd.gateway.web.routes;

import static com.inductiveautomation.ignition.gateway.dataroutes.HttpMethod.POST;
import static com.inductiveautomation.ignition.gateway.dataroutes.RouteGroup.TYPE_JSON;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bwdesigngroup.ignition.tag_cicd.common.TagConfigUtilities;
import com.bwdesigngroup.ignition.tag_cicd.common.WebUtilities;
import com.inductiveautomation.ignition.common.gson.JsonElement;
import com.inductiveautomation.ignition.common.gson.JsonObject;
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
public class TagImportRoutes {
	private static final Logger logger = LoggerFactory.getLogger(TagImportRoutes.class.getName());
	private final RouteGroup routes;
	private final GatewayTagManager tagManager;

	/**
	 * Creates a new instance of the TagImportRoutes class with the given
	 * GatewayContext and RouteGroup.
	 *
	 * @param context the GatewayContext to use
	 * @param group   the RouteGroup to use
	 */
	public TagImportRoutes(GatewayContext context, RouteGroup group) {
		this.routes = group;
		this.tagManager = context.getTagManager();
	}

	/**
	 * Mounts the routes for the TagCICD module.
	 */
	public void mountRoutes() {
		/*
		* Import tag configuration
		* This will be a POST request, with the tag configuration in the body
		* 
		* Example Usage: curl -X POST -H "Content-Type: application/json" -d @tag_configuration.json http://tag-cicd.localtest.me/data/tag-cicd/tags/import?provider=default&baseTagPath=&collisionPolicy=o&importType=json
		*/ 
		this.routes.newRoute("/tags/import")
			.handler(this::importTagConfiguration)
			.type(TYPE_JSON)
			.method(POST)
			.mount();
	}

	/**
	 * Imports a tag configuration from a JSON string in the request body
	 *
	 * @param requestContext      the context of the HTTP request
	 * @param httpServletResponse the HTTP response to send
	 * @return a JsonObject representing the results of the import operation
	 * @throws JSONException if there is an error parsing the JSON string
	 * @throws IOException   if there is an error reading the request body
	 */
	public JsonObject importTagConfiguration(RequestContext requestContext, HttpServletResponse httpServletResponse)
			throws JSONException, IOException {
		// Read the tag configuration from the request body
		String tagConfiguration = requestContext.readBody();

		// If the tag configuration is empty, return an error
		if (tagConfiguration.isEmpty()) {
			return WebUtilities.getBadRequestError(httpServletResponse, "Tag configuration is empty");
		}

		JsonObject tagConfigurationJson = (JsonObject) TagUtilities.stringToJson(tagConfiguration);
		// If tagConfiguration is not valid JSON, return an error
		if (!tagConfigurationJson.isJsonObject()) {
			return WebUtilities.getBadRequestError(httpServletResponse, "Tag configuration is not valid JSON");
		}

		// If the first `tagType` key is "Provider", then we can use that for the
		// provider
		// If not, we will use the provider specified in the request
		String provider = tagConfigurationJson.get("tagType").getAsString();
		if (!provider.equals("Provider")) {
			provider = requestContext.getParameter("provider");

			if (provider == null) {
				return WebUtilities.getBadRequestError(httpServletResponse,
						"Provider is not specified, and tag configuration does not contain a provider");
			}
		} else {
			provider = tagConfigurationJson.get("name").getAsString();
		}

		// If provider is not specified, default to DEFAULT_PROVIDER
		if (provider == null || provider.isEmpty()) {
			provider = TagConfigUtilities.DEFAULT_PROVIDER;
		}

		String collisionPolicyString = requestContext.getParameter("collisionPolicy");

		String basePath = requestContext.getParameter("baseTagPath");
		if (basePath == null) {
			basePath = "";
		}

		// If the `collisionPolicy` is "d" then we will delete the tags before importing
		// else we will use the collision policy specified in the request
		Boolean deleteTags = collisionPolicyString.equals("d");

		// If collisionPolicy is not specified, default to "a"
		if (collisionPolicyString.isEmpty()) {
			collisionPolicyString = "a";
		} else if (deleteTags) {
			collisionPolicyString = "o";
		}

		CollisionPolicy collisionPolicy = CollisionPolicy.fromString(collisionPolicyString);

		// Convert the List of QualityCodes to an array of strings
		JsonObject responseObject = new JsonObject();
		JsonObject createdTags = new JsonObject();
		JsonObject deletedTags = new JsonObject();

		// If we are importing at the base path, remove any tags currently there
		if (deleteTags) {
			logger.info("Deleting all tags in provider " + provider + " before importing");
			TagConfigurationModel baseTagsConfigurationModel = TagConfigUtilities.getTagConfigurationModel(tagManager,
					provider, basePath, true, false);
			List<QualityCode> deletedUdtQualityCodes = TagConfigUtilities.deleteTagsInConfigurationModel(tagManager,
					provider, new BasicTagPath(provider), baseTagsConfigurationModel);

			deletedTags.add(basePath, TagConfigUtilities.convertQualityCodesToArray(deletedUdtQualityCodes));
		}

		// For each tag object in the array tagConfigurationJson.get("tags")
		for (JsonElement tag : tagConfigurationJson.get("tags").getAsJsonArray()) {
			JsonObject tagObject = tag.getAsJsonObject();

			TagPath tagPath = new BasicTagPath(provider);

			if (basePath != null && !basePath.isEmpty()) {
				tagPath = tagPath.getChildPath(basePath);
			}

			List<QualityCode> createdQualityCodes = tagManager
					.importTagsAsync(tagPath, TagUtilities.jsonToString(tagObject), "json", collisionPolicy).join();
			createdTags.add(tagPath.toString(), TagConfigUtilities.convertQualityCodesToArray(createdQualityCodes));
		}

		// If there were deleted or created tags, add them to the response
		TagConfigUtilities.addQualityCodesToJsonObject(responseObject, deletedTags, "deleted_tags");
		TagConfigUtilities.addQualityCodesToJsonObject(responseObject, createdTags, "created_tags");
		return responseObject;

	}
}
