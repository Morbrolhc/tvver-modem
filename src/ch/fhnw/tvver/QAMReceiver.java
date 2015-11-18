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

import java.util.*;
import ch.fhnw.util.ClassUtilities;
import ch.fhnw.tvver.QAMSender;

/**
 * Simple receiver using amplitude modulation.
 * 
 * @author sschubiger
 *
 */
public class QAMReceiver extends AbstractReceiver {

	/* Experimental threshold for detecting start tone. Should be adaptive. */
	private static final Parameter START_THRESH = new Parameter("start", "Start Threshold", 0,0.5f,0.2f);
	/* Threshold for detecting binary "one". */
	private static final Parameter ONE_THRESH   =  new Parameter("one",  "One Threshold", 0,1f,0.5f);

	private QAMSender sender = new QAMSender();

	/* Idle / data state */
	private boolean       idle = true;
	/* Index for accumulating samples */
	private int           energyIdx;
	/* Energy accumulator */
	private final float[] energy = new float[12]; // 16 + 1 Treshold byte
	/* Sample index into the current symbol */
	//private int           sampleIdx;
	/* Symbol phase of start symbol */
	private int           symbolPhase;
	/* ArrayList for comparison Objects */
	float[][] comparison = new float[4][];

    /* ArrayList to record whole Message*/
    private List<Float> message = new ArrayList<>();

	private boolean first = true;

	String bitString = new String();

	private final int symbolSz = (int) 48000/4000; // =12 (samplingFrequency / SimpleAMSender.FREQ);

	public QAMReceiver() {
		super(START_THRESH, ONE_THRESH);
		initComparisonList();
	}

	private static final double PI2  = Math.PI * 2;

	private void initComparisonList(){
		// Decode 00
		comparison[0] = symbol(-1, -1);
		// Decode 01
		comparison[1] = symbol(-1, 1);
		// Decode 10
		comparison[2] = symbol(1, -1);
		// Decode 11
		comparison[3] = symbol(1, 1);

		System.out.println(Arrays.deepToString(comparison));
	}


	private float[] symbol(int msb, int lsb) {
		final float[] result = new float[symbolSz];

		for(int i = 0; i < result.length; i++)
			result[i] = (float)( 0.7f * (lsb*Math.sin(PI2*i/symbolSz)
					+ (msb*Math.cos(PI2*i/symbolSz))));

		return result;
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
		symbolPhase        = symbolSz / 4;
		if(idle) {
			if(sample > getVal(START_THRESH)) {
				//sampleIdx = symbolPhase;
				idle = false;
				energyIdx = 2;
			}
		} else {
			/* Accumulate energy */
			energy[energyIdx] = sample;

			/* End of symbol? */
			if(++energyIdx == symbolSz) {
				/* Enough data for a byte? */
				if(first){
					first = false;
					energyIdx = 0;
					Arrays.fill(energy, 0f);
					return;
				}
				//System.out.println("End of Symbol");

				/*  Collect bits. */
					int val = 0;
					int symbolIndex = 5;
					float minSum = Float.MAX_VALUE;
					for(int i = 0; i < comparison.length; i++){
						float sum = 0f;
						for(int j=2; j<comparison[i].length-3; j++){
							//System.out.println("Comparison i="+comparison[i][j]+" Energy="+energy[j]);
							//System.out.println("comparison["+i+"]["+j+"] "+comparison[i][j]+" - energy"+energy[j]);
							sum += Math.abs((energy[j+1] - energy[j]) -(comparison[i][j+1]-comparison[i][j]) );
						}
						//System.out.println("Sum="+Math.abs(sum));
						if(Math.abs(sum) < minSum){
							symbolIndex = i;
							minSum = Math.abs(sum);
						}
					}

					if(symbolIndex == 0){
						bitString += "00";
					}else if(symbolIndex == 1){
						bitString += "01";
					}else if(symbolIndex == 2){
						bitString += "10";
					}else if(symbolIndex == 3){
						bitString += "11";
					}

					if(bitString.length() >= 8){
						//System.out.println(bitString);
						byte[] bytes = new byte[ bitString.length() / 8 ];
						for(int i=0; i<bytes.length; i++){
							String byteString = bitString.substring(i*8, (i+1)*8);
							bytes[i] = (byte)(int)Integer.valueOf(byteString, 2); // Byte.parseByte(byteString, 2);
						}
						if(bitString.equals("11111111")){
							idle = true;
							first = true;
						}else{
							addData(bytes[0]);
						}
						bitString = "";
					}

						//addData((byte) val);
					/* Advance to next data byte */
					energyIdx = 0;
					Arrays.fill(energy, 0f);
					//idle = true;
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
		for(int i = 0; i < samples.length; i++) {
			process(samples[i]);
		}
	}
}