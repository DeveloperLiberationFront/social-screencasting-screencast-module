package com.wet.wired.jsr.recorder.compression;

import java.io.IOException;

/**
 * This interface is usually used to further compress the data and save it to disk
 * @author KevinLubick
 *
 */
public interface FrameCompressorSavingStrategy {

	void writeDataToCapFile(byte[] dataToWrite, int numBytesToWrite, long frameTime) throws IOException;

	void writeBlankFrameToCapFile(long frameTime) throws IOException;

}
