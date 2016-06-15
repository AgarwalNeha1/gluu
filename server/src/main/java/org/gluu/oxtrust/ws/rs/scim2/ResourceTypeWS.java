/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package org.gluu.oxtrust.ws.rs.scim2;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import com.wordnik.swagger.annotations.Api;
import org.gluu.oxtrust.model.scim2.Constants;
import org.gluu.oxtrust.model.scim2.ListResponse;
import org.gluu.oxtrust.model.scim2.Meta;
import org.gluu.oxtrust.model.scim2.Resource;
import org.gluu.oxtrust.model.scim2.provider.ResourceType;
import org.gluu.oxtrust.model.scim2.schema.SchemaExtensionHolder;
import org.jboss.seam.annotations.Name;

/**
 * @author Rahat Ali Date: 05.08.2015
 */
@Name("resourceTypesWs")
@Path("/scim/v2/ResourceTypes")
@Api(value = "/v2/ResourceTypes", description = "SCIM 2.0 ResourceType Endpoint (https://tools.ietf.org/html/rfc7643#section-6)")
public class ResourceTypeWS extends BaseScimWebService {

	@GET
	@Produces(Constants.MEDIA_TYPE_SCIM_JSON)
	@HeaderParam("Accept") @DefaultValue(Constants.MEDIA_TYPE_SCIM_JSON)
	public Response listResources(@HeaderParam("Authorization") String authorization) throws Exception {

		ListResponse listResponse = new ListResponse();

		List<String> schemas = new ArrayList<String>();
		schemas.add(Constants.LIST_RESPONSE_SCHEMA_ID);
		listResponse.setSchemas(schemas);

		ResourceType userResourceType = new ResourceType();
		userResourceType.setDescription(Constants.USER_CORE_SCHEMA_DESCRIPTION);
		userResourceType.setEndpoint("/v2/Users");
		userResourceType.setName(Constants.USER_CORE_SCHEMA_NAME);
		userResourceType.setId(Constants.USER_CORE_SCHEMA_NAME);
		userResourceType.setSchema(Constants.USER_CORE_SCHEMA_ID);

		Meta userMeta = new Meta();
		userMeta.setLocation(super.applicationConfiguration.getBaseEndpoint() + "/scim/v2/ResourceTypes/User");
		userMeta.setResourceType("ResourceType");
		userResourceType.setMeta(userMeta);

		List<SchemaExtensionHolder> schemaExtensions = new ArrayList<SchemaExtensionHolder>();
		SchemaExtensionHolder userExtensionSchema = new SchemaExtensionHolder();
		userExtensionSchema.setSchema(Constants.USER_EXT_SCHEMA_ID);
		userExtensionSchema.setRequired(false);
		schemaExtensions.add(userExtensionSchema);
		userResourceType.setSchemaExtensions(schemaExtensions);

		ResourceType groupResourceType = new ResourceType();
		groupResourceType.setDescription(Constants.GROUP_CORE_SCHEMA_DESCRIPTION);
		groupResourceType.setEndpoint("/v2/Groups");
		groupResourceType.setName(Constants.GROUP_CORE_SCHEMA_NAME);
		groupResourceType.setId(Constants.GROUP_CORE_SCHEMA_NAME);
		groupResourceType.setSchema(Constants.GROUP_CORE_SCHEMA_ID);

		Meta groupMeta = new Meta();
		groupMeta.setLocation(super.applicationConfiguration.getBaseEndpoint() + "/scim/v2/ResourceTypes/Group");
		groupMeta.setResourceType("ResourceType");
		groupResourceType.setMeta(groupMeta);

		// ResourceType[] resourceTypes = new ResourceType[]{userResourceType, groupResourceType};
		List<Resource> resourceTypes = new ArrayList<Resource>();
		resourceTypes.add(userResourceType);
		resourceTypes.add(groupResourceType);

		listResponse.setResources(resourceTypes);

		listResponse.setTotalResults(resourceTypes.size());
		listResponse.setItemsPerPage(10);
		listResponse.setStartIndex(1);

		URI location = new URI(super.applicationConfiguration.getBaseEndpoint() + "/scim/v2/ResourceTypes");

		// return Response.ok(resourceTypes).location(location).build();
		return Response.ok(listResponse).location(location).build();
	}

	@Path("User")
	@GET
	@Produces(Constants.MEDIA_TYPE_SCIM_JSON)
	@HeaderParam("Accept") @DefaultValue(Constants.MEDIA_TYPE_SCIM_JSON)
	public Response getResourceTypeUser(@HeaderParam("Authorization") String authorization) throws Exception {

		ResourceType userResourceType = new ResourceType();
		userResourceType.setDescription(Constants.USER_CORE_SCHEMA_DESCRIPTION);
		userResourceType.setEndpoint("/v2/Users");
		userResourceType.setName(Constants.USER_CORE_SCHEMA_NAME);
		userResourceType.setId(Constants.USER_CORE_SCHEMA_NAME);
		userResourceType.setSchema(Constants.USER_CORE_SCHEMA_ID);

		Meta userMeta = new Meta();
		userMeta.setLocation(super.applicationConfiguration.getBaseEndpoint() + "/scim/v2/ResourceTypes/User");
		userMeta.setResourceType("ResourceType");
		userResourceType.setMeta(userMeta);

		List<SchemaExtensionHolder> schemaExtensions = new ArrayList<SchemaExtensionHolder>();
		SchemaExtensionHolder userExtensionSchema = new SchemaExtensionHolder();
		userExtensionSchema.setSchema(Constants.USER_EXT_SCHEMA_ID);
		userExtensionSchema.setRequired(false);
		schemaExtensions.add(userExtensionSchema);
		userResourceType.setSchemaExtensions(schemaExtensions);

		// ResourceType[] resourceTypes = new ResourceType[]{userResourceType};

		URI location = new URI(super.applicationConfiguration.getBaseEndpoint() + "/scim/v2/ResourceTypes/User");

		// return Response.ok(resourceTypes).location(location).build();
		return Response.ok(userResourceType).location(location).build();
	}

	@Path("Group")
	@GET
	@Produces(Constants.MEDIA_TYPE_SCIM_JSON)
	@HeaderParam("Accept") @DefaultValue(Constants.MEDIA_TYPE_SCIM_JSON)
	public Response getResourceTypeGroup(@HeaderParam("Authorization") String authorization) throws Exception {

		ResourceType groupResourceType = new ResourceType();
		groupResourceType.setDescription(Constants.GROUP_CORE_SCHEMA_DESCRIPTION);
		groupResourceType.setEndpoint("/v2/Groups");
		groupResourceType.setName(Constants.GROUP_CORE_SCHEMA_NAME);
		groupResourceType.setId(Constants.GROUP_CORE_SCHEMA_NAME);
		groupResourceType.setSchema(Constants.GROUP_CORE_SCHEMA_ID);

		Meta groupMeta = new Meta();
		groupMeta.setLocation(super.applicationConfiguration.getBaseEndpoint() + "/scim/v2/ResourceTypes/Group");
		groupMeta.setResourceType("ResourceType");
		groupResourceType.setMeta(groupMeta);

		// ResourceType[] resourceTypes = new ResourceType[]{groupResourceType};

		URI location = new URI(super.applicationConfiguration.getBaseEndpoint() + "/scim/v2/ResourceTypes/Group");

		// return Response.ok(resourceTypes).location(location).build();
		return Response.ok(groupResourceType).location(location).build();
	}
}
