package com.wet.wired.jsr.recorder.compression;

public class FrameDataPack {
	public int[] newData;
	public long frameTimeStamp;

	public FrameDataPack(int[] newDatas, long frameTimeStamp) {
		this.newData = newDatas;
		this.frameTimeStamp = frameTimeStamp;
	}
}