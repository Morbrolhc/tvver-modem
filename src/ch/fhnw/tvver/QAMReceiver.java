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

import ch.fhnw.ether.media.Parameter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Simple receiver using amplitude modulation.
 * 
 * @author sschubiger
 *
 */
public class QAMReceiver extends AbstractReceiver {

	/* Experimental threshold for detecting start tone. Should be adaptive. */
	private static final Parameter START_THRESH = new Parameter("start", "Start Threshold", 0,0.5f,0.1f);
	/* Threshold for detecting binary "one". */
	private static final Parameter ONE_THRESH   =  new Parameter("one",  "One Threshold", 0,1f,0.5f);

	/* Idle / data state */
	private boolean       idle = true;
	/* Index for accumulating samples */
	private int           energyIdx;
	/* Energy accumulator */
	private final float[] energy = new float[5];
	/* Sample index into the current symbol */
	private int           sampleIdx;
	/* Symbol phase of start symbol */
	private int           symbolPhase;
    /*ArrayList to record whole Message*/
    private List<Float> message = new ArrayList<>();

	public QAMReceiver() {
		super(START_THRESH, ONE_THRESH);
	}
	
	/**
	 * Process one sample (power).
	 * 
	 * @param sample The sample to process.
	 */
//	private void process(float sample) {
//		final int symbolSz = (int) (samplingFrequency / SimpleAMSender.FREQ);
//		symbolPhase        = symbolSz / 4;
//
//		/* Wait for signal to rise above start threshold. */
//		if(idle) {
//			if(sample > getVal(START_THRESH)) {
//				sampleIdx = symbolPhase;
//				idle     = false;
//			}
//		} else {
//			/* Accumulate energy */
//			energy[energyIdx] += sample;
//			/* End of symbol? */
//			if(++sampleIdx == symbolSz) {
//				/* Advance to next symbol */
//				sampleIdx = 0;
//				energyIdx++;
//				/* Enough data for a byte? */
//				if(energyIdx == energy.length) {
//					/*  Collect bits. */
//					int val = 0;
//					for(int i = 0; i < 4; i++)
//						/* Use first symbol as reference value */
//						if(energy[i+1] > getVal(ONE_THRESH) * energy[0])
//							val |= 1 << i;
//                    addData((byte) val);
//					/* Advance to next data byte */
//					energyIdx = 0;
//					sampleIdx = symbolPhase;
//					Arrays.fill(energy, 0f);
//					idle = true;
//				}
//			}
//		}
//	}

    private void process(float sample) {
        if(idle) {
			if(sample > getVal(START_THRESH)) {
				sampleIdx = symbolPhase;
				idle     = false;
			} else {
                message.add(sample);
            }
        }
    }


	/**
	 * Process samples. Samples are squared (power).
	 * 
	 * @param samples The samples to process.
	 */
	@Override
	protected void process(float[] samples) {
		for(int i = 0; i < samples.length; i++)
			process(samples[i]*samples[i]);
	}
}