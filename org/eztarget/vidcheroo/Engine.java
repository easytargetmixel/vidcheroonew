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

public class Engine {

	private static Engine instance = null;
	
	private static VidcherooControlFrame controlFrame;
	private static VidcherooMediaFrame mediaFrame;
	
	private static VidcherooStatus status = VidcherooStatus.NOFILES;
	private static int beatTime = 500;
	
	protected Engine() {
		FileCrawler.getInstance().loadFileList();
		if (FileCrawler.getInstance().getFileListLength() > 0) {
			setStatus(VidcherooStatus.READY);
		}
	}

	public static Engine getInstance() {
		if (instance == null) {
			instance = new Engine();
		}
		return instance;
	}

	public void setControlFrame(VidcherooControlFrame controlFrame) {
		Engine.controlFrame = controlFrame;
	}

	public void setMediaFrame(VidcherooMediaFrame mediaFrame) {
		Engine.mediaFrame = mediaFrame;
	}

	public void play() {
		if (status != VidcherooStatus.NOFILES) {
			// Always go back into ready state before playing to finish old threads.
			setStatus(VidcherooStatus.READY);
			
			Thread t = new Thread() {
				public void run() {
					System.out.println("Starting new Engine Play thread.");
					setStatus(VidcherooStatus.PLAYING);

					while (status == VidcherooStatus.PLAYING) {
						// Play the next file.
						mediaFrame.playMediaFile(FileCrawler.getInstance().getRandomMediaPath());

						// Sleep for one beat length.
						try {
							sleep(beatTime);
						} catch (InterruptedException ex) {
							System.err.println(ex.toString());
						}
					}
					System.out.println("Reached end of Engine Play thread.");
				}
			};

			t.start();
		}
	}

	public void pause() {
		if (status == VidcherooStatus.READY || status == VidcherooStatus.PLAYING) {
			setStatus(VidcherooStatus.READY);
		}
	}

	public void toggleFullscreen() {
		// TODO Auto-generated method stub
		
	}

	public void setTempo(String text) {
//		double dTempo = 0.0;
//		// Attempt to change the tempo.
//		// If the input is a valid double set the new tempo.
//		try {
//			dTempo = Double.parseDouble(tempoTextField.getText());
//		}
//		// Otherwise don't try to change the tempo.
//		catch(Exception ex) {}
//		
//		// Only replace the tempo if a valid BPM value was given.
//		if(dTempo >= 60 && dTempo <= 180) {
//			engine.setTempo(dTempo);
//		}
//		else {
//			tempoTextField.setText(engine.getTempo() + "");
//			engine.setStatusMessage("60.0 < Tempo < 180.0!");
//		}		
	}

	public void setBeatFraction(float f) {
		// TODO Auto-generated method stub
		
	}
	
	private void setStatus(VidcherooStatus newStatus) {
		Engine.status = newStatus;
		
		if (controlFrame != null) {
			controlFrame.setStatus(status);
		}
	}

}
