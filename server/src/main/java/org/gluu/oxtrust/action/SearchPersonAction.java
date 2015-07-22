/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxtrust.action;

import java.io.Serializable;
import java.util.List;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.gluu.oxtrust.ldap.service.PersonService;
import org.gluu.oxtrust.model.GluuCustomPerson;
import org.gluu.oxtrust.util.OxTrustConstants;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.security.Restrict;
import org.jboss.seam.international.StatusMessages;
import org.jboss.seam.log.Log;
import org.xdi.util.Util;

/**
 * Action class for search persons
 * 
 * @author Yuriy Movchan Date: 10.22.2010
 */
@Name("searchPersonAction")
@Scope(ScopeType.CONVERSATION)
@Restrict("#{identity.loggedIn}")
public class SearchPersonAction implements Serializable {

	private static final long serialVersionUID = -4672682869487324438L;

	@Logger
	private Log log;

	@In
	StatusMessages statusMessages;

	@NotNull
	@Size(min = 2, max = 30, message = "Length of search string should be between 2 and 30")
	private String searchPattern;

	private String oldSearchPattern;

	private List<GluuCustomPerson> personList;

	@In
	private PersonService personService;

	@Restrict("#{s:hasPermission('person', 'access')}")
	public String start() {
		return search();
	}

	@Restrict("#{s:hasPermission('person', 'access')}")
	public String search() {
		if (Util.equals(this.oldSearchPattern, this.searchPattern)) {
			return OxTrustConstants.RESULT_SUCCESS;
		}

		try {
			this.personList = personService.searchPersons(this.searchPattern, OxTrustConstants.searchPersonsSizeLimit);
			this.oldSearchPattern = this.searchPattern;
		} catch (Exception ex) {
			log.error("Failed to find persons", ex);
			return OxTrustConstants.RESULT_FAILURE;
		}

		return OxTrustConstants.RESULT_SUCCESS;
	}

	public String getSearchPattern() {
		return searchPattern;
	}

	public void setSearchPattern(String searchPattern) {
		this.searchPattern = searchPattern;
	}

	public List<GluuCustomPerson> getPersonList() {
		return personList;
	}

}
