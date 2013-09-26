package com.wet.wired.jsr.recorder.compression;

class FramePacket {


	long frameTime;

	int[] previousData;
	int[] newData;

	FramePacket(int frameSize) {
		previousData = new int[frameSize];
	}

	void updateFieldsForNextFrame(int[] frameData, long frameTime, boolean reset) 
	{
		this.frameTime = frameTime;
		previousData = newData;
		newData = null;
		if (previousData == null) 
		{
			previousData = new int[frameData.length];
		}
		if (reset) 
		{
			this.newData = new int[frameData.length];
		} 
		else 
		{
			this.newData = frameData;
		}
	}
}
