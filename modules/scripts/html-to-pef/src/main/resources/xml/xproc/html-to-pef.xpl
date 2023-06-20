<?xml version="1.0" encoding="UTF-8"?>
<p:declare-step type="px:html-to-pef.script" version="1.0"
                xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:px="http://www.daisy.org/ns/pipeline/xproc"
                xmlns:d="http://www.daisy.org/ns/pipeline/data"
                xmlns:c="http://www.w3.org/ns/xproc-step"
                xmlns:pef="http://www.daisy.org/ns/2008/pef"
                exclude-inline-prefixes="#all"
                name="main">
    
    <p:documentation xmlns="http://www.w3.org/1999/xhtml">
        <h1 px:role="name">HTML to braille</h1>
        <p px:role="desc">Transforms a HTML document into an embosser ready braille document.</p>
        <a px:role="homepage" href="http://daisy.github.io/pipeline/Get-Help/User-Guide/Scripts/html-to-pef/">
            Online documentation
        </a>
        <address>
            Authors:
            <dl px:role="author">
                <dt>Name:</dt>
                <dd px:role="name">Jostein Austvik Jacobsen</dd>
                <dt>E-mail:</dt>
                <dd><a px:role="contact" href="mailto:josteinaj@gmail.com">josteinaj@gmail.com</a></dd>
                <dt>Organization:</dt>
                <dd px:role="organization" href="http://www.nlb.no/">NLB</dd>
            </dl>
        </address>
    </p:documentation>
    
    <p:option name="source" required="true" px:type="anyFileURI" px:sequence="false" px:media-type="application/xhtml+xml text/html">
        <p:documentation xmlns="http://www.w3.org/1999/xhtml">
            <h2 px:role="name">Input HTML</h2>
            <p px:role="desc" xml:space="preserve">The HTML you want to convert to braille.</p>
        </p:documentation>
    </p:option>
    
    <p:output port="status" px:media-type="application/vnd.pipeline.status+xml">
        <!-- when `include-obfl` is set to true, the conversion may fail but still output a document
             on the "obfl" port -->
        <p:pipe step="convert" port="status"/>
    </p:output>
    
    <p:option name="stylesheet">
        <p:documentation xmlns="http://www.w3.org/1999/xhtml">
          <p px:role="desc" xml:space="preserve" px:inherit="prepend">

A number of [partials](https://sass-lang.com/documentation/at-rules/import#partials) (helper style
sheet modules) are available for use in Sass style sheets:

- [http://www.daisy.org/pipeline/modules/braille/html-to-pef/_tables.scss](http://daisy.github.io/pipeline/modules/braille/html-to-pef/src/main/resources/css/tables):
  for styling tables
- [http://www.daisy.org/pipeline/modules/braille/html-to-pef/_definition-lists.scss](http://daisy.github.io/pipeline/modules/braille/html-to-pef/src/main/resources/css/definition-lists):
  for styling definition lists
</p>
        </p:documentation>
    </p:option>
    
    <!-- defined in ../../../../../../common-options.xpl -->
    <p:option name="stylesheet-parameters"/>
    <p:option name="braille-code"/>
    <p:option name="transform"/>
    <p:option name="include-preview"/>
    <p:option name="include-pef"/>
    <p:option name="include-obfl"/>
    <p:option name="include-css" px:type="boolean" select="'false'">
        <p:documentation xmlns="http://www.w3.org/1999/xhtml">
            <h2 px:role="name">Include HTML with inline CSS</h2>
            <p px:role="desc" xml:space="preserve">Whether or not the include the intermediary HTML with all CSS styles inlined (for debugging).</p>
        </p:documentation>
    </p:option>
    <p:option name="output-file-format"/>
    <p:option name="preview-table"/>
    <p:option name="page-width"/>
    <p:option name="page-height"/>
    <p:option name="duplex"/>
    <p:option name="levels-in-footer"/>
    <p:option name="hyphenation"/>
    <p:option name="hyphenation-at-page-breaks"/>
    <p:option name="line-spacing"/>
    <p:option name="capital-letters"/>
    <p:option name="include-captions"/>
    <p:option name="include-images"/>
    <p:option name="include-line-groups"/>
    <p:option name="include-production-notes"/>
    <p:option name="show-braille-page-numbers"/>
    <p:option name="show-print-page-numbers"/>
    <p:option name="force-braille-page-break"/>
    <p:option name="toc-depth"/>
    <p:option name="toc-exclude-class"/>
    <p:option name="maximum-number-of-sheets"/>
    <p:option name="allow-volume-break-inside-leaf-section-factor"/>
    <p:option name="prefer-volume-break-before-higher-level-factor"/>
    <p:option name="notes-placement"/>
    <p:option name="result"/>
    <p:option name="pef"/>
    <p:option name="preview"/>
    <p:option name="obfl"/>
    <p:option name="html-with-css" px:output="result" px:type="anyDirURI" px:media-type="application/xhtml+xml" select="''">
        <p:documentation xmlns="http://www.w3.org/1999/xhtml">
            <h2 px:role="name">HTML with inline CSS</h2>
            <p px:role="desc">The intermediary HTML file with inline CSS.</p>
        </p:documentation>
    </p:option>
    
    <p:option name="temp-dir" required="true" px:output="temp" px:type="anyDirURI">
        <!-- directory used for temporary files -->
    </p:option>
    
    <p:import href="http://www.daisy.org/pipeline/modules/braille/common-utils/library.xpl">
        <p:documentation>
            px:delete-parameters
            px:parse-query
        </p:documentation>
    </p:import>
    <p:import href="http://www.daisy.org/pipeline/modules/braille/html-to-pef/library.xpl">
        <!-- FIXME: we cannot use a relative url to import px:html-to-pef directly here because this
             script uses px:extends-script in the XML catalog which changes the base URI of the
             script at build time. -->
        <p:documentation>
            px:html-to-pef
            px:html-to-pef.store
        </p:documentation>
    </p:import>
    <p:import href="http://www.daisy.org/pipeline/modules/fileset-utils/library.xpl">
        <p:documentation>
            px:fileset-add-entry
        </p:documentation>
    </p:import>
    <p:import href="http://www.daisy.org/pipeline/modules/html-utils/library.xpl">
        <p:documentation>
            px:html-load
        </p:documentation>
    </p:import>
    
    <!-- ================================================= -->
    <!-- Create a <c:param-set/> of the options            -->
    <!-- ================================================= -->
    <!-- ...for easy piping so we won't have to explicitly -->
    <!-- pass all the variables all the time.              -->
    <!-- ================================================= -->
    <p:in-scope-names name="in-scope-names"/>
    <px:delete-parameters name="input-options" px:message="Collecting parameters" px:message-severity="DEBUG" px:progress=".01"
                          parameter-names="html
                                           stylesheet
                                           stylesheet-parameters
                                           transform
                                           braille-code
                                           output-file-format
                                           include-pef
                                           include-preview
                                           include-obfl
                                           include-css
                                           result
                                           pef
                                           preview
                                           obfl
                                           html-with-css
                                           temp-dir">
        <p:input port="source">
            <p:pipe port="result" step="in-scope-names"/>
        </p:input>
    </px:delete-parameters>
    <p:sink/>
    <px:parse-query name="stylesheet-parameters">
        <p:with-option name="query" select="$stylesheet-parameters"/>
    </px:parse-query>
    <p:sink/>
    
    <!-- ========= -->
    <!-- LOAD HTML -->
    <!-- ========= -->
    <px:fileset-add-entry media-type="application/xhtml+xml">
        <p:input port="source.fileset">
          <p:inline><d:fileset/></p:inline>
        </p:input>
        <p:with-option name="href" select="$source"/>
    </px:fileset-add-entry>
    <px:html-load name="html" px:message="Loading HTML" px:progress=".03"/>
    
    <!-- ============ -->
    <!-- HTML TO PEF -->
    <!-- ============ -->
    <px:html-to-pef name="convert" px:message="Converting from HTML to PEF" px:progress=".90">
        <p:input port="source.in-memory">
            <p:pipe step="html" port="result.in-memory"/>
        </p:input>
        <p:with-option name="temp-dir" select="concat($temp-dir,'convert/')"/>
        <p:with-option name="stylesheet" select="$stylesheet"/>
        <p:with-option name="transform"
                       select="concat($braille-code,($transform,'(translator:liblouis)(formatter:dotify)')[not(.='')][1])"/>
        <p:with-option name="include-obfl" select="$include-obfl"/>
        <p:input port="parameters">
            <p:pipe port="result" step="input-options"/>
            <p:pipe port="result" step="stylesheet-parameters"/>
        </p:input>
    </px:html-to-pef>
    
    <!-- ========= -->
    <!-- STORE PEF -->
    <!-- ========= -->
    <px:html-to-pef.store px:progress=".05">
        <p:input port="obfl">
            <p:pipe step="convert" port="obfl"/>
        </p:input>
        <p:input port="html">
            <p:pipe step="html" port="result.in-memory"/>
        </p:input>
        <p:input port="css">
            <p:pipe step="convert" port="css"/>
        </p:input>
        <p:with-option name="include-pef" select="$include-pef"/>
        <p:with-option name="include-preview" select="$include-preview"/>
        <p:with-option name="include-css" select="$include-css"/>
        <p:with-option name="output-file-format" select="$output-file-format"/>
        <p:with-option name="preview-table" select="$preview-table"/>
        <p:with-option name="output-dir" select="$result"/>
        <p:with-option name="pef-output-dir" select="$pef"/>
        <p:with-option name="preview-output-dir" select="$preview"/>
        <p:with-option name="obfl-output-dir" select="$obfl"/>
        <p:with-option name="css-output-dir" select="$html-with-css"/>
    </px:html-to-pef.store>
    
</p:declare-step>
