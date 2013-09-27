package com.wet.wired.jsr.recorder.compression;

import java.io.IOException;

public interface FrameCompressorSavingStrategy {

	void writeData(byte[] dataToWriteBuffer, boolean currentFrameHasChanges, int numBytesToWrite, FramePacket frame, boolean isFullFrame) throws IOException;

}
