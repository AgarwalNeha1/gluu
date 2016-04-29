/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxtrust.action;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.beanutils.BeanUtils;
import org.gluu.oxtrust.ldap.service.JsonConfigurationService;
import org.gluu.oxtrust.util.OxTrustConstants;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.security.Restrict;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage.Severity;
import org.jboss.seam.international.StatusMessages;
import org.jboss.seam.log.Log;
import org.xdi.config.oxtrust.ApplicationConfiguration;
import org.xdi.config.oxtrust.ImportPersonConfig;
import org.xdi.service.JsonService;
import org.xdi.util.StringHelper;
import org.xdi.util.security.StringEncrypter;
import org.xdi.util.security.StringEncrypter.EncryptionException;

/**
 * Action class for json configuring This class loads the JSON configurations
 * e.g. oxTrustConfig from OpenDJ and serves to front end
 * (configuration/update.xhtml) front end uses this JSON String to render JSON
 * editor When edited JSON is submitted back This action class will take care of
 * saving the edited JSON back to OpenDJ
 * 
 * @author Rahat Ali Date: 12/04/2015
 * @author Yuriy Movchan Date: 10/23/2015
 */
@Name("jsonConfigAction")
@Scope(ScopeType.CONVERSATION)
@Restrict("#{identity.loggedIn}")
public class JsonConfigurationAction implements Serializable {

	private static final String HIDDEN_PASSWORD_TEXT = "hidden";

	private static final long serialVersionUID = -4470460481895022468L;

	@In
	private StatusMessages statusMessages;

	@In
	private FacesMessages facesMessages;

	@Logger
	private Log log;

	@In
	private JsonService jsonService;

	@In
	private JsonConfigurationService jsonConfigurationService;
	
	@In(value = "#{oxTrustConfiguration.cryptoConfigurationSalt}")
	private String cryptoConfigurationSalt;

	private ApplicationConfiguration oxTrustApplicationConfiguration;
	private ImportPersonConfig oxTrustImportPersonConfiguration;

	private String oxTrustConfigJson;
	private String oxTrustImportPersonConfigJson;

	private String oxAuthDynamicConfigJson;

	@Restrict("#{s:hasPermission('configuration', 'access')}")
	public String init() {
		try {
			log.debug("Loading oxauth-config.json and oxtrust-config.json");
			this.oxTrustApplicationConfiguration = jsonConfigurationService.getOxTrustApplicationConfiguration();
			this.oxTrustImportPersonConfiguration = jsonConfigurationService.getOxTrustImportPersonConfiguration();

			this.oxTrustConfigJson = getProtectedOxTrustApplicationConfiguration(this.oxTrustApplicationConfiguration);
			this.oxTrustImportPersonConfigJson = getOxTrustImportPersonConfiguration(this.oxTrustImportPersonConfiguration);
			this.oxAuthDynamicConfigJson = jsonConfigurationService.getOxAuthDynamicConfigJson();

			if ((this.oxTrustConfigJson == null) || (this.oxAuthDynamicConfigJson == null)) {
				return OxTrustConstants.RESULT_FAILURE;
			}

			return OxTrustConstants.RESULT_SUCCESS;
		} catch (Exception ex) {
			log.error("Failed to load configuration from LDAP", ex);
			facesMessages.add(Severity.ERROR, "Failed to load configuration from LDAP");
		}

		return OxTrustConstants.RESULT_FAILURE;
	}

	@Restrict("#{s:hasPermission('configuration', 'access')}")
	public String saveOxAuthDynamicConfigJson() {
		// Update JSON configurations
		try {
			log.debug("Saving oxauth-config.json:" + oxAuthDynamicConfigJson);
			jsonConfigurationService.saveOxAuthDynamicConfigJson(oxAuthDynamicConfigJson);

			facesMessages.add(Severity.INFO, "oxAuthDynamic Configuration is updated.");

			return OxTrustConstants.RESULT_SUCCESS;
		} catch (Exception ex) {
			log.error("Failed to update oxauth-config.json", ex);
			facesMessages.add(Severity.ERROR, "Failed to update oxAuth configuration in LDAP");
		}

		return OxTrustConstants.RESULT_FAILURE;
	}

	@Restrict("#{s:hasPermission('configuration', 'access')}")
	public String saveOxTrustConfigJson() {
		// Update JSON configurations
		try {
			log.debug("Saving oxtrust-config.json:" + this.oxTrustConfigJson);
			this.oxTrustApplicationConfiguration = convertToOxTrustApplicationConfiguration(this.oxTrustConfigJson);
			jsonConfigurationService.saveOxTrustApplicationConfiguration(this.oxTrustApplicationConfiguration);
			facesMessages.add(Severity.INFO, "oxTrust Configuration is updated.");

			return OxTrustConstants.RESULT_SUCCESS;
		} catch (Exception ex) {
			log.error("Failed to update oxtrust-config.json", ex);
			facesMessages.add(Severity.ERROR, "Failed to update oxTrust configuration in LDAP");
		}

		return OxTrustConstants.RESULT_FAILURE;
	}

	@Restrict("#{s:hasPermission('configuration', 'access')}")
	public String saveOxTrustImportPersonConfigJson() {
		// Update JSON configurations
		try {
			log.debug("Saving oxtrust-import-person.json:" + this.oxTrustImportPersonConfigJson);
			this.oxTrustImportPersonConfiguration = convertToOxTrustImportPersonConfiguration(this.oxTrustImportPersonConfigJson);
			jsonConfigurationService.saveOxTrustImportPersonConfiguration(this.oxTrustImportPersonConfiguration);
			facesMessages.add(Severity.INFO, "oxTrust Import Person Configuration is updated.");

			return OxTrustConstants.RESULT_SUCCESS;
		} catch (Exception ex) {
			log.error("Failed to oxtrust-import-person.json", ex);
			facesMessages.add(Severity.ERROR, "Failed to update oxTrust Import Person configuration in LDAP");
		}

		return OxTrustConstants.RESULT_FAILURE;
	}

	private String getProtectedOxTrustApplicationConfiguration(ApplicationConfiguration oxTrustApplicationConfiguration) {
		try {
			ApplicationConfiguration resultOxTrustApplicationConfiguration = (ApplicationConfiguration) BeanUtils.cloneBean(oxTrustApplicationConfiguration);

			resultOxTrustApplicationConfiguration.setSvnConfigurationStorePassword(HIDDEN_PASSWORD_TEXT);
			resultOxTrustApplicationConfiguration.setKeystorePassword(HIDDEN_PASSWORD_TEXT);
			resultOxTrustApplicationConfiguration.setIdpSecurityKeyPassword(HIDDEN_PASSWORD_TEXT);
			resultOxTrustApplicationConfiguration.setIdpBindPassword(HIDDEN_PASSWORD_TEXT);
			resultOxTrustApplicationConfiguration.setMysqlPassword(HIDDEN_PASSWORD_TEXT);
			resultOxTrustApplicationConfiguration.setCaCertsPassphrase(HIDDEN_PASSWORD_TEXT);
			resultOxTrustApplicationConfiguration.setOxAuthClientPassword(HIDDEN_PASSWORD_TEXT);

			return jsonService.objectToJson(resultOxTrustApplicationConfiguration);
		} catch (Exception ex) {
			log.error("Failed to prepare JSON from ApplicationConfiguration: '{0}'", ex, oxTrustApplicationConfiguration);
		}

		return null;
	}

	private String getOxTrustImportPersonConfiguration(ImportPersonConfig oxTrustImportPersonConfiguration) {
		try {
			return jsonService.objectToJson(oxTrustImportPersonConfiguration);
		} catch (Exception ex) {
			log.error("Failed to prepare JSON from ImportPersonConfig: '{0}'", ex, oxTrustImportPersonConfiguration);
		}

		return null;
	}

	private ApplicationConfiguration convertToOxTrustApplicationConfiguration(String oxTrustApplicationConfigurationJson) {
		try {
			ApplicationConfiguration resultOxTrustApplicationConfiguration = jsonService.jsonToObject(oxTrustApplicationConfigurationJson, ApplicationConfiguration.class);

			processPasswordProperty(this.oxTrustApplicationConfiguration, resultOxTrustApplicationConfiguration, "svnConfigurationStorePassword");
			processPasswordProperty(this.oxTrustApplicationConfiguration, resultOxTrustApplicationConfiguration, "keystorePassword");
			processPasswordProperty(this.oxTrustApplicationConfiguration, resultOxTrustApplicationConfiguration, "idpSecurityKeyPassword");
			processPasswordProperty(this.oxTrustApplicationConfiguration, resultOxTrustApplicationConfiguration, "idpBindPassword");
			processPasswordProperty(this.oxTrustApplicationConfiguration, resultOxTrustApplicationConfiguration, "mysqlPassword");
			processPasswordProperty(this.oxTrustApplicationConfiguration, resultOxTrustApplicationConfiguration, "caCertsPassphrase");
			processPasswordProperty(this.oxTrustApplicationConfiguration, resultOxTrustApplicationConfiguration, "oxAuthClientPassword");

			jsonConfigurationService.processScimTestModeIsTrue(this.oxTrustApplicationConfiguration, resultOxTrustApplicationConfiguration);

			return resultOxTrustApplicationConfiguration;
		} catch (Exception ex) {
			log.error("Failed to prepare ApplicationConfiguration from JSON: '{0}'", ex, oxTrustApplicationConfigurationJson);
		}

		return null;
	}

	private ImportPersonConfig convertToOxTrustImportPersonConfiguration(String oxTrustImportPersonConfigJson) {
		try {
			ImportPersonConfig resultOxTrustImportPersonConfiguration = jsonService.jsonToObject(oxTrustImportPersonConfigJson, ImportPersonConfig.class);

			return resultOxTrustImportPersonConfiguration;
		} catch (Exception ex) {
			log.error("Failed to prepare ImportPersonConfig from JSON: '{0}'", ex, oxTrustImportPersonConfigJson);
		}

		return null;
	}

	private void processPasswordProperty(ApplicationConfiguration source, ApplicationConfiguration current, String property) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, EncryptionException {
		String currentValue = BeanUtils.getProperty(current, property);
		if (StringHelper.equals(currentValue, HIDDEN_PASSWORD_TEXT)) {
			String sourceValue = BeanUtils.getSimpleProperty(source, property);
			BeanUtils.setProperty(current, property, sourceValue);
		} else {
			String currentValueEncrypted = StringEncrypter.defaultInstance().encrypt(currentValue, cryptoConfigurationSalt);
			BeanUtils.setProperty(current, property, currentValueEncrypted);
		}
	}

	public String getOxTrustConfigJson() {
		return oxTrustConfigJson;
	}

	public void setOxTrustConfigJson(String oxTrustConfigJson) {
		this.oxTrustConfigJson = oxTrustConfigJson;
	}

	public String getOxTrustImportPersonConfigJson() {
		return oxTrustImportPersonConfigJson;
	}

	public void setOxTrustImportPersonConfigJson(String oxTrustImportPersonConfigJson) {
		this.oxTrustImportPersonConfigJson = oxTrustImportPersonConfigJson;
	}

	public String getOxAuthDynamicConfigJson() {
		return oxAuthDynamicConfigJson;
	}

	public void setOxAuthDynamicConfigJson(String oxAuthDynamicConfigJson) {
		this.oxAuthDynamicConfigJson = oxAuthDynamicConfigJson;
	}
}
