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

import java.util.ArrayList;
import java.util.List;

import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.Parameter;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.ether.media.Stateless;
import ch.fhnw.util.ByteList;

/**
 * Base class for tvver soft modem project receivers.
 * 
 * @author sschubiger
 */
public abstract class AbstractReceiver extends AbstractRenderCommand<IAudioRenderTarget,Stateless<IAudioRenderTarget>> {
	/** Received data. */
	private final ByteList                                          data = new ByteList();
	/** Additional filters etc. to add to the audio pipeline. */
	private final List<AbstractRenderCommand<IAudioRenderTarget,?>> cmds = new ArrayList<>();
	/** The sampling frequency */
	protected float  samplingFrequency;

	public AbstractReceiver(Parameter ... params) {
		super(params);
	}
	
	/**
	 * Process one audio frame. Called by run() process audio data. Implement
	 * this method to receive and decode data.
	 * 
	 * @param samples The audio samples in the range [-1..1].
	 */
	protected abstract void process(float[] samples);	

	/** 
	 * Initialize this receiver. Called after constructor.
	 * @param samplingFrequency The samplingFrequency.
	 */
	public void init(float samplingFrequency) {
		this.samplingFrequency = samplingFrequency;
	}

	/**
	 * Call for the test program to pull the received data.
	 * @return The recevied data.
	 */
	public final byte[] getAndClearData() {
		byte[] result = data.toArray();
		data.clear();
		return result;
	}

	/**
	 * Call for the test program to get additional filters etc. for the audio pipeline.
	 * @return
	 */
	public final List<AbstractRenderCommand<IAudioRenderTarget,?>> getCmds() {
		return cmds;
	}

	/**
	 * Insert a filter etc. into the audio pipeline. Call this method for your pipeline additions.
	 * @param cmd
	 */
	protected final void insert(AbstractRenderCommand<IAudioRenderTarget,?> cmd) {
		cmds.add(cmd);
	}

	/**
	 * Process one audio frame. Called by EtherGL to process audio data. This implementation
	 * extracts the audio samples and forwards the call. Override this method if you have
	 * specific processing needs beyond samples.
	 * 
	 * @param state The audio command state.
	 */
	@Override
	protected void run(Stateless<IAudioRenderTarget> state) throws RenderCommandException {
		process(state.getTarget().getFrame().samples);
	}

	/**
	 * Add one byte of data to the output queue. Call this method if you have received and decoded one
	 * byte of output data.
	 * 
	 * @param data The byte to add to the output queue.
	 */
	protected final void addData(byte data) {
		int i = this.data.size();
		this.data.add(data);

		/*
		switch(Main.dataType) {
		default:
			if(i > 0 && (i % 20) == 0)
				System.out.println();
			System.out.print(i + ":" + (data & 0xFF)+ ",");
			System.out.flush();
			break;
		case TEXT:
			System.out.print((char)data);
			System.out.flush();
			break;
		}
		*/
	}
}
