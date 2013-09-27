package edu.ncsu.lubick;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.log4j.Logger;

/**
 * A RotatingFileManger acts like an OutputStream, but takes any writes and puts
 * them to a file.  After a fixed amount of time, the Manager switches to the next file
 * when writing.  This will be a seamless transition, although on the switch, write() might
 * block for a bit longer than usual.
 * @author Kevin Lubick
 *
 */
public class RotatingFileManager {

	private static Logger logger = Logger.getLogger(RotatingFileManager.class.getName());

	private FileOutputStream currentFileStream = null;

	private File directory;

	private String prefix;

	private RotatingFileManagerListener listener;

	private String fileExtension;

	/**
	 * Makes a new RotatingFileManger in a given directory.
	 * 
	 * Files will be created prefix_NNNN, where NNNN are numbers
	 * @param directory
	 * @param prefix
	 * @param fileExtension 
	 */
	public RotatingFileManager(File directory, String prefix, String fileExtension) 
	{
		this(directory, prefix, fileExtension, new DefaultListener());
	}

	public RotatingFileManager(File directory, String prefix, String fileExtension, RotatingFileManagerListener listener) 
	{
		if (directory.exists() && !directory.isDirectory())
		{
			throw new IllegalArgumentException("The passed in file must be a directory");
		}
		if (!directory.exists())
		{
			if (!directory.mkdirs())
			{
				throw new RuntimeException("Could not make the folder for the files to exist");
			}
		}
		this.directory = directory;
		this.prefix = prefix;
		this.fileExtension = fileExtension;
		this.listener = listener;

//		timer = new Timer(true);
//		timer.schedule(new TimerTask() {
//
//			@Override
//			public void run() {
//				try {
//					logger.debug("ping");
//					makeNextFile();
//				} catch (IOException e) 
//				{
//					logger.error("There was a problem rotating files", e);
//				}
//			}
//		}, 0, DELAY_FOR_NEW_FILE_MS);
	}

	private int countOfFile = 0;

	public void makeNextFile() throws IOException 
	{
		countOfFile++;
		File newFile = new File(directory, this.prefix + listener.getNextSuffix()+"."+fileExtension);
		if (newFile.exists())
		{
			if (!newFile.delete())
			{
				throw new RuntimeException("Could not overwrite file "+newFile);
			}
		}

		if (!newFile.createNewFile())
		{
			throw new RuntimeException("Could not create new file file "+newFile);
		}
		logger.debug("Changing to file "+newFile);

			if (this.currentFileStream!=null)
			{
				this.currentFileStream.close();
			}
			this.currentFileStream = new FileOutputStream(newFile);
			if (countOfFile !=1)
			{
				currentFileStream.write((1600 & 0x0000FF00) >>> 8);	//width
				currentFileStream.write((1600 & 0x000000FF));

				currentFileStream.write((900 & 0x0000FF00) >>> 8);	//height
				currentFileStream.write((900 & 0x000000FF));
			}

		

	}

	public FileOutputStream getCurrentFileStream() {
		return currentFileStream;
	}

	public void stop()
	{
		if (currentFileStream != null)
		{
			try {
				this.currentFileStream.close();
			} catch (IOException e) 
			{
				logger.error("Problem closing fileStream",e);
			}
			this.currentFileStream = null;
		}
	}


	public static class DefaultListener implements RotatingFileManagerListener
	{

		private String padIntTo4Digits(int i)
		{
			if (i<0)
			{
				logger.error("Who put a negative here? "+i);
				return "I cant deal with negatives";
			}
			if (i<10)
			{
				return "000"+i;
			}
			if (i<100)
			{
				return "00"+i;
			}
			if (i<1000)
			{
				return "0"+i;
			}
			return String.valueOf(i);
		}

		private int index = -1;

		@Override
		public String getNextSuffix() {
			index++;
			return padIntTo4Digits(index);
		}

	}

}




