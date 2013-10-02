package edu.ncsu.lubick;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

public class RotatingBufferedCapFileManager extends BasicCapFileManager 
{
	private static final long DELAY_FOR_NEW_FILE_MS = 60*1000;
	
	private static Logger logger = Logger.getLogger(RotatingBufferedCapFileManager.class.getName());

	private ByteArrayOutputStream byteBuffer;

	protected boolean isRecording = true;

	private RotatingFileManager rotatingFileManager;

	private Timer timer;

	protected boolean shouldGoToNextFile = false;

	private int frameHeight;

	private int frameWidth;
	
	
	
	private RotatingBufferedCapFileManager(RotatingFileManager rfm, ByteArrayOutputStream byteStream) {
		super(byteStream);
		this.byteBuffer = byteStream;
		this.rotatingFileManager = rfm;
		
		isRecording = true;
		Thread backThread = new Thread(new Runnable() {

			@Override
			public void run() {
				// Every second, dump the byteBuffer to disk
				while(isRecording)
				{
					try {
						dumpByteBufferToDisk();
						
						Thread.sleep(1000);
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

	
	public static RotatingBufferedCapFileManager makeRotatingFileManager(File directory, String prefix, String fileExtension) throws IOException 
	{
		RotatingFileManager rfm = new RotatingFileManager(directory, prefix, fileExtension);
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
		} catch (IOException e) {
			logger.error("Problem when shutting down",e);
		}
	}


	private void dumpByteBufferToDisk() throws IOException {
		byte[] bytesToWrite;
		synchronized (byteBuffer) 
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
	public void startWritingFrame(boolean isFullFrame) throws IOException 
	{
		if (shouldGoToNextFile && isFullFrame)
		{
			//clear out what is in the disk so far
			dumpByteBufferToDisk();
			this.rotatingFileManager.makeNextFile();
			writeFileHeader();
			shouldGoToNextFile = false;
		}
		super.startWritingFrame(isFullFrame);
	}


	private void writeFileHeader() throws IOException 
	{
		setAndWriteFrameWidth(this.frameWidth);
		setAndWriteFrameHeight(this.frameHeight);
	}
	
	
}
