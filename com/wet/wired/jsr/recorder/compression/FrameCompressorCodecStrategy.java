package com.wet.wired.jsr.recorder.compression;

public interface FrameCompressorCodecStrategy {

	/**
	 * Takes the the inputData and fills packedBytes with a compressed version
	 * of the RGB values represented by the ints in inputData.
	 * 
	 * If forceFullFrame is true, this should use an entire frame instead of a diff from the previous frame
	 * 
	 * Returns the number of bytes that are in dataToWriteBuffer (Don't use the length as that won't have changed from
	 * the input and is probably vastly overestimated
	 * @param inputData
	 * @param aFrame
	 * @param packedBytes
	 * @param forceFullFrame
	 * @return How many bytes were put into packedBytes
	 */
	int compressDataUsingRunLengthEncoding(int[] newData, FramePacket frame, byte[] dataToWriteBuffer, boolean forceFullFrame);

}
