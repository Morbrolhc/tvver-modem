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

import ch.fhnw.util.ClassUtilities;
import ch.fhnw.util.FloatList;

/**
 * Base class for tvver soft modem senders.
 * 
 * @author sschubiger
 *
 */
public abstract class AbstractSender {
	/** The sampling frequency */
	protected float samplingFrequency;

	/**
	 * Synthesize one byte of data as audio samples. Override this method
	 * for transmitting a single byte of data.
	 * 
	 * @param b The data byte to transmit. 
	 * @return The audio representation (as PCM samples) of the data byte. 
	 */
	public float[] synthesize(byte b) {
		return ClassUtilities.EMPTY_floatA;
	}

	/**
	 * Synthesize data as audio samples. Override this method
	 * for transmitting chunks of data.
	 * 
	 * @param data The data to transmit. 
	 * @return The audio representation (as PCM samples) of the data. 
	 */
	public float[] synthesize(byte[] data) {
		FloatList result = new FloatList();
		for(int i = 0; i < data.length; i++)
			result.addAll(synthesize(data[i]));
		return result.toArray();
	}

	/** 
	 * Initialize this sender. Called after constructor.
	 * @param samplingFrequency The samplingFrequency.
	 * @return Optional initialization / calibration sequence. Use this to transmit e.g. 
	 * special synchronization, phase-shift or amplitude values.
	 */
	public float[] init(float samplingFrequency) {
		this.samplingFrequency = samplingFrequency;
		return ClassUtilities.EMPTY_floatA;
	}
}
