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

package com.wet.wired.jsr.converter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.InflaterInputStream;

import org.apache.log4j.Logger;

import edu.ncsu.lubick.ScreenRecordingModule;

public class FrameDecompressor {

	private static final int ALPHA = 0xFF000000;
	private static Logger logger = Logger.getLogger(FrameDecompressor.class.getName());

	public class FramePacket {

		private InputStream iStream;
		private int[] previousData;
		private int result;
		private long frameTimeStamp;
		private byte[] packed;
		private int frameSize;
		private int[] newData;

		private FramePacket(InputStream iStream, int expectedSize) {
			this.frameSize = expectedSize;
			this.iStream = iStream;
			previousData = new int[frameSize];
		}

		private void nextFrame() {
			if (newData != null) {
				previousData = newData;
			}
		}

		public int[] getData() {
			return newData;
		}

		public int getResult() {
			return result;
		}

		public long getTimeStamp() {
			return frameTimeStamp;
		}
	}

	public FramePacket frame;

	public FrameDecompressor(InputStream iStream, int frameSize) {
		frame = new FramePacket(iStream, frameSize);
	}

	public FramePacket unpack() throws IOException {
		frame.nextFrame();

		int i = frame.iStream.read();
		int time = i;
		time = time << 8;
		i = frame.iStream.read();
		time += i;
		time = time << 8;
		i = frame.iStream.read();
		time += i;
		time = time << 8;
		i = frame.iStream.read();
		time += i;

		frame.frameTimeStamp = (long) time;
		logger.trace("ft:"+time);

		byte type = (byte) frame.iStream.read();
		logger.trace("Packed Code:"+type);

		if (type <= 0) {
			frame.result = type;
			return frame;
		}

		
		try (ByteArrayOutputStream bO = new ByteArrayOutputStream();) {
			i = frame.iStream.read();
			int zSize = i;
			zSize = zSize << 8;
			i = frame.iStream.read();
			zSize += i;
			zSize = zSize << 8;
			i = frame.iStream.read();
			zSize += i;
			zSize = zSize << 8;
			i = frame.iStream.read();
			zSize += i;

			logger.trace("Zipped Frame size:"+zSize);

			byte[] zData = new byte[zSize];
			int readCursor = 0;
			int sizeRead = 0;

			while (sizeRead > -1) {
				readCursor += sizeRead;
				if (readCursor >= zSize) {
					break;
				}

				sizeRead = frame.iStream.read(zData, readCursor, zSize - readCursor);
			}

			if (ScreenRecordingModule.useCompression)
			{
				ByteArrayInputStream bI = new ByteArrayInputStream(zData);
				
				//GZIPInputStream zI = new GZIPInputStream(bI);
				InflaterInputStream zI = new InflaterInputStream(bI);
				
				byte[] buffer = new byte[1000];
				sizeRead = zI.read(buffer);

				while (sizeRead > -1) {
					bO.write(buffer, 0, sizeRead);
					bO.flush();

					sizeRead = zI.read(buffer);
				}
				bO.flush();
			}
			else 
			{
				ByteArrayInputStream bI = new ByteArrayInputStream(zData);

				byte[] buffer = new byte[1000];
				sizeRead = bI.read(buffer);

				while (sizeRead > -1) {
					bO.write(buffer, 0, sizeRead);
					bO.flush();

					sizeRead = bI.read(buffer);
				}
				bO.flush();
			}
			frame.packed = bO.toByteArray();
		} 
		catch (IOException e) 
		{
			logger.error("Problem unpacking ",e);
			frame.result = 0;
			return frame;
		}


		

		runLengthDecode();

		return frame;
	}

	private void runLengthDecode() {
		frame.newData = new int[frame.frameSize];

		int inCursor = 0;
		int outCursor = 0;

		int blockSize = 0;

		int rgb = 0xFF000000;

		while (inCursor < frame.packed.length - 3 && outCursor < frame.frameSize) {
			if (frame.packed[inCursor] == -1) {
				inCursor++;

				int count = (frame.packed[inCursor] & 0xFF);
				inCursor++;

				int size = count * 126;
				if (size > frame.newData.length) {
					size = frame.newData.length;
				}

				for (int loop = 0; loop < (126 * count); loop++) {
					frame.newData[outCursor] = frame.previousData[outCursor];
					outCursor++;
					if (outCursor >= frame.newData.length) {
						break;
					}
				}

			} 
			else if (frame.packed[inCursor] < 0) // uncomp
			{
				blockSize = frame.packed[inCursor] & 0x7F;
				inCursor++;

				for (int loop = 0; loop < blockSize; loop++) {
					rgb = ((frame.packed[inCursor] & 0xFF) << 16)
							| ((frame.packed[inCursor + 1] & 0xFF) << 8)
							| (frame.packed[inCursor + 2] & 0xFF) | ALPHA;
					if (rgb == ALPHA) {
						rgb = frame.previousData[outCursor];
					}
					inCursor += 3;
					frame.newData[outCursor] = rgb;
					outCursor++;
					if (outCursor >= frame.newData.length) {
						break;
					}
				}
			} 
			else {
				blockSize = frame.packed[inCursor];
				inCursor++;
				rgb = ((frame.packed[inCursor] & 0xFF) << 16)
						| ((frame.packed[inCursor + 1] & 0xFF) << 8)
						| (frame.packed[inCursor + 2] & 0xFF) | ALPHA;

				boolean transparent = false;
				if (rgb == ALPHA) {
					transparent = true;
				}
				inCursor += 3;
				for (int loop = 0; loop < blockSize; loop++) {
					if (transparent) {
						frame.newData[outCursor] = frame.previousData[outCursor];
					} else {
						frame.newData[outCursor] = rgb;
					}
					outCursor++;
					if (outCursor >= frame.newData.length) {
						break;
					}
				}
			}
		}
		frame.result = outCursor;
	}
}
