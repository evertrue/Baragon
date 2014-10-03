package com.hubspot.baragon.agent.resources;

import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.baragon.agent.BaragonAgentServiceModule;
import com.hubspot.baragon.agent.config.LoadBalancerConfiguration;
import com.hubspot.baragon.agent.config.TestingConfiguration;
import com.hubspot.baragon.agent.lbs.FilesystemConfigHelper;
import com.hubspot.baragon.data.BaragonRequestDatastore;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.exceptions.BaragonAgentOccupiedException;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.ServiceContext;
import com.hubspot.baragon.models.UpstreamInfo;

@Path("/request/{requestId}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RequestResource {
  private static final Logger LOG = LoggerFactory.getLogger(RequestResource.class);

  private final FilesystemConfigHelper configHelper;
  private final BaragonStateDatastore stateDatastore;
  private final BaragonRequestDatastore requestDatastore;
  private final AtomicReference<String> mostRecentRequestId;
  private final AtomicReference<String> activeRequestId;
  private final Optional<TestingConfiguration> maybeTestingConfiguration;
  private final Random random;
  private final LoadBalancerConfiguration loadBalancerConfiguration;
  
  @Inject
  public RequestResource(BaragonStateDatastore stateDatastore,
                         BaragonRequestDatastore requestDatastore,
                         FilesystemConfigHelper configHelper,
                         Optional<TestingConfiguration> maybeTestingConfiguration,
                         LoadBalancerConfiguration loadBalancerConfiguration,
                         Random random,
                         @Named(BaragonAgentServiceModule.AGENT_MOST_RECENT_REQUEST_ID) AtomicReference<String> mostRecentRequestId,
                         @Named(BaragonAgentServiceModule.AGENT_ACTIVE_REQUEST_ID) AtomicReference<String> activeRequestId) {
    this.configHelper = configHelper;
    this.stateDatastore = stateDatastore;
    this.maybeTestingConfiguration = maybeTestingConfiguration;
    this.requestDatastore = requestDatastore;
    this.mostRecentRequestId = mostRecentRequestId;
    this.activeRequestId = activeRequestId;
    this.random = random;
    this.loadBalancerConfiguration = loadBalancerConfiguration;
  }

  @POST
  public Response apply(@PathParam("requestId") String requestId) throws InterruptedException {
    activeRequestId.set(requestId);

    final Optional<BaragonRequest> maybeRequest = requestDatastore.getRequest(requestId);

    if (!maybeRequest.isPresent()) {
      return Response.status(Response.Status.NOT_FOUND).entity(String.format("Request %s does not exist", requestId)).build();
    }

    final BaragonRequest request = maybeRequest.get();

    LOG.info(String.format("Received request to apply %s", request));

    final ServiceContext update;

    if (!request.getLoadBalancerService().getLoadBalancerGroups().contains(loadBalancerConfiguration.getName())) {
      // this service has been deleted or moved off this load balancer -- delete the config

      update = new ServiceContext(request.getLoadBalancerService(), Collections.<UpstreamInfo>emptyList(), System.currentTimeMillis(), false);
    } else {
      // Apply request
      final Map<String, UpstreamInfo> upstreamsMap = stateDatastore.getUpstreamsMap(request.getLoadBalancerService().getServiceId());

      for (UpstreamInfo removeUpstreamInfo : request.getRemoveUpstreams()) {
        upstreamsMap.remove(removeUpstreamInfo.getUpstream());
      }

      for (UpstreamInfo addUpstreamInfo : request.getAddUpstreams()) {
        upstreamsMap.put(addUpstreamInfo.getUpstream(), addUpstreamInfo);
      }

      update = new ServiceContext(request.getLoadBalancerService(), upstreamsMap.values(), System.currentTimeMillis(), true);
    }

    if (maybeTestingConfiguration.isPresent() && maybeTestingConfiguration.get().isEnabled() && maybeTestingConfiguration.get().getApplyDelayMs() > 0) {
      Thread.sleep(maybeTestingConfiguration.get().getApplyDelayMs());
    }

    if (maybeTestingConfiguration.isPresent() && maybeTestingConfiguration.get().isEnabled()) {
      if (random.nextFloat() <= maybeTestingConfiguration.get().getApplyFailRate()) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Random testing failure").build();
      }
    }

    try {
      configHelper.apply(Optional.of(requestId), update, true);
    } catch (BaragonAgentOccupiedException baoe) {
      return Response.status(Status.CONFLICT).entity(baoe.getEntity()).build();
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
    }

    mostRecentRequestId.set(requestId);

    return Response.ok().build();
  }

  @DELETE
  public Response revert(@PathParam("requestId") String requestId) throws InterruptedException {
    final Optional<BaragonRequest> maybeRequest = requestDatastore.getRequest(requestId);

    if (!maybeRequest.isPresent()) {
      return Response.status(Response.Status.NOT_FOUND).entity(String.format("Request %s does not exist", requestId)).build();
    }

    final BaragonRequest request = maybeRequest.get();

    LOG.info(String.format("Received request to revert %s", request));

    final Optional<BaragonService> maybeService = stateDatastore.getService(request.getLoadBalancerService().getServiceId());

    final ServiceContext update;

    if (!maybeService.isPresent() || !maybeService.get().getLoadBalancerGroups().contains(loadBalancerConfiguration.getName())) {
      // this service previously didn't exist, or wasnt on this load balancer -- remove the config

      update = new ServiceContext(request.getLoadBalancerService(), Collections.<UpstreamInfo>emptyList(), System.currentTimeMillis(), false);
    } else {
      update = new ServiceContext(maybeService.get(), stateDatastore.getUpstreamsMap(maybeService.get().getServiceId()).values(), System.currentTimeMillis(), true);
    }

    if (maybeTestingConfiguration.isPresent() && maybeTestingConfiguration.get().isEnabled() && maybeTestingConfiguration.get().getRevertDelayMs() > 0) {
      Thread.sleep(maybeTestingConfiguration.get().getRevertDelayMs());
    }

    if (maybeTestingConfiguration.isPresent() && maybeTestingConfiguration.get().isEnabled()) {
      if (random.nextFloat() <= maybeTestingConfiguration.get().getRevertFailRate()) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Random testing failure").build();
      }
    }

    try {
      configHelper.apply(Optional.of(requestId), update, false);
    } catch (BaragonAgentOccupiedException baoe) {
      return Response.status(Status.CONFLICT).entity(baoe.getEntity()).build();
    } catch (Exception e) {
      e.printStackTrace();
      return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
    }

    return Response.ok().build();
  }
}
