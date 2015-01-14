/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxtrust.ldap.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.gluu.oxtrust.model.DisplayNameEntry;
import org.gluu.oxtrust.util.OxTrustConstants;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;
import org.xdi.ldap.model.Entry;
import org.xdi.service.CacheService;

import com.unboundid.ldap.sdk.Filter;

/**
 * Provides operations with DisplayNameEntry
 * 
 * @author Yuriy Movchan Date: 11.08.2010
 */
@Scope(ScopeType.STATELESS)
@Name("lookupService")
@AutoCreate
public class LookupService implements Serializable {

	private static final long serialVersionUID = -3707238475653913313L;

	@Logger
	private Log log;

	@In
	private LdapEntryManager ldapEntryManager;

	@In
	private CacheService cacheService;

	/**
	 * Returns DisplayNameEntry based on display name
	 * 
	 * @param dn
	 *            display name
	 * @return DisplayNameEntry object
	 */
	public DisplayNameEntry getDisplayNameEntry(String dn) throws Exception {
		String key = dn;
		DisplayNameEntry entry = (DisplayNameEntry) cacheService.get(OxTrustConstants.CACHE_LOOKUP_NAME, key);
		if (entry == null) {
			entry = ldapEntryManager.find(DisplayNameEntry.class, key);

			cacheService.put(OxTrustConstants.CACHE_LOOKUP_NAME, key, entry);
		}

		return entry;
	}

	/**
	 * Returns list of DisplayNameEntry objects
	 * 
	 * @param baseDn
	 *            base DN
	 * @param dns
	 *            list of display names to find
	 * @return list of DisplayNameEntry objects
	 */
	@SuppressWarnings("unchecked")
	public List<DisplayNameEntry> getDisplayNameEntries(String baseDn, List<String> dns) {
		List<String> inums = getInumsFromDns(dns);
		if (inums.size() == 0) {
			return null;
		}

		String key = getCompoundKey(inums);
		List<DisplayNameEntry> entries = (List<DisplayNameEntry>) cacheService.get(OxTrustConstants.CACHE_LOOKUP_NAME, key);
		if (entries == null) {
			Filter searchFilter = buildInumFilter(inums);

			entries = ldapEntryManager.findEntries(baseDn, DisplayNameEntry.class, searchFilter);

			cacheService.put(OxTrustConstants.CACHE_LOOKUP_NAME, key, entries);
		}

		return entries;
	}

	public Filter buildInumFilter(List<String> inums) {
		List<Filter> inumFilters = new ArrayList<Filter>(inums.size());
		for (String inum : inums) {
			inumFilters.add(Filter.createEqualityFilter(OxTrustConstants.inum, inum));
		}

		Filter searchFilter = Filter.createORFilter(inumFilters);

		return searchFilter;
	}

	public List<String> getInumsFromDns(List<String> dns) {
		List<String> inums = new ArrayList<String>();

		if (dns == null) {
			return inums;
		}

		for (String dn : dns) {
			String inum = getInumFromDn(dn);
			if (inum != null) {
				inums.add(inum);
			}
		}

		Collections.sort(inums);
		
		return inums;
	}
	
	private String getCompoundKey(List<String> inums) {
		StringBuilder compoundKey = new StringBuilder();
		for (String inum : inums) {
			if (compoundKey.length() > 0) {
				compoundKey.append("_");
			}
			compoundKey.append(inum);
		}
		
		return compoundKey.toString();
	}

	public List<DisplayNameEntry> getDisplayNameEntriesByEntries(String baseDn, List<? extends Entry> entries) throws Exception {
		if (entries == null) {
			return null;
		}

		List<String> dns = new ArrayList<String>(entries.size());
		for (Entry entry : entries) {
			dns.add(entry.getDn());
		}

		return getDisplayNameEntries(baseDn, dns);
	}

	/**
	 * Get inum from DN
	 * 
	 * @param dn
	 *            DN
	 * @return Inum
	 */
	public String getInumFromDn(String dn) {
		if (dn == null) {
			return null;
		}

		if (!dn.startsWith("inum=")) {
			return null;
		}

		int idx = dn.indexOf(",", 5);
		if (idx == -1) {
			return null;
		}

		return dn.substring(5, idx);
	}

	/**
	 * Get lookupService instance
	 * 
	 * @return LookupService instance
	 */
	public static LookupService instance() throws Exception {
		return (LookupService) Component.getInstance(LookupService.class);
	}

}
