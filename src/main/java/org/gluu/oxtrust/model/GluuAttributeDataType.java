package org.gluu.oxtrust.model;

import java.util.HashMap;
import java.util.Map;

import org.gluu.site.ldap.persistence.annotation.LdapEnum;

/**
 * Attribute Data Type
 * 
 * @author Yuriy Movchan Date: 10.07.2010
 */
public enum GluuAttributeDataType implements LdapEnum {

	STRING("string", "Text"), NUMERIC("numeric", "Numeric"), PHOTO("photo", "Photo"), DATE("generalizedTime", "Date");

	private String value;
	private String displayName;

	private static Map<String, GluuAttributeDataType> mapByValues = new HashMap<String, GluuAttributeDataType>();

	static {
		for (GluuAttributeDataType enumType : values()) {
			mapByValues.put(enumType.getValue(), enumType);
		}
	}

	private GluuAttributeDataType(String value, String displayName) {
		this.value = value;
		this.displayName = displayName;
	}

	public String getValue() {
		return value;
	}

	public String getDisplayName() {
		return displayName;
	}

	public static GluuAttributeDataType getByValue(String value) {
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
