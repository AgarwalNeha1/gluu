package org.gluu.oxtrust.ldap.service;

import java.util.List;

import org.gluu.oxtrust.model.GluuGroup;
import org.gluu.oxtrust.model.GluuGroupVisibility;

public interface IGroupService {

	/**
	 * Add new group entry
	 * 
	 * @param group
	 *            Group
	 */
	public abstract void addGroup(GluuGroup group) throws Exception;

	/**
	 * Remove group entry
	 * 
	 * @param group
	 *            Group
	 */
	public abstract void removeGroup(GluuGroup group);

	/**
	 * Get all groups
	 * 
	 * @return List of groups
	 */
	public abstract List<GluuGroup> getAllGroups();

	/**
	 * Check if person is a member or owner of specified group
	 * 
	 * @param groupDN
	 *            Group DN
	 * @param personDN
	 *            Person DN
	 * @return True if person is a member or owner of specified group
	 */
	public abstract boolean isMemberOrOwner(String groupDN, String personDN);

	/**
	 * Get group by inum
	 * 
	 * @param inum
	 *            Group Inum
	 * @return Group
	 */
	public abstract GluuGroup getGroupByInum(String inum);

	/**
	 * Build DN string for group
	 * 
	 * @param inum
	 *            Group Inum
	 * @return DN string for specified group or DN for groups branch if inum is
	 *         null
	 * @throws Exception
	 */
	public abstract String getDnForGroup(String inum);

	/**
	 * Update group entry
	 * 
	 * @param group
	 *            Group
	 */
	public abstract void updateGroup(GluuGroup group) throws Exception;

	public abstract int countGroups();

	/**
	 * Generate new inum for group
	 * 
	 * @return New inum for group
	 * @throws Exception
	 */
	public abstract String generateInumForNewGroup() throws Exception;

	/**
	 * Generate new iname for group
	 * 
	 * @return New iname for group
	 */
	public abstract String generateInameForNewGroup(String name) throws Exception;

	/**
	 * Search groups by pattern
	 * 
	 * @param pattern
	 *            Pattern
	 * @param sizeLimit
	 *            Maximum count of results
	 * @return List of groups
	 */
	public abstract List<GluuGroup> searchGroups(String pattern, int sizeLimit) throws Exception;

	/**
	 * Get all available visibility types
	 * 
	 * @return Array of visibility types
	 */
	public abstract GluuGroupVisibility[] getVisibilityTypes() throws Exception;

	/**
	 * returns a list of all groups
	 * 
	 * @return list of groups
	 */

	public abstract List<GluuGroup> getAllGroupsList() throws Exception;

	/**
	 * returns GluuGroup by Dn
	 * 
	 * @return GluuGroup
	 */

	public abstract GluuGroup getGroupByDn(String Dn);

	/**
	 * Get Group by iname
	 * 
	 * @param iname
	 * @return Group
	 */
	public abstract GluuGroup getGroupByIname(String iname) throws Exception;

	/**
	 * Get group by DisplayName
	 * 
	 * @param DisplayName
	 * @return group
	 */
	public abstract GluuGroup getGroupByDisplayName(String DisplayName) throws Exception;

}