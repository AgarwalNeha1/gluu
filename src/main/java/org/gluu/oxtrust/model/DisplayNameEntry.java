package org.gluu.oxtrust.model;

import java.io.Serializable;

import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.xdi.ldap.model.Entry;

/**
 * Entry with display Name attribute
 * 
 * @author Yuriy Movchan Date: 11.08.2010
 */
@LdapEntry(sortBy = { "displayName" })
public class DisplayNameEntry extends Entry implements Serializable {

	private static final long serialVersionUID = 2536007777903091939L;

	public DisplayNameEntry() {
	}

	public DisplayNameEntry(String dn, String inum, String displayName) {
		super(dn);
		this.inum = inum;
		this.displayName = displayName;
	}

	@LdapAttribute(ignoreDuringUpdate = true)
	private String inum;

	@LdapAttribute
	private String displayName;

	@LdapAttribute
	private String uid;

	public String getInum() {
		return inum;
	}

	public void setInum(String inum) {
		this.inum = inum;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	@Override
	public String toString() {
		return String.format("DisplayNameEntry [displayName=%s, inum=%s, toString()=%s]", displayName, inum, super.toString());
	}

	/**
	 * @param uid
	 *            the uid to set
	 */
	public void setUid(String uid) {
		this.uid = uid;
	}

	/**
	 * @return the uid
	 */
	public String getUid() {
		return uid;
	}

}
