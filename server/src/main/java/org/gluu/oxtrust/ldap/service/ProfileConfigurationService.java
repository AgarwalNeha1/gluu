package org.gluu.oxtrust.ldap.service;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.velocity.VelocityContext;
import org.gluu.oxtrust.model.GluuSAMLTrustRelationship;
import org.gluu.oxtrust.model.ProfileConfiguration;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xdi.util.StringHelper;
import org.xdi.util.io.FileUploadWrapper;
import org.xml.sax.SAXException;

/**
 * Provides operations with metadata filters
 * 
 */
@Scope(ScopeType.STATELESS)
@Name("profileConfigurationService")
@AutoCreate
public class ProfileConfigurationService {

	private static final String SHIBBOLETH_SSO = "ShibbolethSSO";
	private static final String SAML1_ARTIFACT_RESOLUTION = "SAML1ArtifactResolution";
	private static final String SAML1_ATTRIBUTE_QUERY = "SAML1AttributeQuery";
	private static final String SAML2_SSO = "SAML2SSO";
	private static final String SAML2_ARTIFACT_RESOLUTION = "SAML2ArtifactResolution";
	private static final String SAML2_ATTRIBUTE_QUERY = "SAML2AttributeQuery";

	@In
	private Shibboleth2ConfService shibboleth2ConfService;

	@In
	private TemplateService templateService;

	public List<ProfileConfiguration> getAvailableProfileConfigurations() {
		File profileConfigurationFolder = new File(System.getProperty("catalina.home") + File.separator + "conf" + File.separator
				+ "shibboleth2" + File.separator + "idp" + File.separator + "ProfileConfiguration");
		File[] profileConfigurationTemplates = null;
		if (profileConfigurationFolder.exists() && profileConfigurationFolder.isDirectory()) {
			profileConfigurationTemplates = profileConfigurationFolder.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.endsWith("ProfileConfiguration.xml.vm");
				}
			});
		}
		List<ProfileConfiguration> profileConfigurations = new ArrayList<ProfileConfiguration>();
		for (File profileConfigurationTemplate : profileConfigurationTemplates) {
			profileConfigurations.add(createProfileConfiguration(profileConfigurationTemplate.getName().split("ProfileConfiguration")[0]));
		}
		return profileConfigurations;
	}

	private ProfileConfiguration createProfileConfiguration(String profileConfigurationName) {
		ProfileConfiguration profileConfiguration = new ProfileConfiguration();
		profileConfiguration.setName(profileConfigurationName);
		if (SHIBBOLETH_SSO.equals(profileConfigurationName)) {
			profileConfiguration.setIncludeAttributeStatement(false);
			profileConfiguration.setAssertionLifetime(300000);
			profileConfiguration.setSignResponses("conditional");
			profileConfiguration.setSignAssertions("never");
			profileConfiguration.setSignRequests("conditional");
		}

		if (SAML1_ARTIFACT_RESOLUTION.equals(profileConfigurationName)) {
			profileConfiguration.setSignResponses("conditional");
			profileConfiguration.setSignAssertions("never");
			profileConfiguration.setSignRequests("conditional");
		}

		if (SAML1_ATTRIBUTE_QUERY.equals(profileConfigurationName)) {
			profileConfiguration.setAssertionLifetime(300000);
			profileConfiguration.setSignResponses("conditional");
			profileConfiguration.setSignAssertions("never");
			profileConfiguration.setSignRequests("conditional");
		}

		if (SAML2_SSO.equals(profileConfigurationName)) {
			profileConfiguration.setIncludeAttributeStatement(true);
			profileConfiguration.setAssertionLifetime(300000);
			profileConfiguration.setAssertionProxyCount(0);
			profileConfiguration.setSignResponses("conditional");
			profileConfiguration.setSignAssertions("never");
			profileConfiguration.setSignRequests("conditional");
			profileConfiguration.setEncryptAssertions("conditional");
			profileConfiguration.setEncryptNameIds("never");
		}

		if (SAML2_ARTIFACT_RESOLUTION.equals(profileConfigurationName)) {
			profileConfiguration.setSignResponses("conditional");
			profileConfiguration.setSignAssertions("never");
			profileConfiguration.setSignRequests("conditional");
			profileConfiguration.setEncryptAssertions("conditional");
			profileConfiguration.setEncryptNameIds("never");
		}

		if (SAML2_ATTRIBUTE_QUERY.equals(profileConfigurationName)) {
			profileConfiguration.setAssertionLifetime(300000);
			profileConfiguration.setAssertionProxyCount(0);
			profileConfiguration.setSignResponses("conditional");
			profileConfiguration.setSignAssertions("never");
			profileConfiguration.setSignRequests("conditional");
			profileConfiguration.setEncryptAssertions("conditional");
			profileConfiguration.setEncryptNameIds("never");
		}

		return profileConfiguration;
	}

	public List<ProfileConfiguration> getProfileConfigurationsList(GluuSAMLTrustRelationship trustRelationship) {
		List<ProfileConfiguration> profileConfigurations = new ArrayList<ProfileConfiguration>();
		for (String profileConfigurationName : trustRelationship.getProfileConfigurations().keySet()) {
			profileConfigurations.add(trustRelationship.getProfileConfigurations().get(profileConfigurationName));
		}
		return profileConfigurations;
	}

	public void parseProfileConfigurations(GluuSAMLTrustRelationship trustRelationship) throws SAXException, IOException,
			ParserConfigurationException, FactoryConfigurationError, XPathExpressionException {
		if (trustRelationship.getGluuProfileConfiguration() != null) {
			for (String profileConfigurationXML : trustRelationship.getGluuProfileConfiguration()) {
				Document xmlDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder()
						.parse(new java.io.ByteArrayInputStream(profileConfigurationXML.getBytes()));
				if (xmlDocument.getFirstChild().getAttributes().getNamedItem("xsi:type").getNodeValue().contains(SHIBBOLETH_SSO)) {
					ProfileConfiguration profileConfiguration = createProfileConfiguration(SHIBBOLETH_SSO);

					profileConfiguration.setIncludeAttributeStatement(Boolean.parseBoolean(xmlDocument.getFirstChild().getAttributes()
							.getNamedItem("includeAttributeStatement").getNodeValue()));
					profileConfiguration.setAssertionLifetime(Integer.parseInt(xmlDocument.getFirstChild().getAttributes()
							.getNamedItem("assertionLifetime").getNodeValue()));
					profileConfiguration.setSignResponses(xmlDocument.getFirstChild().getAttributes().getNamedItem("signResponses")
							.getNodeValue());
					profileConfiguration.setSignAssertions(xmlDocument.getFirstChild().getAttributes().getNamedItem("signAssertions")
							.getNodeValue());
					profileConfiguration.setSignRequests(xmlDocument.getFirstChild().getAttributes().getNamedItem("signRequests")
							.getNodeValue());
					Node attribute = xmlDocument.getFirstChild().getAttributes().getNamedItem("signingCredentialRef");
					if (attribute != null) {
						profileConfiguration.setProfileConfigurationCertFileName(attribute.getNodeValue());
					}

					trustRelationship.getProfileConfigurations().put(SHIBBOLETH_SSO, profileConfiguration);
					continue;
				}

				if (xmlDocument.getFirstChild().getAttributes().getNamedItem("xsi:type").getNodeValue().contains(SAML1_ARTIFACT_RESOLUTION)) {
					ProfileConfiguration profileConfiguration = createProfileConfiguration(SAML1_ARTIFACT_RESOLUTION);

					profileConfiguration.setSignResponses(xmlDocument.getFirstChild().getAttributes().getNamedItem("signResponses")
							.getNodeValue());
					profileConfiguration.setSignAssertions(xmlDocument.getFirstChild().getAttributes().getNamedItem("signAssertions")
							.getNodeValue());
					profileConfiguration.setSignRequests(xmlDocument.getFirstChild().getAttributes().getNamedItem("signRequests")
							.getNodeValue());
					Node attribute = xmlDocument.getFirstChild().getAttributes().getNamedItem("signingCredentialRef");
					if (attribute != null) {
						profileConfiguration.setProfileConfigurationCertFileName(attribute.getNodeValue());
					}

					trustRelationship.getProfileConfigurations().put(SAML1_ARTIFACT_RESOLUTION, profileConfiguration);
					continue;
				}

				if (xmlDocument.getFirstChild().getAttributes().getNamedItem("xsi:type").getNodeValue().contains(SAML1_ATTRIBUTE_QUERY)) {
					ProfileConfiguration profileConfiguration = createProfileConfiguration(SAML1_ATTRIBUTE_QUERY);

					profileConfiguration.setAssertionLifetime(Integer.parseInt(xmlDocument.getFirstChild().getAttributes()
							.getNamedItem("assertionLifetime").getNodeValue()));
					profileConfiguration.setSignResponses(xmlDocument.getFirstChild().getAttributes().getNamedItem("signResponses")
							.getNodeValue());
					profileConfiguration.setSignAssertions(xmlDocument.getFirstChild().getAttributes().getNamedItem("signAssertions")
							.getNodeValue());
					profileConfiguration.setSignRequests(xmlDocument.getFirstChild().getAttributes().getNamedItem("signRequests")
							.getNodeValue());
					Node attribute = xmlDocument.getFirstChild().getAttributes().getNamedItem("signingCredentialRef");
					if (attribute != null) {
						profileConfiguration.setProfileConfigurationCertFileName(attribute.getNodeValue());
					}

					trustRelationship.getProfileConfigurations().put(SAML1_ATTRIBUTE_QUERY, profileConfiguration);
					continue;
				}

				if (xmlDocument.getFirstChild().getAttributes().getNamedItem("xsi:type").getNodeValue().contains(SAML2_SSO)) {
					ProfileConfiguration profileConfiguration = createProfileConfiguration(SAML2_SSO);

					profileConfiguration.setIncludeAttributeStatement(Boolean.parseBoolean(xmlDocument.getFirstChild().getAttributes()
							.getNamedItem("includeAttributeStatement").getNodeValue()));
					profileConfiguration.setAssertionLifetime(Integer.parseInt(xmlDocument.getFirstChild().getAttributes()
							.getNamedItem("assertionLifetime").getNodeValue()));
					profileConfiguration.setAssertionProxyCount(Integer.parseInt(xmlDocument.getFirstChild().getAttributes()
							.getNamedItem("assertionProxyCount").getNodeValue()));
					profileConfiguration.setSignResponses(xmlDocument.getFirstChild().getAttributes().getNamedItem("signResponses")
							.getNodeValue());
					profileConfiguration.setSignAssertions(xmlDocument.getFirstChild().getAttributes().getNamedItem("signAssertions")
							.getNodeValue());
					profileConfiguration.setSignRequests(xmlDocument.getFirstChild().getAttributes().getNamedItem("signRequests")
							.getNodeValue());
					profileConfiguration.setEncryptAssertions(xmlDocument.getFirstChild().getAttributes().getNamedItem("encryptAssertions")
							.getNodeValue());
					profileConfiguration.setEncryptNameIds(xmlDocument.getFirstChild().getAttributes().getNamedItem("encryptNameIds")
							.getNodeValue());
					Node attribute = xmlDocument.getFirstChild().getAttributes().getNamedItem("signingCredentialRef");
					if (attribute != null) {
						profileConfiguration.setProfileConfigurationCertFileName(attribute.getNodeValue());
					}

					trustRelationship.getProfileConfigurations().put(SAML2_SSO, profileConfiguration);
					continue;
				}

				if (xmlDocument.getFirstChild().getAttributes().getNamedItem("xsi:type").getNodeValue().contains(SAML2_ARTIFACT_RESOLUTION)) {
					ProfileConfiguration profileConfiguration = createProfileConfiguration(SAML2_ARTIFACT_RESOLUTION);

					profileConfiguration.setSignResponses(xmlDocument.getFirstChild().getAttributes().getNamedItem("signResponses")
							.getNodeValue());
					profileConfiguration.setSignAssertions(xmlDocument.getFirstChild().getAttributes().getNamedItem("signAssertions")
							.getNodeValue());
					profileConfiguration.setSignRequests(xmlDocument.getFirstChild().getAttributes().getNamedItem("signRequests")
							.getNodeValue());
					profileConfiguration.setEncryptAssertions(xmlDocument.getFirstChild().getAttributes().getNamedItem("encryptNameIds")
							.getNodeValue());
					profileConfiguration.setEncryptNameIds(xmlDocument.getFirstChild().getAttributes().getNamedItem("encryptNameIds")
							.getNodeValue());
					Node attribute = xmlDocument.getFirstChild().getAttributes().getNamedItem("signingCredentialRef");
					if (attribute != null) {
						profileConfiguration.setProfileConfigurationCertFileName(attribute.getNodeValue());
					}

					trustRelationship.getProfileConfigurations().put(SAML2_ARTIFACT_RESOLUTION, profileConfiguration);
					continue;
				}

				if (xmlDocument.getFirstChild().getAttributes().getNamedItem("xsi:type").getNodeValue().contains(SAML2_ATTRIBUTE_QUERY)) {
					ProfileConfiguration profileConfiguration = createProfileConfiguration(SAML2_ATTRIBUTE_QUERY);

					profileConfiguration.setAssertionLifetime(Integer.parseInt(xmlDocument.getFirstChild().getAttributes()
							.getNamedItem("assertionLifetime").getNodeValue()));
					profileConfiguration.setAssertionProxyCount(Integer.parseInt(xmlDocument.getFirstChild().getAttributes()
							.getNamedItem("assertionProxyCount").getNodeValue()));
					profileConfiguration.setSignResponses(xmlDocument.getFirstChild().getAttributes().getNamedItem("signResponses")
							.getNodeValue());
					profileConfiguration.setSignAssertions(xmlDocument.getFirstChild().getAttributes().getNamedItem("signAssertions")
							.getNodeValue());
					profileConfiguration.setSignRequests(xmlDocument.getFirstChild().getAttributes().getNamedItem("signRequests")
							.getNodeValue());
					profileConfiguration.setEncryptAssertions(xmlDocument.getFirstChild().getAttributes().getNamedItem("encryptNameIds")
							.getNodeValue());
					profileConfiguration.setEncryptNameIds(xmlDocument.getFirstChild().getAttributes().getNamedItem("encryptNameIds")
							.getNodeValue());
					Node attribute = xmlDocument.getFirstChild().getAttributes().getNamedItem("signingCredentialRef");
					if (attribute != null) {
						profileConfiguration.setProfileConfigurationCertFileName(attribute.getNodeValue());
					}

					trustRelationship.getProfileConfigurations().put(SAML2_ATTRIBUTE_QUERY, profileConfiguration);
					continue;
				}

			}
		}

	}

	public boolean isProfileConfigurationPresent(GluuSAMLTrustRelationship trustRelationship, ProfileConfiguration profileConfiguration) {
		if(trustRelationship.getProfileConfigurations().keySet().contains(profileConfiguration.getName())){
			ProfileConfiguration storedConfiguration = trustRelationship.getProfileConfigurations().get(profileConfiguration.getName());
			return profileConfiguration.equals(storedConfiguration);
		}
		return false;
	}

	public void updateProfileConfiguration(GluuSAMLTrustRelationship trustRelationship, ProfileConfiguration profileConfiguration) {
		trustRelationship.getProfileConfigurations().put(profileConfiguration.getName(), profileConfiguration);

	}

	public void removeProfileConfiguration(GluuSAMLTrustRelationship trustRelationship, ProfileConfiguration profileConfiguration) {
		trustRelationship.getProfileConfigurations().remove(profileConfiguration.getName());

	}

	public static ProfileConfigurationService instance() {
		return (ProfileConfigurationService) Component.getInstance(ProfileConfigurationService.class);
	}

	public void saveProfileConfigurations(GluuSAMLTrustRelationship trustRelationship, Map<String, FileUploadWrapper> fileWrappers) {
		VelocityContext context = new VelocityContext();

		if (trustRelationship.getProfileConfigurations().get(SHIBBOLETH_SSO) != null) {
			ProfileConfiguration profileConfiguration = trustRelationship.getProfileConfigurations().get(SHIBBOLETH_SSO);
			context.put(SHIBBOLETH_SSO + "IncludeAttributeStatement", profileConfiguration.isIncludeAttributeStatement());
			context.put(SHIBBOLETH_SSO + "AssertionLifetime", profileConfiguration.getAssertionLifetime());
			context.put(SHIBBOLETH_SSO + "SignResponses", profileConfiguration.getSignResponses());
			context.put(SHIBBOLETH_SSO + "SignAssertions", profileConfiguration.getSignAssertions());
			context.put(SHIBBOLETH_SSO + "SignRequests", profileConfiguration.getSignRequests());

			saveCertificate(trustRelationship, fileWrappers, SHIBBOLETH_SSO);
			String certName = trustRelationship.getProfileConfigurations().get(SHIBBOLETH_SSO).getProfileConfigurationCertFileName();
			if (StringHelper.isNotEmpty(certName)) {
				context.put(SHIBBOLETH_SSO + "SigningCredentialRef", certName);
			}
		}

		if (trustRelationship.getProfileConfigurations().get(SAML1_ARTIFACT_RESOLUTION) != null) {
			ProfileConfiguration profileConfiguration = trustRelationship.getProfileConfigurations().get(SAML1_ARTIFACT_RESOLUTION);
			context.put(SAML1_ARTIFACT_RESOLUTION + "SignResponses", profileConfiguration.getSignResponses());
			context.put(SAML1_ARTIFACT_RESOLUTION + "SignAssertions", profileConfiguration.getSignAssertions());
			context.put(SAML1_ARTIFACT_RESOLUTION + "SignRequests", profileConfiguration.getSignRequests());
			saveCertificate(trustRelationship, fileWrappers, SAML1_ARTIFACT_RESOLUTION);
			String certName = trustRelationship.getProfileConfigurations().get(SAML1_ARTIFACT_RESOLUTION)
					.getProfileConfigurationCertFileName();
			if (StringHelper.isNotEmpty(certName)) {
				context.put(SAML1_ARTIFACT_RESOLUTION + "SigningCredentialRef", certName);
			}
		}

		if (trustRelationship.getProfileConfigurations().get(SAML1_ATTRIBUTE_QUERY) != null) {
			ProfileConfiguration profileConfiguration = trustRelationship.getProfileConfigurations().get(SAML1_ATTRIBUTE_QUERY);
			context.put(SAML1_ATTRIBUTE_QUERY + "AssertionLifetime", profileConfiguration.getAssertionLifetime());
			context.put(SAML1_ATTRIBUTE_QUERY + "SignResponses", profileConfiguration.getSignResponses());
			context.put(SAML1_ATTRIBUTE_QUERY + "SignAssertions", profileConfiguration.getSignAssertions());
			context.put(SAML1_ATTRIBUTE_QUERY + "SignRequests", profileConfiguration.getSignRequests());
			saveCertificate(trustRelationship, fileWrappers, SAML1_ATTRIBUTE_QUERY);
			String certName = trustRelationship.getProfileConfigurations().get(SAML1_ATTRIBUTE_QUERY).getProfileConfigurationCertFileName();
			if (StringHelper.isNotEmpty(certName)) {
				context.put(SAML1_ATTRIBUTE_QUERY + "SigningCredentialRef", certName);
			}
		}

		if (trustRelationship.getProfileConfigurations().get(SAML2_SSO) != null) {
			ProfileConfiguration profileConfiguration = trustRelationship.getProfileConfigurations().get(SAML2_SSO);
			context.put(SAML2_SSO + "IncludeAttributeStatement", profileConfiguration.isIncludeAttributeStatement());
			context.put(SAML2_SSO + "AssertionLifetime", profileConfiguration.getAssertionLifetime());
			context.put(SAML2_SSO + "AssertionProxyCount", profileConfiguration.getAssertionProxyCount());
			context.put(SAML2_SSO + "SignResponses", profileConfiguration.getSignResponses());
			context.put(SAML2_SSO + "SignAssertions", profileConfiguration.getSignAssertions());
			context.put(SAML2_SSO + "SignRequests", profileConfiguration.getSignRequests());
			context.put(SAML2_SSO + "EncryptNameIds", profileConfiguration.getEncryptNameIds());
			context.put(SAML2_SSO + "EncryptAssertions", profileConfiguration.getEncryptAssertions());
			saveCertificate(trustRelationship, fileWrappers, SAML2_SSO);
			String certName = trustRelationship.getProfileConfigurations().get(SAML2_SSO).getProfileConfigurationCertFileName();
			if (StringHelper.isNotEmpty(certName)) {
				context.put(SAML2_SSO + "SigningCredentialRef", certName);
			}
		}

		if (trustRelationship.getProfileConfigurations().get(SAML2_ARTIFACT_RESOLUTION) != null) {
			ProfileConfiguration profileConfiguration = trustRelationship.getProfileConfigurations().get(SAML2_ARTIFACT_RESOLUTION);
			context.put(SAML2_ARTIFACT_RESOLUTION + "SignResponses", profileConfiguration.getSignResponses());
			context.put(SAML2_ARTIFACT_RESOLUTION + "SignAssertions", profileConfiguration.getSignAssertions());
			context.put(SAML2_ARTIFACT_RESOLUTION + "SignRequests", profileConfiguration.getSignRequests());
			context.put(SAML2_ARTIFACT_RESOLUTION + "EncryptAssertions", profileConfiguration.getEncryptAssertions());
			context.put(SAML2_ARTIFACT_RESOLUTION + "EncryptNameIds", profileConfiguration.getEncryptNameIds());
			saveCertificate(trustRelationship, fileWrappers, SAML2_ARTIFACT_RESOLUTION);
			String certName = trustRelationship.getProfileConfigurations().get(SAML2_ARTIFACT_RESOLUTION)
					.getProfileConfigurationCertFileName();
			if (StringHelper.isNotEmpty(certName)) {
				context.put(SAML2_ARTIFACT_RESOLUTION + "SigningCredentialRef", certName);
			}
		}

		if (trustRelationship.getProfileConfigurations().get(SAML2_ATTRIBUTE_QUERY) != null) {
			ProfileConfiguration profileConfiguration = trustRelationship.getProfileConfigurations().get(SAML2_ATTRIBUTE_QUERY);
			context.put(SAML2_ATTRIBUTE_QUERY + "AssertionLifetime", profileConfiguration.getAssertionLifetime());
			context.put(SAML2_ATTRIBUTE_QUERY + "AssertionProxyCount", profileConfiguration.getAssertionProxyCount());
			context.put(SAML2_ATTRIBUTE_QUERY + "SignResponses", profileConfiguration.getSignResponses());
			context.put(SAML2_ATTRIBUTE_QUERY + "SignAssertions", profileConfiguration.getSignAssertions());
			context.put(SAML2_ATTRIBUTE_QUERY + "SignRequests", profileConfiguration.getSignRequests());
			context.put(SAML2_ATTRIBUTE_QUERY + "EncryptAssertions", profileConfiguration.getEncryptAssertions());
			context.put(SAML2_ATTRIBUTE_QUERY + "EncryptNameIds", profileConfiguration.getEncryptNameIds());
			saveCertificate(trustRelationship, fileWrappers, SAML2_ATTRIBUTE_QUERY);
			String certName = trustRelationship.getProfileConfigurations().get(SAML2_ATTRIBUTE_QUERY).getProfileConfigurationCertFileName();
			if (StringHelper.isNotEmpty(certName)) {
				context.put(SAML2_ATTRIBUTE_QUERY + "SigningCredentialRef", certName);
			}
		}
		
		if(! trustRelationship.getProfileConfigurations().isEmpty()){
			trustRelationship.setGluuProfileConfiguration(new ArrayList<String>());
	
			for (String profileConfigurationName : trustRelationship.getProfileConfigurations().keySet()) {
				trustRelationship.getGluuProfileConfiguration().add(
						templateService.generateConfFile(profileConfigurationName + "ProfileConfiguration.xml", context));
			}
		}else{
			trustRelationship.setGluuProfileConfiguration(null);
		}

	}

	private void saveCertificate(GluuSAMLTrustRelationship trustRelationship, Map<String, FileUploadWrapper> fileWrappers, String name) {
		if (fileWrappers.get(name) != null && fileWrappers.get(name).getStream() != null) {
			String profileConfigurationCertFileName = StringHelper.removePunctuation(name + trustRelationship.getInum());
			shibboleth2ConfService.saveProfileConfigurationCert(profileConfigurationCertFileName, fileWrappers.get(name).getStream());
			trustRelationship.getProfileConfigurations().get(name)
					.setProfileConfigurationCertFileName(StringHelper.removePunctuation(profileConfigurationCertFileName));
		}

	}

}
