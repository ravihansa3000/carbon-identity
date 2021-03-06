/*
 * Copyright (c) 2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.oauth.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.model.OAuthAppDO;
import org.wso2.carbon.identity.core.persistence.JDBCPersistenceManager;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.oauth.IdentityOAuthAdminException;
import org.wso2.carbon.identity.oauth.OAuthUtil;
import org.wso2.carbon.identity.oauth.common.OAuthConstants;
import org.wso2.carbon.identity.oauth.common.exception.InvalidOAuthClientException;
import org.wso2.carbon.identity.oauth.config.OAuthServerConfiguration;
import org.wso2.carbon.identity.oauth.internal.OAuthComponentServiceHolder;
import org.wso2.carbon.identity.oauth.tokenprocessor.PlainTextPersistenceProcessor;
import org.wso2.carbon.identity.oauth.tokenprocessor.TokenPersistenceProcessor;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.util.OAuth2Util;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC Based data access layer for OAuth Consumer Applications.
 */
public class OAuthAppDAO {

    public static final Log log = LogFactory.getLog(OAuthAppDAO.class);
    private TokenPersistenceProcessor persistenceProcessor;

    public OAuthAppDAO() {

        try {
            persistenceProcessor = OAuthServerConfiguration.getInstance().getPersistenceProcessor();
        } catch (IdentityOAuth2Exception e) {
            log.error("Error retrieving TokenPersistenceProcessor. Defaulting to PlainTextPersistenceProcessor");
            persistenceProcessor = new PlainTextPersistenceProcessor();
        }

    }

    public void addOAuthApplication(OAuthAppDO consumerAppDO) throws IdentityOAuthAdminException {
        Connection connection = null;
        PreparedStatement prepStmt = null;

        if (!isDuplicateApplication(consumerAppDO.getUserName(), consumerAppDO.getTenantId(), consumerAppDO)) {
            try {
                connection = JDBCPersistenceManager.getInstance().getDBConnection();
                prepStmt = connection.prepareStatement(SQLQueries.OAuthAppDAOSQLQueries.ADD_OAUTH_APP);
                prepStmt.setString(1, persistenceProcessor.getProcessedClientId(consumerAppDO.getOauthConsumerKey()));
                prepStmt.setString(2, persistenceProcessor.getProcessedClientSecret(consumerAppDO.getOauthConsumerSecret()));
                prepStmt.setString(3, consumerAppDO.getUserName());
                prepStmt.setInt(4, consumerAppDO.getTenantId());
                prepStmt.setString(5, consumerAppDO.getApplicationName());
                prepStmt.setString(6, consumerAppDO.getOauthVersion());
                prepStmt.setString(7, consumerAppDO.getCallbackUrl());
                prepStmt.setString(8, consumerAppDO.getGrantTypes());
                prepStmt.execute();
                connection.commit();

            } catch (IdentityException e) {
                String errorMsg = "Error when getting an Identity Persistence Store instance.";
                throw new IdentityOAuthAdminException(errorMsg, e);
            } catch (SQLException e) {
                throw new IdentityOAuthAdminException("Error when executing the SQL : " + SQLQueries.OAuthAppDAOSQLQueries.ADD_OAUTH_APP);
            } finally {
                IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
            }
        } else {
            throw new IdentityOAuthAdminException("Error when adding the consumer application. " +
                    "An application with the same name already exists.");
        }
    }

    public String[] addOAuthConsumer(String username, int tenantId) throws IdentityOAuthAdminException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        String sqlStmt = null;
        String consumerKey;
        String consumerSecret = OAuthUtil.getRandomNumber();

        do {
            consumerKey = OAuthUtil.getRandomNumber();
        }
        while (isDuplicateConsumer(consumerKey));

        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            sqlStmt = SQLQueries.OAuthAppDAOSQLQueries.ADD_OAUTH_CONSUMER;
            prepStmt = connection.prepareStatement(sqlStmt);
            prepStmt.setString(1, consumerKey);
            prepStmt.setString(2, consumerSecret);
            prepStmt.setString(3, username);
            prepStmt.setInt(4, tenantId);
            // it is assumed that the OAuth version is 1.0a because this is required with OAuth 1.0a
            prepStmt.setString(5, OAuthConstants.OAuthVersions.VERSION_1A);
            prepStmt.execute();

            connection.commit();

        } catch (IdentityException e) {
            throw new IdentityOAuthAdminException("Error when getting an Identity Persistence Store instance.", e);
        } catch (SQLException e) {
            throw new IdentityOAuthAdminException("Error when executing the SQL : " + sqlStmt, e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
        return new String[]{consumerKey, consumerSecret};
    }

    public OAuthAppDO[] getOAuthConsumerAppsOfUser(String username, int tenantId) throws IdentityOAuthAdminException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rSet = null;
        OAuthAppDO[] oauthAppsOfUser;

        try {
            RealmService realmService = OAuthComponentServiceHolder.getRealmService();
            String tenantDomain = realmService.getTenantManager().getDomain(tenantId);
            String tenantAwareUserName = MultitenantUtils.getTenantAwareUsername(username);
            String tenantUnawareUserName = tenantAwareUserName + "@" + tenantDomain;
            boolean isUsernameCaseSensitive = OAuth2Util.isUsernameCaseSensitive(tenantUnawareUserName);

            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            String sql = SQLQueries.OAuthAppDAOSQLQueries.GET_APPS_OF_USER_WITH_TENANTAWARE_OR_TENANTUNAWARE_USERNAME;
            if (!isUsernameCaseSensitive){
                sql = sql.replace("USERNAME", "LOWER(USERNAME)");
            }
            prepStmt = connection.prepareStatement(sql);
            if (isUsernameCaseSensitive){
                prepStmt.setString(1, tenantAwareUserName);
                prepStmt.setString(2, tenantUnawareUserName);
            }else {
                prepStmt.setString(1, tenantAwareUserName.toLowerCase());
                prepStmt.setString(2, tenantUnawareUserName.toLowerCase());
            }

            prepStmt.setInt(3, tenantId);

            rSet = prepStmt.executeQuery();
            List<OAuthAppDO> oauthApps = new ArrayList<OAuthAppDO>();
            while (rSet.next()) {
                if (rSet.getString(3) != null && rSet.getString(3).length() > 0) {
                    OAuthAppDO oauthApp = new OAuthAppDO();
                    oauthApp.setUserName(username);
                    oauthApp.setTenantId(tenantId);
                    oauthApp.setOauthConsumerKey(persistenceProcessor.getPreprocessedClientId(rSet.getString(1)));
                    oauthApp.setOauthConsumerSecret(persistenceProcessor.getPreprocessedClientSecret(rSet.getString(2)));
                    oauthApp.setApplicationName(rSet.getString(3));
                    oauthApp.setOauthVersion(rSet.getString(4));
                    oauthApp.setCallbackUrl(rSet.getString(5));
                    oauthApp.setGrantTypes(rSet.getString(6));
                    oauthApps.add(oauthApp);
                }
            }
            oauthAppsOfUser = oauthApps.toArray(new OAuthAppDO[oauthApps.size()]);
            connection.commit();
        } catch (IdentityException e) {
            throw new IdentityOAuthAdminException("Error when getting an Identity Persistence Store instance.", e);
        } catch (SQLException e) {
            throw new IdentityOAuthAdminException("Error when executing the SQL : " + SQLQueries.OAuthAppDAOSQLQueries.GET_APPS_OF_USER, e);
        } catch (UserStoreException e) {
            throw new IdentityOAuthAdminException("Error while retrieving Tenant Domain for tenant ID : " + tenantId, e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, rSet, prepStmt);
        }
        return oauthAppsOfUser;
    }

    public OAuthAppDO getAppInformation(String consumerKey) throws InvalidOAuthClientException, IdentityOAuth2Exception {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rSet = null;
        OAuthAppDO oauthApp = null;

        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            prepStmt = connection.prepareStatement(SQLQueries.OAuthAppDAOSQLQueries.GET_APP_INFO);
            prepStmt.setString(1, persistenceProcessor.getProcessedClientId(consumerKey));

            rSet = prepStmt.executeQuery();
            List<OAuthAppDO> oauthApps = new ArrayList<>();
            /**
             * We need to determine whether the result set has more than 1 row. Meaning, we found an application for
             * the given consumer key. There can be situations where a user passed a key which doesn't yet have an
             * associated application. We need to barf with a meaningful error message for this case
             */
            boolean rSetHasRows = false;
            while (rSet.next()) {
                // There is at least one application associated with a given key
                rSetHasRows = true;
                if (rSet.getString(4) != null && rSet.getString(4).length() > 0) {
                    oauthApp = new OAuthAppDO();
                    oauthApp.setOauthConsumerKey(consumerKey);
                    oauthApp.setOauthConsumerSecret(persistenceProcessor.getPreprocessedClientSecret(rSet.getString(1)));
                    oauthApp.setUserName(rSet.getString(2));
                    oauthApp.setApplicationName(rSet.getString(3));
                    oauthApp.setOauthVersion(rSet.getString(4));
                    oauthApp.setCallbackUrl(rSet.getString(5));
                    oauthApp.setTenantId(rSet.getInt(6));
                    oauthApp.setGrantTypes(rSet.getString(7));
                    oauthApps.add(oauthApp);
                }
            }
            if (!rSetHasRows) {
                /**
                 * We come here because user submitted a key that doesn't have any associated application with it.
                 * We're throwing an error here because we cannot continue without this info. Otherwise it'll throw
                 * a null values not supported error when it tries to cache this info
                 */

                throw new InvalidOAuthClientException("Cannot find an application associated with the given consumer key : " + consumerKey);
            }
            connection.commit();
        } catch (IdentityException e) {
            throw new IdentityOAuth2Exception("Error while retrieving database connection" ,e);
        } catch (SQLException e) {
            throw new IdentityOAuth2Exception("Error while retrieving the app information", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, rSet, prepStmt);
        }
        return oauthApp;
    }

    public OAuthAppDO getAppInformationByAppName(String appName) throws InvalidOAuthClientException, IdentityOAuth2Exception {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rSet = null;
        OAuthAppDO oauthApp = null;

        try {
            int tenantID = CarbonContext.getThreadLocalCarbonContext().getTenantId();
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            prepStmt = connection.prepareStatement(SQLQueries.OAuthAppDAOSQLQueries.GET_APP_INFO_BY_APP_NAME);
            prepStmt.setString(1, appName);
            prepStmt.setInt(2, tenantID);

            rSet = prepStmt.executeQuery();
            List<OAuthAppDO> oauthApps = new ArrayList<>();
            oauthApp = new OAuthAppDO();
            oauthApp.setApplicationName(appName);
            oauthApp.setTenantId(tenantID);
            /**
             * We need to determine whether the result set has more than 1 row. Meaning, we found an application for
             * the given consumer key. There can be situations where a user passed a key which doesn't yet have an
             * associated application. We need to barf with a meaningful error message for this case
             */
            boolean rSetHasRows = false;
            while (rSet.next()) {
                // There is at least one application associated with a given key
                rSetHasRows = true;
                if (rSet.getString(4) != null && rSet.getString(4).length() > 0) {
                    oauthApp.setOauthConsumerSecret(persistenceProcessor.getPreprocessedClientSecret(rSet.getString(1)));
                    oauthApp.setUserName(rSet.getString(2));
                    oauthApp.setOauthConsumerKey(persistenceProcessor.getPreprocessedClientSecret(rSet.getString(3)));
                    oauthApp.setOauthVersion(rSet.getString(4));
                    oauthApp.setCallbackUrl(rSet.getString(5));
                    oauthApp.setGrantTypes(rSet.getString(6));
                    oauthApps.add(oauthApp);
                }
            }
            if (!rSetHasRows) {
                /**
                 * We come here because user submitted a key that doesn't have any associated application with it.
                 * We're throwing an error here because we cannot continue without this info. Otherwise it'll throw
                 * a null values not supported error when it tries to cache this info
                 */
                String message = "Cannot find an application associated with the given consumer key : " + appName;
                log.debug(message);
                throw new InvalidOAuthClientException(message);
            }
            connection.commit();
        } catch (IdentityException e) {
            throw new IdentityOAuth2Exception("Error while retrieving database connection", e);
        } catch (SQLException e) {
            throw new IdentityOAuth2Exception("Error while retrieving the app information", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, rSet, prepStmt);
        }
        return oauthApp;
    }

    public void updateConsumerApplication(OAuthAppDO oauthAppDO) throws IdentityOAuthAdminException {
        Connection connection = null;
        PreparedStatement prepStmt = null;

        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            prepStmt = connection.prepareStatement(SQLQueries.OAuthAppDAOSQLQueries.UPDATE_CONSUMER_APP);
            prepStmt.setString(1, oauthAppDO.getApplicationName());
            prepStmt.setString(2, oauthAppDO.getCallbackUrl());
            prepStmt.setString(3, oauthAppDO.getGrantTypes());
            prepStmt.setString(4, persistenceProcessor.getProcessedClientId(oauthAppDO.getOauthConsumerKey()));
            prepStmt.setString(5, persistenceProcessor.getProcessedClientSecret(oauthAppDO.getOauthConsumerSecret()));

            int count = prepStmt.executeUpdate();
            if (log.isDebugEnabled()) {
                log.debug("No. of records updated for updating consumer application. : " + count);
            }
            connection.commit();

        } catch (IdentityException e) {
            throw new IdentityOAuthAdminException("Error when getting an Identity Persistence Store instance.", e);
        } catch (SQLException e) {
            throw new IdentityOAuthAdminException("Error when executing the SQL : " + SQLQueries.OAuthAppDAOSQLQueries.UPDATE_CONSUMER_APP, e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
    }

    public void removeConsumerApplication(String consumerKey) throws IdentityOAuthAdminException {
        Connection connection = null;
        PreparedStatement prepStmt = null;

        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            prepStmt = connection.prepareStatement(SQLQueries.OAuthAppDAOSQLQueries.REMOVE_APPLICATION);
            prepStmt.setString(1, consumerKey);

            prepStmt.execute();
            connection.commit();

        } catch (IdentityException e) {
            throw new IdentityOAuthAdminException("Error when getting an Identity Persistence Store instance.", e);
        } catch (SQLException e) {;
            throw new IdentityOAuthAdminException("Error when executing the SQL : " + SQLQueries.OAuthAppDAOSQLQueries.REMOVE_APPLICATION, e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
    }

    private boolean isDuplicateApplication(String username, int tenantId, OAuthAppDO consumerAppDTO) throws IdentityOAuthAdminException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rSet = null;

        boolean isDuplicateApp = false;
        boolean isUsernameCaseSensitive = OAuth2Util.isUsernameCaseSensitive(username);

        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            String sql = SQLQueries.OAuthAppDAOSQLQueries.CHECK_EXISTING_APPLICATION;
            if (!isUsernameCaseSensitive){
                sql = sql.replace("USERNAME", "LOWER(USERNAME)");
            }
            prepStmt = connection.prepareStatement(sql);
            if (isUsernameCaseSensitive){
                prepStmt.setString(1, username);
            }else {
                prepStmt.setString(1, username.toLowerCase());
            }
            prepStmt.setInt(2, tenantId);
            prepStmt.setString(3, consumerAppDTO.getApplicationName());

            rSet = prepStmt.executeQuery();
            if (rSet.next()) {
                isDuplicateApp = true;
            }
            connection.commit();
        } catch (IdentityException e) {
            throw new IdentityOAuthAdminException("Error when getting an Identity Persistence Store instance.", e);
        } catch (SQLException e) {
            throw new IdentityOAuthAdminException("Error when executing the SQL : " + SQLQueries.OAuthAppDAOSQLQueries.CHECK_EXISTING_APPLICATION, e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, rSet, prepStmt);
        }
        return isDuplicateApp;
    }

    private boolean isDuplicateConsumer(String consumerKey) throws IdentityOAuthAdminException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rSet = null;

        boolean isDuplicateConsumer = false;

        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            prepStmt = connection.prepareStatement(SQLQueries.OAuthAppDAOSQLQueries.CHECK_EXISTING_CONSUMER);
            prepStmt.setString(1, persistenceProcessor.getProcessedClientId(consumerKey));

            rSet = prepStmt.executeQuery();
            if (rSet.next()) {
                isDuplicateConsumer = true;
            }
            connection.commit();
        } catch (IdentityException e) {
            throw new IdentityOAuthAdminException("Error when getting an Identity Persistence Store instance.", e);
        } catch (SQLException e) {
            throw new IdentityOAuthAdminException("Error when executing the SQL : " + SQLQueries.OAuthAppDAOSQLQueries.CHECK_EXISTING_CONSUMER);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, rSet, prepStmt);
        }
        return isDuplicateConsumer;
    }

}