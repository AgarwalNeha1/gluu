package org.gluu.oxtrust.ldap.service;

import java.util.List;
import java.util.Map;

import org.gluu.oxtrust.model.GluuCustomAttribute;
import org.gluu.oxtrust.model.GluuCustomPerson;
import org.gluu.oxtrust.model.User;
import org.gluu.site.ldap.exception.DuplicateEntryException;
import org.gluu.site.ldap.persistence.AttributeData;
import org.xdi.model.GluuAttribute;

public interface IPersonService {

	public abstract void addCustomObjectClass(GluuCustomPerson person);

	/**
	 * Add new person
	 * 
	 * @param person
	 *            Person
	 * @throws DuplicateEntryException
	 */
	// TODO: Review this methods. We need to check if uid is unique in outside
	// method
	public abstract void addPerson(GluuCustomPerson person) throws DuplicateEntryException;

	/**
	 * Add person entry
	 * 
	 * @param person
	 *            Person
	 */
	public abstract void updatePerson(GluuCustomPerson person);

	/**
	 * Remove person with persona and contacts branches
	 * 
	 * @param person
	 *            Person
	 */
	public abstract void removePerson(GluuCustomPerson person);

	/**
	 * Search persons by pattern
	 * 
	 * @param pattern
	 *            Pattern
	 * @param sizeLimit
	 *            Maximum count of results
	 * @return List of persons
	 */
	public abstract List<GluuCustomPerson> searchPersons(String pattern, int sizeLimit);

	/**
	 * Search persons by sample object
	 * 
	 * @param person
	 *            Person with set attributes relevant to he current search (for
	 *            example gluuAllowPublication)
	 * @param sizeLimit
	 *            Maximum count of results
	 * @return List of persons
	 */
	public abstract List<GluuCustomPerson> findPersons(GluuCustomPerson person, int sizeLimit);

	/**
	 * Search persons by pattern
	 * 
	 * @param pattern
	 *            Pattern
	 * @param sizeLimit
	 *            Maximum count of results
	 * @param excludedPersons
	 *            list of uids that we don't want returned by service
	 * @return List of persons
	 */
	public abstract List<GluuCustomPerson> searchPersons(String pattern, int sizeLimit, List<GluuCustomPerson> excludedPersons)
			throws Exception;

	public abstract List<GluuCustomPerson> findAllPersons(String[] returnAttributes);

	public abstract List<GluuCustomPerson> findPersonsByUids(List<String> uids, String[] returnAttributes) throws Exception;

	public abstract GluuCustomPerson findPersonByDn(String dn, String... returnAttributes);

	/**
	 * Check if LDAP server contains person with specified attributes
	 * 
	 * @return True if person with specified attributes exist
	 */
	public abstract boolean containsPerson(GluuCustomPerson person);

	public abstract boolean contains(String dn);

	/**
	 * Get person by DN
	 * 
	 * @param dn
	 *            Dn
	 * @return Person
	 */
	public abstract GluuCustomPerson getPersonByDn(String dn);

	/**
	 * Get person by inum
	 * 
	 * @param returnClass
	 *            POJO class which EntryManager should use to return entry
	 *            object
	 * @param inum
	 *            Inum
	 * @return Person
	 */
	public abstract GluuCustomPerson getPersonByInum(String inum);

	/**
	 * Get person by uid
	 * 
	 * @param uid
	 *            Uid
	 * @return Person
	 */
	public abstract GluuCustomPerson getPersonByUid(String uid);

	public abstract int countPersons();

	/**
	 * Generate new inum for person
	 * 
	 * @return New inum for person
	 */
	public abstract String generateInumForNewPerson();

	public abstract String generateInameForNewPerson(String uid);

	/**
	 * Build DN string for person
	 * 
	 * @param inum
	 *            Inum
	 * @return DN string for specified person or DN for persons branch if inum
	 *         is null
	 * @throws Exception
	 */
	public abstract String getDnForPerson(String inum);

	/**
	 * Authenticate user
	 * 
	 * @param userName
	 *            User name
	 * @param password
	 *            User password
	 * @return
	 */
	public abstract boolean authenticate(String userName, String password);

	public abstract List<GluuCustomAttribute> getMandatoryAtributes();

	public abstract String getPersonString(List<GluuCustomPerson> persons) throws Exception;

	public abstract List<GluuCustomPerson> createEntities(Map<String, List<AttributeData>> entriesAttributes) throws Exception;

	public abstract boolean isMemberOrOwner(String[] groupDNs, String personDN) throws Exception;

	/**
	 * Get person by email
	 * 
	 * @param email
	 *            email
	 * @return Person
	 */
	public abstract GluuCustomPerson getPersonByEmail(String email);

	/**
	 * Get person by attribute
	 * 
	 * @param attribute
	 *            attribute
	 * @param value
	 *            value
	 * @return Person
	 */
	public abstract GluuCustomPerson getPersonByAttribute(String attribute, String value) throws Exception;

	/**
	 * Remove custom attribute from all persons.
	 */
	public abstract void removeAttribute(GluuAttribute attribute);

	/**
	 * Get user by uid
	 * 
	 * @param uid
	 *            Uid
	 * @return User
	 */
	public abstract User getUserByUid(String uid);

}