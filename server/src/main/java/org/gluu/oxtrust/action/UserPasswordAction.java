/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxtrust.action;

import java.io.Serializable;

import org.gluu.oxtrust.ldap.service.IPersonService;
import org.gluu.oxtrust.model.GluuCustomPerson;
import org.gluu.oxtrust.util.OxTrustConstants;
import org.gluu.site.ldap.persistence.exception.AuthenticationException;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.security.Restrict;

/**
 * Serves for password updates on UI.
 * 
 */
@Name("userPasswordAction")
@Scope(ScopeType.CONVERSATION)
@Restrict("#{identity.loggedIn}")
@Deprecated
public class UserPasswordAction implements Serializable {

	private static final long serialVersionUID = 6486111971437252913L;

	private String oldPassword;
	private String newPassword;
	private String newPasswordConfirmation;
	private String passwordMessage;

	private GluuCustomPerson person;

	@In
	private IPersonService personService;

	public String validatePassword() {
		String result;
		if (newPasswordConfirmation == null || !newPasswordConfirmation.equals(newPassword)) {
			this.passwordMessage = "Passwords Must be equal";
			result = OxTrustConstants.RESULT_VALIDATION_ERROR;
		} else {
			this.passwordMessage = "";
			result = OxTrustConstants.RESULT_SUCCESS;
		}

		return result;
	}

	@Restrict("#{s:hasPermission('profile', 'access')}")
	public String update(boolean verifyPassword) throws Exception {
		String result;
		boolean verifyOldPass;
		try {
			verifyOldPass = personService.authenticate(getPerson().getUid(), oldPassword);
		} catch (AuthenticationException e) {
			verifyOldPass = false;
		}
		if ((verifyOldPass || !verifyPassword) /*
												 * && validatePassword().equals(
												 * Configuration.RESULT_SUCCESS)
												 */) {
			getPerson().setUserPassword(newPassword);
			personService.updatePerson(getPerson());
			result = OxTrustConstants.RESULT_SUCCESS;
		} else {
			result = OxTrustConstants.RESULT_FAILURE;
		}
		return result;
	}

	public String update() throws Exception {
		return update(false);
	}

	public void cancel() {
	}

	public void setNewPasswordConfirmation(String newPasswordConfirmation) {
		this.newPasswordConfirmation = newPasswordConfirmation;
	}

	public String getNewPasswordConfirmation() {
		return newPasswordConfirmation;
	}

	public void setNewPassword(String newPassword) {
		this.newPassword = newPassword;
	}

	public String getNewPassword() {
		return newPassword;
	}

	public void setOldPassword(String oldPassword) {
		this.oldPassword = oldPassword;
	}

	public String getOldPassword() {
		return oldPassword;
	}

	public void setPasswordMessage(String passwordMessage) {
		this.passwordMessage = passwordMessage;
	}

	public String getPasswordMessage() {
		return passwordMessage;
	}

	/**
	 * @param person
	 *            the person to set
	 */
	public void setPerson(GluuCustomPerson person) {
		this.person = person;
	}

	/**
	 * @return the person
	 */
	public GluuCustomPerson getPerson() {
		return person;
	}

}
