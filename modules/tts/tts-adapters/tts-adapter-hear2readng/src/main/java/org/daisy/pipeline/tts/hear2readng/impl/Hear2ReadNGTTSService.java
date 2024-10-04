package org.daisy.pipeline.tts.hear2readng.impl;

import java.util.Map;
import java.util.Optional;

import javax.sound.sampled.AudioFormat;

import org.daisy.common.properties.Properties;
import org.daisy.common.properties.Properties.Property;
import org.daisy.pipeline.tts.DefaultSpeechRate;
import org.daisy.pipeline.tts.TTSEngine;
import org.daisy.pipeline.tts.TTSService;

import org.osgi.service.component.annotations.Component;

/**
 * OSGI service to instantiate the Hear2ReadNG Text-to-speech engine adapter
 *
 * @author krishnshyam @ gmail.com
 */
@Component(
	name = "hear2readng-tts-service",
	service = { TTSService.class }
)
public class Hear2ReadNGTTSService implements TTSService {

	private static final Property HEAR2READNG_SAMPLERATE = Properties.getProperty("org.daisy.pipeline.tts.hear2readng.samplerate",
	                                                                         true,
	                                                                         "Audio sample rate of Hear2Read NG voices (in Hz)",
	                                                                         false,
	                                                                         "22050");
	private static final Property HEAR2READNG_ADDRESS = Properties.getProperty("org.daisy.pipeline.tts.hear2readng.address",
	                                                                      false,
	                                                                      "Address of Hear2ReadNG cloud speech engine server",
	                                                                      false,
	                                                                      "https://hear2read.org:8443");
																		  
    //TODO!! check below property
	private static final Property HEAR2READNG_PRIORITY = Properties.getProperty("org.daisy.pipeline.tts.hear2readng.priority",
	                                                                       true,
	                                                                       "Priority of Hear2Read NG voices relative to voices of other engines",
	                                                                       false,
	                                                                       "15");
	private static final DefaultSpeechRate SPEECH_RATE = new DefaultSpeechRate();

	@Override
	public TTSEngine newEngine(Map<String,String> properties) throws ServiceDisabledException, SynthesisException {
		String serverAddress = HEAR2READNG_ADDRESS.getValue(properties); // this is a hidden parameter, it is meant to be used in tests only
		int sampleRate = getPropertyAsInt(properties, HEAR2READNG_SAMPLERATE).get();
		int priority = getPropertyAsInt(properties, HEAR2READNG_PRIORITY).get();
		float speechRate = SPEECH_RATE.getValue(properties);
		AudioFormat audioFormat = new AudioFormat((float) sampleRate, 16, 1, true, false);
		return new Hear2ReadNGRestTTSEngine(this, serverAddress, audioFormat, priority, speechRate);
	}

	@Override
	public String getName() {
		return "hear2readng";
	}

	@Override
	public String getDisplayName() {
		return "Hear2Read NG"; // "Hear2ReadNG"
	}

	private static Optional<Integer> getPropertyAsInt(Map<String,String> properties, Property prop) throws SynthesisException {
		String str = prop.getValue(properties);
		if (str != null) {
			try {
				return Optional.of(Integer.valueOf(str));
			} catch (NumberFormatException e) {
				throw new SynthesisException(str + " is not a valid a value for property " + prop.getName());
			}
		}
		return Optional.empty();
	}
}
