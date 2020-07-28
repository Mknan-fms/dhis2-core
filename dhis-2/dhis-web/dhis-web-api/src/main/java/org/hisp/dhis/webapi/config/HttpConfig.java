package org.hisp.dhis.webapi.config;

import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.hisp.dhis.common.Compression;
import org.hisp.dhis.node.DefaultNodeService;
import org.hisp.dhis.node.NodeService;
import org.hisp.dhis.security.SecurityService;
import org.hisp.dhis.security.spring2fa.TwoFactorAuthenticationProvider;
import org.hisp.dhis.security.spring2fa.TwoFactorWebAuthenticationDetailsSource;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.webapi.handler.CustomExceptionMappingAuthenticationFailureHandler;
import org.hisp.dhis.webapi.handler.DefaultAuthenticationSuccessHandler;
import org.hisp.dhis.webapi.mvc.CustomRequestMappingHandlerMapping;
import org.hisp.dhis.webapi.mvc.messageconverter.CsvMessageConverter;
import org.hisp.dhis.webapi.mvc.messageconverter.ExcelMessageConverter;
import org.hisp.dhis.webapi.mvc.messageconverter.JsonMessageConverter;
import org.hisp.dhis.webapi.mvc.messageconverter.JsonPMessageConverter;
import org.hisp.dhis.webapi.mvc.messageconverter.PdfMessageConverter;
import org.hisp.dhis.webapi.mvc.messageconverter.RenderServiceMessageConverter;
import org.hisp.dhis.webapi.mvc.messageconverter.XmlMessageConverter;
import org.hisp.dhis.webapi.view.CustomPathExtensionContentNegotiationStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.FixedContentNegotiationStrategy;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;
import org.springframework.web.accept.ParameterContentNegotiationStrategy;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.parseMediaType;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Configuration
@Order( 1000 )
@ComponentScan( basePackages = { "org.hisp.dhis" } )
@EnableWebMvc
@EnableGlobalMethodSecurity( prePostEnabled = true )
@Slf4j
public class HttpConfig implements WebMvcConfigurer
{

//    @Autowired
//    private DefaultClientDetailsUserDetailsService defaultClientDetailsUserDetailsService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private SecurityService securityService;

//    @Autowired
//    private CustomLdapAuthenticationProvider customLdapAuthenticationProvider;

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler()
    {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setDefaultRolePrefix( "" );
        return expressionHandler;
    }

    @Bean
    public PasswordEncoder encoder()
    {
        return new BCryptPasswordEncoder();
    }

    @Autowired
    public void configureGlobal( AuthenticationManagerBuilder auth )
        throws Exception
    {
        TwoFactorAuthenticationProvider twoFactorAuthenticationProvider = new TwoFactorAuthenticationProvider();
        twoFactorAuthenticationProvider.setPasswordEncoder( encoder() );
        twoFactorAuthenticationProvider.setUserService( userService );
        twoFactorAuthenticationProvider.setUserDetailsService( userDetailsService );
        twoFactorAuthenticationProvider.setSecurityService( securityService );

        // configure the Authentication providers

        auth
            // Two factor
            .authenticationProvider( twoFactorAuthenticationProvider )
            // LDAP Authentication
//            .authenticationProvider( customLdapAuthenticationProvider )
            //  OAUTH2
            .userDetailsService( userDetailsService )
            // Use a non-encoding password for oauth2 secrets, since the secret is generated by the client
            .passwordEncoder( NoOpPasswordEncoder.getInstance() );
    }

    @Bean( "authenticationManager" )
    public AuthenticationManager authenticationManager( AuthenticationManagerBuilder auth )
    {
        return auth.getOrBuild();
    }

    @Bean
    public CustomExceptionMappingAuthenticationFailureHandler authenticationFailureHandler()
    {
        CustomExceptionMappingAuthenticationFailureHandler handler =
            new CustomExceptionMappingAuthenticationFailureHandler();

        handler.setExceptionMappings(
            ImmutableMap.of(
                "org.springframework.security.authentication.CredentialsExpiredException",
                "/dhis-web-commons/security/expired.action" ) );

        handler.setDefaultFailureUrl( "/dhis-web-commons/security/login.action?failed=true" );

        return handler;
    }

    @Bean
    public DefaultAuthenticationSuccessHandler authenticationSuccessHandler()
    {
        return new DefaultAuthenticationSuccessHandler();
    }

    @Bean
    public TwoFactorWebAuthenticationDetailsSource twoFactorWebAuthenticationDetailsSource()
    {
        return new TwoFactorWebAuthenticationDetailsSource();
    }

    @Bean
    public static SessionRegistryImpl sessionRegistry()
    {
        return new org.springframework.security.core.session.SessionRegistryImpl();
    }

//  <bean id="responseStatusExceptionResolver" class="org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver" />
//
//  <bean id="defaultHandlerExceptionResolver" class="org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver" />

    @Bean
    public NodeService nodeService()
    {
        return new DefaultNodeService();
    }

    @Override
    public void configureMessageConverters(
        List<HttpMessageConverter<?>> converters )
    {
        Arrays.stream( Compression.values() )
            .forEach( compression -> converters.add( new JsonMessageConverter( nodeService(), compression ) ) );
        Arrays.stream( Compression.values() )
            .forEach( compression -> converters.add( new XmlMessageConverter( nodeService(), compression ) ) );
        Arrays.stream( Compression.values() )
            .forEach( compression -> converters.add( new CsvMessageConverter( nodeService(), compression ) ) );

        converters.add( new JsonPMessageConverter( nodeService(), null ) );
        converters.add( new PdfMessageConverter( nodeService() ) );
        converters.add( new ExcelMessageConverter( nodeService() ) );

        converters.add( new StringHttpMessageConverter() );
        converters.add( new ByteArrayHttpMessageConverter() );
        converters.add( new FormHttpMessageConverter() );

        converters.add( renderServiceMessageConverter() );
    }

    @Bean
    public RenderServiceMessageConverter renderServiceMessageConverter()
    {
        return new RenderServiceMessageConverter();
    }


//    @Bean
//    public ContentNegotiationManager contentNegotiationManager(
//        CustomPathExtensionContentNegotiationStrategy customPathExtensionContentNegotiationStrategy,
//        ParameterContentNegotiationStrategy parameterContentNegotiationStrategy,
//        HeaderContentNegotiationStrategy headerContentNegotiationStrategy,
//        FixedContentNegotiationStrategy fixedContentNegotiationStrategy )
//    {
//        return new ContentNegotiationManager( Arrays.asList( customPathExtensionContentNegotiationStrategy,
//            parameterContentNegotiationStrategy, headerContentNegotiationStrategy, fixedContentNegotiationStrategy ) );
//    }

//
//    <bean id="renderServiceMessageConverter"
//    class="org.hisp.dhis.webapi.mvc.messageconverter.RenderServiceMessageConverter" />
//
//  <bean id="conversionService" class="org.springframework.format.support.FormattingConversionServiceFactoryBean" />

    @Bean
    public CustomRequestMappingHandlerMapping customRequestMappingHandlerMapping()
    {
        CustomPathExtensionContentNegotiationStrategy pathExtensionNegotiationStrategy =
            new CustomPathExtensionContentNegotiationStrategy( mediaTypeMap );
        pathExtensionNegotiationStrategy.setUseJaf( false );

        String[] mediaTypes = new String[] { "json", "jsonp", "xml", "png", "xls", "pdf", "csv" };

        ParameterContentNegotiationStrategy parameterContentNegotiationStrategy = new ParameterContentNegotiationStrategy(
            mediaTypeMap.entrySet().stream()
                .filter( x -> ArrayUtils.contains( mediaTypes, x.getKey() ) )
                .collect( Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue ) ) );

        HeaderContentNegotiationStrategy headerContentNegotiationStrategy = new HeaderContentNegotiationStrategy();
        FixedContentNegotiationStrategy fixedContentNegotiationStrategy = new FixedContentNegotiationStrategy(
            MediaType.APPLICATION_JSON );

        ContentNegotiationManager manager = new ContentNegotiationManager(
            Arrays.asList(
                pathExtensionNegotiationStrategy,
                parameterContentNegotiationStrategy,
                headerContentNegotiationStrategy,
                fixedContentNegotiationStrategy ) );

        CustomRequestMappingHandlerMapping mapping = new CustomRequestMappingHandlerMapping();
        mapping.setContentNegotiationManager( manager );

        return mapping;
    }

    private Map<String, MediaType> mediaTypeMap = new ImmutableMap.Builder<String, MediaType>()
        .put( "json", MediaType.APPLICATION_JSON )
        .put( "json.gz", parseMediaType( "application/json+gzip" ) )
        .put( "json.zip", parseMediaType( "application/json+zip" ) )
        .put( "jsonp", parseMediaType( "application/javascript" ) )
        .put( "xml", MediaType.APPLICATION_XML )
        .put( "xml.gz", parseMediaType( "application/xml+gzip" ) )
        .put( "xml.zip", parseMediaType( "application/xml+zip" ) )
        .put( "png", MediaType.IMAGE_PNG )
        .put( "pdf", MediaType.APPLICATION_PDF )
        .put( "xls", parseMediaType( "application/vnd.ms-excel" ) )
        .put( "xlsx", parseMediaType( "application/vnd.ms-excel" ) )
        .put( "csv", parseMediaType( "application/csv" ) )
        .put( "csv.gz", parseMediaType( "application/csv+gzip" ) )
        .put( "csv.zip", parseMediaType( "application/csv+zip" ) )
        .put( "geojson", parseMediaType( "application/json+geojson" ) )
        .build();

}