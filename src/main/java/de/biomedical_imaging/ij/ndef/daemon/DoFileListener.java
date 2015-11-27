package de.biomedical_imaging.ij.ndef.daemon;

import ij.IJ;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

public class DoFileListener extends FileAlterationListenerAdaptor {
	private FileAlterationMonitor monitor;
	private ParticleSizerDaemon_ daemon;
	
	public DoFileListener(FileAlterationMonitor monitor,ParticleSizerDaemon_ daemon) {
		this.monitor = monitor;
		this.daemon = daemon;
	}
	
	@Override
	public void onFileChange(File file) {
		super.onFileChange(file);
		if(file.getName().equals("do.txt")){
			String[] com = daemon.getReadCommandAndFilename(file);
		
			if(com[0].equals("DO")){
				daemon.doAnalysis(com[0], com[1]);
				// Update do.txt
				PrintWriter writer;
				try {
					writer = new PrintWriter(file);
					writer.print("DONE;bin.tif;rt.tif");
					writer.close();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
			else if(com[0].equals("STOP")){
				try {
					monitor.stop();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else if(com[0].equals("ERROR")){
				try {
					monitor.stop();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
	}
	
}
