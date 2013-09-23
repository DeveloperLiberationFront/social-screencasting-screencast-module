package com.wet.wired.jsr.recorder.compression;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class BufferedFrameCompressor extends FrameCompressor
{
	private ByteArrayOutputStream byteBuffer = null;

	private OutputStream outputToDisk;

	private boolean isRecording = true;

	private BufferedFrameCompressor(ByteArrayOutputStream byteStream, OutputStream oStream, int frameSize) {
		//We don't want the parent to write to disk.  We'll do that.
		super(byteStream, frameSize);
		this.outputToDisk = oStream;
		this.byteBuffer = byteStream;

		Thread backThread = new Thread(new Runnable() {

			@Override
			public void run() {
				// Every second, dump the byteBuffer to disk
				while(isRecording)
				{
					try {
						Thread.sleep(1000);
						byte[] bytesToWrite;
						synchronized (byteBuffer) 
						{	
							bytesToWrite = byteBuffer.toByteArray();
							byteBuffer.reset();
						}

						outputToDisk.write(bytesToWrite);

					} catch (InterruptedException | IOException e) {
						e.printStackTrace();
					}
				}

			}
		});
		backThread.start();
	}

	public static BufferedFrameCompressor makeBufferedFrameCompressor(OutputStream oStream, int frameSize)
	{
		return new BufferedFrameCompressor(new ByteArrayOutputStream(), oStream, frameSize);
	}

	@Override
	void writeData(byte[] dataToWrite, boolean hasChanges, int numBytesToWrite, FramePacket aFrame) throws IOException {
		synchronized (byteBuffer) {
			super.writeData(dataToWrite, hasChanges, numBytesToWrite, aFrame);
		}

	}

	@Override
	public void stop() {
		super.stop();
		isRecording=false;
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) 
		{
			e.printStackTrace();
		}
	}

}
