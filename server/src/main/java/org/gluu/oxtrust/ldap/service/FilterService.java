/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxtrust.ldap.service;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.velocity.VelocityContext;
import org.gluu.oxtrust.model.GluuSAMLTrustRelationship;
import org.gluu.oxtrust.model.MetadataFilter;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;
import org.w3c.dom.Document;
import org.xdi.util.StringHelper;
import org.xdi.util.io.FileUploadWrapper;
import org.xml.sax.SAXException;

/**
 * Provides operations with metadata filters
 * 
 */
@Scope(ScopeType.STATELESS)
@Name("filterService")
@AutoCreate
public class FilterService {

	public static final String VALIDATION_TYPE = "SchemaValidation";

	public static final Object ENTITY_ROLE_WHITE_LIST_TYPE = "EntityRoleWhiteList";

	public static final Object VALID_UNTIL_REQUIRED_TYPE = "RequiredValidUntil";

	public static final Object SIGNATURE_VALIDATION_TYPE = "SignatureValidation";

	@Logger
	private Log log;

	@In
	private TemplateService templateService;

	@In
	private Shibboleth2ConfService shibboleth2ConfService;

	public List<MetadataFilter> getAvailableMetadataFilters() {
		File filterFolder = new File(System.getProperty("catalina.home") + File.separator + "conf" + File.separator + "shibboleth2"
				+ File.separator + "idp" + File.separator + "MetadataFilter");
		File[] filterTemplates = null;
		if (filterFolder.exists() && filterFolder.isDirectory()) {
			filterTemplates = filterFolder.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.endsWith("Filter.xml.vm");
				}
			});
		}
		List<MetadataFilter> metadataFilters = new ArrayList<MetadataFilter>();
		for (File filterTemplate : filterTemplates) {
			metadataFilters.add(createMetadataFilter(filterTemplate.getName().split("Filter")[0]));
		}
		return metadataFilters;
	}

	private MetadataFilter createMetadataFilter(String filterName) {
		MetadataFilter metadataFilter = new MetadataFilter();
		metadataFilter.setName(filterName);
		metadataFilter.setExtensionSchemas(new ArrayList<String>());
		metadataFilter.setRemoveRolelessEntityDescriptors(true);
		metadataFilter.setRemoveEmptyEntitiesDescriptors(true);
		metadataFilter.setRetainedRoles(new ArrayList<String>());
		metadataFilter.setRequireSignedMetadata(false);

		return metadataFilter;
	}

	public List<MetadataFilter> getMetadataFiltersForTrustRelationship(GluuSAMLTrustRelationship trustRelationship) {
		// TODO Auto-generated method stub
		return new ArrayList<MetadataFilter>();
	}

	/**
	 * Get filterService instance
	 * 
	 * @return FilterService instance
	 */
	public static FilterService instance() {
		return (FilterService) Component.getInstance(FilterService.class);
	}

	public void updateFilter(GluuSAMLTrustRelationship trustRelationship, MetadataFilter metadataFilterSelected) {
		trustRelationship.getMetadataFilters().put(metadataFilterSelected.getName(), metadataFilterSelected);
	}

	public boolean isMetadataFilterPresent(GluuSAMLTrustRelationship trustRelationship, MetadataFilter filter) {
		return trustRelationship.getMetadataFilters().keySet().contains(filter.getName());
	}

	public List<MetadataFilter> getFiltersList(GluuSAMLTrustRelationship trustRelationship) {
		List<MetadataFilter> metadataFilters = new ArrayList<MetadataFilter>();
		for (String filterName : trustRelationship.getMetadataFilters().keySet()) {
			metadataFilters.add(trustRelationship.getMetadataFilters().get(filterName));
		}
		return metadataFilters;
	}

	public void removeFilter(GluuSAMLTrustRelationship trustRelationship, MetadataFilter filter) {
		trustRelationship.getMetadataFilters().remove(filter.getName());
	}

	public void saveFilters(GluuSAMLTrustRelationship trustRelationship, FileUploadWrapper filterCertWrapper) {
		VelocityContext context = new VelocityContext();
		if (trustRelationship.getMetadataFilters().get("validation") != null) {
			List<String> extensionSchemas = trustRelationship.getMetadataFilters().get("validation").getExtensionSchemas();
			if (extensionSchemas != null && !extensionSchemas.isEmpty()) {
				context.put("extensionSchemas", extensionSchemas);
			} else {
				log.warn("validation filter on " + trustRelationship.getIname() + "is invalid. Removing it.");
				trustRelationship.getMetadataFilters().remove("validation");
			}
		}

		if (trustRelationship.getMetadataFilters().get("entityRoleWhiteList") != null) {
			List<String> retainedRoles = trustRelationship.getMetadataFilters().get("entityRoleWhiteList").getRetainedRoles();
			if (retainedRoles != null && !retainedRoles.isEmpty()) {
				context.put("retainedRoles", retainedRoles);
				boolean removeEmptyEntitiesDescriptors = trustRelationship.getMetadataFilters().get("entityRoleWhiteList")
						.getRemoveEmptyEntitiesDescriptors();
				context.put("removeEmptyEntitiesDescriptors", removeEmptyEntitiesDescriptors);
				boolean removeRolelessEntityDescriptors = trustRelationship.getMetadataFilters().get("entityRoleWhiteList")
						.getRemoveRolelessEntityDescriptors();
				context.put("removeRolelessEntityDescriptors", removeRolelessEntityDescriptors);
			} else {
				log.warn("entityRoleWhiteList filter on " + trustRelationship.getIname() + "is invalid. Removing it.");
				trustRelationship.getMetadataFilters().remove("entityRoleWhiteList");
			}

		}

		if (trustRelationship.getMetadataFilters().get("requiredValidUntil") != null) {
			int maxValidityInterval = trustRelationship.getMetadataFilters().get("requiredValidUntil").getMaxValidityInterval();
			context.put("maxValidityInterval", maxValidityInterval);
		}

		if (trustRelationship.getMetadataFilters().get("signatureValidation") != null) {
			String filterCertFileName = StringHelper.removePunctuation(trustRelationship.getInum());
			if (filterCertWrapper.getStream() != null) {
				shibboleth2ConfService.saveFilterCert(filterCertFileName, filterCertWrapper.getStream());
				trustRelationship.getMetadataFilters().get("signatureValidation")
						.setFilterCertFileName(StringHelper.removePunctuation(trustRelationship.getInum()));
			}

			if (StringHelper.isNotEmpty(trustRelationship.getMetadataFilters().get("signatureValidation").getFilterCertFileName())) {
				boolean requireSignedMetadata = trustRelationship.getMetadataFilters().get("signatureValidation")
						.getRequireSignedMetadata();
				context.put("trustEngine", "shibboleth.FedTrustEngine");
				context.put("requireSignedMetadata", requireSignedMetadata);
			} else {
				log.warn("signatureValidation filter on " + trustRelationship.getIname() + "is invalid. Removing it.");
				trustRelationship.getMetadataFilters().remove("signatureValidation");
			}
		}

		trustRelationship.setGluuSAMLMetaDataFilter(new ArrayList<String>());

		for (String filterName : trustRelationship.getMetadataFilters().keySet()) {
			trustRelationship.getGluuSAMLMetaDataFilter().add(templateService.generateConfFile(filterName + "Filter.xml", context));
		}
	}

	public void parseFilters(GluuSAMLTrustRelationship trustRelationship) throws SAXException, IOException, ParserConfigurationException,
			FactoryConfigurationError, XPathExpressionException {
		if (trustRelationship.getGluuSAMLMetaDataFilter() != null) {
			XPath xPath = XPathFactory.newInstance().newXPath();
			for (String filterXML : trustRelationship.getGluuSAMLMetaDataFilter()) {
				Document xmlDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder()
						.parse(new java.io.ByteArrayInputStream(filterXML.getBytes()));
				if (xmlDocument.getFirstChild().getAttributes().getNamedItem("xsi:type").getNodeValue().equals(VALIDATION_TYPE)) {
					MetadataFilter filter = createMetadataFilter("validation");
					XPathExpression contactCountXPath = xPath.compile("count(/MetadataFilter/ExtensionSchema)");
					int schemasNumber = Integer.parseInt(contactCountXPath.evaluate(xmlDocument));

					for (int i = 1; i <= schemasNumber; i++) {
						contactCountXPath = xPath.compile("/MetadataFilter/ExtensionSchema[" + i + "]");
						filter.getExtensionSchemas().add(contactCountXPath.evaluate(xmlDocument));
					}
					trustRelationship.getMetadataFilters().put("validation", filter);
					continue;
				}

				if (xmlDocument.getFirstChild().getAttributes().getNamedItem("xsi:type").getNodeValue().equals(ENTITY_ROLE_WHITE_LIST_TYPE)) {
					MetadataFilter filter = createMetadataFilter("entityRoleWhiteList");
					filter.setRemoveRolelessEntityDescriptors(Boolean.parseBoolean(xmlDocument.getFirstChild().getAttributes()
							.getNamedItem("removeRolelessEntityDescriptors").getNodeValue()));
					filter.setRemoveEmptyEntitiesDescriptors(Boolean.parseBoolean(xmlDocument.getFirstChild().getAttributes()
							.getNamedItem("removeEmptyEntitiesDescriptors").getNodeValue()));

					XPathExpression contactCountXPath = xPath.compile("count(/MetadataFilter/RetainedRole)");
					int schemasNumber = Integer.parseInt(contactCountXPath.evaluate(xmlDocument));

					for (int i = 1; i <= schemasNumber; i++) {
						contactCountXPath = xPath.compile("/MetadataFilter/RetainedRole[" + i + "]");
						filter.getRetainedRoles().add(contactCountXPath.evaluate(xmlDocument));
					}
					trustRelationship.getMetadataFilters().put("entityRoleWhiteList", filter);
					continue;
				}

				if (xmlDocument.getFirstChild().getAttributes().getNamedItem("xsi:type").getNodeValue().equals(VALID_UNTIL_REQUIRED_TYPE)) {
					MetadataFilter filter = createMetadataFilter("requiredValidUntil");
					filter.setMaxValidityInterval(Integer.parseInt(xmlDocument.getFirstChild().getAttributes()
							.getNamedItem("maxValidityInterval").getNodeValue()));
					trustRelationship.getMetadataFilters().put("requiredValidUntil", filter);
					continue;
				}

				if (xmlDocument.getFirstChild().getAttributes().getNamedItem("xsi:type").getNodeValue().equals(SIGNATURE_VALIDATION_TYPE)) {
					MetadataFilter filter = createMetadataFilter("signatureValidation");
					filter.setFilterCertFileName(StringHelper.removePunctuation(trustRelationship.getInum()));
					trustRelationship.getMetadataFilters().put("signatureValidation", filter);
					continue;
				}

			}
		}
	}
}
