/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxtrust.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.gluu.oxtrust.ldap.service.OrganizationService;
import org.gluu.oxtrust.model.GluuCustomPerson;
import org.gluu.oxtrust.model.GluuOrganization;
import org.gluu.oxtrust.model.RegistrationConfiguration;
import org.gluu.oxtrust.model.RegistrationInterceptorScript;
import org.gluu.oxtrust.service.python.interfaces.RegistrationScript;
import org.gluu.oxtrust.util.OxTrustConstants;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;
import org.python.core.PyLong;
import org.python.core.PyObject;
import org.xdi.exception.PythonException;
import org.xdi.model.SimpleCustomProperty;
import org.xdi.oxauth.model.util.Util;
import org.xdi.service.PythonService;

/**
 * @author Oleksiy Tataryn
 *
 */
@Name("registrationInterceptionService")
@Scope(ScopeType.STATELESS)
@AutoCreate
public class RegistrationInterceptionService {

	@Logger
	private Log log;

	public boolean runPreRegistrationScripts(GluuCustomPerson person, Map<String, String[]> requestParameters) {
		GluuOrganization org = OrganizationService.instance().getOrganization();
		RegistrationConfiguration config = org.getOxRegistrationConfiguration();
		if(config != null && config.isRegistrationInterceptorsConfigured()){
			List<RegistrationInterceptorScript> scripts = config.getRegistrationInterceptorScripts();
			List<RegistrationInterceptorScript> sortedEnabledPreregistrationScripts = sort(getActive(getPreregistrationScripts(scripts)));
			if(sortedEnabledPreregistrationScripts != null){
				boolean result = true;
				for(RegistrationInterceptorScript script: sortedEnabledPreregistrationScripts){
					RegistrationScript registrationScript = createRegistrationScriptFromStringWithPythonException(script);
					result &= registrationScript.execute(script.getCustomAttributes(), person, requestParameters);
				}
				return result;
			}else{
				return true;
			}
		}else{
			return true;
		}
	}

	private RegistrationScript createRegistrationScriptFromStringWithPythonException(
			RegistrationInterceptorScript script) {
		String pythonScriptText = script.getCustomScript();
		if (pythonScriptText == null) {
			return null;
		}

		RegistrationScript pythonScript = null;

		PythonService pythonService = PythonService.instance();
		InputStream bis = null;
		try {
            bis = new ByteArrayInputStream(pythonScriptText.getBytes(Util.UTF8_STRING_ENCODING));
            pythonScript = pythonService.loadPythonScript(bis, "RegistrationScriptClass",
            		RegistrationScript.class, new PyObject[] { new PyLong(System.currentTimeMillis()) });
		} catch (UnsupportedEncodingException e) {
            log.error(e.getMessage(), e);
        } catch (PythonException e) {
        	  log.error(e.getMessage(), e);
		} finally {
			IOUtils.closeQuietly(bis);
		}

		return pythonScript;
	}

	private List<RegistrationInterceptorScript> sort(List<RegistrationInterceptorScript> scripts) {
		if(scripts == null || scripts.isEmpty()){
			return null;
		}else{
			Collections.sort(scripts);
			return scripts;
		}
	}

	private List<RegistrationInterceptorScript> getActive(List<RegistrationInterceptorScript> scripts) {
		if(scripts == null || scripts.isEmpty()){
			return null;
		}else{
			List<RegistrationInterceptorScript> activeScripts = new ArrayList<RegistrationInterceptorScript>();
			for(RegistrationInterceptorScript script: scripts){
				if(script.isEnabled()){
					activeScripts.add(script);
				}
			}
			return activeScripts;
		}
	}

	private List<RegistrationInterceptorScript> getPreregistrationScripts(
			List<RegistrationInterceptorScript> scripts) {
		if(scripts == null || scripts.isEmpty()){
			return null;
		}else{
			List<RegistrationInterceptorScript> preregistrationScripts = new ArrayList<RegistrationInterceptorScript>();
			for(RegistrationInterceptorScript script: scripts){
				if(script.getType().equals(OxTrustConstants.PRE_REGISTRATION_SCRIPT)){
					preregistrationScripts.add(script);
				}
			}
			return preregistrationScripts;
		}
		
	}

	public boolean runPostRegistrationScripts(GluuCustomPerson person, Map<String, String[]> requestParameters) {
		GluuOrganization org = OrganizationService.instance().getOrganization();
		RegistrationConfiguration config = org.getOxRegistrationConfiguration();
		if(config.isRegistrationInterceptorsConfigured()){
			List<RegistrationInterceptorScript> scripts = config.getRegistrationInterceptorScripts();
			List<RegistrationInterceptorScript> sortedEnabledPreregistrationScripts = sort(getActive(getPostregistrationScripts(scripts)));
			if(sortedEnabledPreregistrationScripts != null){
				boolean result = true;
				for(RegistrationInterceptorScript script: sortedEnabledPreregistrationScripts){
					RegistrationScript registrationScript = createRegistrationScriptFromStringWithPythonException(script);
					result &= registrationScript.execute(script.getCustomAttributes(), person, requestParameters);
				}
				return result;
			}else{
				return true;
			}
		}else{
			return true;
		}
	}

	private List<RegistrationInterceptorScript> getPostregistrationScripts(
			List<RegistrationInterceptorScript> scripts) {
		if(scripts == null || scripts.isEmpty()){
			return null;
		}else{
			List<RegistrationInterceptorScript> postregistrationScripts = new ArrayList<RegistrationInterceptorScript>();
			for(RegistrationInterceptorScript script: scripts){
				if(script.getType().equals(OxTrustConstants.POST_REGISTRATION_SCRIPT)){
					postregistrationScripts.add(script);
				}
			}
			return postregistrationScripts;
		}
		
	}
}
