/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.submarine.process.bpmn2;

import java.util.Map;

import org.kie.api.runtime.process.ProcessRuntime;
import org.kie.kogito.process.impl.AbstractProcess;
import org.kie.kogito.process.impl.AbstractProcessInstance;

public class BpmnProcessInstance extends AbstractProcessInstance<BpmnVariables> {

    public BpmnProcessInstance(AbstractProcess<BpmnVariables> process, BpmnVariables variables, ProcessRuntime rt) {
        super(process, variables, rt);
    }

    @Override
    protected Map<String, Object> bind(BpmnVariables variables) {
        
        if (variables == null) {
            return null;
        }
        return variables.toMap();
    }

    @Override
    protected void unbind(BpmnVariables variables, Map<String, Object> vmap) {
        
        if (variables == null) {
            return;
        }
        variables.fromMap(vmap);
    }
}