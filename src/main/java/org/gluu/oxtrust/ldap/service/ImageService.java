package org.gluu.oxtrust.ldap.service;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.apache.commons.io.FilenameUtils;
import org.gluu.oxtrust.model.GluuAttributeDataType;
import org.gluu.oxtrust.model.GluuCustomAttribute;
import org.gluu.oxtrust.model.GluuCustomPerson;
import org.gluu.oxtrust.util.OxTrustConstants;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;
import org.richfaces.model.UploadedFile;
import org.xdi.config.oxtrust.ApplicationConfiguration;
import org.xdi.model.GluuImage;
import org.xdi.service.XmlService;
import org.xdi.util.StringHelper;
import org.xdi.util.repository.RepositoryUtility;

/**
 * Service class to work with images in photo repository
 * 
 * @author Yuriy Movchan Date: 11.04.2010
 */
@Name("imageService")
@Scope(ScopeType.APPLICATION)
@AutoCreate
public class ImageService {

	@Logger
	private Log log;

	@In
	private ImageRepository imageRepository;

	@In
	private XmlService xmlService;

	@In(value = "#{oxTrustConfiguration.applicationConfiguration}")
	private ApplicationConfiguration applicationConfiguration;

	public String getXMLFromGluuImage(GluuImage photo) {
		return xmlService.getXMLFromGluuImage(photo);
	}

	public GluuImage getGluuImageFromXML(String xml) {
		return xmlService.getGluuImageFromXML(xml);
	}

	/**
	 * Creates GluuImage object from uploaded file
	 * 
	 * @param creator
	 *            person uploading the file
	 * @param uploadedFile
	 *            uploaded file
	 * @return GluuImage object
	 */
	public GluuImage constructImage(GluuCustomPerson creator, UploadedFile uploadedFile) {
		GluuImage image = new GluuImage();
		image.setUuid(RepositoryUtility.generateUUID());
		image.setCreationDate(new Date());
		image.setCreator(creator.getDn());
		image.setSourceName(FilenameUtils.getName(uploadedFile.getName()));
		image.setSourceContentType(uploadedFile.getContentType());
		image.setSize(uploadedFile.getSize());
		image.setData(uploadedFile.getData());

		return image;
	}

	public GluuImage constructImageWithThumbnail(GluuCustomPerson creator, UploadedFile uploadedFile, int thumbWidth, int thumbHeight) {
		GluuImage image = constructImage(creator, uploadedFile);
		
		try {
			imageRepository.addThumbnail(image, thumbWidth, thumbHeight);
		} catch (Exception ex) {
			log.error("Failed to generate thumbnail for photo {0}", ex, image);
		}

		return image;
	}

	/**
	 * Creates image(s) in repository
	 * 
	 * @param image
	 *            GluuImage object
	 * @return true if files are successfully created, false otherwise
	 * @throws Exception
	 */
	public boolean createImageFiles(GluuImage image, int thumbWidth, int thumbHeight) {
		try {
			return imageRepository.createRepositoryImageFiles(image, thumbWidth, thumbHeight);
		} catch (Exception ex) {
			log.error("Failed to save photo {0}", ex, image);
		}
		return false;
	}

	/**
	 * Creates image(s) in repository
	 * 
	 * @param image
	 *            GluuImage object
	 * @return true if files are successfully created, false otherwise
	 * @throws Exception
	 */
	public boolean createImageFiles(GluuImage image) {
		return createImageFiles(image, applicationConfiguration.getPhotoRepositoryThumbWidth(), applicationConfiguration.getPhotoRepositoryThumbHeight());
	}

	/**
	 * Returns an image
	 * 
	 * @param customAttribute
	 * @return GluuImage object
	 */
	public GluuImage getImage(GluuCustomAttribute customAttribute) {
		if ((customAttribute == null) || StringHelper.isEmpty(customAttribute.getValue())
				|| !GluuAttributeDataType.PHOTO.equals(customAttribute.getMetadata().getDataType())) {
			return null;
		}

		return getGluuImageFromXML(customAttribute.getValue());
	}

	/**
	 * Deletes the image from repository
	 * 
	 * @param customAttribute
	 * @throws Exception
	 */
	public void deleteImage(GluuCustomAttribute customAttribute) throws Exception {
		GluuImage image = getImage(customAttribute);
		deleteImage(image);
	}

	public void deleteImage(GluuImage image) {
		if (image != null) {
			imageRepository.deleteImage(image);
		}
	}

	public byte[] getBlankImageData() {
		return imageRepository.getBlankImage();
	}

	public byte[] getBlankPhotoData() {
		return imageRepository.getBlankPhoto();
	}

	public byte[] getBlankIconData() {
		return imageRepository.getBlankIcon();
	}

	public byte[] getThumImageData(GluuCustomAttribute customAttribute) throws Exception {
		GluuImage image = getImage(customAttribute);
		return getThumImageData(image);
	}

	public byte[] getThumImageData(GluuImage image) {
		if (image != null) {
			try {
				return imageRepository.getThumbImageData(image);
			} catch (Exception ex) {
				log.error("Failed to load GluuImage {0}", ex, image);
			}
		}

		return getBlankImageData();
	}

	public byte[] getThumIconData(GluuImage image) {
		if (image != null) {
			try {
				return imageRepository.getThumbImageData(image);
			} catch (Exception ex) {
				log.error("Failed to load GluuImage {0}", ex, image);
			}
		}

		return getBlankIconData();
	}

	public void moveImageToPersistentStore(GluuImage image) {
		try {
			imageRepository.moveImageToPersistentStore(image);
		} catch (Exception ex) {
			log.error("Failed to load GluuImage {0}", ex, image);
		}
	}

	public void moveLogoImageToPersistentStore(GluuImage image) {
		try {
			imageRepository.moveLogoImageToPersistentStore(image);
		} catch (IOException ex) {
			log.error("Failed to load GluuImage {0}", ex, image);
		}
	}

	public File getThumbFile(GluuImage image) throws Exception {
		return imageRepository.getThumbFile(image);
	}

	public File getSourceFile(GluuImage image) throws Exception {
		return imageRepository.getSourceFile(image);
	}

	public boolean createFaviconImageFiles(GluuImage image) throws Exception {
		try {
			return imageRepository.createRepositoryFaviconImageFiles(image);
		} catch (IOException ex) {
			log.error("Failed to save photo {0}", ex, image);
		}

		return false;
	}

	public boolean isIconImage(GluuImage image) {
		return imageRepository.isIconImage(image);
	}

	/**
	 * Get imageService instance
	 * 
	 * @return ImageService instance
	 */
	public static ImageService instance() {
		return (ImageService) Component.getInstance(ImageService.class);
	}

	public byte[] getImageDate(UploadedFile uploadedFile) {
		if (uploadedFile == null) {
			return null;
		}

		return uploadedFile.getData();
	}

}
