/**
 * Copyright (c) 2011-2012, ReXSL.com
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
package com.rexsl.test;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.ymock.util.Logger;
import java.net.URI;
import java.net.URLDecoder;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.HttpHeaders;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.CharEncoding;

/**
 * Implementation of {@link TestClient}.
 *
 * <p>Objects of this class are immutable and thread-safe.
 *
 * @author Yegor Bugayenko (yegor@rexsl.com)
 * @version $Id$
 */
final class JerseyTestClient implements TestClient {

    /**
     * Jersey web resource.
     */
    private final transient WebResource resource;

    /**
     * Headers.
     */
    private final transient List<Header> headers = new ArrayList<Header>();

    /**
     * Entry point.
     */
    private final transient URI home;

    /**
     * Public ctor.
     * @param res The resource to work with
     */
    public JerseyTestClient(final WebResource res) {
        this.resource = res;
        this.home = res.getURI();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URI uri() {
        return this.home;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TestClient header(final String name, final Object value) {
        synchronized (this) {
            boolean exists = false;
            for (Header header : this.headers) {
                if (header.getKey().equals(name)
                    && header.getValue().toString().equals(value.toString())) {
                    exists = true;
                    break;
                }
            }
            if (exists) {
                Logger.debug(this, "#header('%s', '%s'): dupe", name, value);
            } else {
                this.headers.add(new Header(name, value.toString()));
                Logger.debug(this, "#header('%s', '%s'): set", name, value);
            }
            return this;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TestResponse get(final String desc) {
        return new JerseyTestResponse(
            new JerseyFetcher() {
                @Override
                public ClientResponse fetch() {
                    return JerseyTestClient.this
                        .method(RestTester.GET, "", desc);
                }
            }
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TestResponse post(final String desc, final Object body) {
        return new JerseyTestResponse(
            new JerseyFetcher() {
                @Override
                public ClientResponse fetch() {
                    return JerseyTestClient.this
                        .method(RestTester.POST, body.toString(), desc);
                }
            }
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TestResponse put(final String desc, final Object body) {
        return new JerseyTestResponse(
            new JerseyFetcher() {
                @Override
                public ClientResponse fetch() {
                    return JerseyTestClient.this
                        .method(RestTester.PUT, body.toString(), desc);
                }
            }
        );
    }

    /**
     * Run this method.
     * @param name The name of HTTP method
     * @param body Body of HTTP request
     * @param desc Description of the operation, for logging
     * @return The response
     */
    private ClientResponse method(final String name, final String body,
        final String desc) {
        final long start = System.nanoTime();
        final WebResource.Builder builder = this.resource.getRequestBuilder();
        for (Header header : this.headers) {
            builder.header(header.getKey(), header.getValue());
        }
        final String info = this.home.getUserInfo();
        if (info != null) {
            final String[] parts = info.split(":", 2);
            try {
                builder.header(
                    HttpHeaders.AUTHORIZATION,
                    Logger.format(
                        "Basic %s",
                        Base64.encodeBase64String(
                            Logger.format(
                                "%s:%s",
                                URLDecoder.decode(parts[0], CharEncoding.UTF_8),
                                URLDecoder.decode(parts[1], CharEncoding.UTF_8)
                            ).getBytes()
                        )
                    )
                );
            } catch (java.io.UnsupportedEncodingException ex) {
                throw new IllegalStateException(ex);
            }
        }
        ClientResponse resp;
        if (RestTester.GET.equals(name)) {
            resp = builder.get(ClientResponse.class);
        } else {
            resp = builder.method(name, ClientResponse.class, body);
        }
        Logger.info(
            this,
            "#%s('%s'): \"%s\" completed in %[nano]s [%d %s]: %s",
            name,
            this.home.getPath(),
            desc,
            System.nanoTime() - start,
            resp.getStatus(),
            resp.getClientResponseStatus(),
            this.home
        );
        return resp;
    }

    /**
     * One header.
     */
    private static final class Header
        extends AbstractMap.SimpleEntry<String, String> {
        /**
         * Public ctor.
         * @param key The name of it
         * @param value The value
         */
        public Header(final String key, final String value) {
            super(key, value);
        }
    }

}
