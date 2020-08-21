package org.hisp.dhis.servlet.filter;

/*
 * Copyright (c) 2004-2020, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.hisp.dhis.appmanager.AppManager;

/**
 * @author Austin McGee <austin@dhis2.org>
 */
 @Slf4j
public class AppOverrideFilter
    implements Filter
{
    @Autowired
    private AppManager appManager;

    public static final String[] BUNDLED_APPS = {
        // Javascript apps
        "app-management",
        "cache-cleaner",
        "capture",
        "dashboard",
        "data-administration",
        "data-visualizer",
        "data-quality",
        "datastore",
        "event-reports",
        "event-visualizer",
        "import-export",
        "interpretation",
        "maintenance",
        "maps",
        "menu-management",
        "messaging",
        "pivot",
        "reports",
        "scheduler",
        "settings",
        "tracker-capture",
        "translations",
        "usage-analytics",
        "user",
        "user-profile",
        
        // Struts apps
        "approval",
        "dataentry",
        // "maintenance",
    };

    // -------------------------------------------------------------------------
    // Filter implementation
    // -------------------------------------------------------------------------

    @Override
    public void init( FilterConfig config )
    {
    }

    @Override
    public void doFilter( ServletRequest req, ServletResponse res, FilterChain chain )
        throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        String requestURI = request.getRequestURI();

        List<String> bundledApps = Arrays.asList(BUNDLED_APPS);
        String pattern = "^/dhis-web-(" + String.join("|", bundledApps) + ")";

        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(requestURI);

        if (m.matches()) {
            String namespace = m.group(0);
            String appName = m.group(1);

            log.debug("AppOverrideFilter :: Matched for URI " + requestURI);

            if (appManager.exists(appName)) {
                String newURI = "/api/apps/" + appName + requestURI.substring(namespace.length());

                log.info("AppOverrideFilter :: Overridden app " + appName + " found, forwarding to " + newURI);

                req.getRequestDispatcher(newURI).forward(req, res);
                return;
            } else {
                log.info("AppOverrideFilter :: App " + appName + " not found, falling back to bundled app");
            }
        }

        chain.doFilter(req, res);
    }

    @Override
    public void destroy()
    {
    }
}
