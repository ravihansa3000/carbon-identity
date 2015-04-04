/*
 * Copyright (c) 2015 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.workflow.mgt;

import org.wso2.carbon.workflow.mgt.bean.WorkFlowRequest;

public interface WorkflowRequestHandler {

    /**
     * Creates the workflow request for an event and engage it.
     *
     * @param workFlowRequest
     * @throws WorkflowException
     */
    public void engageWorkflow(WorkFlowRequest workFlowRequest) throws WorkflowException;

    /**
     * Gets the event that this handler is subscribed, used when handling the callback
     *
     * @return
     */
    public String getEventId();

    /**
     * Called on workflow completion
     *
     * @param status          The workflow status
     * @param originalRequest The original request that was made by
     *                        {@link #engageWorkflow(org.wso2.carbon.workflow.mgt.bean.WorkFlowRequest)}
     * @param additionalData  Additional data returned from workflow
     * @throws WorkflowException
     */
    public void onWorkflowCompletion(String status, WorkFlowRequest originalRequest, Object additionalData) throws
            WorkflowException;
}