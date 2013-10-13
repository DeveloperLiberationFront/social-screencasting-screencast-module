package edu.ncsu.lubick;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.log4j.Logger;

/**
 * A RotatingFileManger manages an OutputStream.  On command, the Manager switches to the next file
 * when writing.  
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
	}

	public void makeNextFile() throws IOException 
	{
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




