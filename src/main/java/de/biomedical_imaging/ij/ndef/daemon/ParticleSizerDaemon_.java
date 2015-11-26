package de.biomedical_imaging.ij.ndef.daemon;

import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;

/**
 * 
 * ParticleSizer Daemon tool for the NanoDefine AutoTEM solution. 
 * 
 * @author Thorsten Wagner (wagner@biomedical-imaging.de)
 *
 */
public class ParticleSizerDaemon_ implements PlugIn {

	
	private String coOpPath = ""; /** Cooperation path */
	
	private final String PREF_COOPPATH = "ndef.daemon.cooppath";
	
	@Override
	public void run(String arg) {
		showGUI();
		// Run the particle analyzer with current settings
		
		// Get the results and save them in the cooperation path
		int idHist = Integer.parseInt(ij.Prefs.get("ndef.result.histid", "-1"));
		int idImgWithResultOverlay  = Integer.parseInt(ij.Prefs.get("ndef.result.imagewithoverlayid", "-1"));
		int idBinary = Integer.parseInt(ij.Prefs.get("ndef.result.binaryid", "-1"));
		
		if(idHist == -1 || idImgWithResultOverlay == -1 || idBinary == -1){
			throw new IllegalStateException("Missing some images - did the ParticleSizer run?");
		}
		
		ImagePlus histImp = WindowManager.getImage(idHist);
		ImagePlus resultImp = WindowManager.getImage(idImgWithResultOverlay);
		ImagePlus binaryImp = WindowManager.getImage(idBinary);
		
	}
	
	private void showGUI(){
		loadPreferences();
		
		GenericDialogPlus gdp = new GenericDialogPlus("Particle Sizer Daemon");
		gdp.addFileField("Co-op folder", coOpPath);
		gdp.showDialog();
		
		if(gdp.wasCanceled()){
			return;
		}
		
		coOpPath = gdp.getNextString();
		
		savePreferences();
	}
	
	private void loadPreferences(){
		coOpPath = ij.Prefs.get(PREF_COOPPATH, IJ.getDirectory("home"));
	}
	
	private void savePreferences(){
		ij.Prefs.set(PREF_COOPPATH,coOpPath);
	}
	
}