package org.gluu.oxtrust.action.push;

import java.io.Serializable;
import java.util.List;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.gluu.oxtrust.model.push.PushDevice;
import org.gluu.oxtrust.service.push.PushDeviceService;
import org.gluu.oxtrust.util.OxTrustConstants;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.security.Restrict;
import org.jboss.seam.log.Log;
import org.xdi.util.Util;

/**
 * Action class for Push Device Inventory
 * 
 * @author Yuriy Movchan Date: 02/03/2014
 */
@Name("pushDeviceInventoryAction")
@Scope(ScopeType.CONVERSATION)
@Restrict("#{identity.loggedIn}")
public class PushDeviceInventoryAction implements Serializable {

	private static final long serialVersionUID = 6613070802638642079L;

	@Logger
	private Log log;

	@NotNull
	@Size(min = 0, max = 30, message = "Length of search string should be less than 30")
	private String searchPattern;

	private String oldSearchPattern;

	private List<PushDevice> pushDeviceList;

	@In
	private PushDeviceService pushDeviceService;
	
	@Restrict("#{s:hasPermission('oxpush', 'access')}")
	public String start() {
		return search();
	}

	@Restrict("#{s:hasPermission('oxpush', 'access')}")
	public String search() {
		if (Util.equals(this.oldSearchPattern, this.searchPattern)) {
			return OxTrustConstants.RESULT_SUCCESS;
		}

		try {
			this.pushDeviceList = pushDeviceService.findPushDevices(this.searchPattern, 0);
			this.oldSearchPattern = this.searchPattern;

			return OxTrustConstants.RESULT_SUCCESS;
		} catch (Exception ex) {
			log.error("Failed to find scopes", ex);
		}

		return OxTrustConstants.RESULT_FAILURE;
	}

	public String getSearchPattern() {
		return searchPattern;
	}

	public void setSearchPattern(String searchPattern) {
		this.searchPattern = searchPattern;
	}

	public List<PushDevice> getPushDeviceList() {
		return pushDeviceList;
	}

}
