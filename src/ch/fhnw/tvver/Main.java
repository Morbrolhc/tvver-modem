/*
 * Copyright (c) 2013 - 2015 Stefan Muller Arisona, Simon Schubiger, Samuel von Stachelski
 * Copyright (c) 2013 - 2015 FHNW & ETH Zurich
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *  Neither the name of FHNW / ETH Zurich nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ch.fhnw.tvver;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;

import ch.fhnw.ether.audio.ArrayAudioSource;
import ch.fhnw.ether.audio.FileAudioTarget;
import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.audio.JavaSoundTarget;
import ch.fhnw.ether.audio.URLAudioSource;
import ch.fhnw.ether.audio.fx.WhiteNoise;
import ch.fhnw.ether.audio.fx.AudioGain;
import ch.fhnw.ether.audio.fx.BandPass;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.IRenderTarget;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.ether.media.RenderProgram;
import ch.fhnw.ether.ui.ParameterWindow;
import ch.fhnw.util.ClassUtilities;
import ch.fhnw.util.FloatList;

/**
 * Test class for tvver soft modem project.
 * 
 * @author sschubiger
 *
 */
public class Main {
	/* Test data types.*/
	enum Type{RANDOM, COUNT, TEXT, CONST_00, CONST_01, CONST_55, CONST_AA, CONST_FF}
	final static Type dataType = Type.RANDOM;
	/* Frequency band */
	final static float LOW_FREQ  = 100;
	final static float HIGH_FREQ = 4000;
	/* Noise level */
	final static float NOISE     = 0.15f;
	/* Attenuation steps */
	final static float GAINS[]   = {0.5f};

	/**
	 * Invoke test program.
	 * 
	 * @param args First argument is name of test class without package name and without 
	 * Receiver/Sender postfix. E.g. for ch.fhnw.tvver.DTMFReceiver pass "DTMF" as first argument.
	 * @throws Throwable Upon error.
	 */
	public static void main(String[] args) throws Throwable {
		/* Get class prefix. */
		String prefix = Main.class.getPackage().getName() + "." + args[0];

		/* Construct filenames for intermediate audio data. */
		File   sendFile = new File(args[0] + "_send.wav");
		File   recvFile = new File(args[0] + "_recv.wav");

		/* Create test data. */
		byte[] sendData = createData();
		/* Send data (write it to sendFile). */
		double duration = send(sendData, (AbstractSender)Class.forName(prefix + "Sender").newInstance(), sendFile);
		/* Receive data (read it from sendFile). */
		byte[][] recvData = recv(sendData, duration, (AbstractReceiver)Class.forName(prefix + "Receiver").newInstance(), sendFile, recvFile);
		/* Compute grade. */
		grade(sendData, duration, recvData);
		
		System.exit(0);
	}

	/**
	 * Send data - currently the data is written to a wav file which is the consumed by the receiver.
	 * 
	 * @param data The data to send.
	 * @param sender The sender to use
	 * @param file The wav file for the audio data.
	 * @return Duration of data transmission (without silence at begin and end).
	 * @throws RenderCommandException
	 */
	private static double send(byte[] data, AbstractSender sender, File file) throws RenderCommandException {
		FileAudioTarget target = new FileAudioTarget(file, 1, 48000);

		/* Collect audio data for transmission. */
		FloatList audioData = new FloatList();
		audioData.addAll(silence());
		sender.init(target.getSampleRate());
		double duration = audioData.size();
		audioData.addAll(sender.synthesize(data));
		duration = (audioData.size() - duration) / target.getSampleRate();
		audioData.addAll(silence());

		/* Create an audio source with the audio data. */
		ArrayAudioSource source = new ArrayAudioSource(audioData, target.getNumChannels(), target.getSampleRate(), 1);

		/* Play the audio data */
		target.useProgram(new RenderProgram<>(source));
		target.start();
		target.sleepUntil(IRenderTarget.NOT_RENDERING);
		target.stop();
		return duration;
	}

	/**
	 * Receive data - currently reads audio from a wav file written by the sender.
	 * 
	 * @param sendData The original sent data.
	 * @param duration The duration of the sent data.
	 * @param recv Receiver to use for decoding.
	 * @param sendFile The input file.
	 * @param recvFile The file with channel effects applied (noise, bandpass, etc.). 
	 * @return The received data.
	 */
	private static byte[][] recv(byte[] sendData, double duration, AbstractReceiver recv, File sendFile, File recvFile) throws MalformedURLException, IOException, RenderCommandException {
		/* Source audio data. */
		URLAudioSource source = new URLAudioSource(sendFile.toURI().toURL(), 1);
		/* Some noise on the channel. */
		WhiteNoise     noise  = new WhiteNoise(); noise.setVal("gain", NOISE);
		/* Band-limit the channel. */
		BandPass       band   = new BandPass(1); band.setVal("low", LOW_FREQ); band.setVal("high", HIGH_FREQ);
		/* Attenuation */
		AudioGain      gain   = new AudioGain();

		byte[][] result = new byte[GAINS.length][];

		/* Create a program with source and channel effects. */
		RenderProgram<IAudioRenderTarget> program = new RenderProgram<>(
				source,
				noise, 
				band,
				gain);

		/* Initialize receiver. */
		recv.init(source.getSampleRate());

		/* Add optional effects to audio pipeline. */
		for(AbstractRenderCommand<IAudioRenderTarget,?> cmd : recv.getCmds())
			program.addLast(cmd);
		program.addLast(recv);

		ParameterWindow w = new ParameterWindow(program);

		//IAudioRenderTarget target = new FileAudioTarget(recvFile, source.getNumChannels(), source.getSampleRate());
		IAudioRenderTarget target = new JavaSoundTarget();
		target.useProgram(program);

		while(w.isVisible()) {
			for(int i = 0; i < GAINS.length; i++) {
				/* Set gain. */
				gain.setVal("gain", GAINS[i]);
				/* Play and decode the audio data. */
				target.start();
				target.sleepUntil(IRenderTarget.NOT_RENDERING);
				source.rewind(target);

				result[i] = recv.getAndClearData();
			}
			grade(sendData, duration, result);
		}
		target.stop();

		return result;
	}

	/**
	 * Compute grade based on error rate and bitrate.
	 * 
	 * @param sendData The sent data for error checking.
	 * @param duration The time needed for sending the data.
	 * @param recvData the received data.
	 */
	private static void grade(byte[] sendData, double duration, byte[][] recvData) {
		if(recvData == null || recvData.length == 0)
			System.out.println("Grade: 1.0 (no data received)");
		else {
			/*
			for(byte[] rData : recvData)
				dump(sendData, rData);
			 */

			int errors = 0;
			for(int j = 0; j < recvData.length; j++) {
				errors += Math.max(0, sendData.length - recvData[j].length);
				for(int i = 0; i < Math.min(recvData[j].length, sendData.length); i++) 
					if(recvData[j][i] != sendData[i]) errors++;
			}

			double bitrate   = (sendData.length * 8) / duration;
			double errorrate = errors / (double)(sendData.length * recvData.length); 

			System.out.println("\n-----------------------------------------");
			System.out.println("Errorrate: " + errorrate);
			System.out.println("Bits/sec:  " + bitrate);
			System.out.println("Grade:     " + (Math.min(((1 - errorrate) * (bitrate / 1200)), 5) + 1));
		}
	}

	/**
	 * Dump some debug information.
	 * 
	 * @param sendData The sent data.
	 * @param recvData The received data.
	 */
	@SuppressWarnings("unused")
	private static void dump(byte[] sendData, byte[] recvData) {
		System.out.println("\n-----------------------------------------");	
		switch(dataType) {
		default:
			for(int i = 0; i < Math.min(recvData.length, sendData.length); i++)
				if(recvData[i] != sendData[i])
					System.out.println(i + ":\t" + (recvData[i] & 0xFF) + "\t" + (sendData[i] & 0xFF));			
			break;
		case TEXT:
			System.out.println(new String(recvData));
			break;
		}
	}

	/**
	 * Create test data.
	 * 
	 * @return Test data.
	 */
	private static byte[] createData() {
		switch(dataType) {
		case CONST_00: {
			byte[] result = new byte[64];
			return result;
		}
		case CONST_01: {
			byte[] result = new byte[64];
			Arrays.fill(result, (byte)1);
			return result;
		}
		case CONST_55: {
			byte[] result = new byte[64];
			Arrays.fill(result, (byte)0x55);
			return result;
		}
		case CONST_AA: {
			byte[] result = new byte[64];
			Arrays.fill(result, (byte)0xAA);
			return result;
		}
		case CONST_FF: {
			byte[] result = new byte[64];
			Arrays.fill(result, (byte)0xFF);
			return result;
		}
		case RANDOM: {
			byte[] result = new byte[64];
			for(int i = 0; i < result.length; i++)
				result[i] = (byte)(Math.random() * 255);
			return result;
		}
		case COUNT: {
			byte[] result = new byte[256];
			for(int i = 0; i < result.length; i++)
				result[i] = (byte) i;
			return result;
		}
		case TEXT:
			return "THE QUICK BROWN FOX JUMPS OVER THE LAZY DOG - the quick brown fox jumps over the lazy dog\n".getBytes();
		default:
			return ClassUtilities.EMPTY_byteA;
		}
	}

	/**
	 * Create random amount of silence.
	 * 
	 * @return Silence.
	 */
	private static float[] silence() {
		return new float[(int)((Math.random() * 4096)  + 4096)];
	}
}
