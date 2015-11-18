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

import java.util.BitSet;

/**
 * Simple sender using amplitude modulation.
 * 
 * @author sschubiger
 *
 */
public class QAMSender extends AbstractSender {
	private static final double PI2  = Math.PI * 2;
	/* Carrier frequency. */
	static         final float  FREQ = 4000;

	/**
	 * Create a wave with given amplitude. 
	 * @param msb, lsb
	 * @return Audio data for symbol.
	 */
	private float[] symbol(int msb, int lsb) {
		final int symbolSz = (int) (samplingFrequency / FREQ);
		final float[] result = new float[symbolSz];
        if(msb == 0) msb = -1;
        if(lsb == 0) lsb = -1;

        for(int i = 0; i < result.length; i++)
			result[i] = (float)( 0.7f * (lsb*Math.sin(PI2*i/symbolSz)
                    + (msb*Math.cos(PI2*i/symbolSz))));

		return result;
	}

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
        System.out.println(Integer.toBinaryString(data & 0xFF));
        /* Send data bits. */
		for(int i = 6; i >= 0; i-=2) {
			int msb = 0, lsb = 0;
            lsb = ((data>>>i) & 0b1) == 0b1 ? 1 : 0;
            msb = ((data>>>i) & 0b10) == 0b10 ? 1 : 0;
            System.out.println(msb + " " + lsb);
            result.addAll(symbol(msb, lsb));
		}
		return result.toArray();
	}

	@Override
	public float[] synthesize(byte[] data) {
		FloatList result = new FloatList();
		result.addAll(symbol(1f));
		for(int i = 0; i < data.length; i++)
			result.addAll(synthesize(data[i]));
		return result.toArray();
	}
}
