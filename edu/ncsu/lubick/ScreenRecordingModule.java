package edu.ncsu.lubick;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import com.wet.wired.jsr.converter.RecordingConverter;
import com.wet.wired.jsr.recorder.DesktopScreenRecorder;
import com.wet.wired.jsr.recorder.ScreenRecorderListener;

public class ScreenRecordingModule implements ScreenRecorderListener
{

	public static boolean useCompression = true;

	public static boolean useBufferedFrameCompressor = true;

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


		File movFile = new File(scratchDir,"temp.mov");
		if (movFile.exists())
		{
			if (!movFile.delete())
			{
				throw new RuntimeException("Could not create temporary video file");
			}
		}

		/*OutputStream oStream = new RotatingFileManager(scratchDir, "temp","cap");

		recorder = new DesktopScreenRecorder(oStream, recorderBoss);
		recorder.startRecording();
		System.out.println("recording for 30 seconds" + (useCompression?" with compression":" without compression"));

		for(int i = 1;i<=60;i++)
		{
			System.out.println(i);
			Thread.sleep(1000);
		}


		recorder.stopRecording();
		 */


		for(File file:scratchDir.listFiles())
		{
			if (file.getName().endsWith(".cap"))
			{
				System.out.println("Parsing "+file.toString());
				String[] newArgs = new String[]{"scratch/"+file.getName()}; 
				RecordingConverter.main(newArgs);
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
		System.err.println("Recording Stopped");

	}



}
