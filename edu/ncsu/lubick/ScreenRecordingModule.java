package edu.ncsu.lubick;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.wet.wired.jsr.recorder.DesktopScreenRecorder;
import com.wet.wired.jsr.recorder.ScreenRecorderListener;

public class ScreenRecordingModule implements ScreenRecorderListener
{

	public static boolean useCompression = true;

	public static boolean useBufferedFrameCompressor = true;

	public static boolean useRotatingFileManager = false;
	
	private static DesktopScreenRecorder recorder;

	



	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception
	{
		ScreenRecorderListener recorderBoss = new ScreenRecordingModule();

		//System.out.println(System.getenv());

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

		OutputStream oStream;
		if (useRotatingFileManager )
		{
			oStream = new RotatingFileManager(scratchDir, "temp","cap");
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

            oStream = new FileOutputStream(fileName);
		}
		
		
		recorder = new DesktopScreenRecorder(oStream, recorderBoss);
		recorder.startRecording();
		System.out.println("recording for 1 hour" + (useCompression?" with compression":" without compression"));

		for(int i = 1;i<=60*60;i++)
		{
			System.out.println(i);
			Thread.sleep(1000);
		}


		recorder.stopRecording();
		 
	

		/*for(File file:scratchDir.listFiles())
		{
			if (file.getName().endsWith(".cap"))
			{
				System.out.println("Parsing "+file.toString());
				String[] newArgs = new String[]{"scratch/"+file.getName()}; 
				RecordingConverter.main(newArgs);
				Thread.sleep(1000);
			}
		}*/



	}


	@Override
	public void frameRecorded(boolean fullFrame) throws IOException
	{

	}

	@Override
	public void recordingStopped()
	{
		System.err.println("Recording Stopped");

	}



}
