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

public interface WorkFlowExecutor {

    /**
     * Returns whether this executor can execute the request.
     * {@link #execute(org.wso2.carbon.workflow.mgt.bean.WorkFlowRequest)} will be called only when this return true
     *
     * @param workFlowRequest The request that need to be checked
     * @return
     */
    public abstract boolean canHandle(WorkFlowRequest workFlowRequest);
//todo: return a code, detail to decide whether retry?,...

    /**
     * Returns the priority level that would be the decider for the execution order
     * @return
     */
    public int getPriority();
//todo: add workflow request as a parameter

    /**
     * Execute the workflow. Once workflow is finish it should call the callback service
     * @param workFlowRequest
     * @throws WorkflowException
     */
    public void execute(WorkFlowRequest workFlowRequest) throws WorkflowException;
//todo: return a code, detail to decide whether retry?,...

    /**
     * Returns the name of the executor
     * @return
     */
    public String getName();
}