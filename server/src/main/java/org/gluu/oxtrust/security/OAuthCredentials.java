/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxtrust.security;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.security.Credentials;

/**
 * We're using this custom credentials class instead of built-in one to allow
 * calling our login method without credentials being set User: Dejan Maric
 */
@Name("org.jboss.seam.security.credentials")
@Scope(ScopeType.SESSION)
@Install(precedence = Install.APPLICATION)
@BypassInterceptors
public class OAuthCredentials extends Credentials {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public boolean isInvalid() {
		return false;
	}
}
