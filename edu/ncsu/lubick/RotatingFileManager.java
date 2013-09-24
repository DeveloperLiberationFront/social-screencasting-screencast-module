package edu.ncsu.lubick;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A RotatingFileManger acts like an OutputStream, but takes any writes and puts
 * them to a file.  After a fixed amount of time, the Manager switches to the next file
 * when writing.  This will be a seamless transition, although on the switch, write() might
 * block for a bit longer than usual.
 * @author Kevin Lubick
 *
 */
public class RotatingFileManager extends OutputStream {

	private static final long DELAY_FOR_NEW_FILE_MS = 20*1000;
	
	private FileOutputStream currentFileStream = null;

	private File directory;

	private String prefix;

	private RotatingFileManagerListener listener;

	private String fileExtension;

	private Timer timer;
	
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
		
		timer = new Timer(true);
		timer.schedule(new TimerTask() {
			
			@Override
			public void run() {
				try {
					System.out.println("ping");
					makeNextFile();
				} catch (IOException e) 
				{
					e.printStackTrace();
				}
			}
		}, 0, DELAY_FOR_NEW_FILE_MS);
	}
	
	private int countOfFile = 0;
	
	private void makeNextFile() throws IOException 
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
		System.out.println("Changing to file "+newFile);
		synchronized (this) {
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
		
	}

	@Override
	public void write(int b) throws IOException {
		//This is synchronized to avoid changing the output stream mid-write
		synchronized (this) 
		{
			currentFileStream.write(b);
		}

	}
	
	@Override
	public void write(byte[] b) throws IOException {
		//This is synchronized to avoid changing the output stream mid-write
		synchronized (this) 
		{
			currentFileStream.write(b);
		}
	}
	
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		//This is synchronized to avoid changing the output stream mid-write
		synchronized (this) {
			currentFileStream.write(b, off, len);
		}
		
	}
	
	@Override
	public void close() throws IOException {
		synchronized (this) {
			super.close();
			timer.cancel();
			currentFileStream.close();
		}
	}
	
	public static class DefaultListener implements RotatingFileManagerListener
	{

		private String padIntTo4Digits(int i)
		{
			if (i<0)
			{
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
		public boolean canSwitchFileHuh() {
			return true;
		}

		@Override
		public String getNextSuffix() {
			index++;
			return padIntTo4Digits(index);
		}
		
	}

}




