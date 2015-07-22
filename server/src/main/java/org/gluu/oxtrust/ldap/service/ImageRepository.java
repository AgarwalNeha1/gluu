/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxtrust.ldap.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.jboss.seam.core.ResourceLoader;
import org.jboss.seam.log.Log;
import org.jboss.seam.ui.graphicImage.Image.Type;
import org.xdi.config.oxtrust.ApplicationConfiguration;
import org.xdi.model.GluuImage;
import org.xdi.util.StringHelper;
import org.xdi.util.image.ImageTransformationUtility;
import org.xdi.util.repository.RepositoryUtility;

/**
 * Manage images in photo repository
 * 
 * @author Yuriy Movchan Date: 11.03.2010
 */
@Name("imageRepository")
@Scope(ScopeType.APPLICATION)
@AutoCreate
@Startup
public class ImageRepository {

	@Logger
	private Log log;

	@In(required = false)
	private ResourceLoader resourceLoader;

	@In(value = "#{oxTrustConfiguration.applicationConfiguration}")
	private ApplicationConfiguration applicationConfiguration;

	private static final String TEMP_FOLDER = "tmp";
	private static final String REMOVED_FOLDER = "removed";
	private static boolean createBackupDuringRemoval = true;

	private String sourceHome, thumbHome;
	private String tmpSourceHome, tmpThumbHome;
	private String removedSourceHome, removedThumbHome;

	private File photoRepositoryRootDirFile;

	private byte[] blankImage, blankPhoto, blankIcon;

	private int countLevels;
	private int countFoldersPerLevel;

	private FileTypeMap fileTypeMap;

	@Create
	public void init() throws Exception {
		countLevels = applicationConfiguration.getPhotoRepositoryCountLeveles();
		countFoldersPerLevel = applicationConfiguration.getPhotoRepositoryCountFoldersPerLevel();

		String photoRepositoryRootDir = applicationConfiguration.getPhotoRepositoryRootDir();
		photoRepositoryRootDirFile = new File(photoRepositoryRootDir);

		// Create folders for persistent images
		sourceHome = photoRepositoryRootDir + File.separator + "source";
		thumbHome = photoRepositoryRootDir + File.separator + "thumb";

		createFoldersTree(new File(sourceHome));
		createFoldersTree(new File(thumbHome));

		// Create folders for temporary images
		tmpSourceHome = photoRepositoryRootDir + File.separator + TEMP_FOLDER + File.separator + "source";
		tmpThumbHome = photoRepositoryRootDir + File.separator + TEMP_FOLDER + File.separator + "thumb";

		createFoldersTree(new File(tmpSourceHome));
		createFoldersTree(new File(tmpThumbHome));

		// Create folders for removed images
		if (createBackupDuringRemoval) {
			removedSourceHome = photoRepositoryRootDir + File.separator + REMOVED_FOLDER + File.separator + "source";
			removedThumbHome = photoRepositoryRootDir + File.separator + REMOVED_FOLDER + File.separator + "thumb";

			createFoldersTree(new File(removedSourceHome));
			createFoldersTree(new File(removedThumbHome));
		}

		prepareBlankImage();
		prepareBlankPhoto();
		prepareBlankIcon();

		initFileTypesMap();
	}

	public void initFileTypesMap() throws Exception {
		fileTypeMap = MimetypesFileTypeMap.getDefaultFileTypeMap();
		InputStream is = ImageRepository.class.getClassLoader().getResourceAsStream("META-INF/mimetypes-gluu.default");
		try {
			if (is != null) {
				fileTypeMap = new MimetypesFileTypeMap(is);
			}
		} catch (Exception ex) {
			log.error("Failed to load file types map. Using default one.", ex);
			fileTypeMap = new MimetypesFileTypeMap();
		} finally {
			IOUtils.closeQuietly(is);
		}
	}

	/**
	 * Creates image in repository
	 * 
	 * @param image
	 *            image file
	 * @return true if image was added successfully, false otherwise
	 * @throws Exception
	 */
	public boolean createRepositoryImageFiles(GluuImage image, int thumbWidth, int thumbHeight) throws Exception {
		if (image.getSourceContentType().equals("application/octet-stream")) {
			image.setSourceContentType(fileTypeMap.getContentType(image.getSourceName()));
		}
		
		if (!addThumbnail(image, thumbWidth, thumbHeight)) {
			return false;
		}

		// Generate paths
		setGeneratedImagePathes(image, Type.IMAGE_JPEG.getExtension());

		// Create folders tree
		createImagePathes(image);
		
		// Save thumb image
		FileUtils.writeByteArrayToFile(getThumbFile(image), image.getThumbData());

		// Save source image
		FileUtils.writeByteArrayToFile(getSourceFile(image), image.getData());

		return true;
	}

	public boolean addThumbnail(GluuImage image, int thumbWidth, int thumbHeight) throws Exception {
		if (!image.getSourceContentType().matches("image/(gif|png|jpeg|jpg|bmp)")) {
			return false;
		}

		// Load source image
		org.jboss.seam.ui.graphicImage.Image graphicsImage = new org.jboss.seam.ui.graphicImage.Image();

		graphicsImage.setInput(image.getData());
		graphicsImage.setContentType(Type.IMAGE_PNG);

		if (graphicsImage.getBufferedImage() == null) {
			throw new IOException("The image data is empty");
		}

		// Set source image size
		image.setWidth(graphicsImage.getWidth());
		image.setHeight(graphicsImage.getHeight());

		BufferedImage bi = ImageTransformationUtility.scaleImage(graphicsImage.getBufferedImage(), thumbWidth, thumbHeight);
		graphicsImage.setBufferedImage(bi);

		// Set thumb properties
		image.setThumbWidth(graphicsImage.getWidth());
		image.setThumbHeight(graphicsImage.getHeight());
		image.setThumbContentType(graphicsImage.getContentType().getMimeType());
		
		// Store thumb image
		image.setThumbData(graphicsImage.getImage());

		graphicsImage = null;

		return true;
	}

	private void moveImageToPersistentStore(GluuImage image, boolean saveThumb, String destSourceFilePath, String destThumbFilePath)
			throws IOException {
		if (!image.isStoreTemporary()) {
			return;
		}

		File tmpOrigFile = getSourceFile(image);
		File tmpThumbFile = getThumbFile(image);

		image.setStoreTemporary(false);

		if (!StringHelper.isEmpty(destSourceFilePath)) {
			image.setSourceFilePath(destSourceFilePath);
		}

		if (!StringHelper.isEmpty(destThumbFilePath)) {
			image.setThumbFilePath(destThumbFilePath);
		}

		FileUtils.copyFile(tmpOrigFile, getSourceFile(image));
		if (saveThumb) {
			FileUtils.copyFile(tmpThumbFile, getThumbFile(image));
		}

		deleteFile(tmpOrigFile, true);
		deleteFile(tmpThumbFile, true);
	}

	public void moveImageToPersistentStore(GluuImage image) throws Exception {
		moveImageToPersistentStore(image, true, null, null);
	}

	public void moveLogoImageToPersistentStore(GluuImage image) throws IOException {
		if (!image.isLogo()) {
			return;
		}

		String logoSourceFilePath = "logo" + RepositoryUtility.getFileNameExtension(image.getSourceFilePath());
		String logoThumbFilePath = "logo_thumb" + RepositoryUtility.getFileNameExtension(image.getSourceFilePath());
		moveImageToPersistentStore(image, true, logoSourceFilePath, logoThumbFilePath);
	}

	private void setGeneratedImagePathes(GluuImage image, String thumbExt) throws Exception {
		String uuid = RepositoryUtility.generateUUID();
		String ext = RepositoryUtility.getFileNameExtension(image.getSourceName());
		String sourceFileName = uuid + ext;
		String thumbFileName = uuid + (thumbExt == null ? ext : thumbExt);

		String sourceFilePath = RepositoryUtility.generateTreeFolderPath(countLevels, countFoldersPerLevel, sourceFileName);
		String thumbFilePath = RepositoryUtility.generateTreeFolderPath(countLevels, countFoldersPerLevel, thumbFileName);

		image.setUuid(uuid);
		image.setSourceFilePath(sourceFilePath);
		image.setThumbFilePath(thumbFilePath);
	}

	public File getThumbFile(GluuImage image) {
		if (image.isLogo() && !image.isStoreTemporary()) {
			return new File(applicationConfiguration.getLogoLocation() + File.separator + image.getThumbFilePath());
		}

		String parentFolder = image.isStoreTemporary() ? tmpThumbHome : thumbHome;
		return new File(parentFolder + File.separator + image.getThumbFilePath());
	}

	public File getSourceFile(GluuImage image) {
		if (image.isLogo() && !image.isStoreTemporary()) {
			return new File(applicationConfiguration.getLogoLocation() + File.separator + image.getSourceFilePath());
		}

		String parentFolder = image.isStoreTemporary() ? tmpSourceHome : sourceHome;
		return new File(parentFolder + File.separator + image.getSourceFilePath());
	}

	public byte[] getThumbImageData(GluuImage image) throws Exception {
		return FileUtils.readFileToByteArray(getThumbFile(image));
	}

	public byte[] getSourceImageData(GluuImage image) throws Exception {
		return FileUtils.readFileToByteArray(getSourceFile(image));
	}

	public void deleteImage(GluuImage image) {
		File thumbFile = getThumbFile(image);
		File sourceFile = getSourceFile(image);

		if (!image.isStoreTemporary() && createBackupDuringRemoval) {
			File reovedThumbFile = new File(removedThumbHome + File.separator + image.getThumbFilePath());
			File removedSourceFile = new File(removedSourceHome + File.separator + image.getSourceFilePath());

			try {
				FileUtils.copyFile(thumbFile, reovedThumbFile);
				FileUtils.copyFile(sourceFile, removedSourceFile);
			} catch (IOException ex) {
				log.error("Failed to create backup for photo {0} before removal", ex, image);
			}
		}

		// Delete thumb and source files
		deleteFile(thumbFile, true);
		deleteFile(sourceFile, true);
	}

	private boolean deleteFile(File file, boolean removeEmptyfoldersTree) {
		boolean result = true;

		if (file.exists()) {
			result = file.delete();
			if (removeEmptyfoldersTree) {
				removeEmptyfoldersTree(file.getParentFile(), countLevels);
			}
		}

		return result;
	}

	private void removeEmptyfoldersTree(File folder, int remainLevels) {
		if (photoRepositoryRootDirFile.equals(folder) || (remainLevels == 0)) {
			return;
		}

		File[] files = folder.listFiles();
		if (files == null) { // null if security restricted
			return;
		}

		if (files.length == 0) {
			File parent = folder.getParentFile();
			deleteFile(folder, false);
			removeEmptyfoldersTree(parent, --remainLevels);
		}
	}

	private void createFoldersTree(File folder) {
		if (folder != null && folder.mkdirs()) {
			// findbugs: probably needs to do something here
		}
	}

	private void createImagePathes(GluuImage image) throws Exception {
		createFoldersTree(getSourceFile(image).getParentFile());
		createFoldersTree(getThumbFile(image).getParentFile());
	}

	public byte[] getBlankImage() {
		// findbugs: copy on return to not expose internal representation
		return ArrayUtils.clone(blankImage);
	}

	public byte[] getBlankPhoto() {
		// findbugs: copy on return to not expose internal representation
		return ArrayUtils.clone(blankPhoto);
	}

	public byte[] getBlankIcon() {
		return ArrayUtils.clone(blankIcon);
	}

	private void prepareBlankImage() {
		InputStream is = resourceLoader.getResourceAsStream("/WEB-INF/static/images/blank_image.gif");
		if(is != null){
			try {
				this.blankImage = IOUtils.toByteArray(is);
			} catch (Exception ex) {
				log.error("Failed to load blank image", ex);
			} finally {
				IOUtils.closeQuietly(is);
			}
		}else{
			log.error("Failed to load blank image. ResourceLoader returned null stream.");
		}
	}

	private void prepareBlankPhoto() {
		InputStream is = resourceLoader.getResourceAsStream("/WEB-INF/static/images/anonymous.png");
		if(is != null){
			try {
				this.blankPhoto = IOUtils.toByteArray(is);
			} catch (Exception ex) {
				log.error("Failed to load blank photo", ex);
			} finally {
				IOUtils.closeQuietly(is);
			}
		}else{
			log.error("Failed to load blank photo. ResourceLoader returned null stream.");
		}
	}

	private void prepareBlankIcon() {
		
		InputStream is = resourceLoader.getResourceAsStream("/WEB-INF/static/images/blank_icon.gif");
		if(is != null){
			try {
				this.blankIcon = IOUtils.toByteArray(is);
			} catch (Exception ex) {
				log.error("Failed to load blank icon", ex);
			} finally {
				IOUtils.closeQuietly(is);
			}
		}else{
			log.error("Failed to load blank icon. ResourceLoader returned null stream.");
		}
	}

	public boolean createRepositoryFaviconImageFiles(GluuImage image) throws Exception {
		if (!isIconImage(image)) {
			return false;
		}

		// Generate paths
		setGeneratedImagePathes(image, null);

		// Create folders tree
		createImagePathes(image);

		// Set source image size
		image.setWidth(16);
		image.setHeight(16);

		byte[] data = image.getData();
		FileUtils.writeByteArrayToFile(getThumbFile(image), data);

		// Save source image
		FileUtils.writeByteArrayToFile(getSourceFile(image), data);

		return true;
	}

	public boolean isIconImage(GluuImage image) {
		if (image.getSourceContentType().equals("application/octet-stream")) {
			image.setSourceContentType(fileTypeMap.getContentType(image.getSourceName()));
		}

		return image.getSourceContentType().matches("image/(x-icon|x-ico)");
	}

}
