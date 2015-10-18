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

import ch.fhnw.ether.audio.AudioUtilities;
import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.audio.fx.BandsButterworth;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.ether.media.Stateless;

/**
 * A simple receiver which is using DTMF (dual tone multi frequency) encoding for data.
 * A DTMF signal can hold 4bit of information thus two DTMF signal are used for each byte.
 * 
 * @author sschubiger
 */
public class DTMFReceiver extends AbstractReceiver {
	/* Experimental threshold for detecting start tone. Should be adaptive. */
	private static final float START_THRESH = 0.1f;
	
	/**
	 * Create filters for DTMF frequencies.
	 */
	private final BandsButterworth bands = new BandsButterworth(1, 10, true,
			697,  770, 852, 941, // DTMF low frequencies (rows)
			1209,1336,1477,1633  // DTMF high frequencies (colums)
			);
	
	/** The power values computed by the filters. */
	private final float[] power = new float[8];
	/** index into the tone phase of the transmission */
	private int           dtmfSignal  = 0;
	/** Accumulator for the data. */
	private int           data  = 0;
	/** Toggle indicating if the low or high nibble of data is active. */
	private boolean       lowHigh;
	
	/**
	 * Add the bands filter to the audio pipeline.
	 */
	@Override
	public void init(float samplingFrequency) {
		super.init(samplingFrequency);
		insert(bands);
	}
	
	/**
	 * Process one audio frame. Note that this implementation assumes for simplicity that
	 * a DTMF signal stretches over multiple frames. In order to run with arbitrary frame 
	 * sizes, a buffering should be used (@see ch.fhnw.ether.audio.BlockBuffer).
	 */
	@Override
	protected void run(Stateless<IAudioRenderTarget> state) throws RenderCommandException {
		/* If energy exceeds a threshold (constant found experimentally, should be adaptive), consider it as a DTMF signal. */
		if(AudioUtilities.energy(state.getTarget().getFrame().samples) > START_THRESH) {
			/* count dtmfSignals and let filters stabilize a bit before reading the power values. */
			if(++dtmfSignal == 5) {
				/* Get the power values from the filter bank.*/
				bands.state().get(state.getTarget()).power(power);
				/* Find the peak values in the low four frequencies and high four frequencies. */
				int   lowPeakIdx  = 0;
				int   highPeakIdx = 4;
				for(int i = 1; i < 4; i++) {
					if(power[i]   > power[lowPeakIdx])  lowPeakIdx  = i;
					if(power[i+4] > power[highPeakIdx]) highPeakIdx = i + 4;
				}
				/* Add 4 bits of data. Two bits come from the DTMF low frequency part, two bits from the DTMF high frequency part. */
				/* Shift data to make space for the next two bits. */
				data <<= 2;
				/* Add peak value.*/
				data |= lowPeakIdx;
				/* Shift data to make space for the next two bits. */
				data <<= 2;
				/* Add peak value.*/
				data |= (highPeakIdx - 4);
				/* if a byte is completed, add it to the receive queue. */
				if(lowHigh)	addData((byte) data);
				/* toggle high/low flag for the next nibble */
				lowHigh = !lowHigh;
			}
		} else {
			/* reset DTMF signal counter. */
			dtmfSignal = 0;
		}
	}
	
	/** Unused. */
	@Override
	protected void process(float[] samples) {}
}
