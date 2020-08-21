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

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
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
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.util.StreamUtils;

import org.hisp.dhis.util.DateUtils;
import org.hisp.dhis.appmanager.App;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.appmanager.AppStatus;

/**
 * @author Austin McGee <austin@dhis2.org>
 */
 @Slf4j
public class AppOverrideFilter
    implements Filter
{
    @Autowired
    private AppManager appManager;

    @Autowired
    private ObjectMapper jsonMapper;

    // -------------------------------------------------------------------------
    // Filter implementation
    // -------------------------------------------------------------------------

    @Override
    public void init( FilterConfig config )
    {
    }

    // From AppController.java (some duplication)
    private void serveInstalledAppResource( App app, String resourcePath, HttpServletRequest request, HttpServletResponse response)
        throws IOException {
        // Get page requested

        log.debug( String.format( "Serving app resource: '%s'", resourcePath ) );

        // Handling of 'manifest.webapp'
        if ( "manifest.webapp".equals( resourcePath ) )
        {
            // If request was for manifest.webapp, check for * and replace with host
            if ( "*".equals( app.getActivities().getDhis().getHref() ) )
            {
                String contextPath = "../";
                log.debug( String.format( "Manifest context path: '%s'", contextPath ) );
                app.getActivities().getDhis().setHref( contextPath );
            }

            jsonMapper.writeValue( response.getOutputStream(), app );
        }
        // Any other resource
        else
        {
            // Retrieve file
            Resource resource = appManager.getAppResource( app, resourcePath );

            if ( resource == null )
            {
                response.sendError( HttpServletResponse.SC_NOT_FOUND );
                return;
            }

            String filename = resource.getFilename();
            log.debug( String.format( "App filename: '%s'", filename ) );

            if ( new ServletWebRequest( request, response ).checkNotModified( resource.lastModified() ) )
            {
                response.setStatus( HttpServletResponse.SC_NOT_MODIFIED );
                return;
            }

            String mimeType = request.getSession().getServletContext().getMimeType( filename );

            if ( mimeType != null )
            {
                response.setContentType( mimeType );
            }

            response.setContentLength( (int) resource.contentLength() );
            response.setHeader( "Last-Modified", DateUtils.getHttpDateString( new Date( resource.lastModified() ) ) );
            StreamUtils.copy( resource.getInputStream(), response.getOutputStream() );
        }
    }

    @Override
    public void doFilter( ServletRequest req, ServletResponse res, FilterChain chain )
        throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String requestURI = request.getRequestURI();

        List<String> bundledApps = Arrays.asList(AppManager.BUNDLED_APPS);
        String pattern = "^/dhis-web-(" + String.join("|", bundledApps) + ")(?:/|$)(.*)";

        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(requestURI);

        log.debug("AppOverrideFilter :: Testing " + requestURI +" against pattern " + pattern);

        if (m.find()) {
            String namespace = m.group(0);
            String appName = m.group(1);
            String resourcePath = m.group(2);

            log.info("AppOverrideFilter :: Matched for URI " + requestURI);

            App app = appManager.getApp(appName);

            if (app != null && app.getAppState() != AppStatus.DELETION_IN_PROGRESS) {
                log.info("AppOverrideFilter :: Overridden app " + appName + " found, serving override");
                serveInstalledAppResource(app, resourcePath, request, response);
                // String newURI = "/api/apps/" + appName + requestURI.substring(namespace.length());

                // req.getRequestDispatcher(newURI).forward(req, res);
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
