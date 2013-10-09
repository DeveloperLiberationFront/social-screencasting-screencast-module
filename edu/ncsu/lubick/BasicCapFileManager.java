package edu.ncsu.lubick;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.log4j.Logger;

import com.wet.wired.jsr.recorder.CapFileManager;
import com.wet.wired.jsr.recorder.compression.FrameCompressorCodecStrategy;
import com.wet.wired.jsr.recorder.compression.FrameCompressorSavingStrategy;

public class BasicCapFileManager implements CapFileManager 
{
	private static Logger logger = Logger.getLogger(CapFileManager.class.getName());
	
	private OutputStream oStream;

	public BasicCapFileManager(File locationOfCapFile) throws FileNotFoundException {
		this(new FileOutputStream(locationOfCapFile));
	}
	
	public BasicCapFileManager(OutputStream oStream) {
		this.oStream = oStream;
	}

	@Override
	public void shutDown(){
		try 
		{
			oStream.close();
		} 
		catch (IOException e) {
			logger.error("Problem shutting down file",e);
		}

	}

	@Override
	public void flush() throws IOException {
		oStream.flush();

	}

	@Override
	public void setAndWriteFrameWidth(int width) throws IOException
	{
		this.write((width & 0x0000FF00) >>> 8);
		this.write((width & 0x000000FF));	

	}

	@Override
	public void setAndWriteFrameHeight(int height) throws IOException 
	{
		this.write((height & 0x0000FF00) >>> 8);
		this.write((height & 0x000000FF));

	}

	@Override
	public void startWritingFrame(boolean isFullFrame) throws IOException {
		//This callback can safely be ignored
	}

	@Override
	public void endWritingFrame() {
		//This callback can safely be ignored

	}

	@Override
	public void write(int i) throws IOException {
		oStream.write(i);

	}

	@Override
	public void write(byte[] bA) throws IOException {
		oStream.write(bA);

	}

	@Override
	public void write(byte[] dataToWrite, int offset, int numBytesToWrite) throws IOException {
		oStream.write(dataToWrite, offset, numBytesToWrite);

	}

	@Override
	public FrameCompressorCodecStrategy getCodecStrategy() {
		return null;	//null means use the default
	}

	@Override
	public FrameCompressorSavingStrategy getSavingStrategy() {
		return null; 	//null means use the default
	}

}
