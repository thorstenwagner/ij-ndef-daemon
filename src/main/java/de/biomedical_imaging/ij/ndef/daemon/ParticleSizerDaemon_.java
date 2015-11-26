package de.biomedical_imaging.ij.ndef.daemon;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import javax.swing.JFileChooser;

import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;

/**
 * 
 * ParticleSizer Daemon tool (PDT) for the communication with Digital Micrograph (DM) as part of the NanoDefine AutoTEM solution. 
 * 
 * Protocol:
 * 1. DM saves the input image (e.g. 1.dm3) in the collab. folder and write the following line in the do.txt 
 * "DO;1.dm3;"
 * 2. PDT starts the ParticleSIzer and then save the binary image, the ResultsTable-image and the contour-data-image in the collab folder. If an
 * error occurs during the analysis it writes "ERROR" in the first line of DO.txt
 * 3. PDT deletes the first line in DO.txt and write a new line:
 * "DONE;FilenameOfBinaryImage.tif;FilenameOfResultTableImage.tif;FilenameOfContourImage.tif;"
 * 4. DM reads the files and tidy up the collab. folder.
 * 5. PDT waits for a new DO line in DO.txt until the first line is "STOP"
 * 
 * @author Thorsten Wagner (wagner@biomedical-imaging.de)
 *
 */
public class ParticleSizerDaemon_ implements PlugIn {

	
	private String coOpPath = ""; /** Cooperation path */
	private boolean beChatty = false;
	private final String PREF_COOPPATH = "ndef.daemon.cooppath";
	
	@Override
	public void run(String arg) {
		
		if(beChatty){IJ.log("RUN");}
		showGUI();
		
		// Read files from 
		File doTXT = new File(coOpPath+"/do.txt");
		if(!doTXT.exists()){
			throw new IllegalStateException("There is no do.txt in " + coOpPath);
		}
		
		//Parse filename
		BufferedReader reader = null;
		String line = "";
		try {
			reader = new BufferedReader(new FileReader(doTXT));
			line = reader.readLine();
			reader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			IJ.error("do.txt was not found");
			//return;
		}catch (IOException e) {
			// TODO Auto-generated catch block
			IJ.error("Could not read frist line");
			//return;
		}
		String[] com = line.split(";");
		String command = com[0];
		String filename = com[1];
		if(beChatty){IJ.log("Command: " + command);}
		if(beChatty){IJ.log("filename: " + filename);}
		
		if(command.equals("DO")){
			// Run the particle analyzer with current settings
			
			if(beChatty){IJ.log("Try to open: " + coOpPath + "/" + filename);}
			IJ.openImage(coOpPath + "/" + filename).show();
			IJ.run("Particle Sizer");
			IJ.run("Results table to image");
			
			// Get the results and save them in the cooperation path
			int idHist = Integer.parseInt(ij.Prefs.get("ndef.result.histid", "-1"));
			int idImgWithResultOverlay  = Integer.parseInt(ij.Prefs.get("ndef.result.imagewithoverlayid", "-1"));
			int idBinary = Integer.parseInt(ij.Prefs.get("ndef.result.binaryid", "-1"));
			int idRTasImage = Integer.parseInt(ij.Prefs.get("ndef.result.rtAsImageID", "-1"));
					
			if(idHist == -1 || idImgWithResultOverlay == -1 || idBinary == -1){
				IJ.error("Missing some images - did the ParticleSizer run?");
				throw new IllegalStateException("Missing some images - did the ParticleSizer run?");
			}
			
			ImagePlus histImp = WindowManager.getImage(idHist);
			ImagePlus resultImp = WindowManager.getImage(idImgWithResultOverlay);
			ImagePlus binaryImp = WindowManager.getImage(idBinary);
			ImagePlus rtAsImg = WindowManager.getImage(idRTasImage);
			
			if(binaryImp == null || rtAsImg == null){
				throw new RuntimeException("Binary or ResultsTable-Image could not be found");
			}
			
			IJ.saveAsTiff(binaryImp, coOpPath+"/bin.tif");
			IJ.saveAsTiff(rtAsImg, coOpPath+"/rt.tif");
			
			//Close windows
			histImp.close();
			resultImp.close();
			binaryImp.close();
			rtAsImg.close();
			ResultsTable.getResultsWindow().close();
			
			//Update do.txt
			PrintWriter writer;
			try {
				writer = new PrintWriter(doTXT);
				writer.print("DONE;bin.tif;rt.tif");
				writer.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		}
		else if(command=="STOP"){
			
		}
		
		
		
	}
	
	private void showGUI(){
		loadPreferences();
		/*
		GenericDialogPlus gdp = new GenericDialogPlus("Particle Sizer Daemon");
		gdp.addFileField("Do TXT: ", coOpPath);
		
		gdp.showDialog();
		
		if(gdp.wasCanceled()){
			return;
		}
		*/
		
		JFileChooser fc = new JFileChooser(coOpPath);
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int returnValue = fc.showOpenDialog(null);
		if (returnValue == JFileChooser.APPROVE_OPTION) {
			coOpPath = fc.getSelectedFile().getPath();
		}

		savePreferences();
	}
	
	private void loadPreferences(){
		coOpPath = ij.Prefs.get(PREF_COOPPATH, IJ.getDirectory("home"));
	}
	
	private void savePreferences(){
		ij.Prefs.set(PREF_COOPPATH,coOpPath);
	}
	
}