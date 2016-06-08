package de.biomedical_imaging.ij.ndef.daemon;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.YesNoCancelDialog;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;

/**
 * 
 * ParticleSizer Daemon tool (PDT) for the communication with Digital Micrograph
 * (DM) as part of the NanoDefine AutoTEM solution.
 * 
 * Protocol: 1. DM saves the input image (e.g. 1.dm3) in the collab. folder and
 * write the following line in the do.txt "DO;1.dm3;" 2. PDT starts the
 * ParticleSIzer and then save the binary image, the ResultsTable-image and the
 * contour-data-image in the collab folder. If an error occurs during the
 * analysis it writes "ERROR" in the first line of DO.txt 3. PDT deletes the
 * first line in DO.txt and write a new line:
 * "DONE;FilenameOfBinaryImage.tif;FilenameOfResultTableImage.tif;FilenameOfContourImage.tif;"
 * 4. DM reads the files and tidy up the collab. folder. 5. PDT waits for a new
 * DO line in DO.txt until the first line is "STOP"
 * 
 * @author Thorsten Wagner (wagner@biomedical-imaging.de)
 * 
 */
public class ParticleSizerDaemon_ implements PlugIn {

	private String coOpPath = "";
	/** Cooperation path */
	public final static boolean beChatty = false;
	private final String PREF_COOPPATH = "ndef.daemon.cooppath";
	private final String PREF_REMEMBER_COOPPATH = "ndef.auto.rememberCooperationFolder";
	private final String FILENAME_BINARY_RESULT = "bin.tif";
	private final String FILENAME_RESULTSTABLE_IMAGE = "rt.tif";
	private final String FILENAME_CONTOUR_IMAGE = "contours.tif";
	private FileAlterationMonitor doFileMonitor;
	private boolean restoreBinarySetting;
	private boolean restoreNoPlotting;
	private boolean restoreRectangleSettings;
	private File doTXT;
	private File coOpFolder;
	@Override
	public void run(String arg) {
		showGUI();
		
		//Prepare ParticleAnalyzer Settings
		
		restoreBinarySetting = ij.Prefs.get("ndef.showBinaryResult", false);
		restoreNoPlotting = ij.Prefs.get("ndef.noPlotting", false);
		restoreRectangleSettings = ij.Prefs.get("ndef.doSelectRegion", true);
		ij.Prefs.set("ndef.showBinaryResult", true);
		ij.Prefs.set("ndef.noPlotting", true);
		ij.Prefs.set("ndef.doSelectRegion", false);
		// Read files from
		doTXT = new File(coOpPath + "/do.txt");
		coOpFolder = new File(coOpPath);
		if (!doTXT.exists()) {
			if (beChatty) {
				IJ.log("DO FILE DOES NOT EXIST - CREATE NEW ONE");
			}
			PrintWriter writer = null;
			try {
				writer = new PrintWriter(doTXT);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			writer.println("IDLE");
			writer.close();
		}
		
		doFileMonitor = new FileAlterationMonitor(2 * 1000);
		FileAlterationObserver observer = new FileAlterationObserver(coOpFolder);
		DoFileListener listener = new DoFileListener(this);
		observer.addListener(listener);
		doFileMonitor.addObserver(observer);
		
		
		
		try {
			doFileMonitor.start();

		} catch (Exception e1) {
			IJ.log(e1.getMessage());
			e1.printStackTrace();
		}
		listener.analyseDoFile(doTXT);
	}
	
	public void stopMonitoring(){
		try {
			doFileMonitor.stop();
			if (ParticleSizerDaemon_.beChatty) {
				IJ.log("Monitor stopped");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ij.Prefs.set("ndef.showBinaryResult", restoreBinarySetting);
		ij.Prefs.set("ndef.noPlotting", restoreNoPlotting);
		ij.Prefs.set("ndef.doSelectRegion", restoreRectangleSettings);
	}
	
	public String[] getReadCommandAndFilename(File doFile){
		
		if (!doFile.exists()) {
			IJ.log("There is no do.txt in " + coOpPath);
			throw new IllegalStateException("There is no do.txt in " + coOpPath);
		}
		// Parse filename
				BufferedReader reader = null;
				String line = "";
				try {
					reader = new BufferedReader(new FileReader(doFile));
					line = reader.readLine();
					reader.close();
					if(line==null){
						return new String[]{"IDLE"};
					}
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					IJ.error("do.txt was not found");
					// return;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					IJ.error("Could not read frist line");
					// return;
				}
				String[] com = line.split(";");
				
				return com;
	}

	/**
	 * 
	 * @param command
	 * @param imgname
	 * @return Return the filenames of the result files. If an error occurs the first field 'ERROR'. If ellipse fitting was used, the second field is "null"
	 */
	public String[] doAnalysis(String command, String imgname) {
		// Run the particle analyzer with current settings

		if (beChatty) {
			IJ.log("Try to open: " + coOpPath + "/" + imgname);
		}
		
		ImagePlus img = IJ.openImage(coOpPath + "/" + imgname);
		if(img == null){
				IJ.error("The file "+imgname+" could not be found");
				String[] err = {"ERROR"};
				return err;
		}
		img.show();
		IJ.run("Particle Sizer");
		IJ.run("Results table to image");
		boolean ellipseModeWasUsed = ij.Prefs.get("ndef.useEllipseFittingMode", false);
		if(ellipseModeWasUsed){
			IJ.run("Ellipse contour data as image");
		}else{
			IJ.run("Blob contour data as image");
		}
		int contourDataID = Integer.parseInt(ij.Prefs.get("ndef.result.contourImgID", "-1"));
		// Get the results and save them in the cooperation path
		int idImgWithResultOverlay = Integer.parseInt(ij.Prefs.get(
				"ndef.result.imagewithoverlayid", "-1"));
		int idBinary = Integer.parseInt(ij.Prefs.get("ndef.result.binaryid",
				"-1"));
		int idRTasImage = Integer.parseInt(ij.Prefs.get(
				"ndef.result.rtAsImageID", "-1"));

		if (idImgWithResultOverlay == -1 || idBinary == -1) {
			IJ.error("Missing some images - did the ParticleSizer run?");
			stopMonitoring();
			throw new IllegalStateException(
					"Missing some images - did the ParticleSizer run?");
		}

		ImagePlus resultImp = WindowManager.getImage(idImgWithResultOverlay);
		ImagePlus binaryImp = WindowManager.getImage(idBinary);
		ImagePlus rtAsImg = WindowManager.getImage(idRTasImage);
		ImagePlus contourAsImg = WindowManager.getImage(contourDataID);
		ArrayList<String> res = new ArrayList<String>();
		//When ellipse fitting is used, there is no meaningfull binary image
		if(binaryImp!=null){
			IJ.saveAsTiff(binaryImp, coOpPath + "/" + FILENAME_BINARY_RESULT);
			binaryImp.changes = false;
			binaryImp.close();
			res.add(FILENAME_BINARY_RESULT);
		}else{
			res.add("null");
		}
	
		IJ.saveAsTiff(rtAsImg, coOpPath + "/" + FILENAME_RESULTSTABLE_IMAGE);
		IJ.saveAsTiff(contourAsImg, coOpPath + "/" + FILENAME_CONTOUR_IMAGE);
		// Close windows
		resultImp.changes = false;
		resultImp.close();
		
		rtAsImg.changes = false;
		rtAsImg.close();
		contourAsImg.changes =false;
		contourAsImg.close();
		ResultsTable.getResultsWindow().close(false);
		
		String[] result = {FILENAME_BINARY_RESULT, FILENAME_RESULTSTABLE_IMAGE,FILENAME_CONTOUR_IMAGE};
		return result;
	}

	private void showGUI() {
		boolean useOldPath = ij.Prefs.get(PREF_REMEMBER_COOPPATH, false);
		if(useOldPath){
			coOpPath = ij.Prefs.get(PREF_COOPPATH, IJ.getDirectory("home"));
		}
		else{
				
			loadPreferences();
			JFileChooser fc = new JFileChooser(coOpPath);
			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int returnValue = fc.showOpenDialog(null);
			if (returnValue == JFileChooser.APPROVE_OPTION) {
				coOpPath = fc.getSelectedFile().getPath();
			}
	
			int result = JOptionPane.showConfirmDialog(null, "Remember path " + coOpPath + " and skip the selection next time?","Save path",JOptionPane.YES_NO_OPTION);
			if(result==JOptionPane.YES_OPTION){
				ij.Prefs.set(PREF_REMEMBER_COOPPATH, true);
			}else{
				ij.Prefs.set(PREF_REMEMBER_COOPPATH, false);
			}
			savePreferences();
		}
	}

	private void loadPreferences() {
		coOpPath = ij.Prefs.get(PREF_COOPPATH, IJ.getDirectory("home"));
	}

	private void savePreferences() {
		ij.Prefs.set(PREF_COOPPATH, coOpPath);
	}

}