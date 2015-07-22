/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxtrust.action;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import javax.faces.context.FacesContext;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.gluu.oxtrust.ldap.service.FederationService;
import org.gluu.oxtrust.ldap.service.OrganizationService;
import org.gluu.oxtrust.ldap.service.Shibboleth2ConfService;
import org.gluu.oxtrust.model.GluuMetadataSourceType;
import org.gluu.oxtrust.model.GluuSAMLFederationProposal;
import org.gluu.oxtrust.util.OxTrustConstants;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage.Severity;
import org.xdi.ldap.model.GluuStatus;
import org.xdi.util.StringHelper;
import org.xdi.util.io.ExcludeFilterInputStream;
import org.xdi.util.io.FileUploadWrapper;
import org.xdi.util.io.ResponseHelper;

@Scope(ScopeType.CONVERSATION)
@Name("joinFederationAction")
public class JoinFederationAction implements Serializable {

	private static final long serialVersionUID = -1032167044333943680L;

	private GluuSAMLFederationProposal federationProposal;

	private String inum;

	@In(value = "#{facesContext}")
	private FacesContext facesContext;

	@In
	private FederationService federationService;

	@In
	private Shibboleth2ConfService shibboleth2ConfService;

	@In
	private FacesMessages facesMessages;

	@In(create = true)
	@Out(scope = ScopeType.CONVERSATION)
	private TrustContactsAction trustContactsAction;

	private FileUploadWrapper fileWrapper = new FileUploadWrapper();

	public String add() {
		if (this.federationProposal != null) {
			return OxTrustConstants.RESULT_SUCCESS;
		}

		this.federationProposal = new GluuSAMLFederationProposal();
		this.federationProposal.setOwner(OrganizationService.instance().getOrganization().getDn());
		this.federationProposal.setStatus(GluuStatus.INACTIVE);

		init();

		return OxTrustConstants.RESULT_SUCCESS;
	}

	public String view() {
		if (this.federationProposal != null) {
			return OxTrustConstants.RESULT_SUCCESS;
		}

		this.federationProposal = federationService.getProposalByInum(inum);

		this.fileWrapper = new FileUploadWrapper();
		this.fileWrapper.setFileName(this.federationProposal.getSpMetaDataFN());

		init();

		return OxTrustConstants.RESULT_SUCCESS;
	}

	public void init() {
		trustContactsAction.initContacts(this.federationProposal);
	}

	public boolean isActive() {
		return GluuStatus.ACTIVE.equals(this.federationProposal.getStatus());
	}

	public String acceptToggle() {
		if (isActive()) {
			this.federationProposal.setStatus(GluuStatus.INACTIVE);
			federationService.updateFederationProposal(federationProposal);
		} else {
			this.federationProposal.setStatus(GluuStatus.ACTIVE);
			federationService.updateFederationProposal(federationProposal);
		}
		return OxTrustConstants.RESULT_SUCCESS;
	}

	public String delete() {

		federationService.removeFederationProposal(this.federationProposal);

		return OxTrustConstants.RESULT_SUCCESS;
	}

	public String save(boolean federation) {
		if (this.federationProposal.isRulesAccepted() || federation) {
			if (inum == null) {
				this.inum = federationService.generateInumForNewFederationProposal();
				String dn = federationService.getDnForFederationProposal(this.getInum());

				this.federationProposal.setInum(this.getInum());
				this.federationProposal.setDn(dn);
				if (!federation && !saveSpMetaDataFile()) {
					return OxTrustConstants.RESULT_FAILURE;
				}
				trustContactsAction.saveContacts();
				this.federationProposal.setFederation(federation);
				federationService.addFederationProposal(federationProposal);
			} else {
				if (!federation && !saveSpMetaDataFile()) {
					return OxTrustConstants.RESULT_FAILURE;
				}
				trustContactsAction.saveContacts();
				federationService.updateFederationProposal(federationProposal);
			}

			return OxTrustConstants.RESULT_SUCCESS;
		} else {
			facesMessages.add(Severity.ERROR, "You should accept Federation Policies and Operating Procedures");
			return OxTrustConstants.RESULT_FAILURE;
		}
	}

	public String cancel() {
		return OxTrustConstants.RESULT_SUCCESS;
	}

	private boolean saveSpMetaDataFile() {
		boolean result = false;
		if (GluuMetadataSourceType.FILE.equals(federationProposal.getSpMetaDataSourceType())) {
			result = saveSpMetaDataFileSourceTypeFile();
		} else if (GluuMetadataSourceType.URI.equals(federationProposal.getSpMetaDataSourceType())) {
			result = saveSpMetaDataFileSourceTypeURI();
		}

		if (!result) {
			facesMessages.add(Severity.ERROR, "Failed to save meta-data file. Please check if you provide correct file");
			return result;
		}

		if (shibboleth2ConfService.isCorrectMetadataFile(federationProposal.getSpMetaDataFN())) {
			return true;
		}

		facesMessages.add(Severity.ERROR, "Failed to parse meta-data file. Please check if you provide correct file");
		shibboleth2ConfService.removeMetadataFile(federationProposal.getSpMetaDataFN());

		return false;
	}

	private boolean saveSpMetaDataFileSourceTypeFile() {
		String metadataFileName = federationProposal.getSpMetaDataFN();
		boolean emptySpMetadataFileName = StringHelper.isEmpty(metadataFileName);

		if (fileWrapper.getStream() == null) {
			if (emptySpMetadataFileName) {
				return false;
			}

			// Admin doesn't provide new file. Check if we already has this file
			String filePath = shibboleth2ConfService.getMetadataFilePath(metadataFileName);
			if (filePath == null) {
				return false;
			}

			File file = new File(filePath);
			if (!file.exists()) {
				return false;
			}

			// File already exist
			return true;
		}

		if (emptySpMetadataFileName) {
			// Generate new file name
			metadataFileName = shibboleth2ConfService.getNewMetadataFileName(this.federationProposal,
					federationService.getAllFederationProposals());
		}

		// Save new file
		boolean result = shibboleth2ConfService.saveMetadataFile(metadataFileName, fileWrapper.getStream());
		if (result) {
			federationProposal.setSpMetaDataFN(metadataFileName);
		}

		return result;

	}

	private boolean saveSpMetaDataFileSourceTypeURI() {
		String metadataFileName = federationProposal.getSpMetaDataFN();
		boolean emptyMetadataFileName = StringHelper.isEmpty(metadataFileName);

		if (emptyMetadataFileName) {
			// Generate new file name
			metadataFileName = shibboleth2ConfService.getNewMetadataFileName(this.federationProposal,
					federationService.getAllFederationProposals());
		}

		boolean result = shibboleth2ConfService.saveMetadataFile(federationProposal.getSpMetaDataURL(), metadataFileName);
		if (result) {
			federationProposal.setSpMetaDataFN(metadataFileName);
		}

		return result;
	}

	public GluuSAMLFederationProposal getFederationProposal() {
		return federationProposal;
	}

	public void setInum(String inum) {
		this.inum = inum;
	}

	public String getInum() {
		return inum;
	}

	public void setFileWrapper(FileUploadWrapper fileWrapper) {
		this.fileWrapper = fileWrapper;
	}

	public FileUploadWrapper getFileWrapper() {
		return fileWrapper;
	}

	public String getMetadata() throws IOException {
		if (federationProposal == null) {
			return null;
		}

		String filename = federationProposal.getSpMetaDataFN();
		File metadataFile = null;
		if (!StringUtils.isEmpty(filename)) {
			metadataFile = new File(shibboleth2ConfService.getMetadataFilePath(filename));

			if (metadataFile.exists()) {
				return FileUtils.readFileToString(metadataFile);
			}
		}

		return null;
	}

	public String downloadFederation() throws IOException {
		boolean result = false;
		if (StringHelper.isNotEmpty(inum)) {
			GluuSAMLFederationProposal federation = federationService.getProposalByInum(inum);
			if (!federation.isFederation() || !federation.getStatus().equals(GluuStatus.ACTIVE)) {
				return OxTrustConstants.RESULT_FAILURE;
			}

			Shibboleth2ConfService shibboleth2ConfService = Shibboleth2ConfService.instance();
			ByteArrayOutputStream bos = new ByteArrayOutputStream(16384);
			String head = String
					.format("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<EntitiesDescriptor Name=\"%s\"  xmlns=\"urn:oasis:names:tc:SAML:2.0:metadata\">\n",
							StringHelper.removePunctuation(federation.getInum()));
			bos.write(head.getBytes());
			for (GluuSAMLFederationProposal proposal : federationService.getAllActiveFederationProposals()) {
				if (proposal.getContainerFederation() != null && proposal.getContainerFederation().equals(federation)) {
					String filename = proposal.getSpMetaDataFN();
					if (!StringUtils.isEmpty(filename)) {
						File metadataFile = new File(shibboleth2ConfService.getMetadataFilePath(filename));
						InputStream is = FileUtils.openInputStream(metadataFile);
						ExcludeFilterInputStream filtered = new ExcludeFilterInputStream(is, "<?", "?>");
						IOUtils.copy(filtered, bos);
					}
				}
			}
			String tail = "</EntitiesDescriptor>";
			bos.write(tail.getBytes());

			result = ResponseHelper.downloadFile("federation.xml", OxTrustConstants.CONTENT_TYPE_TEXT_PLAIN, bos.toByteArray(), facesContext);
		}
		return result ? OxTrustConstants.RESULT_SUCCESS : OxTrustConstants.RESULT_FAILURE;
	}

	public void setRules(String rules) {
		this.federationProposal.setFederationRules(rules);
	}

	public String getRules() {
		String rules = null;
		if (this.federationProposal.isFederation()) {
			rules = this.federationProposal.getFederationRules();
		} else if (this.federationProposal.getContainerFederation() != null) {
			rules = this.federationProposal.getContainerFederation().getFederationRules();
		}
		return rules;
	}

}
