/*
 * Copyright (C) 2014 Easy Target
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eztarget.vidcheroo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;

public class MediaFileParser {
	private static MediaFileParser instance = null;
			
	private static ArrayList<VidcherooMediaFile> mediaFiles = new ArrayList<VidcherooMediaFile>();
	//TODO: Read this value from stored settings.
		
	protected MediaFileParser() {
		
	}
	
	public static MediaFileParser getInstance() {
		if(instance == null) {
			instance = new MediaFileParser();
		}
		return instance;
	}
	
	private static final String PROPERTY_FILE_NAME = "_durations.vch";
	private static final boolean PARSE_FILES = true;
	private static final int PARSE_FRAME_WIDTH = 300;
	private static final int PARSE_FRAME_HEIGHT = 200;
	
	public static void parseMediaPath(final String fMediaPath) {
		if (Engine.getStatus() == VidcherooStatus.NOVLC) {
			return;
		}
		
		if (fMediaPath == null) {
			System.err.println("ERROR: Media path to parse is null.");
			Engine.setStatus(VidcherooStatus.NOFILES);
			return;
		}
		
		Engine.setStatus(VidcherooStatus.PARSING);
		
		Thread parseThread = new Thread() {
			public void run() {
				System.out.println("Looking for media files in " + fMediaPath);

				if(fMediaPath.length() > 1) {
					File fileDirectory = new File(fMediaPath);
					
					if (fileDirectory.length() > 0) {
						boolean isAnalysed = false;
						
						// Go through all the files in this directory and see if one of them is a properties file.
						for (final File fileEntry : fileDirectory.listFiles()) {
							if (fileEntry.getName().equals(PROPERTY_FILE_NAME)) {
								System.out.println(PROPERTY_FILE_NAME + " found.");
								restoreAnalysationProperties(fMediaPath);
								isAnalysed = true;
								break;
							}
						}
						
						// If no properties file was found, run the analysis.
						if (!isAnalysed) {
							// Create a small media frame that is used to shortly play the files, parse them and get their length.
							VidcherooMediaFrame parseFrame;
							parseFrame = new VidcherooMediaFrame(PARSE_FRAME_WIDTH, PARSE_FRAME_HEIGHT, "Vidcheroo Analyser");
							mediaFiles = new ArrayList<VidcherooMediaFile>();
							
							// At the end, store the result in a properties file.
							Properties properties = new Properties();
							parseFrame.setVisible(true);
							
							// Again, go through all the files in the directory.
							for (final File fileEntry : fileDirectory.listFiles()) {
								// Right away ignore directories and files without name extensions.
								if (!fileEntry.isDirectory()) {
									String mediaFilePath = fMediaPath + "/" + fileEntry.getName();
									//TODO: Only load possible media files.
									VidcherooMediaFile file = new VidcherooMediaFile();
									file.path = mediaFilePath;
									
									if (PARSE_FILES) {
										file.length = parseFrame.getMediaLength(mediaFilePath);
									} else {
										file.length = VidcherooMediaFile.NOT_PARSED;
									}
									
									file.id = mediaFiles.size();
									mediaFiles.add(file);
									properties.setProperty(file.path, file.length + "");
								}
							}
							
							parseFrame.setVisible(false);
							parseFrame.removeAll();
							parseFrame = null;
							
							storeProperties(properties, fMediaPath);
						}
					}
				}
				
				// Enable the Engine by setting the resulting status.
				System.out.println("Number of found files: " + mediaFiles.size());
				if (mediaFiles.size() > 1) Engine.setStatus(VidcherooStatus.READY);
				else Engine.setStatus(VidcherooStatus.NOFILES);
			}


		};
		
		// Call-back to run():
		parseThread.start();
	}
	
	public static VidcherooMediaFile getRandomMediaFile() {
		if(mediaFiles.size() > 0) {
			int randomMediaIndex = (int) (Math.random() * mediaFiles.size());
			return mediaFiles.get(randomMediaIndex);
		} else {
			//TODO: Return placeholder video file.
			return null;
		}
	}
	 
	public static int getFileListLength() {
		return mediaFiles.size();
	}
	
	/*
	 * Store & Restore Analysation Results
	 */
	
	private static void restoreAnalysationProperties(String fMediaPath) {
		InputStream input = null;
	 
		try {
			// (Re-)initialise the media file list.
			mediaFiles = new ArrayList<VidcherooMediaFile>();
			
			// Load the properties file in the given directory.
			input = new FileInputStream(fMediaPath + "/" + PROPERTY_FILE_NAME);
			Properties prop = new Properties();
			prop.load(input);
			
			// Go through the enumeration of properties, create instances of media files
			// and add them to the array list.
			Enumeration<?> enumeration = prop.propertyNames();
			int idCounter = 0;
			while (enumeration.hasMoreElements()) {
				String propKey = (String) enumeration.nextElement();
				String propValue = prop.getProperty(propKey);
				VidcherooMediaFile mediaFile = new VidcherooMediaFile();
				mediaFile.id = idCounter;
				mediaFile.path = propKey;
				try {
					mediaFile.length = Long.parseLong(propValue);
				} catch (Exception e) {
					mediaFile.length = VidcherooMediaFile.LENGTH_INDETERMINABLE;
					System.err.println("WARNING: Could not restore length for " + mediaFile.path + ".");
				}
				
				mediaFiles.add(mediaFile);
				
				idCounter++;
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
					System.out.println("Closed " + PROPERTY_FILE_NAME  + ".");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private static void storeProperties(Properties properties, String mediaPath) {
		// Save the analysed properties, if at least 2 were found.
		if (mediaFiles.size() > 1) {
			OutputStream output = null;

			try {
				// Store the properties in the media folder.
				output = new FileOutputStream(mediaPath + "/" + PROPERTY_FILE_NAME);
				properties.store(output, null);
			} catch (IOException io) {
				io.printStackTrace();
			} finally {
				if (output != null) {
					try {
						output.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
		 
			}
		}
	}
}
