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

package org.apache.streampipes.container.api;

import org.apache.streampipes.commons.exceptions.SpRuntimeException;
import org.apache.streampipes.container.declarer.Declarer;
import org.apache.streampipes.container.declarer.InvocableDeclarer;
import org.apache.streampipes.container.declarer.SemanticEventProcessingAgentDeclarer;
import org.apache.streampipes.container.init.RunningInstances;
import org.apache.streampipes.model.Response;
import org.apache.streampipes.model.base.InvocableStreamPipesEntity;
import org.apache.streampipes.model.runtime.RuntimeOptionsRequest;
import org.apache.streampipes.model.runtime.RuntimeOptionsResponse;
import org.apache.streampipes.model.staticproperty.Option;
import org.apache.streampipes.rest.shared.annotation.JacksonSerialized;
import org.apache.streampipes.sdk.extractor.AbstractParameterExtractor;
import org.apache.streampipes.sdk.extractor.StaticPropertyExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;

public abstract class InvocablePipelineElementResource<I extends InvocableStreamPipesEntity, D extends Declarer<?>,
        P extends AbstractParameterExtractor<I>> extends AbstractPipelineElementResource<D> {

    private static final Logger LOG = LoggerFactory.getLogger(InvocablePipelineElementResource.class);

    protected abstract Map<String, D> getElementDeclarers();
    protected abstract String getInstanceId(String uri, String elementId);

    protected Class<I> clazz;

    public InvocablePipelineElementResource(Class<I> clazz) {
        this.clazz = clazz;
    }

    @POST
    @Path("{elementId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @JacksonSerialized
    public javax.ws.rs.core.Response invokeRuntime(@PathParam("elementId") String elementId, I graph) {

        try {
            if (isDebug()) {
              graph = createGroundingDebugInformation(graph);
            }

            InvocableDeclarer declarer = (InvocableDeclarer) getDeclarerById(elementId);

            if (declarer != null) {
                String runningInstanceId = getInstanceId(graph.getElementId(), elementId);
                if (!RunningInstances.INSTANCE.exists(runningInstanceId)) {
                    RunningInstances.INSTANCE.add(runningInstanceId, graph, declarer.getClass().newInstance());
                    Response resp = RunningInstances.INSTANCE.getInvocation(runningInstanceId).invokeRuntime(graph);
                    if(resp.isSuccess() && graph.getState() != null && graph.isStateful()){
                        //TODO: Handle fail while setting state
                        setState(elementId, runningInstanceId, graph.getState());
                    }
                    return ok(resp);
                } else {
                    LOG.info("Pipeline element {} with id {} seems to be already running, skipping invocation request.", graph.getName(), runningInstanceId);
                    Response resp = new Response(graph.getElementId(), true, "Pipeline element already running");
                    return ok(resp);
                }

            }
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
            return ok(new Response(elementId, false, e.getMessage()));
        }

        return ok(new Response(elementId, false, "Could not find the element with id: " + elementId));
    }

    @POST
    @Path("{elementId}/configurations")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @JacksonSerialized
    public javax.ws.rs.core.Response fetchConfigurations(@PathParam("elementId") String elementId,
                                                         RuntimeOptionsRequest runtimeOptionsRequest) {

        ResolvesContainerProvidedOptions resolvesOptions = (ResolvesContainerProvidedOptions) getDeclarerById(elementId);

        List<Option> availableOptions =
                resolvesOptions.resolveOptions(runtimeOptionsRequest.getRequestId(),
                StaticPropertyExtractor.from(
                        runtimeOptionsRequest.getStaticProperties(),
                        runtimeOptionsRequest.getInputStreams(),
                        runtimeOptionsRequest.getAppId()
                ));

        return ok(new RuntimeOptionsResponse(runtimeOptionsRequest, availableOptions));
    }

    @POST
    @Path("{elementId}/output")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @JacksonSerialized
    public javax.ws.rs.core.Response fetchOutputStrategy(@PathParam("elementId") String elementId, I runtimeOptionsRequest) {
        try {
            //I runtimeOptionsRequest = JacksonSerializer.getObjectMapper().readValue(payload, clazz);
            ResolvesContainerProvidedOutputStrategy<I, P> resolvesOutput =
                    (ResolvesContainerProvidedOutputStrategy<I, P>)
                            getDeclarerById
                                    (elementId);
            return ok(resolvesOutput.resolveOutputStrategy
                    (runtimeOptionsRequest, getExtractor(runtimeOptionsRequest)));
        } catch (SpRuntimeException e) {
            e.printStackTrace();
            return ok(new Response(elementId, false));
        }
    }


    // TODO move endpoint to /elementId/instances/runningInstanceId
    @DELETE
    @Path("{elementId}/{runningInstanceId}")
    @Produces(MediaType.APPLICATION_JSON)
    public javax.ws.rs.core.Response detach(@PathParam("elementId") String elementId, @PathParam("runningInstanceId") String runningInstanceId) {

        InvocableDeclarer runningInstance = RunningInstances.INSTANCE.getInvocation(runningInstanceId);

        if (runningInstance != null) {
            Response resp = runningInstance.detachRuntime(runningInstanceId);

            if (resp.isSuccess()) {
                RunningInstances.INSTANCE.remove(runningInstanceId);
            }

            return ok(resp);
        }

        return ok(new Response(elementId, false, "Could not find the running instance with id: " + runningInstanceId));
    }


    @GET
    @Path("{elementId}/{runningInstanceId}/state")
    @Produces(MediaType.APPLICATION_JSON)
    public javax.ws.rs.core.Response getState(@PathParam("elementId") String elementId, @PathParam("runningInstanceId") String runningInstanceId) {

        InvocableDeclarer runningInstance = RunningInstances.INSTANCE.getInvocation(runningInstanceId);


        if (runningInstance != null && runningInstance instanceof SemanticEventProcessingAgentDeclarer) {
            String serializedState = ((SemanticEventProcessingAgentDeclarer) runningInstance).getState();
            
            return ok(new Response(elementId, true, serializedState));
        }

        return ok(new Response(elementId, false, "Could not find the running instance with id: " + runningInstanceId));
    }

    @PUT
    @Path("{elementId}/{runningInstanceId}/state")
    @Produces(MediaType.APPLICATION_JSON)
    public javax.ws.rs.core.Response setState(@PathParam("elementId") String elementId, @PathParam("runningInstanceId") String runningInstanceId, String payload) {

        InvocableDeclarer runningInstance = RunningInstances.INSTANCE.getInvocation(runningInstanceId);


        if (runningInstance != null && runningInstance instanceof SemanticEventProcessingAgentDeclarer) {
            ((SemanticEventProcessingAgentDeclarer) runningInstance).setState(payload);
            return ok(new Response(elementId, true, "State update successful."));
        }

        return ok(new Response(elementId, false, "Could not find the running instance with id: " + runningInstanceId));
    }


    protected abstract P getExtractor(I graph);

    protected abstract I createGroundingDebugInformation(I graph);

    private Boolean isDebug() {
        return "true".equals(System.getenv("SP_DEBUG"));
    }
}

