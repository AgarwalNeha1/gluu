package org.gluu.oxtrust.util;

import org.gluu.oxtrust.model.GluuCustomPerson;
import org.gluu.oxtrust.model.OxAuthClient;
import org.gluu.oxtrust.model.association.ClientAssociation;
import org.gluu.oxtrust.model.association.PersonAssociation;
import org.jboss.seam.annotations.Name;

@Name("mapperUtil")
public class MapperUtil {

	/**
	 * Maps persons association attribute with personAssociation
	 * 
	 * @param source
	 * @param destination
	 * @return
	 */
	public static PersonAssociation map(GluuCustomPerson source,

	PersonAssociation destination) {

		if (source == null) {
			return null;
		}

		if (destination == null) {
			destination = new PersonAssociation();
		}

		destination.setUserAssociation(source.getInum());
		destination.setEntryAssociations(source.getAssociatedClient());

		return destination;

	}

	/**
	 * Maps Clients association attribute with ClientAssociation
	 * 
	 * @param source
	 * @param destination
	 * @return
	 */
	public static ClientAssociation map(OxAuthClient source, ClientAssociation destination) {

		if (source == null) {

			return null;
		}

		if (destination == null) {
			destination = new ClientAssociation();
		}

		destination.setEntryAssociation(source.getInum());
		destination.setUserAssociations(source.getAssociatedPersons());

		return destination;

	}

}
