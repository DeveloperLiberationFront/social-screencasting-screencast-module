/*
 * This software is OSI Certified Open Source Software
 * 
 * The MIT License (MIT)
 * Copyright 2000-2001 by Wet-Wired.com Ltd., Portsmouth England
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the 
 * Software is furnished to do so, subject to the following conditions: 
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software. 
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO
 * EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES
 * OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 * 
 */

package com.wet.wired.jsr.recorder;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;

import com.wet.wired.jsr.recorder.compression.FrameCompressor;
import com.wet.wired.jsr.recorder.compression.FrameDataPack;

public abstract class ScreenRecorder implements Runnable {

	private static final int FRAME_RATE_LIMITER = 190;
	private static Logger logger = Logger.getLogger(ScreenRecorder.class.getName());

	private Rectangle recordArea;

	private int frameSize;
	private int[] rawData;

	private CapFileManager capFileManager;

	private boolean recording = false;
	private boolean running = false;

	private long startTime;
	private long frameTime;

	private ScreenRecorderListener listener;



	private StreamPacker streamPacker;

	public ScreenRecorder(CapFileManager capFileManager, ScreenRecorderListener listener) {

		this.listener = listener;
		this.capFileManager = capFileManager;
	}

	public void triggerRecordingStop() {
		recording = false;

	}

	@Override
	public void run() {
		startTime = System.currentTimeMillis();

		recording = true;
		running = true;
		long lastFrameTime = 0;
		long time = 0;

		frameSize = recordArea.width * recordArea.height;
		streamPacker = new StreamPacker(capFileManager, frameSize);

		while (recording) {
			time = System.currentTimeMillis();
			while (time - lastFrameTime < FRAME_RATE_LIMITER) {
				try {
					Thread.sleep(10);
				} catch (Exception e) {
				}
				time = System.currentTimeMillis();
			}
			lastFrameTime = time;

			try {
				recordFrame();
			} catch (Exception e) {
				logger.error("Problem in main loop",e);

				capFileManager.shutDown();

				break;
			}
		}

		running = false;
		recording = false;


		listener.recordingStopped();
	}

	public abstract Rectangle initialiseScreenCapture();

	public abstract BufferedImage captureScreen(Rectangle areaToRecord);

	public void recordFrame() throws IOException {
		//long t1 = System.currentTimeMillis();
		BufferedImage bImage = captureScreen(recordArea);
		long t2 = System.currentTimeMillis();
		frameTime = t2 - startTime;

		
		
		//ImageIO.write
		/*rawData = new int[frameSize];

		bImage.getRGB(0, 0, recordArea.width, recordArea.height, rawData, 0,
				recordArea.width);
		//long t3 = System.currentTimeMillis();

		streamPacker.packToStream(new FrameDataPack(rawData, frameTime));*/

		//if (logger.isTraceEnabled()) logger.trace("Times");
		//if (logger.isTraceEnabled()) logger.trace("  capture time:"+(t2-t1));
		//if (logger.isTraceEnabled()) logger.trace("  data grab time:"+(t3-t2));

		listener.frameRecorded(true);
	}

	public void startRecording() {
		recordArea = initialiseScreenCapture();

		if (recordArea == null) {
			return;
		}
		try 
		{
			capFileManager.setAndWriteFrameWidth(recordArea.width);
			capFileManager.setAndWriteFrameHeight(recordArea.height);

		} catch (Exception e) {
			logger.error("Problem writing initialized area");
		}

		new Thread(this, "Screen Recorder").start();
	}

	public void stopRecording() {
		triggerRecordingStop();

		int count = 0;
		while (running == true && count < 10) {
			try {
				Thread.sleep(100);
			} catch (Exception e) {
			}
			count++;
		}

		try {
			capFileManager.flush();
			capFileManager.shutDown();
		} catch (Exception e) {
			logger.error("Problem while quitting");
		}
	}

	public boolean isRecording() {
		return recording;
	}

	public int getFrameSize() {
		return frameSize;
	}

	private class StreamPacker implements Runnable {
		BlockingQueue<FrameDataPack> queue = new ArrayBlockingQueue<>(2);
		private FrameCompressor compressor;

		public StreamPacker(CapFileManager capFileManager, int frameSize) 
		{
			compressor = new FrameCompressor(capFileManager, frameSize);

			new Thread(this, "Stream Packer").start();
		}

		public void packToStream(FrameDataPack pack) {	
			try {
				queue.put(pack);
			} catch (InterruptedException e) {
				logger.error("Blocking queue was interrupted",e);
			}
		}

		@Override
		public void run() {
			while (recording) {
					try {
						FrameDataPack pack = queue.take();
						
						//Sometimes, recording will have stopped while the queue was blocking
						if (pack == null || !recording)
						{
							continue;
						}
						
						long t1 = System.currentTimeMillis();
						compressor.packFrame(pack);
						long t2 = System.currentTimeMillis();
						if (logger.isTraceEnabled()) logger.trace("  pack time:"+(t2-t1));

					} catch (IOException | InterruptedException e) {
						logger.error("Problem packing frame",e);

						capFileManager.shutDown();

						return;
					}
	
			}
			compressor.stop();
		}
	}
}