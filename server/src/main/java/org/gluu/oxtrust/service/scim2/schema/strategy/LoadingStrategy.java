/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxtrust.service.scim2.schema.strategy;

import org.gluu.oxtrust.model.scim2.schema.SchemaType;

/**
 * Loading strategy
 *
 * @author Val Pecaoco
 */
public interface LoadingStrategy {

    SchemaType load(SchemaType schemaType) throws Exception;
}
