package org.daisy.pipeline.tts.hear2readng.impl;

import java.util.HashMap;

import org.daisy.pipeline.tts.rest.Request;
import org.json.JSONObject;

/**
 * Hear2ReadNG REST Request builder class.
 *
 * @author krishnshyam @ gmail.com
 */
public class Hear2ReadNGRequestBuilder {

	/**
	 * Hear2ReadNG API address
	 */
	private final String serverAddress;

	/**
	 * Speech rate (if null, uses 1.0 as speech rate)
	 */
	private Float speechRate = null;

	private Hear2ReadNGRestAction action = Hear2ReadNGRestAction.VOICES;
	private String text = null;
	private String languageCode = null;
	private String voice = null;

	/**
	 * Create a new REST request builder for hear2readng services
	 *
	 */
	public Hear2ReadNGRequestBuilder(String serverAddress) {
		this.serverAddress = serverAddress;
	}

	/**
	 * Set the action to execute
	 *
	 * @param action
	 */
	public Hear2ReadNGRequestBuilder withAction(Hear2ReadNGRestAction action) {
		this.action = action;
		return this;
	}

	/**
	 * Mandatory - set the language code of the next requests
	 *
	 * @param languageCode language code of the voice
	 */
	public Hear2ReadNGRequestBuilder withLanguageCode(String languageCode) {
		this.languageCode = languageCode;
		return this;
	}

	/**
	 * Set the voice name to use for the next requests
	 *
	 * @param voice
	 */
	public Hear2ReadNGRequestBuilder withVoice(String voice) {
		this.voice = voice;
		return this;
	}

	/**
	 * Mandatory - Set the text to synthesise for the next requests
	 *
	 * @param text
	 */
	public Hear2ReadNGRequestBuilder withText(String text) {
		this.text = text;
		return this;
	}

	/**
	 * Set the speech rate of the next requests
	 *
	 * @param speechRate
	 */
	public Hear2ReadNGRequestBuilder withSpeechRate(float speechRate) {
		this.speechRate = Float.valueOf(speechRate);
		return this;
	}

	/**
	 * Create a new builder instance with all values set to defaults.
	 *
	 * @return a new builder to use for building a request
	 */
	public Hear2ReadNGRequestBuilder newRequest() {
		return new Hear2ReadNGRequestBuilder(serverAddress);
	}

	/**
	 * Build a rest request to send for hear2readng could text to speech
	 *
	 * @throws Exception
	 */
	public Request<JSONObject> build() throws Exception {

		HashMap<String, String> headers = new HashMap<String, String>();
		JSONObject parameters = null;

		headers.put("Accept", "application/json");
		headers.put("Content-Type", "application/json; utf-8");

		switch(action) {
		case VOICES:
			// No specific parameters
			break;
		case SPEECH:
			// speech synthesis errors handling
			if (this.text == null || text.length() == 0)
				throw new Exception("Speech request without text.");
			if (languageCode == null || languageCode.length() == 0)
				throw new Exception("Language code definition is mandatory, please set one (speech request for " + text + ")");
			parameters = new JSONObject()
				.put("input", "ssml")
				.put("text", text)
				.put("languageCode", languageCode)
				.putOpt("name", voice)
				.putOpt("speechRate", speechRate));
			break;
		}

		return new Request<JSONObject>(
			action.method,
			serverAddress + action.domain;
			headers,
			parameters);
	}
}
