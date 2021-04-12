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
package org.apache.streampipes.config.backend;

import java.util.Arrays;
import java.util.List;

public class MessagingSettings {

  private Integer batchSize;
  private Integer messageMaxBytes;
  private Integer lingerMs;
  private Integer acks;

  private List<SpDataFormat> prioritizedFormats;
  private List<SpProtocol> prioritizedProtocols;
  private List<SpEdgeNodeProtocol> prioritizedEdgeProtocols;

  public static MessagingSettings fromDefault() {
    List<SpProtocol> prioritzedProtocolList;
    if (System.getenv(BackendConfigKeys.PRIORITIZED_PROTOCOL) != null) {
      switch (System.getenv(BackendConfigKeys.PRIORITIZED_PROTOCOL).toLowerCase()) {
        case "mqtt":
          prioritzedProtocolList = Arrays.asList(SpProtocol.MQTT, SpProtocol.KAFKA, SpProtocol.JMS);
          break;
        case "kafka":
          prioritzedProtocolList = Arrays.asList(SpProtocol.KAFKA, SpProtocol.MQTT, SpProtocol.JMS);
          break;
        case "jms":
          prioritzedProtocolList = Arrays.asList(SpProtocol.JMS, SpProtocol.KAFKA, SpProtocol.MQTT);
          break;
        default:
          prioritzedProtocolList = Arrays.asList(SpProtocol.KAFKA, SpProtocol.MQTT, SpProtocol.JMS);
      }
    } else {
      prioritzedProtocolList = Arrays.asList(SpProtocol.KAFKA, SpProtocol.MQTT, SpProtocol.JMS);
    }

    List<SpEdgeNodeProtocol> prioritizedEdgeProtocolList;
    if (System.getenv(BackendConfigKeys.PRIORITIZED_EDGE_PROTOCOL) != null) {
      switch (System.getenv(BackendConfigKeys.PRIORITIZED_EDGE_PROTOCOL).toLowerCase()) {
        case "mqtt":
          prioritizedEdgeProtocolList = Arrays.asList(SpEdgeNodeProtocol.MQTT, SpEdgeNodeProtocol.KAFKA);
          break;
        case "kafka":
          prioritizedEdgeProtocolList = Arrays.asList(SpEdgeNodeProtocol.KAFKA, SpEdgeNodeProtocol.MQTT);
          break;
        default:
          prioritizedEdgeProtocolList = Arrays.asList(SpEdgeNodeProtocol.MQTT, SpEdgeNodeProtocol.KAFKA);
      }
    } else {
      prioritizedEdgeProtocolList = Arrays.asList(SpEdgeNodeProtocol.MQTT, SpEdgeNodeProtocol.KAFKA);
    }

    return new MessagingSettings(
            1638400, 5000012, 20, 2,
            Arrays.asList(SpDataFormat.JSON, SpDataFormat.CBOR, SpDataFormat.FST, SpDataFormat.SMILE),
            prioritzedProtocolList, prioritizedEdgeProtocolList);
  }

  public MessagingSettings(Integer batchSize, Integer messageMaxBytes, Integer lingerMs,
                           Integer acks, List<SpDataFormat> prioritizedFormats,
                           List<SpProtocol> prioritizedProtocols, List<SpEdgeNodeProtocol> prioritizedEdgeProtocols) {
    this.batchSize = batchSize;
    this.messageMaxBytes = messageMaxBytes;
    this.lingerMs = lingerMs;
    this.acks = acks;
    this.prioritizedFormats = prioritizedFormats;
    this.prioritizedProtocols = prioritizedProtocols;
    this.prioritizedEdgeProtocols = prioritizedEdgeProtocols;
  }

  public MessagingSettings() {

  }

  public Integer getBatchSize() {
    return batchSize;
  }

  public void setBatchSize(Integer batchSize) {
    this.batchSize = batchSize;
  }

  public Integer getMessageMaxBytes() {
    return messageMaxBytes;
  }

  public void setMessageMaxBytes(Integer messageMaxBytes) {
    this.messageMaxBytes = messageMaxBytes;
  }

  public Integer getLingerMs() {
    return lingerMs;
  }

  public void setLingerMs(Integer lingerMs) {
    this.lingerMs = lingerMs;
  }

  public Integer getAcks() {
    return acks;
  }

  public void setAcks(Integer acks) {
    this.acks = acks;
  }

  public List<SpDataFormat> getPrioritizedFormats() {
    return prioritizedFormats;
  }

  public void setPrioritizedFormats(List<SpDataFormat> prioritizedFormats) {
    this.prioritizedFormats = prioritizedFormats;
  }

  public List<SpProtocol> getPrioritizedProtocols() {
    return prioritizedProtocols;
  }

  public void setPrioritizedProtocols(List<SpProtocol> prioritizedProtocols) {
    this.prioritizedProtocols = prioritizedProtocols;
  }

  public List<SpEdgeNodeProtocol> getPrioritizedEdgeProtocols() {
    return prioritizedEdgeProtocols;
  }

  public void setPrioritizedEdgeProtocols(List<SpEdgeNodeProtocol> prioritizedEdgeProtocols) {
    this.prioritizedEdgeProtocols = prioritizedEdgeProtocols;
  }
}
