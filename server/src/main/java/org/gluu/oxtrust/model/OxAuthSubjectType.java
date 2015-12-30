package org.gluu.oxtrust.model;

import org.gluu.site.ldap.persistence.annotation.LdapEnum;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Javier Rojas Blum
 * @version November 9, 2015
 */
public enum OxAuthSubjectType implements LdapEnum {

    PAIRWISE("pairwise", "pairwise"),
    PUBLIC("public", "public");

    private String value;
    private String displayName;

    private static Map<String, OxAuthSubjectType> mapByValues = new HashMap<String, OxAuthSubjectType>();

    static {
        for (OxAuthSubjectType enumType : values()) {
            mapByValues.put(enumType.getValue(), enumType);
        }
    }

    private OxAuthSubjectType(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public String getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static OxAuthSubjectType getByValue(String value) {
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
