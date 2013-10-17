package com.wet.wired.jsr.recorder.compression;

public class CompressionFramePacket {


	long frameTime;

	public int[] previousData;
	public int[] newData;

	public byte[] dataToWriteBuffer = new byte[1];
	public boolean isFullFrame;



	public CompressionFramePacket(int frameSize) 
	{
		previousData = new int[frameSize];
	}

	public void updateFieldsForNextFrame(FrameDataPack pack) {
		this.frameTime = pack.frameTimeStamp;
		previousData = newData;
		if (previousData == null) 
		{
			previousData = new int[pack.newData.length];
		}
		this.newData = pack.newData;
	}


	public void resizeInternalBytesIfNeeded() {
		//Worst case scenario, we'll need 4 times as many bytes as ints that come in
		if (dataToWriteBuffer.length != (newData.length * 4))
		{
			dataToWriteBuffer = new byte[newData.length * 4];
		}

	}

	public boolean shouldForceFullFrameHuh() {
		return this.isFullFrame;
	}
}
