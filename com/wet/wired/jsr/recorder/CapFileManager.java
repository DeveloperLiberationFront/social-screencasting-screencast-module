package com.wet.wired.jsr.recorder;

import java.io.IOException;

import com.wet.wired.jsr.recorder.compression.FrameCompressorCodecStrategy;
import com.wet.wired.jsr.recorder.compression.FrameCompressorSavingStrategy;

/**
 * This is the class that handles writing out CapFiles.  Implementations may write to multiple files
 * or one file.  Additionally, this manager can specify what strategies are used for a codec and writing to disk.
 * 
 * @author Kevin Lubick
 *
 */
public interface CapFileManager {

	public void shutDown();
	public void flush() throws IOException;
	

	public void setAndWriteFrameWidth(int width) throws IOException;
	public void setAndWriteFrameHeight(int height) throws IOException;
	
	public void notifyStartWritingFrame(boolean isFullFrame) throws IOException;
	public void notifyEndWritingFrame();
	
	public void write(int i) throws IOException;
	public void write(byte[] bA) throws IOException;
	public void write(byte[] dataToWrite, int offset, int numBytesToWrite) throws IOException;
	
	
	/**
	 * Returns the CodecStrategy associated with this FileManager or null to use the default
	 * @return
	 */
	public FrameCompressorCodecStrategy getCodecStrategy();
	/**
	 * Returns the SavingStrategy associated with this FileManager or null to use the default
	 * @return
	 */
	public FrameCompressorSavingStrategy getSavingStrategy();


	
	
	

	

}
