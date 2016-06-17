/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxtrust.action;

import org.apache.commons.lang.StringUtils;
import org.gluu.oxtrust.ldap.service.ClientService;
import org.gluu.oxtrust.ldap.service.ScopeService;
import org.gluu.oxtrust.model.GluuGroup;
import org.gluu.oxtrust.model.OxAuthClient;
import org.gluu.oxtrust.model.OxAuthScope;
import org.gluu.oxtrust.util.OxTrustConstants;
import org.gluu.site.ldap.persistence.exception.LdapMappingException;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.security.Restrict;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage.Severity;
import org.jboss.seam.log.Log;
import org.xdi.model.DisplayNameEntry;
import org.xdi.model.SelectableEntity;
import org.xdi.oxauth.model.common.GrantType;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.service.LookupService;
import org.xdi.util.StringHelper;
import org.xdi.util.Util;

import java.io.Serializable;
import java.util.*;

/**
 * Action class for viewing and updating clients.
 *
 * @author Reda Zerrad Date: 06.11.2012
 * @author Yuriy Movchan Date: 04/07/2014
 * @author Javier Rojas Blum
 * @version December 10, 2015
 */
@Scope(ScopeType.CONVERSATION)
@Name("updateClientAction")
@Restrict("#{identity.loggedIn}")
public class UpdateClientAction implements Serializable {

    private static final long serialVersionUID = -5756470620039988876L;

    @Logger
    private Log log;

    @In
    private ClientService clientService;

    @In
    private ScopeService scopeService;

    @In
    private LookupService lookupService;

    @In
    private FacesMessages facesMessages;

    private String inum;

    private boolean update;

    private OxAuthClient client;

    private List<String> loginUris;
    private List<String> logoutUris;
    private List<String> clientlogoutUris;
    

    private List<DisplayNameEntry> scopes;
    private List<ResponseType> responseTypes;
    private List<GrantType> grantTypes;
    private List<String> contacts;
    private List<String> defaultAcrValues;
    private List<String> requestUris;

    // @NotNull
    // @Size(min = 2, max = 30, message =
    // "Length of search string should be between 2 and 30")
    private String searchAvailableScopePattern;
    private String oldSearchAvailableScopePattern;

    private String availableLoginUri = "https://";
    private String availableLogoutUri = "https://";
    private String availableClientlogoutUri  = "https://";
    private String availableContact = "";
    private String availableDefaultAcrValue = "";
    private String availableRequestUri = "https://";

    private List<OxAuthScope> availableScopes;
    private List<GluuGroup> availableGroups;
    private List<SelectableEntity<ResponseType>> availableResponseTypes;
    private List<SelectableEntity<GrantType>> availableGrantTypes;

    @Restrict("#{s:hasPermission('client', 'access')}")
    public String add() throws Exception {
        if (this.client != null) {
            return OxTrustConstants.RESULT_SUCCESS;
        }

        this.update = false;
        this.client = new OxAuthClient();

        try {
            this.loginUris = getNonEmptyStringList(client.getOxAuthRedirectURIs());
            this.logoutUris = getNonEmptyStringList(client.getOxAuthPostLogoutRedirectURIs());
            this.clientlogoutUris = getNonEmptyStringList(client.getLogoutUri());
            this.scopes = getInitialScopeDisplayNameEntiries();
            this.responseTypes = getInitialResponseTypes();
            this.grantTypes = getInitialGrantTypes();
            this.contacts = getNonEmptyStringList(client.getContacts());
            this.defaultAcrValues = getNonEmptyStringList(client.getDefaultAcrValues());
            this.requestUris = getNonEmptyStringList(client.getRequestUris());
        } catch (LdapMappingException ex) {
            log.error("Failed to prepare lists", ex);

            return OxTrustConstants.RESULT_FAILURE;
        }

        return OxTrustConstants.RESULT_SUCCESS;
    }

    @Restrict("#{s:hasPermission('client', 'access')}")
    public String update() throws Exception {
        if (this.client != null) {
            return OxTrustConstants.RESULT_SUCCESS;
        }

        this.update = true;
        log.info("this.update : " + this.update);
        try {
            log.info("inum : " + inum);
            this.client = clientService.getClientByInum(inum);
        } catch (LdapMappingException ex) {
            log.error("Failed to find client {0}", ex, inum);
        }

        if (this.client == null) {
            log.error("Failed to load client {0}", inum);
            return OxTrustConstants.RESULT_FAILURE;
        }

        try {
            this.loginUris = getNonEmptyStringList(client.getOxAuthRedirectURIs());
            this.logoutUris = getNonEmptyStringList(client.getOxAuthPostLogoutRedirectURIs());
            this.clientlogoutUris = getNonEmptyStringList(client.getLogoutUri());
            this.scopes = getInitialScopeDisplayNameEntiries();
            this.responseTypes = getInitialResponseTypes();
            this.grantTypes = getInitialGrantTypes();
            this.contacts = getNonEmptyStringList(client.getContacts());
            this.defaultAcrValues = getNonEmptyStringList(client.getDefaultAcrValues());
            this.requestUris = getNonEmptyStringList(client.getRequestUris());
        } catch (LdapMappingException ex) {
            log.error("Failed to prepare lists", ex);
            return OxTrustConstants.RESULT_FAILURE;
        }

        return OxTrustConstants.RESULT_SUCCESS;
    }

    private List<String> getNonEmptyStringList(List<String> currentList) {
        if (currentList != null && currentList.size() > 0) {
            return new ArrayList<String>(currentList);
        } else {
            return new ArrayList<String>();
        }
    }

    private List<String> getNonEmptyStringList(String[] currentList) {
        if (currentList != null && currentList.length > 0) {
            return new ArrayList<String>(Arrays.asList(currentList));
        } else {
            return new ArrayList<String>();
        }
    }

    @Restrict("#{s:hasPermission('client', 'access')}")
    public void cancel() {
    }

    @Restrict("#{s:hasPermission('client', 'access')}")
    public String save() throws Exception {
        updateLoginURIs();
        updateLogoutURIs();
        updateClientLogoutURIs();
        updateScopes();
        updateResponseTypes();
        updateGrantTypes();
        updateContacts();
        updateDefaultAcrValues();
        updateRequestUris();
        
        if(this.client.getClientUri() != null){
        	this.client.setClientUri(this.client.getClientUri().trim());
        }
        
        if(this.client.getJwksUri() != null){
        	this.client.setJwksUri(this.client.getJwksUri().trim());
        }
        
        if(this.client.getLogoUri() != null){
        	this.client.setLogoUri(this.client.getLogoUri().trim());
        }
        
        if(this.client.getPolicyUri() != null){
        	this.client.setPolicyUri(this.client.getPolicyUri().trim());
        }
        
        if(this.client.getSectorIdentifierUri() != null){
        	this.client.setSectorIdentifierUri(this.client.getSectorIdentifierUri().trim());
        }
        
        if(this.client.getTosUri() != null){
        	this.client.setTosUri(this.client.getTosUri().trim());
        }
        
        if(this.client.getInitiateLoginUri()!= null){
        	this.client.setInitiateLoginUri(this.client.getInitiateLoginUri().trim());
        }
        
        if (update) {
            // Update client
            try {
                clientService.updateClient(this.client);
            } catch (LdapMappingException ex) {

                log.error("Failed to update client {0}", ex, this.inum);

                facesMessages.add(Severity.ERROR, "Failed to update client");
                return OxTrustConstants.RESULT_FAILURE;
            }
        } else {
            this.inum = clientService.generateInumForNewClient();
            String dn = clientService.getDnForClient(this.inum);

            // Save client
            this.client.setDn(dn);
            this.client.setInum(this.inum);
            try {
                clientService.addClient(this.client);
            } catch (LdapMappingException ex) {
                log.info("error saving client ");
                log.error("Failed to add new client {0}", ex, this.inum);

                facesMessages.add(Severity.ERROR, "Failed to add new client");
                return OxTrustConstants.RESULT_FAILURE;
            }

            this.update = true;
        }

        return OxTrustConstants.RESULT_SUCCESS;
    }

    @Restrict("#{s:hasPermission('client', 'access')}")
    public String delete() throws Exception {
        if (update) {
            // Remove client
            try {
                clientService.removeClient(this.client);
                return OxTrustConstants.RESULT_SUCCESS;
            } catch (LdapMappingException ex) {
                log.error("Failed to remove client {0}", ex, this.inum);
            }
        }

        return OxTrustConstants.RESULT_FAILURE;
    }

    @Restrict("#{s:hasPermission('client', 'access')}")
    public void removeLoginURI(String uri) {
        removeFromList(this.loginUris, uri);
    }

    @Restrict("#{s:hasPermission('client', 'access')}")
    public void removeLogoutURI(String uri) {
        removeFromList(this.logoutUris, uri);
    }

    @Restrict("#{s:hasPermission('client', 'access')}")
    public void removeClientLogoutURI(String uri) {
        removeFromList(this.clientlogoutUris, uri);
    }

    @Restrict("#{s:hasPermission('client', 'access')}")
    public void removeContact(String contact) {
        if (StringUtils.isEmpty(contact)) {
            return;
        }

        for (Iterator<String> iterator = contacts.iterator(); iterator.hasNext(); ) {
            String tmpContact = iterator.next();
            if (contact.equals(tmpContact)) {
                iterator.remove();
                break;
            }
        }
    }

    @Restrict("#{s:hasPermission('client', 'access')}")
    public void removeDefaultAcrValue(String defaultAcrValue) {
        if (StringUtils.isEmpty(defaultAcrValue)) {
            return;
        }

        for (Iterator<String> iterator = defaultAcrValues.iterator(); iterator.hasNext(); ) {
            String tmpDefaultAcrValue = iterator.next();
            if (defaultAcrValue.equals(tmpDefaultAcrValue)) {
                iterator.remove();
                break;
            }
        }
    }

    @Restrict("#{s:hasPermission('client', 'access')}")
    public void removeRequestUri(String requestUri) {
        if (StringUtils.isEmpty(requestUri)) {
            return;
        }

        for (Iterator<String> iterator = requestUris.iterator(); iterator.hasNext(); ) {
            String tmpRequestUri = iterator.next();
            if (requestUri.equals(tmpRequestUri)) {
                iterator.remove();
                break;
            }
        }
    }

    private void removeFromList(List<String> uriList, String uri) {
        if (StringUtils.isEmpty(uri)) {
            return;
        }

        for (Iterator<String> iterator = uriList.iterator(); iterator.hasNext(); ) {
            String tmpUri = iterator.next();
            if (uri.equals(tmpUri)) {
                iterator.remove();
                break;
            }
        }
    }

    private void addScope(OxAuthScope scope) {
        DisplayNameEntry oneScope = new DisplayNameEntry(scope.getDn(), scope.getInum(), scope.getDisplayName());
        this.scopes.add(oneScope);
    }

    public void removeScope(String inum) throws Exception {
        if (StringHelper.isEmpty(inum)) {
            return;
        }

        String removeScopeInum = scopeService.getDnForScope(inum);

        for (Iterator<DisplayNameEntry> iterator = this.scopes.iterator(); iterator.hasNext(); ) {
            DisplayNameEntry oneScope = iterator.next();
            if (removeScopeInum.equals(oneScope.getDn())) {
                iterator.remove();
                break;
            }
        }
    }

    public void acceptSelectLoginUri() {
        if (StringHelper.isEmpty(this.availableLoginUri)) {
            return;
        }

        if (!this.loginUris.contains(this.availableLoginUri)) {
            this.loginUris.add(this.availableLoginUri);
        }

        this.availableLoginUri = "https://";
    }

    public void acceptSelectLogoutUri() {
        if (StringHelper.isEmpty(this.availableLogoutUri)) {
            return;
        }

        if (!this.logoutUris.contains(this.availableLogoutUri)) {
            this.logoutUris.add(this.availableLogoutUri);
        }

        this.availableLogoutUri = "https://";
    }
    
    public void acceptSelectClientLogoutUri() {
    	if (StringHelper.isEmpty(this.availableClientlogoutUri )) {
            return;
        }

        if (!this.clientlogoutUris.contains(this.availableClientlogoutUri )) {
            this.clientlogoutUris.add(this.availableClientlogoutUri );
        }

        this.availableClientlogoutUri  = "https://";
    }

    public void acceptSelectContact() {
        if (StringHelper.isEmpty(this.availableContact)) {
            return;
        }

        if (!contacts.contains((availableContact))) {
            contacts.add(availableContact);
        }

        this.availableContact = "";
    }

    public void acceptSelectDefaultAcrValue() {
        if (StringHelper.isEmpty(this.availableDefaultAcrValue)) {
            return;
        }

        if (!defaultAcrValues.contains((availableDefaultAcrValue))) {
            defaultAcrValues.add(availableDefaultAcrValue);
        }

        this.availableDefaultAcrValue = "";
    }

    public void acceptSelectRequestUri() {
        if (StringHelper.isEmpty(this.availableRequestUri)) {
            return;
        }

        if (!this.requestUris.contains(this.availableRequestUri)) {
            this.requestUris.add(this.availableRequestUri);
        }

        this.availableRequestUri = "https://";
    }

    public void acceptSelectScopes() {
        if (this.availableScopes == null) {
            return;
        }

        Set<String> addedScopeInums = new HashSet<String>();
        for (DisplayNameEntry scope : scopes) {
            addedScopeInums.add(scope.getInum());
        }

        for (OxAuthScope aScope : this.availableScopes) {
            if (aScope.isSelected() && !addedScopeInums.contains(aScope.getInum())) {
                addScope(aScope);
            }
        }
    }

    public void cancelSelectScopes() {
    }

    public void cancelSelectGroups() {
    }

    public void cancelSelectLoginUri() {
        this.availableLoginUri = "http://";
    }

    public void cancelSelectLogoutUri() {
        this.availableLogoutUri = "http://";
    }
    
    public void cancelClientLogoutUri() {
        this.availableClientlogoutUri = "http://";
    }

    public void cancelSelectContact() {
    }

    public void cancelSelectDefaultAcrValue() {
    }

    public void cancelSelectRequestUri() {
    }

    private void updateLoginURIs() {
        if (this.loginUris == null || this.loginUris.size() == 0) {
            this.client.setOxAuthRedirectURIs(null);
            return;
        }

        List<String> tmpUris = new ArrayList<String>();
        for (String uri : this.loginUris) {
            tmpUris.add(uri.trim());
        }

        this.client.setOxAuthRedirectURIs(tmpUris);
    }

    private void updateLogoutURIs() {
        if (this.logoutUris == null || this.logoutUris.size() == 0) {
            this.client.setOxAuthPostLogoutRedirectURIs(null);
            return;
        }

        List<String> tmpUris = new ArrayList<String>();
        for (String uri : this.logoutUris) {
            tmpUris.add(uri.trim());
        }

        this.client.setOxAuthPostLogoutRedirectURIs(tmpUris);

    }

    private void updateClientLogoutURIs() {
        if (this.clientlogoutUris == null || this.clientlogoutUris.size() == 0) {
            this.client.setLogoutUri(null);
            return;
        }

        List<String> tmpUris = new ArrayList<String>();
        for (String uri : this.clientlogoutUris) {
            tmpUris.add(uri.trim());
        }

        this.client.setLogoutUri(tmpUris);

    }

    private void updateContacts() {
        if (contacts == null || contacts.size() == 0) {
            client.setContacts(null);
            return;
        }

        List<String> tmpContacts = new ArrayList<String>();
        for (String contact : contacts) {
            tmpContacts.add(contact);
        }

        client.setContacts(tmpContacts);
    }

    private void updateDefaultAcrValues() {
        if (defaultAcrValues == null || defaultAcrValues.size() == 0) {
            client.setDefaultAcrValues(null);
            return;
        }

        List<String> tmpDefaultAcrValues = new ArrayList<String>();
        for (String defaultAcrValue : defaultAcrValues) {
            tmpDefaultAcrValues.add(defaultAcrValue);
        }

        client.setDefaultAcrValues(tmpDefaultAcrValues.toArray(new String[tmpDefaultAcrValues.size()]));
    }

    private void updateRequestUris() {
        if (requestUris == null || requestUris.size() == 0) {
            client.setRequestUris(null);
            return;
        }

        List<String> tmpRequestUris = new ArrayList<String>();
        for (String requestUri : requestUris) {
            tmpRequestUris.add(requestUri.trim());
        }

        client.setRequestUris(tmpRequestUris.toArray(new String[tmpRequestUris.size()]));
    }

    private void updateScopes() {
        if (this.scopes == null || this.scopes.size() == 0) {
            this.client.setOxAuthScopes(null);
            return;
        }

        List<String> tmpScopes = new ArrayList<String>();
        for (DisplayNameEntry scope : this.scopes) {
            tmpScopes.add(scope.getDn());
        }

        this.client.setOxAuthScopes(tmpScopes);
    }

    private void updateResponseTypes() {
        List<ResponseType> currentResponseTypes = this.responseTypes;

        if (currentResponseTypes == null || currentResponseTypes.size() == 0) {
            this.client.setResponseTypes(null);
            return;
        }

        this.client.setResponseTypes(currentResponseTypes.toArray(new ResponseType[currentResponseTypes.size()]));
    }

    private void updateGrantTypes() {
        List<GrantType> currentGrantTypes = this.grantTypes;

        if (currentGrantTypes == null || currentGrantTypes.size() == 0) {
            this.client.setGrantTypes(null);
            return;
        }

        this.client.setGrantTypes(currentGrantTypes.toArray(new GrantType[currentGrantTypes.size()]));
    }

    public void selectAddedScopes() {
        if (this.availableScopes == null) {
            return;
        }

        Set<String> addedScopeInums = new HashSet<String>();
        for (DisplayNameEntry scope : this.scopes) {
            addedScopeInums.add(scope.getInum());
        }

        for (OxAuthScope aScope : this.availableScopes) {
            aScope.setSelected(addedScopeInums.contains(aScope.getInum()));
        }
    }

    public void searchAvailableScopes() {
        if (Util.equals(this.oldSearchAvailableScopePattern, this.searchAvailableScopePattern)) {
            return;
        }

        try {

            this.availableScopes = scopeService.searchScopes(this.searchAvailableScopePattern, OxTrustConstants.searchClientsSizeLimit);
            this.oldSearchAvailableScopePattern = this.searchAvailableScopePattern;
            selectAddedScopes();
        } catch (Exception ex) {
            log.error("Failed to find attributes", ex);
        }
    }

    private List<DisplayNameEntry> getInitialScopeDisplayNameEntiries() throws Exception {
        List<DisplayNameEntry> result = new ArrayList<DisplayNameEntry>();
        if ((client.getOxAuthScopes() == null) || (client.getOxAuthScopes().size() == 0)) {
            return result;
        }

        List<DisplayNameEntry> tmp = lookupService.getDisplayNameEntries(scopeService.getDnForScope(null), this.client.getOxAuthScopes());
        if (tmp != null) {
            result.addAll(tmp);
        }

        return result;
    }

    private List<ResponseType> getInitialResponseTypes() {
        List<ResponseType> result = new ArrayList<ResponseType>();

        ResponseType[] currentResponseTypes = this.client.getResponseTypes();
        if ((currentResponseTypes == null) || (currentResponseTypes.length == 0)) {
            return result;
        }

        result.addAll(Arrays.asList(currentResponseTypes));

        return result;
    }

    private List<GrantType> getInitialGrantTypes() {
        List<GrantType> result = new ArrayList<GrantType>();

        GrantType[] currentGrantTypes = this.client.getGrantTypes();
        if (currentGrantTypes == null || currentGrantTypes.length == 0) {
            return result;
        }

        result.addAll(Arrays.asList(currentGrantTypes));

        return result;
    }

    @Restrict("#{s:hasPermission('client', 'access')}")
    public void acceptSelectResponseTypes() {
        List<ResponseType> addedResponseTypes = getResponseTypes();

        for (SelectableEntity<ResponseType> availableResponseType : this.availableResponseTypes) {
            ResponseType responseType = availableResponseType.getEntity();
            if (availableResponseType.isSelected() && !addedResponseTypes.contains(responseType)) {
                addResponseType(responseType.getValue());
            }

            if (!availableResponseType.isSelected() && addedResponseTypes.contains(responseType)) {
                removeResponseType(responseType.getValue());
            }
        }
    }

    @Restrict("#{s:hasPermission('client', 'access')}")
    public void acceptSelectGrantTypes() {
        List<GrantType> addedGrantTypes = getGrantTypes();

        for (SelectableEntity<GrantType> availableGrantType : this.availableGrantTypes) {
            GrantType grantType = availableGrantType.getEntity();
            if (availableGrantType.isSelected() && !addedGrantTypes.contains(grantType)) {
                addGrantType(grantType.toString());
            }

            if (!availableGrantType.isSelected() && addedGrantTypes.contains(grantType)) {
                removeGrantType(grantType.toString());
            }
        }
    }

    @Restrict("#{s:hasPermission('client', 'access')}")
    public void cancelSelectResponseTypes() {
    }

    @Restrict("#{s:hasPermission('client', 'access')}")
    public void cancelSelectGrantTypes() {
    }

    @Restrict("#{s:hasPermission('client', 'access')}")
    public void addResponseType(String value) {
        if (StringHelper.isEmpty(value)) {
            return;
        }

        ResponseType addResponseType = ResponseType.getByValue(value);
        if (addResponseType != null) {
            this.responseTypes.add(addResponseType);
        }
    }

    @Restrict("#{s:hasPermission('client', 'access')}")
    public void addGrantType(String value) {
        if (StringHelper.isEmpty(value)) {
            return;
        }

        GrantType addGrantType = GrantType.fromString(value);
        if (addGrantType != null) {
            this.grantTypes.add(addGrantType);
        }
    }

    @Restrict("#{s:hasPermission('client', 'access')}")
    public void removeResponseType(String value) {
        if (StringHelper.isEmpty(value)) {
            return;
        }

        ResponseType removeResponseType = ResponseType.getByValue(value);
        if (removeResponseType != null) {
            this.responseTypes.remove(removeResponseType);
        }
    }

    @Restrict("#{s:hasPermission('client', 'access')}")
    public void removeGrantType(String value) {
        if (StringHelper.isEmpty(value)) {
            return;
        }

        GrantType removeGrantType = GrantType.fromString(value);
        if (removeGrantType != null) {
            this.grantTypes.remove(removeGrantType);
        }
    }

    @Restrict("#{s:hasPermission('client', 'access')}")
    public void searchAvailableResponseTypes() {
        if (this.availableResponseTypes != null) {
            selectAddedResponseTypes();
            return;
        }

        List<SelectableEntity<ResponseType>> tmpAvailableResponseTypes = new ArrayList<SelectableEntity<ResponseType>>();

        for (ResponseType responseType : ResponseType.values()) {
            tmpAvailableResponseTypes.add(new SelectableEntity<ResponseType>(responseType));
        }

        this.availableResponseTypes = tmpAvailableResponseTypes;
        selectAddedResponseTypes();
    }

    @Restrict("#{s:hasPermission('client', 'access')}")
    public void searchAvailableGrantTypes() {
        if (this.availableGrantTypes != null) {
            selectAddedGrantTypes();
            return;
        }

        List<SelectableEntity<GrantType>> tmpAvailableGrantTypes = new ArrayList<SelectableEntity<GrantType>>();

        tmpAvailableGrantTypes.add(new SelectableEntity<GrantType>(GrantType.AUTHORIZATION_CODE));
        tmpAvailableGrantTypes.add(new SelectableEntity<GrantType>(GrantType.IMPLICIT));
        tmpAvailableGrantTypes.add(new SelectableEntity<GrantType>(GrantType.REFRESH_TOKEN));

        this.availableGrantTypes = tmpAvailableGrantTypes;
        selectAddedGrantTypes();
    }

    private void selectAddedResponseTypes() {
        List<ResponseType> addedResponseTypes = getResponseTypes();

        for (SelectableEntity<ResponseType> availableResponseType : this.availableResponseTypes) {
            availableResponseType.setSelected(addedResponseTypes.contains(availableResponseType.getEntity()));
        }
    }

    private void selectAddedGrantTypes() {
        List<GrantType> addedGrantTypes = getGrantTypes();

        for (SelectableEntity<GrantType> availableGrantType : this.availableGrantTypes) {
            availableGrantType.setSelected(addedGrantTypes.contains(availableGrantType.getEntity()));
        }
    }

    public String getInum() {
        return inum;
    }

    public void setInum(String inum) {
        this.inum = inum;
    }

    public OxAuthClient getClient() {
        return client;
    }

    public boolean isUpdate() {
        return update;
    }

    public String getAvailableLoginUri() {
        return availableLoginUri;
    }

    public void setAvailableLoginUri(String availableLoginUri) {
        this.availableLoginUri = availableLoginUri;
    }

    public String getAvailableLogoutUri() {
        return availableLogoutUri;
    }

    public void setAvailableLogoutUri(String availableLogoutUri) {
        this.availableLogoutUri = availableLogoutUri;
    }


    public String getAvailableContact() {
        return availableContact;
    }

    public void setAvailableContact(String availableContact) {
        this.availableContact = availableContact;
    }

    public String getAvailableDefaultAcrValue() {
        return availableDefaultAcrValue;
    }

    public void setAvailableDefaultAcrValue(String availableDefaultAcrValue) {
        this.availableDefaultAcrValue = availableDefaultAcrValue;
    }

    public String getAvailableRequestUri() {
        return availableRequestUri;
    }

    public void setAvailableRequestUri(String availableRequestUri) {
        this.availableRequestUri = availableRequestUri;
    }

    public List<OxAuthScope> getAvailableScopes() {
        return this.availableScopes;
    }

    public List<GluuGroup> getAvailableGroups() {
        return this.availableGroups;
    }

    public List<SelectableEntity<ResponseType>> getAvailableResponseTypes() {
        return this.availableResponseTypes;
    }

    public List<SelectableEntity<GrantType>> getAvailableGrantTypes() {
        return this.availableGrantTypes;
    }

    public List<String> getLoginUris() {
        return loginUris;
    }

    public List<String> getLogoutUris() {
        return logoutUris;
    }

    public List<DisplayNameEntry> getScopes() {
        return this.scopes;
    }

    public List<ResponseType> getResponseTypes() {
        return responseTypes;
    }

    public List<GrantType> getGrantTypes() {
        return grantTypes;
    }

    public List<String> getContacts() {
        return contacts;
    }

    public List<String> getDefaultAcrValues() {
        return defaultAcrValues;
    }

    public List<String> getRequestUris() {
        return requestUris;
    }

    public String getSearchAvailableScopePattern() {
        return this.searchAvailableScopePattern;
    }

    public void setSearchAvailableScopePattern(String searchAvailableScopePattern) {
        this.searchAvailableScopePattern = searchAvailableScopePattern;
    }

	/**
	 * @return the availableClientlogoutUri
	 */
	public String getAvailableClientlogoutUri() {
		return availableClientlogoutUri;
	}

	/**
	 * @param availableClientlogoutUri the availableClientlogoutUri to set
	 */
	public void setAvailableClientlogoutUri(String availableClientlogoutUri) {
		this.availableClientlogoutUri = availableClientlogoutUri;
	}

	/**
	 * @return the clientlogoutUris
	 */
	public List<String> getClientlogoutUris() {
		return clientlogoutUris;
	}

	/**
	 * @param clientlogoutUris the clientlogoutUris to set
	 */
	public void setClientlogoutUris(List<String> clientlogoutUris) {
		this.clientlogoutUris = clientlogoutUris;
	}

}
