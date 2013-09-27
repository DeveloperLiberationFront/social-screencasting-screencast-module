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

package com.wet.wired.jsr.recorder.compression;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import org.apache.log4j.Logger;

import com.wet.wired.jsr.recorder.CapFileManager;

import edu.ncsu.lubick.ScreenRecordingModule;

public class FrameCompressor implements FrameCompressorCodecStrategy, FrameCompressorSavingStrategy 
{

	private static final int PERIOD_FOR_FULL_FRAMES = 20;
	
	private static final int MAX_BLOCK_LENGTH = 126;

	private static final byte STREAK_OF_SAME_AS_LAST_TIME_BLOCKS_CONSTANT = (byte) 0xFF; //this is -1.  See explanatory comments below

	private static Logger logger = Logger.getLogger(FrameCompressor.class.getName());
	
	private FramePacket frame;
	private CapFileManager capFileManager;
	private boolean currentFrameHasChanges;
	private Deflater deflator = new Deflater(Deflater.BEST_SPEED);
	private DeflaterOutputStream zO;
	private ByteArrayOutputStream bO;

	private FrameCompressorCodecStrategy codecStrategy;
	private FrameCompressorSavingStrategy saveToDiskStrategy;
	
	private int frameCounter = 0;
	
	byte[] dataToWriteBuffer = new byte[1];	//It's not a local variable because then we'd have to reallocate it everytime


	public FrameCompressor(CapFileManager capFileManager, int frameSize, FrameCompressorCodecStrategy fccs, FrameCompressorSavingStrategy fcss) 
	{
		frame = new FramePacket(frameSize);
		this.capFileManager = capFileManager;
		
		if (fccs == null)
		{
			this.codecStrategy = this;
		}
		if (fcss == null)
		{
			this.saveToDiskStrategy = this;
		}
	}

	//for timing
	//private long t1,t2,t3,t4;
	
	//private long sumI1 = 0,sumI2 = 0,sumI3 = 0, sumSum = 0;
	
	//private int timingCounter = 0;
	
	//private int reps = 100;
	
	public void packFrame(int[] newData, long frameTimeStamp, boolean reset) throws IOException 
	{
		//t1 = System.nanoTime();
		//For performance reasons, we reuse the frame
		frame.updateFieldsForNextFrame(newData, frameTimeStamp, reset);

		//Worst case scenario, we'll need 4 times as many bytes as ints that come in
		if (dataToWriteBuffer.length != (newData.length * 4))
		{
			dataToWriteBuffer = new byte[newData.length * 4];
		}
		
		//t2 = System.nanoTime();
		boolean isFullFrame = false;
		if (frameCounter % PERIOD_FOR_FULL_FRAMES == 0)
		{
			isFullFrame = true;
			frameCounter = 0;
		}
		frameCounter++;
		
		int numBytesToWrite = codecStrategy.compressDataUsingRunLengthEncoding(newData, frame, dataToWriteBuffer, isFullFrame);
		
		//t3 = System.nanoTime();
		
		
		capFileManager.startWritingFrame(isFullFrame);
		saveToDiskStrategy.writeData(dataToWriteBuffer, currentFrameHasChanges, numBytesToWrite, frame);
		capFileManager.endWritingFrame();
		//t4 = System.nanoTime();
		//if (logger.isTraceEnabled()) 
		//{
		//	sumI1 += (t2-t1);
		//	sumI2 += (t3-t2);
		//	sumI3 += (t4-t3);
		//	sumSum += (t4-t1);
		//	if (timingCounter % reps == 0 && timingCounter > 0)
		//	{
		//		logger.trace("===================Summary======================");
		//		logger.trace(String.format("AVERAGE: First Interval ,%1.3fms, Second Interval ,%1.3fms, Third Interval ,%1.3fms,"
		//				,((sumI1)/1000000.0)/reps,(sumI2)/(1000000.0)/reps,((sumI3)/1000000.0)/reps));
		//		logger.trace(String.format("By percents: First Interval %f%%, Second Interval %f%%, Third Interval %f%%",
		//				(sumI1)*100.0/sumSum, (sumI2)*100.0/sumSum, (sumI3)*100.0/sumSum));
		//		logger.trace("raw: ," + sumI1 +','+ sumI2 +','+ sumI3 +','+ sumSum +',');
		//	}
		//	else
		//	{
				//logger.trace(String.format("First Interval ,%1.3fms, Second Interval ,%1.3fms, Third Interval ,%1.3fms,"
				//		,(t2-t1)/1000000.0,(t3-t2)/1000000.0,(t4-t3)/1000000.0));
				//long total = t4-t1;
				//logger.trace(String.format("By percents: First Interval %f%%, Second Interval %f%%, Third Interval %f%%",
				//		(t2-t1)*100.0/total, (t3-t2)*100.0/total, (t4-t3)*100.0/total));
		//	}
			
		//	timingCounter++;
		//}
	}


	/**
	 * Takes the the inputData and fills packedBytes with a compressed version
	 * of the RGB values represented by the ints in inputData.
	 * 
	 * If forceFullFrame is true, this should use an entire frame instead of a diff from the previous frame
	 * 
	 * Returns 
	 * @param inputData
	 * @param aFrame
	 * @param packedBytes
	 * @param forceFullFrame
	 * @return How many bytes were put into packedBytes
	 */
	@Override
	public int compressDataUsingRunLengthEncoding(int[] inputData, FramePacket aFrame, byte[] packedBytes, boolean forceFullFrame) 
	{
		if (logger.isTraceEnabled()) logger.trace("Extracting data from inputData of size "+inputData.length);
		//if (logger.isTraceEnabled()) logger.trace('\n'+Arrays.toString(inputData)+'\n');
		
		
		//How many total blocks.  This is mainly used for debugging and tracing efficiency
		int blocks = 0;

		boolean currentlyInCompressedBlock = true;
		int sizeOfCurrentBlock = 0;
		
		byte thisBlocksRedValue = 0;
		byte thisBlocksGreenValue = 0;
		byte thisBlocksBlueValue = 0;

		//I'm not sure what this is used for
		int currentStreakOfSameAsLastTimeBlocks = 0;

		//The three cursors.  
		//		Input Cursor keeps track of the data read in.  Each time through the loop will increment this
		//		Output Cursor keeps track of the farthest we have written to packedBytes
		//		Uncompressed Cursor keeps track of the location of the byte used to signal how many bytes of uncompressed
		//						data will folow.
		int inputCursor = 0;
		int outputCursor = 0;
		int uncompressedCursor = -1;  // Initialize with a Sentinel value of -1

		byte red;
		byte green;
		byte blue;

		currentFrameHasChanges = false;
		
		//Keeps track if this will be the last byte from newData
		boolean lastEntry = false; 

		while (inputCursor < inputData.length) 
		{
			//If we are not on our last input
			if (inputCursor == inputData.length - 1) {
				lastEntry = true;
			}

			//Compare this byte to the previous one.  If it is the same, that is nothing changed,
			//make it black.  This is the value that will be interpreted as "same as last time"
			if (inputData[inputCursor] == aFrame.previousData[inputCursor] && !forceFullFrame) {
				red = 0;
				green = 0;
				blue = 0;
			} 
			else //extract the red, green and blue.  If the pixel happens to be actually black
				//Then, we add a tiny value to the blue, just to keep our signal (the signal that nothing changed)
				//in tune
			{
				
				if ((inputData[inputCursor] & 0x00FFFFFF) != 0)
				{				
					red = (byte) ((inputData[inputCursor] & 0x00FF0000) >>> 16);
					green = (byte) ((inputData[inputCursor] & 0x0000FF00) >>> 8);
					blue = (byte) ((inputData[inputCursor] & 0x000000FF));
				}
				else //this is black, rgb = 000, so we flub it to 001 to avoid thinking that it is the same as last time
				{
					red = 0;
					green = 0;
					blue = 1;
				}
			}
			
			//I know, really I shouldn't comment out logger statements, but I'd rather not take the (microscopic) 
			//performance hit in a loop that executes millions of times per second.
			//if (logger.isTraceEnabled()) logger.trace(String.format("R:%d G:%d B:%d", red,green,blue));

			//The following comment can be sung to the tune of "Peanut Butter Jelly time"
			//It's SINGLE PARSE ENCODING TIME, SINGLE PARSE ENCODING TIME, way yo, way yo, now der you go, der you go
			//SINGLE PARSE ENCODING,SINGLE PARSE ENCODING,SINGLE PARSE ENCODING WITH FIXED BLOCK SIZE.
			
			//Seriously, the following algorithm is just a single-parse, run-length encoding algorithm
			//http://en.wikipedia.org/wiki/Run-length_encoding
			//The main difference here is that instead of compressing the following 
			//W,W,W,W,W,W,W,W,W,W,W,W,B,e,f,W,W,W,W,W,W,W,W,W,W,W,W,B,B,B (length 28)
			//to
			//12,W,1,B,1,e,1,f,12,W,3,B (length 12)
			//this algorithm would encode it to
			//12,W,-125,B,e,f,12,W,3,B (length 10)
			//grouping the "noncompressed" Bef into their own special block.  The idea is that this will happen a lot, so
			//we don't want to waste the space.
			//Why -125?  Well, for noncompressed blocks, the formula is just (blockSize + 0x80) and since we are dealing with bytes
			//3 + 0x80 = 3 + -128 = -125
			//For a compressed block size, the byte will be positive, for an uncompressed block size, that byte will be negative
			//
			//Of course, instead of using letters of the alphabet, this encodes tuples of red,green,blue.  The size of the blocks
			//count the number of tuples, not 3*tuples.
			//The only catch is that these blocks can only be 126 units long (you'll see why its not 127 in a minute), so there is some logic in there to partition that off
			//E.g.
			//We are compressing 50 straight pixels with the colors 101,102,103.  
			//This gets encoded as 50, 101, 102, 103
			//If a block exceeds 126, we have to make a new block, so assume that we are compressing 500 straight pixels with the colors 101,102,103.  
			//500/126 = 3, 500%126 = 122
			//This gets encoded as 126, 101,102,103, 126, 101,102,103, 126, 101,102,103, 122, 101,102,103, ...
			//
			//Furthermore, as extra super compression, "same as last time" values are compressed a bit differently than normal colors
			//if they stretch for more than one block.
			//E.g.
			//Streaks of <=126 "same as last time pixels" are treated the same as before.  Suppose we have 50 of them
			//This gets encoded as 50, 0, 0, 0
			//We are compressing 500 straight "same as last time" pixels.  
			//500/126 = 3, 500%126 = 122
			//This gets encoded as -1, 3, 122, 0, 0, 0
			//The -1 (0xFF) is the symbol for one full set of blocks of "same as last times" and the number that follows it is how many said sets
			//Finally, the 122 signifies a compressed block of 122 length follows, with colors 0,0,0
			//Note, if we had to compress 255*126+1 or more "same as last time pixels, say 33000 of them, we need to wrap because a byte can't hold more than 255
			//33000 / 126 = 261, 33000 % 126 = 114
			//This gets encoded as -1, 255, -1, 6, 126, 0, 0, 0
			//but, since bytes are signed, this actually looks like -1, -1, -1, 6, 126, 0, 0, 0
			//
			//Now, why can't a block be 127?  Imagine that we had a block size of uncompressed data of length 127
			//We would be encoding that as a negative offset, 127 + 0x80 = 255 == -1 and the decompressor would
			//most likely interpret -1 as a full set of blocks of "same as last time" pixels
			
			//Does this color match the color we are tracking in this compression block?
			if (thisBlocksRedValue == red && thisBlocksGreenValue == green && thisBlocksBlueValue == blue) {
				//Are we in a block presently, or do we have to start a new one?
				if (currentlyInCompressedBlock == false) 
				{
					if (uncompressedCursor > -1) {
						blocks++;
						currentFrameHasChanges = true;
						//Go back in the stream and tell the decompressor that the next
						//blockSize bytes are uncompressed (this is done by making the 
						//number negative (the +0x80 would turn 0 into -128, 5 into -123, etc
						packedBytes[uncompressedCursor] = (byte) (sizeOfCurrentBlock + 0x80);
					}
					//Since we had two colors in a row, we are in a new (compressed block)
					currentlyInCompressedBlock = true;
					sizeOfCurrentBlock = 0;
					currentStreakOfSameAsLastTimeBlocks = 0;
				} 
				else if (sizeOfCurrentBlock == MAX_BLOCK_LENGTH || lastEntry == true) 
				{
					//If this block has been all "same as last time" values
					if (thisBlocksRedValue == 0 && thisBlocksGreenValue == 0 && thisBlocksBlueValue == 0) 
					{
						//Are we starting a new streak of SameAsALastTimeBlocks.  That is, have we seen
						//MAX_BLOCK_LENGTH of them already and we see another one now?
						if (currentStreakOfSameAsLastTimeBlocks == 0) 
						{
							blocks++;
							currentStreakOfSameAsLastTimeBlocks++;
							packedBytes[outputCursor] = STREAK_OF_SAME_AS_LAST_TIME_BLOCKS_CONSTANT;
							outputCursor++;
							packedBytes[outputCursor] = (byte) currentStreakOfSameAsLastTimeBlocks;
							outputCursor++;
						} 
						else 
						{
							//Update the streak to be one more than previously
							currentStreakOfSameAsLastTimeBlocks++;
							packedBytes[outputCursor - 1] = (byte) currentStreakOfSameAsLastTimeBlocks;
						}
						//This is equivalent to shutting down the old streak of SameAsLastTimeBlocks.
						//If the streak of "black" continues, a new block will be made
						if (currentStreakOfSameAsLastTimeBlocks == 255) {
							currentStreakOfSameAsLastTimeBlocks = 0;
						}
					} 
					else //It was just a normal color
					{
						//Write out the block of size MAX_BLOCK_LENGTH and the current color
						blocks++;
						currentFrameHasChanges = true;
						//This feelslike this should be sizeOfCurrentBlock +1 (to make it 127), but because a new block is 
						//made and the block size is 1 at the end of the loop, the pixel we are looking at now is rolled
						//into that new block and not the old one that we filled
						packedBytes[outputCursor] = (byte) sizeOfCurrentBlock;	
						outputCursor++;
						packedBytes[outputCursor] = thisBlocksRedValue;
						outputCursor++;
						packedBytes[outputCursor] = thisBlocksGreenValue;
						outputCursor++;
						packedBytes[outputCursor] = thisBlocksBlueValue;
						outputCursor++;

						currentStreakOfSameAsLastTimeBlocks = 0;
					}
					currentlyInCompressedBlock = true;
					sizeOfCurrentBlock = 0;
				}
				//Else we proceed as normal
				//We just increment our block size
			} 
			else //the previous color was different than this one
			{
				if (currentlyInCompressedBlock == true)	//we were in a block, so we'll have to close that out
				{
					if (sizeOfCurrentBlock > 0) //I'm pretty sure this only applies to the first pixel
					{
						blocks++;
						currentFrameHasChanges = true;
						//Write out the compressed block.  This time, the block size is positive to indicate
						//that all of the following blockSize blocks are of the next color
						packedBytes[outputCursor] = (byte) sizeOfCurrentBlock;
						outputCursor++;
						packedBytes[outputCursor] = thisBlocksRedValue;
						outputCursor++;
						packedBytes[outputCursor] = thisBlocksGreenValue;
						outputCursor++;
						packedBytes[outputCursor] = thisBlocksBlueValue;
						outputCursor++;
					}

					uncompressedCursor = -1;
					currentlyInCompressedBlock = false;
					sizeOfCurrentBlock = 0;

					currentStreakOfSameAsLastTimeBlocks = 0;
				} 
				else if (sizeOfCurrentBlock == MAX_BLOCK_LENGTH || lastEntry == true) //This is a very rare case.  Ending a block exactly at a multiple of MAX_BLOCK_LENGTH
				{
					if (uncompressedCursor > -1) 
					{
						blocks++;
						currentFrameHasChanges = true;
						packedBytes[uncompressedCursor] = (byte) (sizeOfCurrentBlock + 0x80);
					}

					uncompressedCursor = -1;
					currentlyInCompressedBlock = false;
					sizeOfCurrentBlock = 0;

					currentStreakOfSameAsLastTimeBlocks = 0;
				}

				if (uncompressedCursor == -1) {
					uncompressedCursor = outputCursor;
					//This line leaves a blank spot that we'll come
					//back to later to fill out how many spots are of a different
					//color.  That's why we are saving it to uncompressedCursor
					outputCursor++;
				}
				//write out the uncompressed bytes
				packedBytes[outputCursor] = red;
				outputCursor++;
				packedBytes[outputCursor] = green;
				outputCursor++;
				packedBytes[outputCursor] = blue;
				outputCursor++;

				thisBlocksRedValue = red;
				thisBlocksGreenValue = green;
				thisBlocksBlueValue = blue;
			}
			
			//FINALLY
			//This code gets run every time
			
			inputCursor++;
			sizeOfCurrentBlock++;
			//if (logger.isTraceEnabled()) logger.trace(String.format("Input Cursor: %d, blockSize: %d, OutputCursor: %d, UncompressedCursor: %d, Blocks: %d", inputCursor,sizeOfCurrentBlock,outputCursor,uncompressedCursor,blocks));
		}
		
		
		if (logger.isTraceEnabled()) logger.trace("Finished conversion with "+blocks+" blocks making up "+outputCursor +" bytes");
		//if (logger.isTraceEnabled()) logger.trace('\n'+Arrays.toString(packedBytes)+'\n');
		return outputCursor;
	}

	@Override
	public void writeData(byte[] dataToWrite, boolean hasChanges, int numBytesToWrite, FramePacket aFrame) throws IOException 
	{
			//Write out when this frame happened
			capFileManager.write(((int) aFrame.frameTime & 0xFF000000) >>> 24);
			capFileManager.write(((int) aFrame.frameTime & 0x00FF0000) >>> 16);
			capFileManager.write(((int) aFrame.frameTime & 0x0000FF00) >>> 8);
			capFileManager.write(((int) aFrame.frameTime & 0x000000FF));

			//If the frame had new stuff
			if (hasChanges == false) {
				capFileManager.write(0);
				capFileManager.flush();
				//I'm not sure why this needs to get updated
				aFrame.newData = aFrame.previousData;
				capFileManager.endWritingFrame();
				return;
			} else {
				capFileManager.write(1);
				capFileManager.flush();
			}

			if (ScreenRecordingModule.useCompression)
			{

				if (bO == null)
				{
					bO = new ByteArrayOutputStream();
				}

				if (zO == null)
				{
					//zO = new GZIPOutputStream(bO);
					zO = new DeflaterOutputStream(bO, deflator, numBytesToWrite);
				}

				//Makes way for the next compressed bit (makes a new header...)
				deflator.reset();
				byte[] bA;

				zO.write(dataToWrite, 0, numBytesToWrite);
				zO.finish();

				bA = bO.toByteArray();

				if (logger.isTraceEnabled()) logger.trace(String.format("Compressed %d bytes to %d bytes",numBytesToWrite, bA.length));


				capFileManager.write((bA.length & 0xFF000000) >>> 24);
				capFileManager.write((bA.length & 0x00FF0000) >>> 16);
				capFileManager.write((bA.length & 0x0000FF00) >>> 8);
				capFileManager.write((bA.length & 0x000000FF));

				capFileManager.write(bA);
				capFileManager.flush();
				bO.reset();
			}
			else 
			{
				capFileManager.write((numBytesToWrite & 0xFF000000) >>> 24);
				capFileManager.write((numBytesToWrite & 0x00FF0000) >>> 16);
				capFileManager.write((numBytesToWrite & 0x0000FF00) >>> 8);
				capFileManager.write((numBytesToWrite & 0x000000FF));

				capFileManager.write(dataToWrite, 0, numBytesToWrite);
				capFileManager.flush();
			}
			
	}

	public void stop() {
		//Do nothing
	}
}
