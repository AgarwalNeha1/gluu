package org.gluu.oxtrust.model;

import java.util.HashMap;
import java.util.Map;

import org.gluu.site.ldap.persistence.annotation.LdapEnum;

/**
 * oxAuth Application type
 * 
 * @author Reda Zerrad Date: 06.11.2012
 */
public enum OxAuthApplicationType implements LdapEnum {

	WEB("web", "Web"), NATIVE("native", "Native");

	private String value;
	private String displayName;

	private static Map<String, OxAuthApplicationType> mapByValues = new HashMap<String, OxAuthApplicationType>();

	static {
		for (OxAuthApplicationType enumType : values()) {
			mapByValues.put(enumType.getValue(), enumType);
		}
	}

	private OxAuthApplicationType(String value, String displayName) {
		this.value = value;
		this.displayName = displayName;
	}

	public String getValue() {
		return value;
	}

	public String getDisplayName() {
		return displayName;
	}

	public static OxAuthApplicationType getByValue(String value) {
		return mapByValues.get(value);
	}

	public Enum<? extends LdapEnum> resolveByValue(String value) {
		return getByValue(value);
	}

	@Override
	public String toString() {
		return value;
	}

}
