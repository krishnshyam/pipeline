package org.daisy.pipeline.tts.hear2readng.impl;

/**
 * Possible actions to request with REST and Hear2ReadNG API.
 *
 * @author  krishnshyam @ gmail.com
 */
public enum Hear2ReadNGRestAction {

	VOICES("GET","/ngsynthesizer/getVoices"),
	SPEECH("POST","/ngsynthesizer/ng-synthesize");

	public String method;
	public String domain;

	/**
	 * @param method the HTTP method (usually GET or POST)
	 * @param domain the domain/endpoint of the requested action
	 */
	Hear2ReadNGRestAction(String method, String domain) {
		this.method = method;
		this.domain = domain;
	}
}
