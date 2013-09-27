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

import java.io.File;
import java.io.IOException;

import javax.media.ConfigureCompleteEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.DataSink;
import javax.media.EndOfMediaEvent;
import javax.media.Format;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.NoDataSinkException;
import javax.media.NoProcessorException;
import javax.media.PrefetchCompleteEvent;
import javax.media.Processor;
import javax.media.RealizeCompleteEvent;
import javax.media.ResourceUnavailableEvent;
import javax.media.control.TrackControl;
import javax.media.datasink.DataSinkEvent;
import javax.media.datasink.DataSinkListener;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.protocol.FileTypeDescriptor;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import edu.ncsu.lubick.ScreenRecordingModule;

public class RecordingConverter implements ControllerListener, DataSinkListener {

	private static Logger logger = Logger.getLogger(RecordingConverter.class.getName());
	private static boolean finished = false;
	private Object waitSync = new Object();
	private boolean stateTransitionOK = true;
	
	static {
		PropertyConfigurator.configure(ScreenRecordingModule.LOGGING_FILE_PATH);
		logger = Logger.getLogger(ScreenRecordingModule.class.getName());
	}

	public static void main(String[] args) 
	{
		//args = new String[]{"scratch/temp0002.cap"};
		
		if ((args.length != 1) || !args[0].endsWith("cap")) {
			//System.out.println("Usage: java -jar screen_cap_to_video.jar <screen_cap_file.cap>");
			logger.info("Usage: java -jar screen_cap_to_video.jar <screen_cap_file.cap>");
			return;
		}

		String movieFile = new String(args[0]);
		movieFile = movieFile.replace("cap", "mov");

		RecordingConverter recordingConverter;
		try {
			recordingConverter = new RecordingConverter();
			recordingConverter.process(args[0], movieFile);
		} catch (Exception e) {
			logger.fatal("There was a problem processing that file",e);
		}
		System.gc();
	}

	public void process(String recordingFile, String movieFile) throws IOException, NoProcessorException, NoDataSinkException, InterruptedException {

		MediaLocator mediaLocator = new MediaLocator(new File(movieFile).toURI().toURL());
		
		PlayerDataSource playerDataSource = new PlayerDataSource(recordingFile);
		Processor processor = Manager.createProcessor(playerDataSource);
		processor.addControllerListener(this);
		processor.configure();

		if (!waitForState(processor, Processor.Configured)) {
			logger.fatal("Failed to configure the processor.");
			return;
		}

		processor.setContentDescriptor(new ContentDescriptor(
				FileTypeDescriptor.QUICKTIME));

		TrackControl trackControl[] = processor.getTrackControls();
		Format format[] = trackControl[0].getSupportedFormats();
		trackControl[0].setFormat(format[0]);
		processor.realize();
		if (!waitForState(processor, Processor.Realized)) {
			logger.fatal("Failed to realize the processor.");
			return;
		}

		DataSource dataSource = processor.getDataOutput();
		DataSink dataSink = Manager.createDataSink(dataSource, mediaLocator);
		dataSink.open();
		processor.start();
		dataSink.start();
		waitForFileDone();
		dataSource.disconnect();
		processor.stop();
		dataSink.stop();
		processor.removeControllerListener(this);
		processor.deallocate();
		dataSink.close();
		
		
		Thread.sleep(2000);
	}

	void waitForFileDone() {

		while (!finished) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				logger.error("Minor problem with thread",e);
			}
		}

		return;
	}

	public void dataSinkUpdate(DataSinkEvent evt) {

	}

	boolean waitForState(Processor p, int state) {
		synchronized (waitSync) {
			try {
				while (p.getState() < state && stateTransitionOK)
					waitSync.wait();
			} catch (Exception e) {
			}
		}
		return stateTransitionOK;
	}

	public void controllerUpdate(ControllerEvent evt) {

		if (evt instanceof ConfigureCompleteEvent
				|| evt instanceof RealizeCompleteEvent
				|| evt instanceof PrefetchCompleteEvent) {
			synchronized (waitSync) {
				stateTransitionOK = true;
				waitSync.notifyAll();
			}
		} else if (evt instanceof ResourceUnavailableEvent) {
			synchronized (waitSync) {
				stateTransitionOK = false;
				waitSync.notifyAll();
			}
		} else if (evt instanceof EndOfMediaEvent) {
			evt.getSourceController().stop();
			evt.getSourceController().close();
			markFinished();
		}
	}

	private static void markFinished() {
		finished = true;		
	}
}