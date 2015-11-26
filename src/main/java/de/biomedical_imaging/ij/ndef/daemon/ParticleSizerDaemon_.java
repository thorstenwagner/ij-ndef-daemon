package de.biomedical_imaging.ij.ndef.daemon;

import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.plugin.PlugIn;


public class ParticleSizerDaemon_ implements PlugIn {

	@Override
	public void run(String arg) {
		GenericDialogPlus gdp = new GenericDialogPlus("Particle Sizer Daemon");
		gdp.addFileField("Co-op folder", IJ.getDirectory("home"));
		gdp.showDialog();
		
		if(gdp.wasCanceled()){
			return;
		}
		
		String coOpPath = gdp.getNextString();
		
	}
	
}