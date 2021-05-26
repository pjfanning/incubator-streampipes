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
package org.apache.streampipes.backend;

import org.apache.shiro.web.env.EnvironmentLoaderListener;
import org.apache.shiro.web.servlet.OncePerRequestFilter;
import org.apache.shiro.web.servlet.ShiroFilter;
import org.apache.streampipes.commons.networking.Networking;
import org.apache.streampipes.manager.health.PipelineHealthCheck;
import org.apache.streampipes.manager.operations.Operations;
import org.apache.streampipes.model.pipeline.Pipeline;
import org.apache.streampipes.model.pipeline.PipelineOperationStatus;
import org.apache.streampipes.rest.notifications.NotificationListener;
import org.apache.streampipes.storage.api.IPipelineStorage;
import org.apache.streampipes.storage.management.StorageDispatcher;
import org.apache.streampipes.svcdiscovery.SpServiceDiscovery;
import org.apache.streampipes.svcdiscovery.SpServiceTags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.ServletContextListener;
import java.util.HashMap;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Configuration
@EnableAutoConfiguration
@Import({StreamPipesResourceConfig.class, WelcomePageController.class})
public class StreamPipesBackendApplication {

  private static final Logger LOG = LoggerFactory.getLogger(StreamPipesBackendApplication.class.getCanonicalName());

  private static final int MAX_PIPELINE_START_RETRIES = 3;
  private static final int WAIT_TIME_AFTER_FAILURE_IN_SECONDS = 10;

  private static final int HEALTH_CHECK_INTERVAL = 60;
  private static final TimeUnit HEALTH_CHECK_UNIT = TimeUnit.SECONDS;

  private ScheduledExecutorService executorService;
  private ScheduledExecutorService healthCheckExecutorService;

  private Map<String, Integer> failedPipelines = new HashMap<>();

  public static void main(String[] args) {
    System.setProperty("org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH", "true");
    try {
      String host = Networking.getHostname();
      Integer port = Networking.getPort(8030);
      SpServiceDiscovery
              .getServiceDiscovery()
              .registerService("core",
                      "core",
                      host,
                      port,
                      Arrays.asList(SpServiceTags.CORE, SpServiceTags.CONNECT_MASTER));

      SpringApplication.run(StreamPipesBackendApplication.class, args);
    } catch (UnknownHostException e) {
      LOG.error("Could not auto-resolve host address - please manually provide the hostname using the SP_HOST environment variable");
    }
  }

  @PostConstruct
  public void init() {
    this.executorService = Executors.newSingleThreadScheduledExecutor();
    this.healthCheckExecutorService = Executors.newSingleThreadScheduledExecutor();

    executorService.schedule(this::startAllPreviouslyStoppedPipelines, 5, TimeUnit.SECONDS);
    LOG.info("Pipeline health check will run every {} seconds", HEALTH_CHECK_INTERVAL);
    healthCheckExecutorService.scheduleAtFixedRate(new PipelineHealthCheck(),
            HEALTH_CHECK_INTERVAL,
            HEALTH_CHECK_INTERVAL,
            HEALTH_CHECK_UNIT);
  }

  private void schedulePipelineStart(Pipeline pipeline, boolean restartOnReboot) {
    executorService.schedule(() -> {
      startPipeline(pipeline, restartOnReboot);
    }, WAIT_TIME_AFTER_FAILURE_IN_SECONDS, TimeUnit.SECONDS);
  }

  @PreDestroy
  public void onExit() {
    LOG.info("Shutting down StreamPipes...");
    LOG.info("Flagging currently running pipelines for restart...");
    List<Pipeline> pipelinesToStop = getAllPipelines()
            .stream()
            .filter(Pipeline::isRunning)
            .collect(Collectors.toList());

    LOG.info("Found {} running pipelines which will be stopped...", pipelinesToStop.size());

    pipelinesToStop.forEach(pipeline -> {
      pipeline.setRestartOnSystemReboot(true);
      StorageDispatcher.INSTANCE.getNoSqlStore().getPipelineStorageAPI().updatePipeline(pipeline);
    });

    LOG.info("Gracefully stopping all running pipelines...");
    List<PipelineOperationStatus> status = Operations.stopAllPipelines(true);
    status.forEach(s -> {
      if (s.isSuccess()) {
        LOG.info("Pipeline {} successfully stopped", s.getPipelineName());
      } else {
        LOG.error("Pipeline {} could not be stopped", s.getPipelineName());
      }
    });

    LOG.info("Thanks for using Apache StreamPipes - see you next time!");
  }

  private void startAllPreviouslyStoppedPipelines() {
    LOG.info("Checking for orphaned pipelines...");
    List<Pipeline> orphanedPipelines = getAllPipelines()
            .stream()
            .filter(Pipeline::isRunning)
            .collect(Collectors.toList());

    LOG.info("Found {} orphaned pipelines", orphanedPipelines.size());

    orphanedPipelines.forEach(pipeline -> {
      LOG.info("Restoring orphaned pipeline {}", pipeline.getName());
      startPipeline(pipeline, false);
    });

    LOG.info("Checking for gracefully shut down pipelines to be restarted...");

    List<Pipeline> pipelinesToRestart = getAllPipelines()
            .stream()
            .filter(p -> !(p.isRunning()))
            .filter(Pipeline::isRestartOnSystemReboot)
            .collect(Collectors.toList());

    LOG.info("Found {} pipelines that we are attempting to restart...", pipelinesToRestart.size());

    pipelinesToRestart.forEach(pipeline -> {
      startPipeline(pipeline, false);
    });

    LOG.info("No more pipelines to restore...");
  }

  private void startPipeline(Pipeline pipeline, boolean restartOnReboot) {
    PipelineOperationStatus status = Operations.startPipeline(pipeline);
    if (status.isSuccess()) {
      LOG.info("Pipeline {} successfully restarted", status.getPipelineName());
      Pipeline storedPipeline = getPipelineStorage().getPipeline(pipeline.getPipelineId());
      storedPipeline.setRestartOnSystemReboot(restartOnReboot);
      getPipelineStorage().updatePipeline(storedPipeline);
    } else {
      storeFailedRestartAttempt(pipeline);
      int failedAttemptCount = failedPipelines.get(pipeline.getPipelineId());
      if (failedAttemptCount <= MAX_PIPELINE_START_RETRIES) {
        LOG.error("Pipeline {} could not be restarted - I'll try again in {} seconds ({}/{} failed attempts)",
                pipeline.getName(),
                WAIT_TIME_AFTER_FAILURE_IN_SECONDS,
                failedAttemptCount,
                MAX_PIPELINE_START_RETRIES);

        schedulePipelineStart(pipeline, restartOnReboot);
      } else {
        LOG.error("Pipeline {} could not be restarted - are all pipeline element containers running?",
                status.getPipelineName());
      }
    }
  }

  private void storeFailedRestartAttempt(Pipeline pipeline) {
    String pipelineId = pipeline.getPipelineId();
    if (!failedPipelines.containsKey(pipelineId)) {
      failedPipelines.put(pipelineId, 1);
    } else {
      int failedAttempts = failedPipelines.get(pipelineId) + 1;
      failedPipelines.put(pipelineId, failedAttempts);
    }
  }

  private List<Pipeline> getAllPipelines() {
    return getPipelineStorage()
            .getAllPipelines();
  }

  private IPipelineStorage getPipelineStorage() {
    return StorageDispatcher
            .INSTANCE
            .getNoSqlStore()
            .getPipelineStorageAPI();
  }

  @Bean
  public FilterRegistrationBean shiroFilterBean() {
    FilterRegistrationBean<OncePerRequestFilter> bean = new FilterRegistrationBean<>();
    bean.setFilter(new ShiroFilter());
    bean.addUrlPatterns("/api/*");
    return bean;
  }

  @Bean
  public ServletListenerRegistrationBean shiroListenerBean() {
    return listener(new EnvironmentLoaderListener());
  }

  @Bean
  public ServletListenerRegistrationBean streamPipesNotificationListenerBean() {
    return listener(new NotificationListener());
  }

  private ServletListenerRegistrationBean listener(ServletContextListener listener) {
    ServletListenerRegistrationBean<ServletContextListener> bean =
            new ServletListenerRegistrationBean<>();
    bean.setListener(listener);
    return bean;
  }

}
