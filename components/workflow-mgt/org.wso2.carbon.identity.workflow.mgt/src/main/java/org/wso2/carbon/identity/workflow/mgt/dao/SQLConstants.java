/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.workflow.mgt.dao;

/**
 * Holds the SQL queries and constants
 */
public class SQLConstants {

    public static final String REQUEST_COLUMN = "REQUEST";
    public static final String DESCRIPTION_COLUMN = "DESCRIPTION";

    public static final String ID_COLUMN = "ID";
    public static final String WF_NAME_COLUMN = "WF_NAME";
    public static final String TEMPLATE_ID_COLUMN = "TEMPLATE_ID";
    public static final String TEMPLATE_IMPL_ID_COLUMN = "IMPL_ID";
    public static final String CONDITION_COLUMN = "ASSOC_CONDITION";
    public static final String PROFILE_NAME_COLUMN = "PROFILE_NAME";
    public static final String HOST_URL_COLUMN = "HOST_URL";
    public static final String USERNAME_COLUMN = "USERNAME";
    public static final String PASSWORD_COLUMN = "PASSWORD";
    public static final String CALLBACK_USER_COLUMN = "CALLBACK_USERNAME";
    public static final String CALLBACK_PASSWORD_COLUMN = "CALLBACK_PASSWORD";

    public static final String WORKFLOW_ID_COLUMN = "WORKFLOW_ID";
    public static final String EVENT_ID_COLUMN = "EVENT_ID";
    public static final String PARAM_NAME_COLUMN = "PARAM_NAME";
    public static final String PARAM_VALUE_COLUMN = "PARAM_VALUE";
    public static final String ASSOCIATION_NAME_COLUMN = "ASSOC_NAME";


    public static final String ADD_WORKFLOW_REQUEST_QUERY = "INSERT INTO WF_REQUEST(UUID, CREATED_AT, UPDATED_AT, " +
            "REQUEST, STATUS) VALUES (?, ?, ?, ?, ?)";

    public static final String GET_WORKFLOW_REQUEST_QUERY = "SELECT UUID, REQUEST, STATUS FROM WF_REQUEST WHERE UUID" +
            " = ?";

    public static final String ADD_WORKFLOW_QUERY = "INSERT INTO WF_WORKFLOW(ID, WF_NAME, DESCRIPTION, TEMPLATE_ID, " +
            "IMPL_ID, TENANT_ID) VALUES (?,?, ?, ?, ?, ?)";

    public static final String ADD_WORKFLOW_PARAMS_QUERY = "INSERT INTO WF_WORKFLOW_CONFIG_PARAM(WORKFLOW_ID, " +
            "PARAM_NAME, PARAM_VALUE) VALUES(?, ?, ?)";

    public static final String GET_ASSOCIATIONS_FOR_EVENT_QUERY = "SELECT WF_WORKFLOW_ASSOCIATION.WORKFLOW_ID, " +
            "WF_WORKFLOW.TEMPLATE_ID, WF_WORKFLOW.IMPL_ID, WF_WORKFLOW_ASSOCIATION.ASSOC_CONDITION FROM WF_WORKFLOW, " +
            "WF_WORKFLOW_ASSOCIATION WHERE WF_WORKFLOW_ASSOCIATION.EVENT_ID = ? AND WF_WORKFLOW_ASSOCIATION" +
            ".WORKFLOW_ID = WF_WORKFLOW.ID AND WF_WORKFLOW.TENANT_ID = ?";

    public static final String GET_ASSOCIATIONS_FOR_WORKFLOW_QUERY = "SELECT WF_WORKFLOW.WF_NAME, " +
            "WF_WORKFLOW_ASSOCIATION.ID, WF_WORKFLOW_ASSOCIATION.ASSOC_NAME, WF_WORKFLOW_ASSOCIATION.EVENT_ID, " +
            "WF_WORKFLOW_ASSOCIATION.ASSOC_CONDITION FROM WF_WORKFLOW, WF_WORKFLOW_ASSOCIATION WHERE WF_WORKFLOW.ID =" +
            " WF_WORKFLOW_ASSOCIATION.WORKFLOW_ID AND WF_WORKFLOW.ID = ?";

    public static final String GET_ALL_ASSOCIATIONS_QUERY = "SELECT WF_WORKFLOW.WF_NAME, WF_WORKFLOW_ASSOCIATION.ID, " +
            "WF_WORKFLOW_ASSOCIATION.ASSOC_NAME, WF_WORKFLOW_ASSOCIATION.EVENT_ID, WF_WORKFLOW_ASSOCIATION" +
            ".ASSOC_CONDITION FROM WF_WORKFLOW,WF_WORKFLOW_ASSOCIATION WHERE WF_WORKFLOW.ID = WF_WORKFLOW_ASSOCIATION" +
            ".WORKFLOW_ID";

    public static final String DELETE_ASSOCIATION_QUERY = "DELETE FROM WF_WORKFLOW_ASSOCIATION WHERE ID = ?";

    public static final String GET_WORKFLOW_PARAMS = "SELECT PARAM_NAME, PARAM_VALUE FROM WF_WORKFLOW_CONFIG_PARAM " +
            "WHERE WORKFLOW_ID = ?";

    public static final String DELETE_WORKFLOW_QUERY = "DELETE FROM WF_WORKFLOW WHERE ID = ?";

    public static final String ASSOCIATE_WF_TO_EVENT = "INSERT INTO WF_WORKFLOW_ASSOCIATION(EVENT_ID, " +
            "ASSOC_NAME, ASSOC_CONDITION, WORKFLOW_ID) VALUES (?, ?, ?, ?)";

    public static final String LIST_WORKFLOWS_QUERY = "SELECT ID, WF_NAME, DESCRIPTION, TEMPLATE_ID, IMPL_ID FROM " +
            "WF_WORKFLOW WHERE TENANT_ID = ? ORDER BY WF_NAME";

    //queries for BPS profiles
    public static final String ADD_BPS_PROFILE_QUERY =
            "INSERT INTO WF_BPS_PROFILE(PROFILE_NAME, HOST_URL, USERNAME, PASSWORD, CALLBACK_USERNAME," +
                    " CALLBACK_PASSWORD, TENANT_ID) VALUES (?, ?, ?, ?, ?, ?, ?)";

    public static final String GET_BPS_PROFILE_QUERY = "SELECT HOST_URL, USERNAME, PASSWORD, " +
            "CALLBACK_USERNAME, CALLBACK_PASSWORD FROM WF_BPS_PROFILE WHERE PROFILE_NAME = ?";

    public static final String LIST_BPS_PROFILES_QUERY =
            "SELECT PROFILE_NAME, HOST_URL, USERNAME, CALLBACK_USERNAME FROM " +
                    "WF_BPS_PROFILE WHERE TENANT_ID = ?";

    public static final String DELETE_BPS_PROFILES_QUERY = "DELETE FROM WF_BPS_PROFILE WHERE PROFILE_NAME = ?";

    private SQLConstants() {

    }

}
