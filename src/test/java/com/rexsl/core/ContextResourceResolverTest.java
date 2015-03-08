/**
 * Copyright (c) 2011-2015, ReXSL.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met: 1) Redistributions of source code must retain the above
 * copyright notice, this list of conditions and the following
 * disclaimer. 2) Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution. 3) Neither the name of the ReXSL.com nor
 * the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.rexsl.core;

import com.jcabi.matchers.XhtmlMatchers;
import com.rexsl.mock.MkServletContext;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.servlet.ServletContext;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

/**
 * Test case for {@link ContextResourceResolver}.
 * @author Yegor Bugayenko (yegor@tpc2.com)
 * @author Krzysztof Krason (Krzysztof.Krason@gmail.com)
 * @version $Id$
 */
public final class ContextResourceResolverTest {

    /**
     * Temporary folder.
     * @checkstyle VisibilityModifier (3 lines)
     */
    @Rule
    public transient TemporaryFolder temp = new TemporaryFolder();

    /**
     * ContextResourceResolver can resolve resource by HREF.
     * @throws Exception If something goes wrong
     */
    @Test
    public void resolvesResourceByHref() throws Exception {
        final String href = "/test.xml?123";
        final String ref = "/test.xml";
        final ServletContext ctx = new MkServletContext()
            .withResource(ref, "<r>\u0443</r>");
        final URIResolver resolver = new ContextResourceResolver(ctx);
        final Source src = resolver.resolve(href, null);
        MatcherAssert.assertThat(src.getSystemId(), Matchers.equalTo(ref));
        MatcherAssert.assertThat(src, XhtmlMatchers.hasXPath("/r[.='\u0443']"));
    }

    /**
     * ContextResourceResolver can resolve resource by relative local path.
     * @throws Exception If something goes wrong
     */
    @Test
    public void resolvesResourceByRelativeLocalPath() throws Exception {
        final String ref = "/a/text.txt";
        final ServletContext ctx = new MkServletContext()
            .withResource(ref, "");
        final URIResolver resolver = new ContextResourceResolver(ctx);
        final Source src = resolver.resolve("./text.txt?xxx", "/a/smth.xml?z");
        MatcherAssert.assertThat(src.getSystemId(), Matchers.equalTo(ref));
    }

    /**
     * ContextResourceResolver can resolve resource by HREF and blank base.
     * @throws Exception If something goes wrong
     */
    @Test
    public void resolvesResourceByHrefAndBlankBase() throws Exception {
        final String href = "/test2.xml?1234";
        final String ref = "/test2.xml";
        final ServletContext ctx = new MkServletContext()
            .withResource(ref, "<r>\u0444</r>");
        final URIResolver resolver = new ContextResourceResolver(ctx);
        final Source src = resolver.resolve(href, "");
        MatcherAssert.assertThat(src.getSystemId(), Matchers.equalTo(ref));
        MatcherAssert.assertThat(src, XhtmlMatchers.hasXPath("/r[.='\u0444']"));
    }

    /**
     * ContextResourceResolver can resolve when a resource is an Absolute URI.
     * @throws Exception If something goes wrong
     */
    @Test
    public void resolvesWhenResourcesIsAnAbsoluteLink() throws Exception {
        final String href = "http://localhost/xsl/file.xsl";
        final ServletContext ctx = new MkServletContext();
        final ContextResourceResolver.ConnectionProvider provider =
            Mockito.mock(ContextResourceResolver.ConnectionProvider.class);
        final HttpURLConnection conn = this.mockConnection();
        Mockito.doReturn(HttpURLConnection.HTTP_OK)
            .when(conn).getResponseCode();
        Mockito.doReturn(
            IOUtils.toInputStream(
                // @checkstyle LineLength (1 line)
                "<stylesheet xmlns='http://www.w3.org/1999/XSL/Transform' version='1.0'/>"
            )
        )
            .when(conn).getInputStream();
        Mockito.when(provider.open(Mockito.any(URL.class))).thenReturn(conn);
        final URIResolver resolver = new ContextResourceResolver(
            ctx,
            provider
        );
        final Source src = resolver.resolve(href, null);
        MatcherAssert.assertThat(src, Matchers.notNullValue());
        TransformerFactory.newInstance().newTransformer(src);
    }

    /**
     * ContextResourceResolver throws exception if absolute URI fetched through
     * HTTP has invalid (non-OK) HTTP response status code.
     * @throws Exception If something goes wrong
     */
    @Test(expected = javax.xml.transform.TransformerException.class)
    public void throwsExceptionWhenAbsoluteResourceHasInvalidStatusCode()
        throws Exception {
        final String href = "http://localhost/some-non-existing-file.xsl";
        final ServletContext ctx = new MkServletContext();
        final ContextResourceResolver.ConnectionProvider provider =
            Mockito.mock(ContextResourceResolver.ConnectionProvider.class);
        final HttpURLConnection conn = this.mockConnection();
        Mockito.doReturn(HttpURLConnection.HTTP_NOT_FOUND)
            .when(conn).getResponseCode();
        Mockito.when(provider.open(Mockito.any(URL.class))).thenReturn(conn);
        final URIResolver resolver = new ContextResourceResolver(ctx, provider);
        resolver.resolve(href, null);
    }

    /**
     * ContextResourceResolver throws exception when absolute URI
     * fetched through HTTP throws IO exception.
     * @throws Exception If something goes wrong
     */
    @Test(expected = javax.xml.transform.TransformerException.class)
    public void throwsExceptionWhenAbsoluteResourceThrowsIoException()
        throws Exception {
        final String href = "http://localhost/erroneous-file.xsl";
        final ServletContext ctx = new MkServletContext();
        final HttpURLConnection conn = this.mockConnection();
        Mockito.doThrow(new IOException("ouch")).when(conn).connect();
        final ContextResourceResolver.ConnectionProvider provider =
            Mockito.mock(ContextResourceResolver.ConnectionProvider.class);
        Mockito.when(provider.open(Mockito.any(URL.class))).thenReturn(conn);
        final URIResolver resolver = new ContextResourceResolver(ctx);
        resolver.resolve(href, null);
    }

    /**
     * ContextResourceResolver throws exception when resource is absent and
     * is not an absolute URI.
     * @throws Exception If something goes wrong
     */
    @Test(expected = javax.xml.transform.TransformerException.class)
    public void throwsWhenResourceIsAbsent() throws Exception {
        final String href = "/xsl/file.xsl";
        final ServletContext ctx = new MkServletContext();
        final URIResolver resolver = new ContextResourceResolver(ctx);
        resolver.resolve(href, null);
    }

    /**
     * ContextResourceResolver can resolve a file.
     * @throws Exception If something goes wrong
     */
    @Test
    public void resolvesUrlWithFile() throws Exception {
        final File file = this.temp.newFile("file.xml");
        FileUtils.writeStringToFile(file, "<data/>");
        final String href = file.toURI().toString();
        final ServletContext ctx = new MkServletContext();
        final URIResolver resolver = new ContextResourceResolver(ctx);
        final Source src = resolver.resolve(href, null);
        MatcherAssert.assertThat(src.getSystemId(), Matchers.equalTo(href));
        MatcherAssert.assertThat(src, XhtmlMatchers.hasXPath("/data"));
    }

    /**
     * Mock {@link HttpURLConnection} for the specific HREF.
     * @return The connection mock
     */
    private HttpURLConnection mockConnection() {
        return Mockito.mock(HttpURLConnection.class);
    }

}
