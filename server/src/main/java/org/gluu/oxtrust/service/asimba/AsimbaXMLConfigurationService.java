/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */

package org.gluu.oxtrust.service.asimba;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import org.gluu.oxtrust.util.OxTrustConstants;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;
import org.richfaces.model.UploadedFile;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import org.gluu.asimba.util.ldap.LDAPUtility;
import org.gluu.oxtrust.ldap.service.SSLService;
import org.gluu.oxtrust.util.KeystoreWrapper;
import org.gluu.oxtrust.util.Utils;
import org.jboss.seam.annotations.In;
import org.w3c.dom.Document;

/**
 * Asimba XML configuration service.
 * 
 * @author Dmitry Ognyannikov, 2016
 */
@Name("asimbaXMLConfigurationService")
@AutoCreate
@Scope(ScopeType.APPLICATION)
public class AsimbaXMLConfigurationService implements Serializable {
    
    /** Name of the file that contains property-list for configuring server */
    private static final String PROPERTIES_FILENAME = "asimba.properties";

    /** Name of the system property that specified the asimba.properties file location */
    private static final String PROPERTIES_FILENAME_PROPERTY = "asimba.properties.file";
    
    private static final String ASIMBA_XML_CONFIGURATION_PATH = "webapps/asimba/WEB-INF/conf/asimba.xml";
    
    @Logger
    private Log log;
    
    @In
    private SSLService sslService;
    
    private String keystoreFilePath;
    private String keystoreType;
    private String keystorePassword;
    private String asimbaAias;
    private String asimbaAiasPassword;
    
    @Create
    public void init() {
        parse();
    }
    
    /**
     * Return Asimba XML configuration file path.
     */
    private String getConfigurationFilePath() {
        String basePath = LDAPUtility.getBaseDirectory();
        
        StringBuilder configFile = new StringBuilder(basePath);
        if (!configFile.toString().endsWith(File.separator))
            configFile.append(File.separator);
        configFile.append(ASIMBA_XML_CONFIGURATION_PATH.replaceAll("/", File.separator));
        return configFile.toString();
    }
    
    /**
     * Parse Asimba XML configuration file.
     */
    private void parse() {
        try {
            // parse XML
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(getConfigurationFilePath()));
            XPath xPath = XPathFactory.newInstance().newXPath();
            keystoreFilePath = xPath.evaluate("/asimba-server/crypto/signing/signingfactory/keystore/file", document);
            log.info("AsimbaXMLConfig keystoreFilePath: " + keystoreFilePath);
            keystoreType = xPath.evaluate("/asimba-server/crypto/signing/signingfactory/keystore/type", document);
            if (keystoreType == null || "".equals(keystoreType))
                keystoreType = KeyStore.getDefaultType();
            log.info("AsimbaXMLConfig keystoreType: " + keystoreType);
            keystorePassword = xPath.evaluate("/asimba-server/crypto/signing/signingfactory/keystore/keystore_password", document);
            asimbaAias = xPath.evaluate("/asimba-server/crypto/signing/signingfactory/keystore/alias", document);
            asimbaAiasPassword = xPath.evaluate("/asimba-server/crypto/signing/signingfactory/keystore/password", document);
        } catch (Exception e) {
            log.error("parse() exception", e);
            keystoreFilePath = null;
            keystoreType = null;
            asimbaAias = null;
            asimbaAiasPassword = null;
        }
    }
    
    /**
     * Add trust certificate file to Asimba's Keystore.
     * 
     * @param uploadedFile Certificate file 
     * @param alias Certificate alias 
     * @return path
     * @throws IOException 
     */
    public synchronized String addCertificateFile(UploadedFile uploadedFile, String alias) throws IOException {
        byte[] certsBytes = Utils.readFully(uploadedFile.getInputStream());
        return addCertificateFile(certsBytes, alias);
    }
    
    /**
     * Add trust certificate file to Asimba's Keystore.
     * 
     * @param certsBytes Certificates as byte array
     * @param alias Certificate alias 
     * @return path
     * @throws IOException 
     */
    public synchronized String addCertificateFile(byte[] certsBytes, String alias) throws IOException {
        // load certificate
        X509Certificate certs[] = null;
        try {
            // load PEM certificate from uploadedFile 
            X509Certificate cert = sslService.getPEMCertificate(new ByteArrayInputStream(certsBytes));
            if (cert != null) {
                certs = new X509Certificate[1];
                certs[0] = cert;
            }
        } catch (Exception e) {
            log.warn("Certificate parsing exception", e);
        }
        
        if (certs == null) {
            // try load with other way (.crt certificates, base64 encoded, etc).
            try {
                certs = SSLService.loadCertificates(certsBytes);
            } catch (Exception e) {
                log.warn("Certificate parsing exception", e);
                return "Certificate parsing exception: " + e.getMessage();
            }
        }
        
        // update keystore
        try {
            parse();
            
            KeystoreWrapper wrapper = getKeystore();
            for (X509Certificate cert : certs) {
                wrapper.addCertificate(cert, alias);
                break;
            }
            wrapper.save();
            
            return OxTrustConstants.RESULT_SUCCESS;
        } catch (Exception e) {
            log.error("Add Certificate to keystore exception", e);
            return "Add Certificate to keystore exception : " + e.getMessage();
        }
    }
    
    public KeystoreWrapper getKeystore() throws Exception {
        return new KeystoreWrapper(keystoreFilePath, keystorePassword, keystoreType);
    }
}