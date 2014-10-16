package com.hubspot.baragon;

import java.util.Collections;

import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.inject.Scopes;
import com.hubspot.baragon.data.BaragonLoadBalancerDatastore;
import com.hubspot.baragon.managers.RequestManager;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.BaragonRequestState;
import com.hubspot.baragon.models.BaragonResponse;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.UpstreamInfo;
import com.hubspot.baragon.worker.BaragonRequestWorker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(JukitoRunner.class)
public class PendingStateTests {
  private static final Logger LOG = LoggerFactory.getLogger(PendingStateTests.class);

  public static final String FAKE_LB_GROUP = "fake";
  public static final String REAL_LB_GROUP = "real";

  public static class Module extends JukitoModule {
    @Override
    protected void configureTest() {
      install(new BaragonTestingModule());
      bindMock(BaragonLoadBalancerDatastore.class).in(Scopes.SINGLETON);
    }
  }

  @Before
  public void setupMocks(BaragonLoadBalancerDatastore loadBalancerDatastore) {
    when(loadBalancerDatastore.getLoadBalancerGroups()).thenReturn(Collections.singleton(REAL_LB_GROUP));
    when(loadBalancerDatastore.getBasePaths(anyString())).thenReturn(Collections.<String>emptyList());
    when(loadBalancerDatastore.getBasePathServiceId(anyString(), anyString())).thenReturn(Optional.<String>absent());
  }

  @Test
  public void removeUpstreamFromNonExistentLoadBalancer(RequestManager requestManager, BaragonRequestWorker worker) {
    final String requestId = "test-123";
    final BaragonService service = BaragonService.builder("testservice", "/test")
        .setLoadBalancerGroups(ImmutableList.of(FAKE_LB_GROUP))
        .build();

    final UpstreamInfo fakeUpstream = new UpstreamInfo("testhost:8080", Optional.of(requestId), Optional.<String>absent());

    final BaragonRequest request = BaragonRequest.builder(requestId, service)
        .setRemoveUpstreams(Collections.singletonList(fakeUpstream))
        .build();

    try {
      LOG.info("Going to enqueue request: {}", request);
      final BaragonResponse response = requestManager.enqueueRequest(request);
      assertEquals(BaragonRequestState.WAITING, response.getLoadBalancerState());
      worker.run();
      final Optional<BaragonResponse> maybeNewResponse = requestManager.getResponse(requestId);
      LOG.info("Got response: {}", maybeNewResponse);
      assertTrue(maybeNewResponse.isPresent());
      assertEquals(maybeNewResponse.get().getLoadBalancerState(), BaragonRequestState.INVALID_REQUEST_NOOP);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  @Test
  public void addUpstreamToNonExistentLoadBalancer(RequestManager requestManager, BaragonRequestWorker worker) {
    final String requestId = "test-126";

    final BaragonService service = BaragonService.builder("testservice", "/test")
        .setLoadBalancerGroups(ImmutableList.of(FAKE_LB_GROUP))
        .build();

    final UpstreamInfo fakeUpstream = new UpstreamInfo("testhost:8080", Optional.of(requestId), Optional.<String>absent());

    final BaragonRequest request = BaragonRequest.builder(requestId, service)
        .setAddUpstreams(Collections.singletonList(fakeUpstream))
        .build();

    try {
      LOG.info("Going to enqueue request: {}", request);
      final BaragonResponse response = requestManager.enqueueRequest(request);
      assertEquals(BaragonRequestState.WAITING, response.getLoadBalancerState());
      worker.run();
      final Optional<BaragonResponse> maybeNewResponse = requestManager.getResponse(requestId);
      LOG.info("Got response: {}", maybeNewResponse);
      assertTrue(maybeNewResponse.isPresent());
      assertEquals(maybeNewResponse.get().getLoadBalancerState(), BaragonRequestState.FAILED);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  @Test
  public void removeUpstreamFromNonExistentAndRealLoadBalancer(RequestManager requestManager, BaragonRequestWorker worker) {
    final String requestId = "test-124";
    final BaragonService service = BaragonService.builder("testservice", "/test")
        .setLoadBalancerGroups(ImmutableList.of(FAKE_LB_GROUP, REAL_LB_GROUP))
        .build();

    final UpstreamInfo fakeUpstream = new UpstreamInfo("testhost:8080", Optional.of(requestId), Optional.<String>absent());

    final BaragonRequest request = BaragonRequest.builder(requestId, service)
        .setRemoveUpstreams(Collections.singletonList(fakeUpstream))
        .build();

    try {
      LOG.info("Going to enqueue request: {}", request);
      final BaragonResponse response = requestManager.enqueueRequest(request);
      assertEquals(BaragonRequestState.WAITING, response.getLoadBalancerState());
      worker.run();
      final Optional<BaragonResponse> maybeNewResponse = requestManager.getResponse(requestId);
      LOG.info("Got response: {}", maybeNewResponse);
      assertTrue(maybeNewResponse.isPresent());
      assertEquals(maybeNewResponse.get().getLoadBalancerState(), BaragonRequestState.FAILED);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  @Test
  public void removeNonExistentUpstreamFromRealLoadBalancer(RequestManager requestManager, BaragonRequestWorker worker) {
    final String requestId = "test-125";
    final BaragonService service = BaragonService.builder("testservice", "/test")
        .setLoadBalancerGroups(ImmutableList.of(REAL_LB_GROUP))
        .build();

    final UpstreamInfo fakeUpstream = new UpstreamInfo("testhost:8080", Optional.of(requestId), Optional.<String>absent());

    final BaragonRequest request = BaragonRequest.builder(requestId, service)
        .setRemoveUpstreams(Collections.singletonList(fakeUpstream))
        .build();

    try {
      LOG.info("Going to enqueue request: {}", request);
      final BaragonResponse response = requestManager.enqueueRequest(request);
      assertEquals(BaragonRequestState.WAITING, response.getLoadBalancerState());
      worker.run();
      final Optional<BaragonResponse> maybeNewResponse = requestManager.getResponse(requestId);
      LOG.info("Got response: {}", maybeNewResponse);
      assertTrue(maybeNewResponse.isPresent());
      assertEquals(maybeNewResponse.get().getLoadBalancerState(), BaragonRequestState.INVALID_REQUEST_NOOP);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
}
