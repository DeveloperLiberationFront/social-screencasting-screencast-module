package edu.ncsu.lubick;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

/**
 * Uses a rotating File Manager to act as a CapFileManager for the recording.
 * 
 * Basically adds a byteBuffer between the recording interface that needs to write to this
 * and writing to the harddisk.  This helps keep lag for individual writes pretty minimal
 * @author KevinLubick
 *
 */
public class RotatingBufferedCapFileManager extends BasicCapFileManager 
{
	private static final int MILLIS_BETWEEN_DISK_DUMPS = 2000;
	private static final long DELAY_FOR_NEW_FILE_MS = 60*1000;
	private static final int EOF_SIGNAL = -1;
	private static Logger logger = Logger.getLogger(RotatingBufferedCapFileManager.class.getName());

	private ByteArrayOutputStream byteBuffer;
	private RotatingFileManager rotatingFileManager;
	
	protected boolean isRecording = true;
	private Timer timer;
	protected boolean shouldGoToNextFile = false;
	private int frameHeight;
	private int frameWidth;
	
	
	
	private RotatingBufferedCapFileManager(RotatingFileManager rfm, ByteArrayOutputStream underlyingByteStream) {
		//reuses all the basic functionality of a CapFileManager, but it's not with a outputStream that writes
		//to disk, it's a ByteOutputArrayStream that we will monitor and dump to disk.
		super(underlyingByteStream);
		this.byteBuffer = underlyingByteStream;
		this.rotatingFileManager = rfm;
		
		isRecording = true;
		Thread backThread = new Thread(new Runnable() {

			@Override
			public void run() {
				// Every so often, dump the byteBuffer to disk
				while(isRecording)
				{
					try {
						dumpByteBufferToDisk();
						
						Thread.sleep(MILLIS_BETWEEN_DISK_DUMPS);
					} catch (InterruptedException | IOException e) {
						logger.error("Problem in writing loop",e);
					}
				}

			}
		},"Buffered To Disk Thread");
		backThread.start();
		
		timer = new Timer(true);
		timer.schedule(new TimerTask() {

			@Override
			public void run() 
			{
				logger.debug("ping");
				shouldGoToNextFile = true;
			}
		}, DELAY_FOR_NEW_FILE_MS, DELAY_FOR_NEW_FILE_MS);
	}

	
	public static RotatingBufferedCapFileManager makeBufferedRotatingFileManager(File directory, String prefix, String fileExtension) throws IOException 
	{
		RotatingFileManager rfm = new RotatingFileManager(directory, prefix, fileExtension);
		return makeBufferedRotatingFileManager(rfm);
	}
	
	public static RotatingBufferedCapFileManager makeBufferedRotatingFileManager(RotatingFileManager rfm) throws IOException
	{
		rfm.makeNextFile();
		
		return new RotatingBufferedCapFileManager(rfm, new ByteArrayOutputStream());
	}
	
	
	@Override
	public void shutDown() {
		super.shutDown();
		timer.cancel();
		isRecording = false;
		try {
			dumpByteBufferToDisk();
			closeOffFile();
		} 
		catch (IOException e) {
			logger.error("Problem when shutting down",e);
		}
	}


	private void dumpByteBufferToDisk() throws IOException {
		byte[] bytesToWrite;
		
		synchronized (byteBuffer) //Makes sure that this doesn't change when we read from it
		{	
			bytesToWrite = byteBuffer.toByteArray();
			byteBuffer.reset();
		}
		//Just to make sure that the isRecording isn't changed
		//right before we write to disk
		if (isRecording && bytesToWrite.length != 0)
		{
			rotatingFileManager.getCurrentFileStream().write(bytesToWrite);
		}

	}
	
	@Override
	public void setAndWriteFrameWidth(int width) throws IOException
	{
		this.frameWidth = width;
		super.setAndWriteFrameWidth(width);
	}

	@Override
	public void setAndWriteFrameHeight(int height) throws IOException 
	{
		this.frameHeight = height;
		super.setAndWriteFrameHeight(height);
	}
	
	@Override
	public void notifyStartWritingFrame(boolean isFullFrame) throws IOException 
	{
		if (shouldGoToNextFile && isFullFrame)
		{
			
			//clear out what is in the disk so far
			dumpByteBufferToDisk();
			closeOffFile();
			this.rotatingFileManager.makeNextFile();
			writeFileHeader();
			shouldGoToNextFile = false;
		}
		super.notifyStartWritingFrame(isFullFrame);
	}


	private void closeOffFile() 
	{
		logger.debug("Closing old file");
		try {
			rotatingFileManager.getCurrentFileStream().write(EOF_SIGNAL);
		} catch (IOException e) {
			logger.error("There was a problem closing the old file");
			return;
		}
		logger.debug("Succesfully closed");
	}


	private void writeFileHeader() throws IOException 
	{
		setAndWriteFrameWidth(this.frameWidth);
		setAndWriteFrameHeight(this.frameHeight);
	}
	
	
}
