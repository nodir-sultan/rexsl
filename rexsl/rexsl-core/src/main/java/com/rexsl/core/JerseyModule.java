/**
 * Copyright (c) 2011, ReXSL.com
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

import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import com.ymock.util.Logger;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang.StringUtils;

/**
 * Basic configuration module.
 *
 * @author Yegor Bugayenko (yegor@rexsl.com)
 * @version $Id$
 */
public final class JerseyModule extends JerseyServletModule {

    /**
     * {@inheritDoc}
     */
    @Override
    protected void configureServlets() {
        this.filterRegex(
            String.format("^(?!%s)$", Settings.INSTANCE.excludes())
        ).through(XslBrowserFilter.class);
        final Map<String, String> args = new HashMap<String, String>();
        args.put(
            "com.sun.jersey.config.property.packages",
            StringUtils.join(Settings.INSTANCE.packages(), ",")
        );
        args.put(
            "com.sun.jersey.config.property.WebPageContentRegex",
            String.format("^(%s)$", Settings.INSTANCE.excludes())
        );
        args.put(
            "com.sun.jersey.config.feature.Redirect",
            Boolean.TRUE.toString()
        );
        args.put(
            "com.sun.jersey.config.feature.ImplicitViewables",
            Boolean.TRUE.toString()
        );
        this.filter("/*").through(GuiceContainer.class, args);
        Logger.info(
            this,
            "#configureServlets(): done"
        );
    }

}
