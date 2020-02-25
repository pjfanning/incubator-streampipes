/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.apache.streampipes.model.node;

import java.util.List;

public class NodeInfo {

    private String nodeId;
    private NodeMetadata nodeMetadata;
    private NodeBrokerInfo nodeBrokerInfo;

    private List<NodeHardwareCapability> nodeHardwareCapabilities;

    private List<String> supportedPipelineElementAppIds;

    public NodeInfo() {

    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public NodeMetadata getNodeMetadata() {
        return nodeMetadata;
    }

    public void setNodeMetadata(NodeMetadata nodeMetadata) {
        this.nodeMetadata = nodeMetadata;
    }

    public List<NodeHardwareCapability> getNodeHardwareCapabilities() {
        return nodeHardwareCapabilities;
    }

    public void setNodeHardwareCapabilities(List<NodeHardwareCapability> nodeHardwareCapabilities) {
        this.nodeHardwareCapabilities = nodeHardwareCapabilities;
    }

    public List<String> getSupportedPipelineElementAppIds() {
        return supportedPipelineElementAppIds;
    }

    public void setSupportedPipelineElementAppIds(List<String> supportedPipelineElementAppIds) {
        this.supportedPipelineElementAppIds = supportedPipelineElementAppIds;
    }

    public NodeBrokerInfo getNodeBrokerInfo() {
        return nodeBrokerInfo;
    }

    public void setNodeBrokerInfo(NodeBrokerInfo nodeBrokerInfo) {
        this.nodeBrokerInfo = nodeBrokerInfo;
    }
}
