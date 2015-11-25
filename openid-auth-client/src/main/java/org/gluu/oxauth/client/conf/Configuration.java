/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.client.conf;

import java.io.File;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.client.OpenIdClient;
import org.gluu.oxauth.client.exception.ConfigurationException;
import org.gluu.site.ldap.LDAPConnectionProvider;
import org.gluu.site.ldap.OperationsFacade;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.gluu.site.ldap.persistence.exception.LdapMappingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.util.StringHelper;
import org.xdi.util.properties.FileConfiguration;
import org.xdi.util.security.PropertiesDecrypter;

/**
 * Base OpenId configuration
 * 
 * @author Yuriy Movchan
 * @version 0.1, 11/02/2015
 */
public abstract class Configuration<C extends AppConfiguration, L extends LdapAppConfiguration> {

	private final Logger logger = LoggerFactory.getLogger(Configuration.class);

	static {
		if ((System.getProperty("catalina.base") != null) && (System.getProperty("catalina.base.ignore") == null)) {
			BASE_DIR = System.getProperty("catalina.base");
		} else if (System.getProperty("catalina.home") != null) {
			BASE_DIR = System.getProperty("catalina.home");
		} else if (System.getProperty("jboss.home.dir") != null) {
			BASE_DIR = System.getProperty("jboss.home.dir");
		} else {
			BASE_DIR = null;
		}
	}

	private static final String BASE_DIR;
	private static final String DIR = BASE_DIR + File.separator + "conf" + File.separator;

	private static final String SALT_FILE_NAME = "salt";

	private String confDir;
	private String saltFilePath;

	private FileConfiguration ldapConfiguration;
	private C appConfiguration;
	private OpenIdClient openIdClient;

	private String cryptoConfigurationSalt;

	private LdapEntryManager ldapEntryManager;

	private long ldapFileLastModifiedTime;

	private AtomicBoolean isActive;

	protected Configuration() {
		this.isActive = new AtomicBoolean(true);
		try {
			create();
		} finally {
			this.isActive.set(false);
		}
	}

	private void create() {
		this.ldapConfiguration = loadLdapConfiguration();

		this.confDir = confDir();
		this.saltFilePath = confDir + SALT_FILE_NAME;

		this.cryptoConfigurationSalt = loadCryptoConfigurationSalt();

		this.ldapEntryManager = createLdapEntryManager();

		if (!createFromLdap()) {
			logger.error("Failed to load configuration from Ldap. Please fix it!!!.");
			throw new ConfigurationException("Failed to load configuration from Ldap.");
		} else {
			logger.info("Configuration loaded successfully.");
		}

		this.openIdClient = createAuthClient();
	}

	public void destroy() {
		if (this.ldapEntryManager != null) {
			destroyLdapEntryManager(this.ldapEntryManager);
		}
	}

	private FileConfiguration loadLdapConfiguration() {
		String ldapConfigurationFileName = getLdapConfigurationFileName();
		try {
			if (StringHelper.isEmpty(ldapConfigurationFileName)) {
				throw new ConfigurationException("Failed to load Ldap configuration file!");
			}

			String ldapConfigurationFilePath = DIR + ldapConfigurationFileName;

			FileConfiguration ldapConfiguration = new FileConfiguration(ldapConfigurationFilePath);
			if (ldapConfiguration.isLoaded()) {
				File ldapFile = new File(ldapConfigurationFilePath);
				if (ldapFile.exists()) {
					this.ldapFileLastModifiedTime = ldapFile.lastModified();
				}
	
				return ldapConfiguration;
			}
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			throw new ConfigurationException("Failed to load Ldap configuration from " + ldapConfigurationFileName, ex);
		}

		throw new ConfigurationException("Failed to load Ldap configuration from " + ldapConfigurationFileName);
	}

	private String loadCryptoConfigurationSalt() {
		try {
			FileConfiguration cryptoConfiguration = new FileConfiguration(this.saltFilePath);

			return cryptoConfiguration.getString("encodeSalt");
		} catch (Exception ex) {
			logger.error("Failed to load configuration from {}", saltFilePath, ex);
			throw new ConfigurationException("Failed to load configuration from " + saltFilePath, ex);
		}
	}

	private String confDir() {
		final String confDir = getLdapConfiguration().getString("confDir");
		if (StringUtils.isNotBlank(confDir)) {
			return confDir;
		}

		return DIR;
	}

	private boolean createFromLdap() {
		logger.info("Loading configuration from Ldap...");
		try {
			final L ldapConf = loadConfigurationFromLdap();
			if (ldapConf != null) {
				this.appConfiguration = initAppConfiguration(ldapConf);
				return true;
			}
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}

		return false;
	}

	private L loadConfigurationFromLdap(String... returnAttributes) {
		try {
			final String dn = getLdapConfiguration().getString("configurationEntryDN");

			final L ldapConf = this.ldapEntryManager.find(getAppConfigurationType(), dn, returnAttributes);
			return ldapConf;
		} catch (LdapMappingException ex) {
			logger.error(ex.getMessage());
		}

		return null;
	}

	private LdapEntryManager createLdapEntryManager() {
		Properties connectionProperties = (Properties) this.ldapConfiguration.getProperties();
		Properties decryptedConnectionProperties = PropertiesDecrypter.decryptProperties(connectionProperties, this.cryptoConfigurationSalt);

		LDAPConnectionProvider connectionProvider = new LDAPConnectionProvider(decryptedConnectionProperties);
		LdapEntryManager ldapEntryManager = new LdapEntryManager(new OperationsFacade(connectionProvider, null));

		logger.debug("Created LdapEntryManager: {}", ldapEntryManager);

		return ldapEntryManager;
	}

	private void destroyLdapEntryManager(final LdapEntryManager ldapEntryManager) {
		boolean result = ldapEntryManager.destroy();
		if (result) {
			logger.debug("Destoyed LdapEntryManager: {}", ldapEntryManager);
		} else {
			logger.error("Failed to destoy LdapEntryManager: {}", ldapEntryManager);
		}
	}

	private OpenIdClient createAuthClient() {
		OpenIdClient openIdClient = new OpenIdClient(this.appConfiguration);
		openIdClient.init();

		return openIdClient;
	}

	public FileConfiguration getLdapConfiguration() {
		return ldapConfiguration;
	}

	public String getCryptoConfigurationSalt() {
		return cryptoConfigurationSalt;
	}

	public OpenIdClient getOpenIdClient() {
		return openIdClient;
	}

	protected abstract String getLdapConfigurationFileName();

	protected abstract Class<L> getAppConfigurationType();

	protected abstract C initAppConfiguration(L ldapAppConfiguarion);

}
