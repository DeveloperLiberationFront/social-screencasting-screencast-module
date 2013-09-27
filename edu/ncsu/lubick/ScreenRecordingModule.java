package edu.ncsu.lubick;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.wet.wired.jsr.converter.RecordingConverter;
import com.wet.wired.jsr.recorder.CapFileManager;
import com.wet.wired.jsr.recorder.DesktopScreenRecorder;
import com.wet.wired.jsr.recorder.ScreenRecorderListener;

public class ScreenRecordingModule implements ScreenRecorderListener
{
	private static final String LOGGING_FILE_PATH = "./log4j.settings";
	
	public static boolean useCompression = true;

	public static boolean useBufferedFrameCompressor = true;

	public static boolean useRotatingFileManager = false;
	
	private static DesktopScreenRecorder recorder;

	private static Logger logger;

	//Static initializer to get the logging path set up and create the hub
	static {
		PropertyConfigurator.configure(ScreenRecordingModule.LOGGING_FILE_PATH);
		logger = Logger.getLogger(ScreenRecordingModule.class.getName());
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception
	{
		ScreenRecorderListener recorderBoss = new ScreenRecordingModule();

		logger.trace(System.getenv());

		File scratchDir = new File("scratch/");
		if (!scratchDir.exists())
		{
			if (!scratchDir.mkdir())
			{
				throw new RuntimeException("Could not create scratch folder");
			}
		}


		for(File file:scratchDir.listFiles())
		{
			if (file.getName().startsWith("temp"))
			{
				if (!file.delete())
				{
					throw new RuntimeException("Could not clear out old mov files");
				}
			}
		}

		CapFileManager fileManager;
		if (useRotatingFileManager )
		{
			fileManager = RotatingBufferedCapFileManager.makeRotatingFileManager(scratchDir, "temp","cap");
		}
		else
		{
			String fileName = "scratch/temp.cap";

            File temp = new File(fileName);
            if (temp.exists())
            {
                    if (!temp.delete())
                    {
                            throw new RuntimeException("Could not clear out old cap file");
                    }

            }
            if (!temp.createNewFile())
            {
                    throw new RuntimeException("Could not create new cap file");
            }

            fileManager = new BasicCapFileManager(temp);
		}
		
		
		recorder = new DesktopScreenRecorder(fileManager, recorderBoss);
		recorder.startRecording();
		logger.info("recording for 60 seconds" + (useCompression?" with compression":" without compression"));

		for(int i = 1;i<=60;i++)
		{
			logger.debug(i);
			Thread.sleep(1000);
		}


		recorder.stopRecording();
		 
	

		for(File file:scratchDir.listFiles())
		{
			if (file.getName().endsWith(".cap"))
			{
				logger.info("Converting to video "+file.toString());
				String[] newArgs = new String[]{"scratch/"+file.getName()}; 
				RecordingConverter.main(newArgs);
				Thread.sleep(1000);
				logger.info("Finished converting");
			}
		}



	}


	@Override
	public void frameRecorded(boolean fullFrame) throws IOException
	{

	}

	@Override
	public void recordingStopped()
	{
		logger.info("Recording Stopped");

	}



}
