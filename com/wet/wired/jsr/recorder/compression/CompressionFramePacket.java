package com.wet.wired.jsr.recorder.compression;

public class CompressionFramePacket {


	long frameTime;

	int[] previousData;
	int[] newData;

	CompressionFramePacket(int frameSize) {
		previousData = new int[frameSize];
	}

	public void updateFieldsForNextFrame(int[] newFrameData, long newFrameTime, boolean reset) 
	{
		this.frameTime = newFrameTime;
		previousData = newData;
		newData = null;
		if (previousData == null) 
		{
			previousData = new int[newFrameData.length];
		}
		if (reset) 
		{
			this.newData = new int[newFrameData.length];
		} 
		else 
		{
			this.newData = newFrameData;
		}
	}
}
