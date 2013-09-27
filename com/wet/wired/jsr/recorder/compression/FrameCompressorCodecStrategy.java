package com.wet.wired.jsr.recorder.compression;

public interface FrameCompressorCodecStrategy {

	int compressDataUsingRunLengthEncoding(int[] newData, FramePacket frame, byte[] dataToWriteBuffer, boolean b);

}
