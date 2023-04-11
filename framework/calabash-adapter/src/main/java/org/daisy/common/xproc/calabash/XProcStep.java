package org.daisy.common.xproc.calabash;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XAtomicStep;

import net.sf.saxon.s9api.SaxonApiException;

import org.daisy.common.saxon.SaxonHelper;
import org.daisy.common.saxon.SaxonInputValue;
import org.daisy.common.transform.InputValue;
import org.daisy.common.transform.Mult;
import org.daisy.common.transform.OutputValue;
import org.daisy.common.transform.StringWithNamespaceContext;
import org.daisy.common.transform.TransformerException;
import org.daisy.common.transform.XMLTransformer;
import org.daisy.common.xproc.XProcErrorException;

/**
 * {@link com.xmlcalabash.core.XProcStep} that also implements the {@link XMLTransformer} API.
 */
public interface XProcStep extends com.xmlcalabash.core.XProcStep, XMLTransformer {

	@Override
	public default Runnable transform(Map<QName,InputValue<?>> input, Map<QName,OutputValue<?>> output) {
		return () -> {
			try {
				for (QName n : input.keySet()) {
					InputValue<?> i = input.get(n);
					if (i instanceof SaxonInputValue) {
						if (n.getNamespaceURI() != null && !"".equals(n.getNamespaceURI()))
							throw new IllegalArgumentException("unexpected value on input port '" + n + "'");
						XProcStep.this.setInput(n.getLocalPart(), XMLCalabashInputValue.of(i).asReadablePipe());
					} else if (i instanceof StringWithNamespaceContext) {
						XProcStep.this.setOption(new net.sf.saxon.s9api.QName(n),
						                         XMLCalabashOptionValue.of(i).asRuntimeValue());
					} else {
						Mult<? extends InputValue<?>> m = i.mult(2);
						i = m.get();
						try {
							Map<net.sf.saxon.s9api.QName,RuntimeValue> params
								= XMLCalabashParameterInputValue.of(i).asRuntimeValueMap();
							if (n.getNamespaceURI() != null && !"".equals(n.getNamespaceURI()))
								throw new IllegalArgumentException("unexpected value on input port '" + n + "'");
							for (net.sf.saxon.s9api.QName p : params.keySet())
								XProcStep.this.setParameter(n.getLocalPart(), p, params.get(p));
						} catch (IllegalArgumentException e) {
							i = m.get();
							try {
								XProcStep.this.setOption(new net.sf.saxon.s9api.QName(n),
								                         XMLCalabashOptionValue.of(i).asRuntimeValue());
							} catch (IllegalArgumentException ee) {
								throw new IllegalArgumentException("unexpected value on input port '" + n + "'");
							}
						}
					}
				}
				for (QName n : output.keySet()) {
					OutputValue<?> o = output.get(n);
					if (n.getNamespaceURI() != null && !"".equals(n.getNamespaceURI()))
						throw new IllegalArgumentException("unexpected value on output port '" + n + "'");
					try {
						XProcStep.this.setOutput(n.getLocalPart(), XMLCalabashOutputValue.of(o).asWritablePipe());
					} catch (IllegalArgumentException e) {
						throw new IllegalArgumentException("unexpected value on output port '" + n + "'");
					}
				}
			} catch (IllegalArgumentException e) {
				throw new TransformerException(e);
			}
			try {
				XProcStep.this.run();
			} catch (SaxonApiException|XProcException e) {
				throw new TransformerException(e);
			}
		};
	}

	/**
	 * Port names of regular and parameter inputs are expected to not collide.
	 */
	public static XProcStep of(XMLTransformer transformer, XProcRuntime runtime, XAtomicStep step) {
		return new XProcStep() {

			private Map<String,ReadablePipe> inputs = null;
			private Map<String,WritablePipe> outputs = null;
			private Map<QName,RuntimeValue> options = null;
			private Map<String,Map<net.sf.saxon.s9api.QName,RuntimeValue>> parameters = null;

			public void setInput(String port, ReadablePipe input) {
				if (inputs == null)
					inputs = new HashMap<>();
				inputs.put(port, input);
			}

			public void setOutput(String port, WritablePipe output) {
				if (outputs == null)
					outputs = new HashMap<>();
				outputs.put(port, output);
			}

			public void setOption(net.sf.saxon.s9api.QName name, RuntimeValue value) {
				if (options == null)
					options = new HashMap<>();
				options.put(SaxonHelper.jaxpQName(name), value);
			}

			public void setParameter(net.sf.saxon.s9api.QName name, RuntimeValue value) {
				throw new XProcException(step, "A port should be specified for a parameter.");
			}

			public void setParameter(String port, net.sf.saxon.s9api.QName name, RuntimeValue value) {
				if (parameters == null)
					parameters = new HashMap<>();
				Map<net.sf.saxon.s9api.QName,RuntimeValue> params = parameters.get(port);
				if (params == null) {
					params = new HashMap<>();
					parameters.put(port, params);
				}
				params.put(name, value);
			}

			public void reset() {
				if (inputs != null) inputs.clear();
				if (outputs != null) outputs.clear();
				if (options != null) options.clear();
				if (parameters != null) parameters.clear();
			}

			public void run() {
				try {
					Map<QName,InputValue<?>> input = new HashMap<>();
					if (inputs != null)
						for (String port : inputs.keySet())
							input.put(new QName(port), new XMLCalabashInputValue(inputs.get(port)));
					if (options != null)
						for (QName name : options.keySet())
							input.put(name, new XMLCalabashOptionValue(options.get(name)));
					if (parameters != null)
						for (String port : parameters.keySet())
							input.put(new QName(port), new XMLCalabashParameterInputValue(parameters.get(port)));
					Map<QName,OutputValue<?>> output = new HashMap<>();
					if (outputs != null)
						for (String port : outputs.keySet())
							output.put(new QName(port), new XMLCalabashOutputValue(outputs.get(port), runtime));
					transformer.transform(input, output).run();
				} catch (Throwable e) {
					throw raiseError(e, step);
				}
			}

			@Override
			public Runnable transform(Map<QName,InputValue<?>> input, Map<QName,OutputValue<?>> output) {
				return transformer.transform(input, output);
			}
		};
	}

	public static XProcException raiseError(Throwable e, XAtomicStep step) {
		if (e instanceof XProcException)
			throw (XProcException)e;
		else if (e instanceof XProcErrorException) {
			throw CalabashExceptionFromXProcError.from(((XProcErrorException)e).getXProcError());
		} else {
			StackTraceElement[] stackTrace = new RuntimeException().getStackTrace();
			stackTrace = Arrays.copyOfRange(stackTrace, 1, stackTrace.length);
			if (e instanceof TransformerException) {
				XProcException cause = e.getCause() instanceof XProcException
					? (XProcException)e.getCause()
					: e.getCause() instanceof XProcErrorException
						? CalabashExceptionFromXProcError.from(((XProcErrorException)e.getCause()).getXProcError())
						: e.getCause() != null
							? XProcException.fromException(e.getCause())
							                .rebase(step.getLocation(), stackTrace)
							: null;
				QName code = ((TransformerException)e).getCode();
				return new XProcException(
					code != null
						? new net.sf.saxon.s9api.QName(code.getPrefix(), code.getNamespaceURI(), code.getLocalPart())
						: null,
					step, e, cause);
			} else {
				/* If the exception is a TransformerException we assume it comes from the {@link
				 * XMLTransformer.transform()} method used to implement the step. An
				 * IllegalArgumentException should not happen (indicates a bug in the XProc
				 * step). Because no other exceptions are mentioned in the contract of this method
				 * we treat them as unexpected exceptions as well. */
				return new XProcException(step,
				                          "Unexpected error in " + step.getType(),
				                          XProcException.fromException(e)
				                                        .rebase(step.getLocation(), stackTrace));
			}
		}
	}
}
