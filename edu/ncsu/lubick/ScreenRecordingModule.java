package edu.ncsu.lubick;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

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
		String fileName = "scratch/temp.cap";

		File movFile = new File(scratchDir,"temp.mov");
		if (movFile.exists())
		{
			if (!movFile.delete())
			{
				throw new RuntimeException("Could not create temporary video file");
			}
		}



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

		FileOutputStream oStream = new FileOutputStream(fileName);
		recorder = new DesktopScreenRecorder(oStream, recorderBoss);
		recorder.startRecording();
		System.out.println("recording for 10 seconds" + (useCompression?" with compression":" without compression"));

		for(int i = 1;i<=10;i++)
		{
			System.out.println(i);
			Thread.sleep(1000);
		}


		recorder.stopRecording();

		String[] newArgs = new String[]{"scratch/temp.cap"}; 


		System.out.println("Rendering");

		RecordingConverter.main(newArgs);

		//ProcessBuilder pb = new ProcessBuilder("java", "-jar", "screen_converter.jar", "temp.cap");

		//pb.directory(new File("scratch/"));
		//Process p = pb.start();
		//System.out.println(new Date());
		//inheritIO(p.getInputStream(), System.out);
		//inheritIO(p.getErrorStream(), System.err);
		//System.out.println("Rendering");
		//System.out.println(p.waitFor());

		//System.out.println("Has Rendered");
	}




	/*private static void inheritIO(final InputStream src, final PrintStream dest) {
		new Thread(new Runnable() {
			public void run() {
				Scanner sc = new Scanner(src);
				while (sc.hasNextLine()) {
					dest.println(new Date() + sc.nextLine());
				}
				sc.close();
			}
		}).start();
	}*/

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
