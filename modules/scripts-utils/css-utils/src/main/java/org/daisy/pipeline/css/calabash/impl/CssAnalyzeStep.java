package org.daisy.pipeline.css.calabash.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import static com.google.common.collect.Iterators.forArray;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XAtomicStep;

import net.sf.saxon.ma.map.MapItem;
import net.sf.saxon.s9api.SaxonApiException;

import org.daisy.common.file.URLs;
import org.daisy.common.saxon.SaxonHelper;
import static org.daisy.common.stax.XMLStreamWriterHelper.writeAttribute;
import static org.daisy.common.stax.XMLStreamWriterHelper.writeStartElement;
import org.daisy.common.xproc.calabash.XMLCalabashInputValue;
import org.daisy.common.xproc.calabash.XMLCalabashOutputValue;
import org.daisy.common.xproc.calabash.XProcStep;
import org.daisy.common.xproc.calabash.XProcStepProvider;
import org.daisy.common.xproc.XProcMonitor;
import org.daisy.pipeline.css.Medium;
import org.daisy.pipeline.css.sass.SassAnalyzer;
import org.daisy.pipeline.css.sass.SassAnalyzer.SassVariable;
import org.daisy.pipeline.css.UserAgentStylesheetRegistry;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.xml.sax.InputSource;

public class CssAnalyzeStep extends DefaultStep implements XProcStep {

	private ReadablePipe sourcePipe = null;
	private ReadablePipe contextPipe = null;
	private WritablePipe resultPipe = null;
	private final InMemoryURIResolver inMemoryResolver;
	private final URIResolver cssURIResolver;
	private final UserAgentStylesheetRegistry userAgentStylesheets;

	private final static QName c_param_set = new QName(XProcConstants.NS_XPROC_STEP, "param-set");
	private final static QName c_param = new QName(XProcConstants.NS_XPROC_STEP, "param");
	private final static QName _name = new QName("name");
	private final static QName _value = new QName("value");
	private static final net.sf.saxon.s9api.QName _user_stylesheet = new net.sf.saxon.s9api.QName("user-stylesheet");
	private static final net.sf.saxon.s9api.QName _include_user_agent_stylesheet
		= new net.sf.saxon.s9api.QName("include-user-agent-stylesheet");
	private static final net.sf.saxon.s9api.QName _parameters = new net.sf.saxon.s9api.QName("parameters");
	private static final net.sf.saxon.s9api.QName _content_type = new net.sf.saxon.s9api.QName("content-type");
	private static final net.sf.saxon.s9api.QName _media = new net.sf.saxon.s9api.QName("media");

	private static final String DEFAULT_MEDIUM = "embossed";

	private CssAnalyzeStep(XProcRuntime runtime, XAtomicStep step,
	                       URIResolver resolver, UserAgentStylesheetRegistry userAgentStylesheets) {
		super(runtime, step);
		inMemoryResolver = new InMemoryURIResolver();
		cssURIResolver = fallback(inMemoryResolver, resolver, simpleURIResolver);
		this.userAgentStylesheets = userAgentStylesheets;
	}

	@Override
	public void setInput(String port, ReadablePipe pipe) {
		if ("source".equals(port))
			sourcePipe = pipe;
		else
			contextPipe = pipe;
	}

	@Override
	public void setOutput(String port, WritablePipe pipe) {
		resultPipe = pipe;
	}

	@Override
	public void reset() {
		sourcePipe.resetReader();
		contextPipe.resetReader();
		resultPipe.resetWriter();
	}

	@Override
	public void run() throws SaxonApiException {
		super.run();
		try {
			Map<String,String> params = new LinkedHashMap<>(); // use LinkedHashMap to get same order as insertion order
			RuntimeValue paramOption = getOption(_parameters);
			if (paramOption != null)
				for (Map.Entry<String,Object> e
				         : SaxonHelper.mapFromMapItem(
				               (MapItem)SaxonHelper.getSingleItem(paramOption.getValue().getUnderlyingValue()),
				               Object.class
				           ).entrySet())
					params.put(e.getKey(), "" + e.getValue());
			List<Medium> media = Medium.parseMultiple(getOption(_media, DEFAULT_MEDIUM));
			inMemoryResolver.setContext(contextPipe);
			Node doc = new XMLCalabashInputValue(sourcePipe).ensureSingleItem().asNodeIterator().next();
			if (!(doc instanceof Document))
				throw new IllegalArgumentException();
			URI baseURI = new URI(doc.getBaseURI());
			Source sourceDocument = new DOMSource(doc, baseURI.toASCIIString());
			List<Source> stylesheets = new ArrayList<>(); {
				if (getOption(_include_user_agent_stylesheet, false))
					for (URL u : userAgentStylesheets.get(Collections.singleton("text/x-scss"),
					                                      Arrays.asList(getOption(_content_type, "").trim().split("\\s+")),
					                                      media))
						stylesheets.add(
							new SAXSource(new InputSource(u.toString())));
				String s = getOption(_user_stylesheet, "");
				if (s != null) {
					StringTokenizer t = new StringTokenizer(s);
					while (t.hasMoreTokens())
						stylesheets.add(
							new SAXSource(
								new InputSource(URLs.resolve(baseURI, URLs.asURI(t.nextToken())).toASCIIString())));
				}
			}
			for (SassVariable v : new SassAnalyzer(media, cssURIResolver, null)
			                          .analyze(stylesheets, sourceDocument)
			                          .getVariables()) {
				if (params.containsKey(v.getName()) && v.isDefault())
					continue;
				params.put(v.getName(), v.getValue());
			}
			XMLStreamWriter writer = new XMLCalabashOutputValue(resultPipe, runtime).asXMLStreamWriter();
			writer.writeStartDocument();
			writeStartElement(writer, c_param_set);
			for (String p : params.keySet()) {
				writeStartElement(writer, c_param);
				writeAttribute(writer, _name, p);
				writeAttribute(writer, _value, params.get(p));
				writer.writeEndElement();
			}
			writer.writeEndElement();
			writer.writeEndDocument();
		} catch (Throwable e) {
			throw XProcStep.raiseError(e, step);
		}
	}

	@Component(
		name = "pxi:css-analyze",
		service = { XProcStepProvider.class },
		property = { "type:String={http://www.daisy.org/ns/pipeline/xproc/internal}css-analyze" }
	)
	public static class Provider implements XProcStepProvider {

		private URIResolver resolver;
		private UserAgentStylesheetRegistry userAgentStylesheets;

		@Override
		public XProcStep newStep(XProcRuntime runtime, XAtomicStep step, XProcMonitor monitor, Map<String,String> properties) {
			return new CssAnalyzeStep(runtime, step, resolver, userAgentStylesheets);
		}

		@Reference(
			name = "URIResolver",
			unbind = "-",
			service = URIResolver.class,
			cardinality = ReferenceCardinality.MANDATORY,
			policy = ReferencePolicy.STATIC
		)
		public void setUriResolver(URIResolver resolver) {
			this.resolver = resolver;
		}

		@Reference(
			name = "UserAgentStylesheetRegistry",
			unbind = "-",
			service = UserAgentStylesheetRegistry.class,
			cardinality = ReferenceCardinality.MANDATORY,
			policy = ReferencePolicy.STATIC
		)
		public void setUserAgentStylesheetRegistry(UserAgentStylesheetRegistry registry) {
			this.userAgentStylesheets = registry;
		}
	}

	private static URIResolver simpleURIResolver = new URIResolver() {
			@Override
			public Source resolve(String href, String base) throws TransformerException {
				try {
					URI uri; {
						if (base != null)
							uri = new URI(base).resolve(new URI(href));
						else
							uri = new URI(href);
					}
					return new SAXSource(new InputSource(uri.toASCIIString()));
				} catch (URISyntaxException e) {
					throw new TransformerException(e);
				}
			}
		};

	private static URIResolver fallback(URIResolver... resolvers) {
		return new URIResolver() {
			public Source resolve(String href, String base) throws javax.xml.transform.TransformerException {
				Iterator<URIResolver> iterator = forArray(resolvers);
				while (iterator.hasNext()) {
					Source source = iterator.next().resolve(href, base);
					if (source != null)
						return source; }
				return null;
			}
		};
	}

	private static final Logger logger = LoggerFactory.getLogger(CssAnalyzeStep.class);

}
