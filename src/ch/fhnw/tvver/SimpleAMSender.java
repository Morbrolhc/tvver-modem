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

import ch.fhnw.util.FloatList;

/**
 * Simple sender using amplitude modulation.
 * 
 * @author sschubiger
 *
 */
public class SimpleAMSender extends AbstractSender {
	private static final double PI2  = Math.PI * 2;
	/* Carrier frequency. */
	static         final float  FREQ = 3000;

	/**
	 * Create a wave with given amplitude. 
	 * @param amp Amplitude for this symbol.
	 * @return Audio data for symbol.
	 */
	private float[] symbol(float amp) {
		final int symbolSz = (int) (samplingFrequency / FREQ);
		final float[] result = new float[symbolSz];

		for(int i = 0; i < result.length; i++)
			result[i] = (float)(Math.sin((PI2 * i) / symbolSz)) * amp;

		return result;
	}

	/**
	 * Create amplitude modulated wave for a given data byte.
	 * @param data Data byte to encode.
	 */
	@Override
	public float[] synthesize(byte data) {
		FloatList result = new FloatList();

		/* Send start bit. */
		result.addAll(symbol(1f));
		/* Send data bits. */
		for(int i = 0; i < 8; i++)
			result.addAll(symbol((data & (1 << i)) == 0 ? 0 : 0.8f));

		return result.toArray();
	}
}
