package org.gluu.oxtrust.model;

import java.util.HashMap;
import java.util.Map;

import org.gluu.site.ldap.persistence.annotation.LdapEnum;

/**
 * @author �Oleksiy Tataryn�
 */
public enum GluuValidationStatus implements LdapEnum {

	VALIDATION("validation", "Validation"), VALIDATION_FAILED("validation failed", "Validation Failed"), VALIDATION_SCHEDULED(
			"validation_scheduled", "Validation Scheduled"), VALIDATION_SUCCESS("validation_success", "Validation Success");

	private String value;
	private String displayName;

	private static Map<String, GluuValidationStatus> mapByValues = new HashMap<String, GluuValidationStatus>();
	static {
		for (GluuValidationStatus enumType : values()) {
			mapByValues.put(enumType.getValue(), enumType);
		}
	}

	private GluuValidationStatus(String value, String displayName) {
		this.value = value;
		this.displayName = displayName;
	}

	public String getValue() {
		return value;
	}

	public String getDisplayName() {
		return displayName;
	}

	public static GluuValidationStatus getByValue(String value) {
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
