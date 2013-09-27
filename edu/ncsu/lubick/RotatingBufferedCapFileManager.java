package edu.ncsu.lubick;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

public class RotatingBufferedCapFileManager extends BasicCapFileManager 
{
	private static final long DELAY_FOR_NEW_FILE_MS = 10*1000;
	
	private static Logger logger = Logger.getLogger(RotatingBufferedCapFileManager.class.getName());
	

	
	
	private RotatingBufferedCapFileManager(RotatingFileManager rfm) {
		super(oStream);

	}

	
	public static RotatingBufferedCapFileManager makeRotatingFileManager(File directory, String prefix, String fileExtension) 
	{
		RotatingFileManager rfm = new RotatingFileManager(directory, prefix, fileExtension);
		rfm.makeNextFile();
		
	}
	
}
