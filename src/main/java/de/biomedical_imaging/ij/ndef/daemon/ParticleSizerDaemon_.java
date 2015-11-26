package de.biomedical_imaging.ij.ndef.daemon;

import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.plugin.PlugIn;

/**
 * 
 * ParticleSizer Daemon tool for the NanoDefine AutoTEM solution. 
 * 
 * @author Thorsten Wagner (wagner@biomedical-imaging.de)
 *
 */
public class ParticleSizerDaemon_ implements PlugIn {

	private String coOpPath = "";
	
	private final String PREF_COOPPATH = "ndef.daemon.cooppath";
	
	@Override
	public void run(String arg) {
		showGUI();
		
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