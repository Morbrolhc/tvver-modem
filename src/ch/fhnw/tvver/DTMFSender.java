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

import ch.fhnw.util.ArrayUtilities;

public class DTMFSender extends AbstractSender {
	/** DTMF signal duration in seconds (including pause between two signals). */
	private static final double DURATION = 0.03;
	private static final double PI2      = Math.PI * 2;

	/** DTMF frequency table. Low frequency indicates row, high frequency for column. */
	private static final float[] DTMF = {
			697,1209, // 1
			697,1336, // 2
			697,1477, // 3
			697,1633, // A

			770,1209, // 4
			770,1336, // 5
			770,1477, // 6
			770,1633, // B

			852,1209, // 7
			852,1336, // 8
			852,1477, // 9
			852,1633, // C

			941,1209, // *
			941,1336, // 0
			941,1477, // #
			941,1633, // D
	};

	/**
	 * Synthesize one nibble as DTMF audio data.
	 * 
	 * @param nibble The nibble to synthesize.
	 * @return The samples representing the DTMF signal.
	 */
	public float[] dtmf(int nibble) {
		/* Allocate space for samples. */
		float[] result = new float[(int)(DURATION * samplingFrequency)];

		/* Get low and high frequncies from table */
		float f_low  = DTMF[(nibble & 0xF) * 2 + 0];
		float f_high = DTMF[(nibble & 0xF) * 2 + 1];

		/* Generate sine waves with low and high frequencies. */
		for(int i = 0; i < result.length / 2; i++) {
			double t = (PI2 * i) / samplingFrequency;
			result[i] = (float)(0.4 * (Math.sin(f_low * t) + Math.sin(f_high * t))); 
		}
		return result;
	}

	/**
	 * Concatenate two synthesized DTMF nibbles together for one byte.
	 */
	@Override
	public float[] synthesize(byte data) {
		return ArrayUtilities.cat(dtmf(data >> 4), dtmf(data));
	}

}
