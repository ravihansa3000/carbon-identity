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

package org.wso2.carbon.identity.workflow.mgt.extension;

import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.workflow.mgt.WorkFlowExecutorManager;
import org.wso2.carbon.identity.workflow.mgt.util.WorkflowDataType;
import org.wso2.carbon.identity.workflow.mgt.bean.RequestParameter;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkFlowRequest;
import org.wso2.carbon.identity.workflow.mgt.exception.RuntimeWorkflowException;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractWorkflowRequestHandler implements WorkflowRequestHandler {

    /**
     * Used to skip the workflow execution on the successive call after workflow completion.
     */
    private static ThreadLocal<Boolean> workFlowCompleted = new ThreadLocal<Boolean>();

    public static void unsetWorkFlowCompleted() {

        AbstractWorkflowRequestHandler.workFlowCompleted.remove();
    }

    public static Boolean getWorkFlowCompleted() {

        return workFlowCompleted.get();
    }

    public static void setWorkFlowCompleted(Boolean workFlowCompleted) {

        AbstractWorkflowRequestHandler.workFlowCompleted.set(workFlowCompleted);
    }

    public boolean startWorkFlow(Map<String, Object> wfParams, Map<String, Object> nonWfParams)
            throws WorkflowException {

        if (isWorkflowCompleted()) {
            return true;
        }
        WorkFlowRequest workFlowRequest = new WorkFlowRequest();
        List<RequestParameter> parameters = new ArrayList<RequestParameter>(wfParams.size() + nonWfParams.size());
        for (Map.Entry<String, Object> paramEntry : wfParams.entrySet()) {
            parameters.add(getParameter(paramEntry.getKey(), paramEntry.getValue(), true));
        }
        for (Map.Entry<String, Object> paramEntry : nonWfParams.entrySet()) {
            parameters.add(getParameter(paramEntry.getKey(), paramEntry.getValue(), false));
        }
        workFlowRequest.setRequestParameters(parameters);
        workFlowRequest.setTenantId(CarbonContext.getThreadLocalCarbonContext().getTenantId());
        engageWorkflow(workFlowRequest);
        return false;
    }

    protected boolean isValueValid(String paramName, Object paramValue, String expectedType) {

        switch (expectedType) {
            case WorkflowDataType.BOOLEAN_TYPE:
                return paramValue instanceof Boolean;
            case WorkflowDataType.STRING_TYPE:
                return paramValue instanceof String;
            case WorkflowDataType.INTEGER_TYPE:
                return paramValue instanceof Integer || paramValue instanceof Long || paramValue instanceof Character ||
                        paramValue instanceof Byte || paramValue instanceof Short;
            case WorkflowDataType.DOUBLE_TYPE:
                return paramValue instanceof Float || paramValue instanceof Double;
            case WorkflowDataType.STRING_LIST_TYPE:
            case WorkflowDataType.DOUBLE_LIST_TYPE:
            case WorkflowDataType.INTEGER_LIST_TYPE:
            case WorkflowDataType.BOOLEAN_LIST_TYPE:
                return paramValue instanceof Collection;
            case WorkflowDataType.STRING_STRING_MAP_TYPE:
                return paramValue instanceof Map;
        }
        return false;
    }

    /**
     * Wraps the parameters to the WorkflowParameter
     *
     * @param name     Name of the parameter
     * @param value    Value of the parameter
     * @param required Whether it is required to sent to the workflow executor
     * @return
     */
    protected RequestParameter getParameter(String name, Object value, boolean required)
            throws RuntimeWorkflowException {

        RequestParameter parameter = new RequestParameter();
        parameter.setName(name);
        parameter.setValue(value);
        parameter.setRequiredInWorkflow(required);
        String valueType = getParamDefinitions().get(name);
        if (valueType == null || value == null) {
            //null value as param, or undefined param
            parameter.setValueType(WorkflowDataType.OTHER_TYPE);
        } else {
            if (isValueValid(name, value, valueType)) {
                parameter.setValueType(valueType);
            } else {
                throw new RuntimeWorkflowException("Invalid value for '" + name + "', Expected: '" + valueType + "', " +
                        "but was of " + value.getClass().getName());
            }
        }
        return parameter;
    }

    @Override
    public void engageWorkflow(WorkFlowRequest workFlowRequest) throws WorkflowException {

        workFlowRequest.setEventType(getEventId());
        WorkFlowExecutorManager.getInstance().executeWorkflow(workFlowRequest);
    }

    @Override
    public void onWorkflowCompletion(String status, WorkFlowRequest originalRequest, Map<String, Object>
            responseParams) throws WorkflowException {

        Map<String, Object> requestParams = new HashMap<String, Object>();
        for (RequestParameter parameter : originalRequest.getRequestParameters()) {
            requestParams.put(parameter.getName(), parameter.getValue());
        }
        if (retryNeedAtCallback()) {
            setWorkFlowCompleted(true);
        }
        onWorkflowCompletion(status, requestParams, responseParams, originalRequest.getTenantId());
    }

    /**
     * Callback method from the executor
     *
     * @param status                   The return status from the workflow executor
     * @param requestParams            The params that were in the original request
     * @param responseAdditionalParams The params sent from the workflow executor
     * @param tenantId
     */
    public abstract void onWorkflowCompletion(String status, Map<String, Object> requestParams, Map<String, Object>
            responseAdditionalParams, int tenantId) throws WorkflowException;

    /**
     * Whether the same request is initiated at the callback. If set to <code>true</code>, this will take actions to
     * skip the request initiated at the callback.
     * <b>Note:</b> Do not set this to true unless necessary, It will lead to memory leaks
     *
     * @return
     */
    public abstract boolean retryNeedAtCallback();

    public boolean isWorkflowCompleted() {

        if (retryNeedAtCallback() && getWorkFlowCompleted() != null && getWorkFlowCompleted()) {
            unsetWorkFlowCompleted();
            return true;
        } else return false;
    }
}
