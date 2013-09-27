//package com.wet.wired.jsr.recorder.compression;
//
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.io.OutputStream;
//
//import org.apache.log4j.Logger;
//
//import com.wet.wired.jsr.recorder.CapFileManager;
//
//public class BufferedFrameCompressor extends FrameCompressor
//{
//	private static Logger logger = Logger.getLogger(BufferedFrameCompressor.class.getName());
//	
//	private ByteArrayOutputStream byteBuffer = null;
//
//	private OutputStream outputToDisk;
//
//	private boolean isRecording = true;
//
//	private BufferedFrameCompressor(ByteArrayOutputStream byteStream, CapFileManager capFileManager, int frameSize) {
//		//We don't want the parent to write to disk.  We'll do that.
//		super(byteStream, frameSize);
//		this.outputToDisk = capFileManager;
//		this.byteBuffer = byteStream;
//
//		Thread backThread = new Thread(new Runnable() {
//
//			@Override
//			public void run() {
//				// Every second, dump the byteBuffer to disk
//				while(isRecording)
//				{
//					try {
//						byte[] bytesToWrite;
//						synchronized (byteBuffer) 
//						{	
//							bytesToWrite = byteBuffer.toByteArray();
//							byteBuffer.reset();
//						}
//						//Just to make sure that the isRecording isn't changed
//						//right before we write to disk
//						synchronized (outputToDisk) 
//						{
//							if (isRecording)
//							{
//								outputToDisk.write(bytesToWrite);
//							}
//						}
//
//						Thread.sleep(1000);
//
//					} catch (InterruptedException | IOException e) {
//						logger.error("Problem in writing loop",e);
//					}
//				}
//
//			}
//		},"Buffered To Disk Thread");
//		backThread.start();
//	}
//
//	public static BufferedFrameCompressor makeBufferedFrameCompressor(CapFileManager capFileManager, int frameSize)
//	{
//		return new BufferedFrameCompressor(new ByteArrayOutputStream(), capFileManager, frameSize);
//	}
//
//	@Override
//	void writeData(byte[] dataToWrite, boolean hasChanges, int numBytesToWrite, FramePacket aFrame) throws IOException {
//		
//		//we have to make sure that the byte buffer isn't getting unloaded (write to disk)
//		//or that our outputToDisk is in use elsewhere
//		synchronized (outputToDisk) {
//			synchronized (byteBuffer) {
//				super.writeData(dataToWrite, hasChanges, numBytesToWrite, aFrame);
//			}
//		}
//
//	}
//
//	@Override
//	public void stop() 
//	{
//		//Make sure that the last outputToDisk write finishes
//		synchronized (outputToDisk) 
//		{
//			super.stop();
//			isRecording=false;
//		}
//		
//	}
//
//}
