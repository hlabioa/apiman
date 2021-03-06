/*
 * Copyright 2014 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.overlord.apiman.dt.ui.server.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.overlord.apiman.dt.ui.client.local.services.ConfigurationService;
import org.overlord.apiman.dt.ui.client.shared.beans.ApiAuthConfigurationBean;
import org.overlord.apiman.dt.ui.client.shared.beans.ApiAuthType;
import org.overlord.apiman.dt.ui.client.shared.beans.ApiConfigurationBean;
import org.overlord.apiman.dt.ui.client.shared.beans.AppConfigurationBean;
import org.overlord.apiman.dt.ui.client.shared.beans.BasicAuthCredentialsBean;
import org.overlord.apiman.dt.ui.client.shared.beans.BearerTokenCredentialsBean;
import org.overlord.apiman.dt.ui.client.shared.beans.ConfigurationBean;
import org.overlord.apiman.dt.ui.client.shared.beans.UserConfigurationBean;
import org.overlord.apiman.dt.ui.server.UIConfig;
import org.overlord.apiman.dt.ui.server.UIVersion;
import org.overlord.apiman.dt.ui.server.auth.ITokenGenerator;

/**
 * Generates the initial configuration JSON used by the UI when it
 * first loads up.  This initial JSON is loaded into the client-side
 * {@link ConfigurationService}.  Also responsible for pushing updated
 * configuration to the client if it changes.
 *
 * @author eric.wittmann@redhat.com
 */
public class ConfigurationServlet extends HttpServlet {
    
    private static final long serialVersionUID = -1529967410524613367L;

    /**
     * Constructor.
     */
    public ConfigurationServlet() {
    }
    
    /**
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            IOException {

        try {
            response.getOutputStream().write("var APIMAN_CONFIG_DATA = ".getBytes("UTF-8")); //$NON-NLS-1$ //$NON-NLS-2$
            JsonFactory f = new JsonFactory();
            JsonGenerator g = f.createJsonGenerator(response.getOutputStream(), JsonEncoding.UTF8);
            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(Inclusion.NON_NULL);
            g.setCodec(mapper);
            g.useDefaultPrettyPrinter();
            
            // Get data from various sources.
            String endpoint = UIConfig.config.getString(UIConfig.APIMAN_DT_UI_API_ENDPOINT);
            if (endpoint == null) {
                endpoint = getDefaultEndpoint(request);
            }
            UIVersion version = UIVersion.get();
            String authType = UIConfig.config.getString(UIConfig.APIMAN_DT_UI_API_AUTH_TYPE);
            
            ConfigurationBean configBean = new ConfigurationBean();
            configBean.setApiman(new AppConfigurationBean());
            configBean.setUser(new UserConfigurationBean());
            configBean.setApi(new ApiConfigurationBean());
            configBean.getApiman().setVersion(version.getVersionString());
            configBean.getApiman().setBuiltOn(version.getVersionDate());
            configBean.getApiman().setGatewayBaseUrl(UIConfig.config.getString(UIConfig.APIMAN_DT_UI_GATEWAY_URL, 
                    "http://localhost:8080/apiman-rt/gateway")); //$NON-NLS-1$
            configBean.getUser().setUsername(request.getRemoteUser());
            configBean.getApi().setEndpoint(endpoint);
            configBean.getApi().setAuth(new ApiAuthConfigurationBean());
            if (ApiAuthType.basic.toString().equals(authType)) {
                configBean.getApi().getAuth().setType(ApiAuthType.basic);
                configBean.getApi().getAuth().setBasic(new BasicAuthCredentialsBean());
                String username = UIConfig.config.getString(UIConfig.APIMAN_DT_UI_API_BASIC_AUTH_USER);
                String password = UIConfig.config.getString(UIConfig.APIMAN_DT_UI_API_BASIC_AUTH_PASS);
                configBean.getApi().getAuth().getBasic().setUsername(username);
                configBean.getApi().getAuth().getBasic().setPassword(password);
            } else if (ApiAuthType.bearerToken.toString().equals(authType)) {
                configBean.getApi().getAuth().setType(ApiAuthType.bearerToken);
                String tokenGeneratorClassName = UIConfig.config.getString(UIConfig.APIMAN_DT_UI_API_AUTH_TOKEN_GENERATOR);
                if (tokenGeneratorClassName == null)
                    throw new ServletException("No token generator class specified."); //$NON-NLS-1$
                Class<?> c = Class.forName(tokenGeneratorClassName);
                ITokenGenerator tokenGenerator = (ITokenGenerator) c.newInstance();
                configBean.getApi().getAuth().setBearerToken(new BearerTokenCredentialsBean());
                configBean.getApi().getAuth().getBearerToken().setToken(tokenGenerator.generateToken(request));
                configBean.getApi().getAuth().getBearerToken().setRefreshPeriod(tokenGenerator.getRefreshPeriod());
            } else if (ApiAuthType.samlBearerToken.toString().equals(authType)) {
                configBean.getApi().getAuth().setType(ApiAuthType.samlBearerToken);
                String tokenGeneratorClassName = UIConfig.config.getString(UIConfig.APIMAN_DT_UI_API_AUTH_TOKEN_GENERATOR);
                if (tokenGeneratorClassName == null)
                    throw new ServletException("No token generator class specified."); //$NON-NLS-1$
                Class<?> c = Class.forName(tokenGeneratorClassName);
                ITokenGenerator tokenGenerator = (ITokenGenerator) c.newInstance();
                configBean.getApi().getAuth().setBearerToken(new BearerTokenCredentialsBean());
                configBean.getApi().getAuth().getBearerToken().setToken(tokenGenerator.generateToken(request));
                configBean.getApi().getAuth().getBearerToken().setRefreshPeriod(tokenGenerator.getRefreshPeriod());
            } else if (ApiAuthType.authToken.toString().equals(authType)) {
                configBean.getApi().getAuth().setType(ApiAuthType.authToken);
                String tokenGeneratorClassName = UIConfig.config.getString(UIConfig.APIMAN_DT_UI_API_AUTH_TOKEN_GENERATOR);
                if (tokenGeneratorClassName == null)
                    throw new ServletException("No token generator class specified."); //$NON-NLS-1$
                Class<?> c = Class.forName(tokenGeneratorClassName);
                ITokenGenerator tokenGenerator = (ITokenGenerator) c.newInstance();
                configBean.getApi().getAuth().setBearerToken(new BearerTokenCredentialsBean());
                configBean.getApi().getAuth().getBearerToken().setToken(tokenGenerator.generateToken(request));
                configBean.getApi().getAuth().getBearerToken().setRefreshPeriod(tokenGenerator.getRefreshPeriod());
            }
            g.writeObject(configBean);
            
            g.flush();
            response.getOutputStream().write(";".getBytes("UTF-8")); //$NON-NLS-1$ //$NON-NLS-2$
            g.close();
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    /**
     * Gets the default API endpoint by using information the current {@link HttpServletRequest}.
     * @param request
     */
    private String getDefaultEndpoint(HttpServletRequest request) {
        StringBuilder builder = new StringBuilder();
        builder.append(request.getScheme())
               .append("://") //$NON-NLS-1$
               .append(request.getServerName())
               .append(":") //$NON-NLS-1$
               .append(request.getServerPort())
               .append("/apiman-dt-api"); //$NON-NLS-1$
        return builder.toString();
    }
}
