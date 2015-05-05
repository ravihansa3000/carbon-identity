/*
 * Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.oauth.cache;

import org.wso2.carbon.identity.core.model.OAuthAppDO;
import org.wso2.carbon.utils.CarbonUtils;

public class AppInfoCache extends BaseCache<String, OAuthAppDO>{

    private static final String OAUTH_APP_INFO_CACHE_NAME = "AppInfoCache";

    private static final AppInfoCache instance =
            new AppInfoCache(OAUTH_APP_INFO_CACHE_NAME);

    private AppInfoCache(String cacheName) {
        super(cacheName);
    }

    /**
     * Returns AppInfoCache instance
     *
     * @return instance of OAuthAppInfoCache
     */
    public static AppInfoCache getInstance(){
        CarbonUtils.checkSecurity();
        return instance;
    }

    @Override
    public void addToCache(String key, OAuthAppDO entry) {
        super.addToCache(key, entry);
    }

    @Override
    public OAuthAppDO getValueFromCache(String key) {
        return super.getValueFromCache(key);
    }

    @Override
    public void clearCacheEntry(String key) {
        super.clearCacheEntry(key);
    }
}