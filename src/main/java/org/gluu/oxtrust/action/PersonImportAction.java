package org.gluu.oxtrust.action;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.gluu.oxtrust.ldap.load.conf.ImportPersonConfiguration;
import org.gluu.oxtrust.ldap.service.ExcelService;
import org.gluu.oxtrust.ldap.service.PersonService;
import org.gluu.oxtrust.model.GluuAttribute;
import org.gluu.oxtrust.model.GluuAttributeDataType;
import org.gluu.oxtrust.model.GluuCustomPerson;
import org.gluu.oxtrust.model.table.Table;
import org.gluu.oxtrust.util.OxTrustConstants;
import org.gluu.site.ldap.persistence.AttributeData;
import org.gluu.site.ldap.persistence.exception.EntryPersistenceException;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.security.Restrict;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage.Severity;
import org.jboss.seam.international.StatusMessages;
import org.jboss.seam.log.Log;
import org.richfaces.event.FileUploadEvent;
import org.richfaces.model.UploadedFile;
import org.xdi.util.StringHelper;

/**
 * Action class for load persons from Excel file
 * 
 * @author Yuriy Movchan Date: 02.14.2011
 */
@Name("personImportAction")
@Scope(ScopeType.CONVERSATION)
@Restrict("#{identity.loggedIn}")
public class PersonImportAction implements Serializable {

	private static final long serialVersionUID = -1270460481895022468L;

	private static final String[] PERSON_IMPORT_PERSON_LOCKUP_RETURN_ATTRIBUTES = { "uid", "displayName" };
	public static final String PERSON_PASSWORD_ATTRIBUTE = "userPassword";

	@Logger
	private Log log;

	@In
	StatusMessages statusMessages;

	@In
	private PersonService personService;

	@In
	private transient ExcelService excelService;

	@In
	private FacesMessages facesMessages;

	@In
	private transient ImportPersonConfiguration importPersonConfiguration;

	private UploadedFile uploadedFile;
	private FileDataToImport fileDataToImport;
	private List<GluuAttribute> attributes;
	private Map<String, GluuAttribute> attributesDisplayNameMap;
	private byte[] fileData;

	private boolean isInitialized;

	@Restrict("#{s:hasPermission('import', 'person')}")
	public String init() {
		if (this.isInitialized) {
			return OxTrustConstants.RESULT_SUCCESS;
		}

		this.attributes = importPersonConfiguration.getAttributes();
		this.attributesDisplayNameMap = getAttributesDisplayNameMap(this.attributes);

		this.fileDataToImport = new FileDataToImport();

		this.isInitialized = true;

		return OxTrustConstants.RESULT_SUCCESS;
	}

	@Restrict("#{s:hasPermission('import', 'person')}")
	public String importPersons() throws Exception {
		if (!fileDataToImport.isReady()) {
			return OxTrustConstants.RESULT_FAILURE;
		}

		log.info("Attempting to add {0} persons", fileDataToImport.getPersons().size());
		try {
			for (GluuCustomPerson person : fileDataToImport.getPersons()) {
				String inum = personService.generateInumForNewPerson();
				String dn = personService.getDnForPerson(inum);

				person.setDn(dn);
				person.setInum(inum);

				personService.addPerson(person);
				log.debug("Added new person: {0}", person.getUid());
			}
		} catch (EntryPersistenceException ex) {
			log.error("Failed to add new person", ex);

		}

		log.info("All persons {0} added successfully", fileDataToImport.getPersons().size());

		removeFileToImport();

		return OxTrustConstants.RESULT_SUCCESS;
	}

	@Restrict("#{s:hasPermission('import', 'person')}")
	public void validateFileToImport() throws Exception {
		removeFileDataToImport();

		if (uploadedFile == null) {
			return;
		}

		if (uploadedFile != null) {
			Table table;
			InputStream is = new ByteArrayInputStream(this.fileData);
			try {
				table = excelService.readExcelFile(is);
			} finally {
				IOUtils.closeQuietly(is);
			}

			this.fileDataToImport.setTable(table);

			if (table != null) {
				this.fileDataToImport.setFileName(FilenameUtils.getName(uploadedFile.getName()));
				this.fileDataToImport.setImportAttributes(getAttributesForImport(table));
				this.fileDataToImport.setReady(true);
			}
		}

		if (this.fileDataToImport.isReady()) {
			boolean valid = prepareAndValidateImportData(this.fileDataToImport.getTable(), this.fileDataToImport.getImportAttributes());
			this.fileDataToImport.setReady(valid);

			if (!valid) {
				removeFileDataToImport();
			}
		}
	}

	@Restrict("#{s:hasPermission('import', 'person')}")
	public void cancel() {
		destroy();
	}

	@Destroy
	public void destroy() {
		removeFileDataToImport();
		removeFileToImport();
	}

	public UploadedFile getUploadedFile() {
		return uploadedFile;
	}

	public FileDataToImport getFileDataToImport() {
		return this.fileDataToImport;
	}

	public void removeFileDataToImport() {
		this.fileDataToImport.reset();
	}

	@Restrict("#{s:hasPermission('import', 'person')}")
	public void uploadFile(FileUploadEvent event) {
		removeFileToImport();

		this.uploadedFile = event.getUploadedFile();
		this.fileData = this.uploadedFile.getData();
	}

	@Restrict("#{s:hasPermission('import', 'person')}")
	public void removeFileToImport() {
		if (uploadedFile != null) {
			try {
				uploadedFile.delete();
			} catch (IOException ex) {
				log.error("Failed to remove temporary file", ex);
			}

			this.uploadedFile = null;
		}
		removeFileDataToImport();
	}

	private boolean prepareAndValidateImportData(Table table, List<ImportAttribute> importAttributes) throws Exception {
		String attributesString = getAttributesString(this.attributes);
		if ((table == null) || (importAttributes == null)) {
			facesMessages.add(Severity.ERROR, "Import failed. Missing columns: {0}", attributesString);
			return false;
		}

		List<GluuAttribute> mandatoryAttributes = getMandatoryAttributes(this.attributes);
		List<ImportAttribute> mandatoryImportAttributes = getMandatoryImportAttributes(importAttributes);
		if (mandatoryAttributes.size() != mandatoryImportAttributes.size()) {
			facesMessages.add(Severity.ERROR, "Import failed. Required columns: {0}", attributesString);
			return false;
		}

		if (table.getCountRows() < 1) {
			facesMessages.add(Severity.ERROR, "Import failed. No data found");
			return false;
		}

		// Convert Excel table to GluuCustomPersons
		List<GluuCustomPerson> persons = convertTableToPersons(table, importAttributes);
		if (persons == null) {
			return false;
		}

		// Check if person already exist
		if (!validatePersons(persons)) {
			return false;
		}

		// Fill persons with default values
		if (!setDefaultPersonAttributes(persons, importAttributes)) {
			return false;
		}

		// Store persons
		log.info("Prepared {0} persons for creation", persons.size());
		this.fileDataToImport.setPersons(persons);

		return true;
	}

	private List<ImportAttribute> getMandatoryImportAttributes(List<ImportAttribute> importAttributes) {
		List<ImportAttribute> result = new ArrayList<ImportAttribute>();
		for (ImportAttribute importAttribute : importAttributes) {
			if ((importAttribute.getCol() != -1) && importAttribute.getAttribute().isRequred()) {
				result.add(importAttribute);
			}
		}

		return result;
	}

	private List<GluuAttribute> getMandatoryAttributes(List<GluuAttribute> attributes) {
		List<GluuAttribute> result = new ArrayList<GluuAttribute>();
		for (GluuAttribute attribute : attributes) {
			if (attribute.isRequred()) {
				result.add(attribute);
			}
		}

		return result;
	}

	private boolean setDefaultPersonAttributes(List<GluuCustomPerson> persons, List<ImportAttribute> importAttributes) throws Exception {
		boolean isGeneratePassword = false;
		for (ImportAttribute importAttribute : importAttributes) {
			if (importAttribute.getAttribute().getName().equalsIgnoreCase(PERSON_PASSWORD_ATTRIBUTE)
					&& !importAttribute.getAttribute().isRequred()) {
				isGeneratePassword = true;
				break;
			}
		}

		for (GluuCustomPerson person : persons) {
			if (StringHelper.isEmpty(person.getCommonName())) {
				person.setCommonName(person.getGivenName() + " " + person.getSurname());
			} else {
				person.setCommonName(person.getCommonName() + " " + person.getGivenName() + " " + person.getSurname());
			}
			person.setDisplayName(person.getCommonName());

			String iname = personService.generateInameForNewPerson(person.getUid());
			person.setIname(iname);

			if (isGeneratePassword && StringHelper.isEmpty(person.getUserPassword())) {
				person.setUserPassword(RandomStringUtils.randomAlphanumeric(16));
			}
		}

		return true;
	}

	private boolean validatePersons(List<GluuCustomPerson> persons) throws Exception {
		Set<String> uids = new HashSet<String>();
		for (GluuCustomPerson person : persons) {
			uids.add(person.getUid());
		}

		if (uids.size() != persons.size()) {
			facesMessages.add(Severity.ERROR, "Import failed. There are persons with simular uid(s) in input file");
			return false;
		}

		List<GluuCustomPerson> existPersons = personService.findPersonsByUids(new ArrayList<String>(uids),
				PERSON_IMPORT_PERSON_LOCKUP_RETURN_ATTRIBUTES);
		if (existPersons.size() > 0) {
			facesMessages.add(Severity.ERROR, "Import failed. There are persons with existing uid(s): {0}",
					personService.getPersonString(existPersons));
			return false;
		}

		return true;
	}

	protected List<GluuCustomPerson> convertTableToPersons(Table table, List<ImportAttribute> importAttributes) throws Exception {
		// Prepare for conversion to list of GluuCustomPerson and check data
		// type
		Map<String, List<AttributeData>> entriesAttributes = new HashMap<String, List<AttributeData>>();
		int rows = table.getCountRows();
		boolean validTable = true;
		for (int i = 1; i <= rows; i++) {
			List<AttributeData> attributeDataList = new ArrayList<AttributeData>();
			for (ImportAttribute importAttribute : importAttributes) {
				if (importAttribute.getCol() == -1) {
					continue;
				}

				GluuAttribute attribute = importAttribute.getAttribute();
				String cellValue = table.getCellValue(importAttribute.getCol(), i);
				if (StringHelper.isEmpty(cellValue)) {
					if (attribute.isRequred()) {
						facesMessages.add(Severity.ERROR, "Import failed. Empty '{0}' not allowed", attribute.getDisplayName());
						validTable = false;
					}
					continue;
				}

				String ldapValue = getTypedValue(attribute, cellValue);
				if (StringHelper.isEmpty(ldapValue)) {
					facesMessages.add(Severity.ERROR, "Invalid value '{0}' in column '{1}' at row {2} were specified", cellValue,
							attribute.getDisplayName(), i + 1);
					validTable = false;
					continue;
				}

				AttributeData attributeData = new AttributeData(attribute.getName(), ldapValue);
				attributeDataList.add(attributeData);
			}
			entriesAttributes.put(Integer.toString(i), attributeDataList);
		}

		if (!validTable) {
			return null;
		}

		// Convert to GluuCustomPerson and set right DN
		List<GluuCustomPerson> persons = personService.createEntities(entriesAttributes);
		log.info("Found {0} persons in input Excel file", persons.size());

		return persons;
	}

	private String getTypedValue(GluuAttribute attribute, String value) {
		if (GluuAttributeDataType.STRING.equals(attribute.getDataType())) {
			return value;
		}

		return null;
	}

	private String getAttributesString(List<GluuAttribute> attributes) {
		StringBuilder sb = new StringBuilder();

		for (Iterator<GluuAttribute> iterator = attributes.iterator(); iterator.hasNext();) {
			GluuAttribute attribute = iterator.next();
			sb.append('\'').append(attribute.getDisplayName()).append('\'');
			if (!attribute.isRequred()) {
				sb.append(" (non mandatory)");
			}
			if (iterator.hasNext()) {
				sb.append(", ");
			}

		}

		return sb.toString();
	}

	private List<ImportAttribute> getAttributesForImport(Table table) {
		List<ImportAttribute> importAttributes = new ArrayList<ImportAttribute>();
		if ((table == null) || (table.getCountCols() < 1) || (table.getCountRows() < 1)) {
			return importAttributes;
		}

		int cols = table.getCountCols();
		List<String> addedAttributes = new ArrayList<String>(this.attributes.size());
		for (int i = 0; i <= cols; i++) {
			String cellValue = table.getCellValue(i, 0);
			if (StringHelper.isEmpty(cellValue)) {
				continue;
			}

			String attributeName = cellValue.toLowerCase();
			GluuAttribute attribute = attributesDisplayNameMap.get(attributeName);
			if (attribute != null) {
				addedAttributes.add(attributeName);
				ImportAttribute importAttribute = new ImportAttribute(i, attribute);
				importAttributes.add(importAttribute);
			}
		}

		for (GluuAttribute attribute : this.attributes) {
			if (!addedAttributes.contains(attribute.getName())) {
				ImportAttribute importAttribute = new ImportAttribute(-1, attribute);
				importAttributes.add(importAttribute);
			}
		}

		return importAttributes;
	}

	private Map<String, GluuAttribute> getAttributesDisplayNameMap(List<GluuAttribute> attributes) {
		Map<String, GluuAttribute> result = new HashMap<String, GluuAttribute>();
		for (GluuAttribute attribute : attributes) {
			result.put(attribute.getDisplayName().toLowerCase(), attribute);
		}

		return result;
	}

	public static class FileDataToImport implements Serializable {

		private static final long serialVersionUID = 7334362213305310293L;

		private String fileName;
		private Table table;
		private List<ImportAttribute> importAttributes;
		private List<GluuCustomPerson> persons;
		private boolean ready;

		public FileDataToImport() {
		}

		public FileDataToImport(Table table) {
			this.table = table;
		}

		public List<ImportAttribute> getImportAttributes() {
			return importAttributes;
		}

		public void setImportAttributes(List<ImportAttribute> importAttributes) {
			this.importAttributes = importAttributes;
		}

		public Table getTable() {
			return table;
		}

		public void setTable(Table table) {
			this.table = table;
		}

		public String getFileName() {
			return fileName;
		}

		public void setFileName(String fileName) {
			this.fileName = fileName;
		}

		public List<GluuCustomPerson> getPersons() {
			return persons;
		}

		public void setPersons(List<GluuCustomPerson> persons) {
			this.persons = persons;
		}

		public boolean isReady() {
			return ready;
		}

		public void setReady(boolean ready) {
			this.ready = ready;
		}

		public void reset() {
			this.fileName = null;
			this.table = null;
			this.importAttributes = null;
			this.persons = null;
			this.ready = false;
		}
	}

	public static class ImportAttribute implements Serializable {

		private static final long serialVersionUID = -5640983196565086530L;

		private GluuAttribute attribute;
		private int col;

		public ImportAttribute(int col, GluuAttribute attribute) {
			this.col = col;
			this.attribute = attribute;
		}

		public int getCol() {
			return col;
		}

		public void setCol(int col) {
			this.col = col;
		}

		public GluuAttribute getAttribute() {
			return attribute;
		}

		public void setAttribute(GluuAttribute attribute) {
			this.attribute = attribute;
		}
	}

}
