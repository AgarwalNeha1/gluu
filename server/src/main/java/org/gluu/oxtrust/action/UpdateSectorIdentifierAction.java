package org.gluu.oxtrust.action;

import org.apache.commons.lang.StringUtils;
import org.gluu.oxtrust.ldap.service.ClientService;
import org.gluu.oxtrust.ldap.service.SectorIdentifierService;
import org.gluu.oxtrust.model.OxAuthClient;
import org.gluu.oxtrust.model.OxAuthSectorIdentifier;
import org.gluu.oxtrust.util.OxTrustConstants;
import org.gluu.site.ldap.persistence.exception.LdapMappingException;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.security.Restrict;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.jboss.seam.log.Log;
import org.xdi.config.oxtrust.ApplicationConfiguration;
import org.xdi.model.DisplayNameEntry;
import org.xdi.service.LookupService;
import org.xdi.util.StringHelper;
import org.xdi.util.Util;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.*;

/**
 * Action class for view and update sector identifier form.
 *
 * @author Javier Rojas Blum
 * @version January 15, 2016
 */
@Scope(ScopeType.CONVERSATION)
@Name("updateSectorIdentifierAction")
@Restrict("#{identity.loggedIn}")
public class UpdateSectorIdentifierAction implements Serializable {

    private static final long serialVersionUID = 572441515451149802L;

    @Logger
    private Log log;

    private String inum;
    private boolean update;

    private OxAuthSectorIdentifier sectorIdentifier;

    private List<String> loginUris;

    private List<DisplayNameEntry> clientDisplayNameEntries;

    @NotNull
    @Size(min = 2, max = 30, message = "Length of search string should be between 2 and 30")
    private String searchAvailableClientPattern;

    private String oldSearchAvailableClientPattern;

    private String availableLoginUri = "https://";

    private List<OxAuthClient> availableClients;

    @In
    private SectorIdentifierService sectorIdentifierService;

    @In
    private LookupService lookupService;

    @In
    private ClientService clientService;

    @In
    private FacesMessages facesMessages;

    @In(value = "#{oxTrustConfiguration.applicationConfiguration}")
    private ApplicationConfiguration applicationConfiguration;

    @Restrict("#{s:hasPermission('sectorIdentifier', 'access')}")
    public String add() throws Exception {
        if (this.sectorIdentifier != null) {
            return OxTrustConstants.RESULT_SUCCESS;
        }

        this.update = false;
        this.sectorIdentifier = new OxAuthSectorIdentifier();

        try {
            this.loginUris = getNonEmptyStringList(sectorIdentifier.getRedirectUris());
            this.clientDisplayNameEntries = loadClientDisplayNameEntries();
        } catch (LdapMappingException ex) {
            log.error("Failed to load person display names", ex);

            return OxTrustConstants.RESULT_FAILURE;
        }

        return OxTrustConstants.RESULT_SUCCESS;
    }

    @Restrict("#{s:hasPermission('sectorIdentifier', 'access')}")
    public String update() throws Exception {
        if (this.sectorIdentifier != null) {
            return OxTrustConstants.RESULT_SUCCESS;
        }

        this.update = true;
        log.info("this.update : " + this.update);
        try {
            log.info("inum : " + inum);
            this.sectorIdentifier = sectorIdentifierService.getSectorIdentifierByInum(inum);
        } catch (LdapMappingException ex) {
            log.error("Failed to find sector identifier {0}", ex, inum);
        }

        if (this.sectorIdentifier == null) {
            log.info("Sector identifier is null ");
            return OxTrustConstants.RESULT_FAILURE;
        }

        try {
            this.loginUris = getNonEmptyStringList(sectorIdentifier.getRedirectUris());
            this.clientDisplayNameEntries = loadClientDisplayNameEntries();
        } catch (LdapMappingException ex) {
            log.error("Failed to load person display names", ex);

            return OxTrustConstants.RESULT_FAILURE;
        }
        log.info("returning Success");
        return OxTrustConstants.RESULT_SUCCESS;
    }

    @Restrict("#{s:hasPermission('sectorIdentifier', 'access')}")
    public void cancel() {
    }

    @Restrict("#{s:hasPermission('sectorIdentifier', 'access')}")
    public String save() throws Exception {
        List<DisplayNameEntry> oldClientDisplayNameEntries = null;
        try {
            oldClientDisplayNameEntries = loadClientDisplayNameEntries();
        } catch (LdapMappingException ex) {
            log.info("error getting old clients");
            log.error("Failed to load client display names", ex);

            facesMessages.add(StatusMessage.Severity.ERROR, "Failed to update sector identifier");
            return OxTrustConstants.RESULT_FAILURE;
        }

        updateLoginURIs();
        updateClientDisplayNameEntries();
        if (update) {
            // Update sectorIdentifier
            try {
                sectorIdentifierService.updateSectorIdentifier(this.sectorIdentifier);
                updateClients(oldClientDisplayNameEntries, this.clientDisplayNameEntries);
            } catch (LdapMappingException ex) {
                log.info("error updating sector identifier ", ex);
                log.error("Failed to update sector identifier {0}", ex, this.inum);

                facesMessages.add(StatusMessage.Severity.ERROR, "Failed to update sector identifier");
                return OxTrustConstants.RESULT_FAILURE;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            this.inum = sectorIdentifierService.generateInumForNewSectorIdentifier();
            String dn = sectorIdentifierService.getDnForSectorIdentifier(this.inum);

            // Save sectorIdentifier
            this.sectorIdentifier.setDn(dn);
            this.sectorIdentifier.setInum(this.inum);
            try {
                sectorIdentifierService.addSectorIdentifier(this.sectorIdentifier);
                updateClients(oldClientDisplayNameEntries, this.clientDisplayNameEntries);
            } catch (LdapMappingException ex) {
                log.info("error saving sector identifier ");
                log.error("Failed to add new sector identifier {0}", ex, this.sectorIdentifier.getInum());

                facesMessages.add(StatusMessage.Severity.ERROR, "Failed to add new sector identifier");
                return OxTrustConstants.RESULT_FAILURE;
            }

            this.update = true;
        }
        log.info(" returning success updating or saving sector identifier");
        return OxTrustConstants.RESULT_SUCCESS;
    }

    @Restrict("#{s:hasPermission('sectorIdentifier', 'access')}")
    public String delete() throws Exception {
        if (update) {
            // Remove sectorIdentifier
            try {
                sectorIdentifierService.removeSectorIdentifier(this.sectorIdentifier);
                return OxTrustConstants.RESULT_SUCCESS;
            } catch (LdapMappingException ex) {
                log.error("Failed to remove sector identifier {0}", ex, this.sectorIdentifier.getInum());
            }
        }

        return OxTrustConstants.RESULT_FAILURE;
    }

    private List<DisplayNameEntry> loadClientDisplayNameEntries() throws Exception {
        List<DisplayNameEntry> result = new ArrayList<DisplayNameEntry>();
        List<DisplayNameEntry> tmp = lookupService.getDisplayNameEntries(clientService.getDnForClient(null), this.sectorIdentifier.getClientIds());
        if (tmp != null) {
            result.addAll(tmp);
        }

        return result;
    }

    private List<String> getNonEmptyStringList(List<String> currentList) {
        if (currentList != null && currentList.size() > 0) {
            return new ArrayList<String>(currentList);
        } else {
            return new ArrayList<String>();
        }
    }

    public void addClient(OxAuthClient client) {
        DisplayNameEntry displayNameEntry = new DisplayNameEntry(client.getDn(), client.getInum(), client.getDisplayName());
        this.clientDisplayNameEntries.add(displayNameEntry);
    }

    public void removeClient(String inum) throws Exception {
        if (StringHelper.isEmpty(inum)) {
            return;
        }

        String removeClientInum = clientService.getDnForClient(inum);

        for (Iterator<DisplayNameEntry> iterator = this.clientDisplayNameEntries.iterator(); iterator.hasNext(); ) {
            DisplayNameEntry displayNameEntry = iterator.next();
            if (removeClientInum.equals(displayNameEntry.getDn())) {
                iterator.remove();
                break;
            }
        }
    }

    public String getSearchAvailableClientPattern() {
        return this.searchAvailableClientPattern;
    }

    public void setSearchAvailableClientPattern(String searchAvailableClientPattern) {
        this.searchAvailableClientPattern = searchAvailableClientPattern;
    }

    public List<OxAuthClient> getAvailableClients() {
        return this.availableClients;
    }

    public void searchAvailableClients() {
        if (Util.equals(this.oldSearchAvailableClientPattern, this.searchAvailableClientPattern)) {
            return;
        }

        try {
            this.availableClients = clientService.searchClients(this.searchAvailableClientPattern, OxTrustConstants.searchClientsSizeLimit);
            this.oldSearchAvailableClientPattern = this.searchAvailableClientPattern;
            selectAddedClients();
        } catch (Exception ex) {
            log.error("Failed to find clients", ex);
        }
    }

    public void selectAddedClients() {
        if (this.availableClients == null) {
            return;
        }

        Set<String> addedClientInums = new HashSet<String>();
        for (DisplayNameEntry entry : clientDisplayNameEntries) {
            addedClientInums.add(entry.getInum());
        }

        for (OxAuthClient client : this.availableClients) {
            client.setSelected(addedClientInums.contains(client.getInum()));
        }
    }

    public void acceptSelectClients() {
        if (this.availableClients == null) {
            return;
        }

        Set<String> addedClientInums = new HashSet<String>();
        for (DisplayNameEntry entry : clientDisplayNameEntries) {
            addedClientInums.add(entry.getInum());
        }

        for (OxAuthClient client : this.availableClients) {
            if (client.isSelected() && !addedClientInums.contains(client.getInum())) {
                addClient(client);
            }
        }
    }

    public void cancelSelectClients() {
    }

    private void updateClientDisplayNameEntries() {
        List<String> clientDisplayNameEntries = new ArrayList<String>();
        this.sectorIdentifier.setClientIds(clientDisplayNameEntries);

        for (DisplayNameEntry displayNameEntry : this.clientDisplayNameEntries) {
            clientDisplayNameEntries.add(displayNameEntry.getDn());
        }
    }

    private void updateClients(List<DisplayNameEntry> oldClientDisplayNameEntries, List<DisplayNameEntry> newClientDisplayNameEntries) throws Exception {
        log.debug("Old clients: {0}", oldClientDisplayNameEntries);
        log.debug("New clients: {0}", newClientDisplayNameEntries);

        String sectorIdentifierDn = this.sectorIdentifier.getDn();

        // Convert members to array of DNs
        String[] oldClientDns = convertToDNsArray(oldClientDisplayNameEntries);
        String[] newClientDns = convertToDNsArray(newClientDisplayNameEntries);

        Arrays.sort(oldClientDns);
        Arrays.sort(newClientDns);

        boolean[] retainOldClients = new boolean[oldClientDns.length];
        Arrays.fill(retainOldClients, false);

        List<String> addedMembers = new ArrayList<String>();
        List<String> removedMembers = new ArrayList<String>();
        List<String> existingMembers = new ArrayList<String>();

        // Add new values
        for (String value : newClientDns) {
            int idx = Arrays.binarySearch(oldClientDns, value);
            if (idx >= 0) {
                // Old members array contains member. Retain member
                retainOldClients[idx] = true;
            } else {
                // This is new member
                addedMembers.add(value);
            }
        }

        // Remove clients which we don't have in new clients
        for (int i = 0; i < oldClientDns.length; i++) {
            if (retainOldClients[i]) {
                existingMembers.add(oldClientDns[i]);
            } else {
                removedMembers.add(oldClientDns[i]);
            }
        }

        for (String dn : addedMembers) {
            OxAuthClient client = clientService.getClientByDn(dn);
            log.debug("Adding sector identifier {0} to client {1}", sectorIdentifierDn, client.getDisplayName());

            client.setSectorIdentifierUri(getSectorIdentifierUrl());

            clientService.updateClient(client);
            Events.instance().raiseEvent(OxTrustConstants.EVENT_PERSON_ADDED_TO_GROUP, client, sectorIdentifierDn);
        }

        for (String dn : removedMembers) {
            OxAuthClient client = clientService.getClientByDn(dn);
            log.debug("Removing sector identifier {0} from client {1}", sectorIdentifierDn, client.getDisplayName());

            client.setSectorIdentifierUri(null);

            clientService.updateClient(client);
            Events.instance().raiseEvent(OxTrustConstants.EVENT_PERSON_REMOVED_FROM_GROUP, client, sectorIdentifierDn);
        }
    }

    private String[] convertToDNsArray(List<DisplayNameEntry> clientDisplayNameEntries) {
        String[] dns = new String[clientDisplayNameEntries.size()];
        int i = 0;
        for (DisplayNameEntry displayNameEntry : clientDisplayNameEntries) {
            dns[i++] = displayNameEntry.getDn();
        }

        return dns;
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

    public void cancelSelectLoginUri() {
        this.availableLoginUri = "http://";
    }

    private void updateLoginURIs() {
        if (this.loginUris == null || this.loginUris.size() == 0) {
            this.sectorIdentifier.setRedirectUris(null);
            return;
        }

        List<String> tmpUris = new ArrayList<String>();
        for (String uri : this.loginUris) {
            tmpUris.add(uri);
        }

        this.sectorIdentifier.setRedirectUris(tmpUris);
    }

    @Restrict("#{s:hasPermission('sectorIdentifier', 'access')}")
    public void removeLoginURI(String uri) {
        removeFromList(this.loginUris, uri);
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

    public String getSectorIdentifierUrl() {
        return applicationConfiguration.getOxAuthSectorIdentifierUrl() + "/" + inum;
    }

    public String getInum() {
        return inum;
    }

    public void setInum(String inum) {
        this.inum = inum;
    }

    public OxAuthSectorIdentifier getSectorIdentifier() {
        return sectorIdentifier;
    }

    public List<String> getLoginUris() {
        return loginUris;
    }

    public void setLoginUris(List<String> loginUris) {
        this.loginUris = loginUris;
    }

    public List<DisplayNameEntry> getClientDisplayNameEntries() {
        return clientDisplayNameEntries;
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
}
