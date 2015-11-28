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

	public DoFileListener(FileAlterationMonitor monitor,
			ParticleSizerDaemon_ daemon) {
		this.monitor = monitor;
		this.daemon = daemon;
	}

	@Override
	public void onFileChange(File file) {
		super.onFileChange(file);
		if (file.getName().equals("do.txt")) {
			if (ParticleSizerDaemon_.beChatty) {
				IJ.log("DO.TXT was changed");
			}
			analyseDoFile(file);
		}
	}

	public void analyseDoFile(File file) {
		String[] com = daemon.getReadCommandAndFilename(file);

		if (com[0].equals("DO")) {
			String[] resultFilenames = daemon.doAnalysis(com[0], com[1]);
			// Update do.txt
			PrintWriter writer;
			String res;

			if (resultFilenames[0].equals("ERROR")) {
				res = "ERROR";
				try {
					monitor.stop();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				IJ.error("An error occured. Stop monitoring do.txt.");
			} else {
				// Build String
				res = "DONE;";
				for (int i = 0; i < resultFilenames.length - 1; i++) {
					res += resultFilenames[i] + ";";
				}
				res += resultFilenames[resultFilenames.length - 1];
			}
			try {
				writer = new PrintWriter(file);
				writer.print(res);
				writer.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else if (com[0].equals("STOP")) {
			try {
				monitor.stop();
				if (ParticleSizerDaemon_.beChatty) {
					IJ.log("Monitor stopped");
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				IJ.log("Monitor stopped");
				e.printStackTrace();
			}
		} else if (com[0].equals("ERROR")) {
			try {
				monitor.stop();
				IJ.error("An error occured. Stop monitoring do.txt.");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (com[0].equals("IDLE")) {
			if (ParticleSizerDaemon_.beChatty) {
				IJ.log("Idle");
			}
		}
	}

}
