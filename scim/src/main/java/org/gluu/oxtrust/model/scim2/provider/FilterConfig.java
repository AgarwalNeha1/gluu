/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package org.gluu.oxtrust.model.scim2.provider;

import java.io.Serializable;

/**
 * A complex type that specifies FILTER configuration options.
 */
public class FilterConfig implements Serializable {

	private final boolean supported;
	private final long maxResults;

	/**
	 * Create a <code>FilterConfig</code> instance.
	 *
	 * @param supported
	 *            Specifies whether the FILTER operation is supported.
	 * @param maxResults
	 *            Specifies the maximum number of resources returned in a
	 *            response.
	 */
	public FilterConfig(final boolean supported, final long maxResults) {
		this.supported = supported;
		this.maxResults = maxResults;
	}

	/**
	 * Indicates whether the FILTER operation is supported.
	 * 
	 * @return {@code true} if the FILTER operation is supported.
	 */
	public boolean isSupported() {
		return supported;
	}

	public long getMaxResults() {
		return maxResults;
	}
}
