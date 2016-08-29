/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package org.gluu.oxtrust.service.scim2;

import org.gluu.oxtrust.ldap.service.IPersonService;
import org.gluu.oxtrust.ldap.service.PersonService;
import org.gluu.oxtrust.model.GluuCustomPerson;
import org.gluu.oxtrust.model.scim2.User;
import org.gluu.oxtrust.service.external.ExternalScimService;
import org.gluu.oxtrust.util.CopyUtils2;
import org.gluu.oxtrust.util.Utils;
import org.gluu.site.ldap.exception.DuplicateEntryException;
import org.gluu.site.ldap.persistence.exception.EntryPersistenceException;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.jboss.seam.log.Log;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Centralizes calls by the UserWebService and BulkWebService service classes
 *
 * @author Val Pecaoco
 */
@Name("scim2UserService")
@Scope(ScopeType.STATELESS)
@AutoCreate
public class Scim2UserService implements Serializable {

    @Logger
    private Log log;

    @In
    private IPersonService personService;

    @In
    private ExternalScimService externalScimService;

    public User createUser(User user) throws Exception {

        log.debug(" copying gluuperson ");
        GluuCustomPerson gluuPerson = CopyUtils2.copy(user, null, false);
        if (gluuPerson == null) {
            throw new Exception("Scim2UserService.createUser(): Failed to create user; GluuCustomPerson is null");
        }

        personService = PersonService.instance();

        log.debug(" generating inum ");
        String inum = personService.generateInumForNewPerson(); // inumService.generateInums(Configuration.INUM_TYPE_PEOPLE_SLUG);
        // //personService.generateInumForNewPerson();
        log.debug(" getting DN ");
        String dn = personService.getDnForPerson(inum);

        log.debug(" getting iname ");
        String iname = personService.generateInameForNewPerson(user.getUserName());

        log.debug(" setting dn ");
        gluuPerson.setDn(dn);

        log.debug(" setting inum ");
        gluuPerson.setInum(inum);

        log.debug(" setting iname ");
        gluuPerson.setIname(iname);

        log.debug(" setting commonName ");
        gluuPerson.setCommonName(gluuPerson.getGivenName() + " " + gluuPerson.getSurname());

        log.info("gluuPerson.getMemberOf().size() : " + gluuPerson.getMemberOf().size());
        if (user.getGroups().size() > 0) {
            log.info(" jumping to groupMembersAdder ");
            log.info("gluuPerson.getDn() : " + gluuPerson.getDn());
            Utils.groupMembersAdder(gluuPerson, gluuPerson.getDn());
        }

        // As per spec, the SP must be the one to assign the meta attributes
        log.info(" Setting meta: create user ");
        DateTimeFormatter dateTimeFormatter = ISODateTimeFormat.dateTime().withZoneUTC();  // Date should be in UTC format
        Date dateCreated = DateTime.now().toDate();
        String relativeLocation = "/scim/v2/Users/" + inum;
        gluuPerson.setAttribute("oxTrustMetaCreated", dateTimeFormatter.print(dateCreated.getTime()));
        gluuPerson.setAttribute("oxTrustMetaLastModified", dateTimeFormatter.print(dateCreated.getTime()));
        gluuPerson.setAttribute("oxTrustMetaLocation", relativeLocation);

        // Sync email, forward ("oxTrustEmail" -> "mail")
        gluuPerson = Utils.syncEmailForward(gluuPerson, true);

        // For custom script: create user
        if (externalScimService.isEnabled()) {
            externalScimService.executeScimCreateUserMethods(gluuPerson);
        }

        log.debug("adding new GluuPerson");
        personService.addPerson(gluuPerson);

        User createdUser = CopyUtils2.copy(gluuPerson, null);

        return createdUser;
    }

    public User updateUser(String id, User user) throws Exception {

        personService = PersonService.instance();

        GluuCustomPerson gluuPerson = personService.getPersonByInum(id);
        if (gluuPerson == null) {

            throw new EntryPersistenceException("Scim2UserService.updateUser(): " + "Resource " + id + " not found");

        } else {

            // Validate if attempting to update userName of a different id
            if (user.getUserName() != null) {

                GluuCustomPerson personToFind = new GluuCustomPerson();
                personToFind.setUid(user.getUserName());

                List<GluuCustomPerson> foundPersons = personService.findPersons(personToFind, 2);
                if (foundPersons != null && foundPersons.size() > 0) {
                    for (GluuCustomPerson foundPerson : foundPersons) {
                        if (foundPerson != null && !foundPerson.getInum().equalsIgnoreCase(gluuPerson.getInum())) {
                            throw new DuplicateEntryException("Cannot update userName of a different id: " + user.getUserName());
                        }
                    }
                }
            }
        }

        GluuCustomPerson updatedGluuPerson = CopyUtils2.copy(user, gluuPerson, true);

        if (user.getGroups().size() > 0) {
            Utils.groupMembersAdder(updatedGluuPerson, personService.getDnForPerson(id));
        }

        log.info(" Setting meta: update user ");
        DateTimeFormatter dateTimeFormatter = ISODateTimeFormat.dateTime().withZoneUTC();  // Date should be in UTC format
        Date dateLastModified = DateTime.now().toDate();
        updatedGluuPerson.setAttribute("oxTrustMetaLastModified", dateTimeFormatter.print(dateLastModified.getTime()));
        if (updatedGluuPerson.getAttribute("oxTrustMetaLocation") == null || (updatedGluuPerson.getAttribute("oxTrustMetaLocation") != null && updatedGluuPerson.getAttribute("oxTrustMetaLocation").isEmpty())) {
            String relativeLocation = "/scim/v2/Users/" + id;
            updatedGluuPerson.setAttribute("oxTrustMetaLocation", relativeLocation);
        }

        // Sync email, forward ("oxTrustEmail" -> "mail")
        updatedGluuPerson = Utils.syncEmailForward(updatedGluuPerson, true);

        // For custom script: update user
        if (externalScimService.isEnabled()) {
            externalScimService.executeScimUpdateUserMethods(updatedGluuPerson);
        }

        personService.updatePerson(updatedGluuPerson);

        log.debug(" person updated ");

        User updatedUser = CopyUtils2.copy(updatedGluuPerson, null);

        return updatedUser;
    }

    public void deleteUser(String id) throws Exception {

        personService = PersonService.instance();

        GluuCustomPerson gluuPerson = personService.getPersonByInum(id);
        if (gluuPerson == null) {

            throw new EntryPersistenceException("Scim2UserService.deleteUser(): " + "Resource " + id + " not found");

        } else {

            // For custom script: delete user
            if (externalScimService.isEnabled()) {
                externalScimService.executeScimDeleteUserMethods(gluuPerson);
            }

            log.info("person.getMemberOf().size() : " + gluuPerson.getMemberOf().size());
            if (gluuPerson.getMemberOf() != null) {

                if (gluuPerson.getMemberOf().size() > 0) {

                    String dn = personService.getDnForPerson(id);
                    log.info("DN : " + dn);

                    Utils.deleteUserFromGroup(gluuPerson, dn);
                }
            }

            personService.removePerson(gluuPerson);
        }
    }
}
