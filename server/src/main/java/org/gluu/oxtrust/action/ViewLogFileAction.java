/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxtrust.action;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.gluu.oxtrust.ldap.service.ApplianceService;
import org.gluu.oxtrust.model.GluuAppliance;
import org.gluu.oxtrust.model.LogViewerConfig;
import org.gluu.oxtrust.util.OxTrustConstants;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.security.Restrict;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.log.Log;
import org.xdi.model.SimpleCustomProperty;
import org.xdi.service.JsonService;
import org.xdi.util.StringHelper;
import org.xdi.util.io.ReverseLineReader;

/**
 * Action class for configuring log viewer
 * 
 * @author Yuriy Movchan Date: 07/08/2013
 */
@Name("viewLogFileAction")
@Scope(ScopeType.CONVERSATION)
@Restrict("#{identity.loggedIn}")
public class ViewLogFileAction implements Serializable {

	private static final long serialVersionUID = -3310340481895022468L;

	@Logger
	private Log log;

	@In
	private FacesMessages facesMessages;

	@In
	private JsonService jsonService;

	private GluuAppliance appliance;

	private LogViewerConfig logViewerConfiguration;
	private Map<Integer, String> logFiles;

	private boolean initialized;

	private int activeLogFileIndex;

	private int displayLastLinesCount;

	@Restrict("#{s:hasPermission('log', 'access')}")
	public String init() {
		if (this.logViewerConfiguration != null) {
			return OxTrustConstants.RESULT_SUCCESS;
		}

		this.appliance = ApplianceService.instance().getAppliance();

		initConfigurations();
		
		this.activeLogFileIndex = -1;
		
		this.displayLastLinesCount = 400;
		this.initialized = true;

		return OxTrustConstants.RESULT_SUCCESS;
	}

	private void initConfigurations() {
		this.logViewerConfiguration = prepareLogViewerConfig();

		this.logFiles = prepareLogFiles();
	}

	private LogViewerConfig prepareLogViewerConfig() {
		LogViewerConfig logViewerConfig = null;

		String oxLogViewerConfig = appliance.getOxLogViewerConfig();
		if (StringHelper.isNotEmpty(oxLogViewerConfig)) {
			try {
				logViewerConfig = jsonService.jsonToObject(appliance.getOxLogViewerConfig(), LogViewerConfig.class);
			} catch (Exception ex) {
				log.error("Failed to load log viewer configuration '{0}'", ex, oxLogViewerConfig);
			}
		}

		if (logViewerConfig == null) {
			logViewerConfig = new LogViewerConfig();
		}

		return logViewerConfig;
	}

	private Map<Integer, String> prepareLogFiles() {
		Map<Integer, String> logFiles = new HashMap<Integer, String>();

		int fileIndex = 0;
		for (SimpleCustomProperty logTemplate : this.logViewerConfiguration.getLogTemplates()) {
			String logTemplatePattern = logTemplate.getValue2();
			if (StringHelper.isEmpty(logTemplatePattern)) {
				continue;
			}

			String logTemplatePath = FilenameUtils.getFullPath(logTemplatePattern);
			String logTemplateFile = FilenameUtils.getName(logTemplatePattern);

			File logTemplateBaseDir = new File(logTemplatePath);

			FileFilter fileFilter = new AndFileFilter(FileFileFilter.FILE, new WildcardFileFilter(logTemplateFile));
			File[] files = logTemplateBaseDir.listFiles(fileFilter);
			if (files == null) {
				continue;
			}

			for (int i = 0; i < files.length; i++) {
				logFiles.put(fileIndex++, files[i].getPath());
			}
		}

		return logFiles;
	}

	public boolean isInitialized() {
		return initialized;
	}

	public LogViewerConfig getLogViewerConfiguration() {
		return logViewerConfiguration;
	}

	public Map<Integer, String> getLogFiles() {
		return logFiles;
	}

	public String getTailOfLogFile() {
		if (this.activeLogFileIndex == -1) {
			return "";
		}

		File activeLogFile = new File(this.logFiles.get(activeLogFileIndex));
		ReverseLineReader reverseLineReader = new ReverseLineReader(activeLogFile, Charset.defaultCharset().name());
		try {
			List<String> logFileLastLines = reverseLineReader.readLastLines(this.displayLastLinesCount);
			
			StringBuilder sb = new StringBuilder();
			for (String logFileLastLine : logFileLastLines) {
				sb.append(logFileLastLine);
				sb.append('\n');
			}
			
			return sb.toString();
		} catch (IOException ex) {
			log.error("Failed to read log file '{0}'", ex, this.logFiles.get(activeLogFileIndex));
			String result = String.format("Failed to read log file '%s'", this.logFiles.get(activeLogFileIndex));
			
			return result;
		} finally {
			try {
				reverseLineReader.close();
			} catch (IOException ex) {
				log.error("Failed to destory ReverseLineReader", ex);
			}
		}
	}

	public int getActiveLogFileIndex() {
		return activeLogFileIndex;
	}

	public void setActiveLogFileIndex(int activeLogFileIndex) {
		this.activeLogFileIndex = activeLogFileIndex;
	}


	public int getDisplayLastLinesCount() {
		return displayLastLinesCount;
	}

	public void setDisplayLastLinesCount(int displayLinesCount) {
		this.displayLastLinesCount = displayLinesCount;
	}

}
