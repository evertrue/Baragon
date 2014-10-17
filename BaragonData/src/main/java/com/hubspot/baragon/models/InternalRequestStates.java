package com.hubspot.baragon.models;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.hubspot.baragon.managers.AgentManager;
import com.hubspot.baragon.managers.RequestManager;
import com.hubspot.baragon.utils.JavaUtils;

public enum InternalRequestStates {
  PENDING(BaragonRequestState.WAITING, true, false) {
    @Override
    public Optional<InternalRequestStates> handle(BaragonRequest request, AgentManager agentManager, RequestManager requestManager) {
      // check for missing load balancer groups
      final Set<String> missingLoadBalancerGroups = new HashSet<>(requestManager.getMissingLoadBalancerGroups(request));
      final Set<String> desiredLoadBalancerGroups = new HashSet<>(request.getLoadBalancerService().getLoadBalancerGroups());

      if (!missingLoadBalancerGroups.isEmpty()) {
        if (missingLoadBalancerGroups.equals(desiredLoadBalancerGroups) && request.getAddUpstreams().isEmpty()) {
          requestManager.setRequestMessage(request.getLoadBalancerRequestId(), String.format("Request not applied because load balancer groups do not exist: [%s]", request.getLoadBalancerRequestId(), JavaUtils.COMMA_JOINER.join(missingLoadBalancerGroups)));
          requestManager.setRequestState(request.getLoadBalancerRequestId(), InternalRequestStates.INVALID_REQUEST_NOOP);
          return Optional.of(InternalRequestStates.INVALID_REQUEST_NOOP);
        } else {
          requestManager.setRequestMessage(request.getLoadBalancerRequestId(), String.format("Request references non-existent load balancer groups: [%s]", JavaUtils.COMMA_JOINER.join(missingLoadBalancerGroups)));
          requestManager.setRequestState(request.getLoadBalancerRequestId(), InternalRequestStates.FAILED_REVERTED);
          return Optional.of(InternalRequestStates.FAILED_REVERTED);
        }
      }

      // check of there are actual changes that need to be applied
      if (!requestManager.requestHasChanges(request)) {
        requestManager.setRequestMessage(request.getLoadBalancerRequestId(), String.format("Request is no-op because all upstream changes have already been applied."));
        requestManager.setRequestState(request.getLoadBalancerRequestId(), InternalRequestStates.INVALID_REQUEST_NOOP);
        return Optional.of(InternalRequestStates.INVALID_REQUEST_NOOP);
      }

      // fire off agent requests
      return Optional.of(SEND_APPLY_REQUESTS);
    }
  },
  SEND_APPLY_REQUESTS(BaragonRequestState.WAITING, true, false) {
    @Override
    public Optional<InternalRequestStates> handle(BaragonRequest request, AgentManager agentManager, RequestManager requestManager) {
      agentManager.sendRequests(request, AgentRequestType.APPLY);

      return Optional.of(CHECK_APPLY_RESPONSES);
    }
  },

  CHECK_APPLY_RESPONSES(BaragonRequestState.WAITING, true, false) {
    @Override
    public Optional<InternalRequestStates> handle(BaragonRequest request, AgentManager agentManager, RequestManager requestManager) {
      switch (agentManager.getRequestsStatus(request, AgentRequestType.APPLY)) {
        case FAILURE:
          final Map<String, Collection<AgentResponse>> agentResponses = agentManager.getAgentResponses(request.getLoadBalancerRequestId());
          requestManager.setRequestMessage(request.getLoadBalancerRequestId(), String.format("Apply failed (%s), reverting...", buildResponseString(agentResponses, AgentRequestType.APPLY)));
          return Optional.of(FAILED_SEND_REVERT_REQUESTS);
        case SUCCESS:
          requestManager.commitRequest(request);
          return Optional.of(COMPLETED);
        case RETRY:
          return Optional.of(SEND_APPLY_REQUESTS);
        default:
          return Optional.absent();
      }
    }
  },

  COMPLETED(BaragonRequestState.SUCCESS, false, true),

  FAILED_SEND_REVERT_REQUESTS(BaragonRequestState.WAITING, true, false) {
    @Override
    public Optional<InternalRequestStates> handle(BaragonRequest request, AgentManager agentManager, RequestManager requestManager) {
      agentManager.sendRequests(request, AgentRequestType.REVERT);

      return Optional.of(FAILED_CHECK_REVERT_RESPONSES);
    }
  },

  FAILED_CHECK_REVERT_RESPONSES(BaragonRequestState.WAITING, true, false) {
    @Override
    public Optional<InternalRequestStates> handle(BaragonRequest request, AgentManager agentManager, RequestManager requestManager) {
      final Map<String, Collection<AgentResponse>> agentResponses;

      switch (agentManager.getRequestsStatus(request, AgentRequestType.REVERT)) {
        case FAILURE:
          agentResponses = agentManager.getAgentResponses(request.getLoadBalancerRequestId());
          requestManager.setRequestMessage(request.getLoadBalancerRequestId(), String.format("Apply failed (%s), revert failed (%s)", buildResponseString(agentResponses, AgentRequestType.APPLY), buildResponseString(agentResponses, AgentRequestType.REVERT)));
          return Optional.of(FAILED_REVERT_FAILED);
        case SUCCESS:
          agentResponses = agentManager.getAgentResponses(request.getLoadBalancerRequestId());
          requestManager.setRequestMessage(request.getLoadBalancerRequestId(), String.format("Apply failed (%s), revert OK.", buildResponseString(agentResponses, AgentRequestType.APPLY)));
          return Optional.of(FAILED_REVERTED);
        case RETRY:
          return Optional.of(FAILED_SEND_REVERT_REQUESTS);
        default:
          return Optional.absent();
      }
    }
  },

  FAILED_REVERTED(BaragonRequestState.FAILED, false, true),
  FAILED_REVERT_FAILED(BaragonRequestState.FAILED, false, true),

  CANCELLED_SEND_REVERT_REQUESTS(BaragonRequestState.CANCELING, false, false) {
    @Override
    public Optional<InternalRequestStates> handle(BaragonRequest request, AgentManager agentManager, RequestManager requestManager) {
      agentManager.sendRequests(request, AgentRequestType.CANCEL);

      return Optional.of(CANCELLED_CHECK_REVERT_RESPONSES);
    }
  },

  CANCELLED_CHECK_REVERT_RESPONSES(BaragonRequestState.CANCELING, false, false) {
    @Override
    public Optional<InternalRequestStates> handle(BaragonRequest request, AgentManager agentManager, RequestManager requestManager) {
      switch (agentManager.getRequestsStatus(request, AgentRequestType.CANCEL)) {
        case FAILURE:
          final Map<String, Collection<AgentResponse>> agentResponses = agentManager.getAgentResponses(request.getLoadBalancerRequestId());
          requestManager.setRequestMessage(request.getLoadBalancerRequestId(), String.format("Cancel failed (%s)", buildResponseString(agentResponses, AgentRequestType.CANCEL)));
          return Optional.of(FAILED_CANCEL_FAILED);
        case SUCCESS:
          return Optional.of(CANCELLED);
        case RETRY:
          return Optional.of(CANCELLED_SEND_REVERT_REQUESTS);
        default:
          return Optional.absent();
      }
    }
  },

  FAILED_CANCEL_FAILED(BaragonRequestState.FAILED, false, true),
  CANCELLED(BaragonRequestState.CANCELED, false, true),

  INVALID_REQUEST_NOOP(BaragonRequestState.INVALID_REQUEST_NOOP, false, true);

  private static final Logger LOG = LoggerFactory.getLogger(InternalRequestStates.class);

  private final boolean cancelable;
  private final boolean removable;
  private final BaragonRequestState requestState;

  private InternalRequestStates(BaragonRequestState requestState, boolean cancelable, boolean removable) {
    this.requestState = requestState;
    this.cancelable = cancelable;
    this.removable = removable;
  }

  public BaragonRequestState toRequestState() {
    return requestState;
  }

  public boolean isCancelable() {
    return cancelable;
  }

  public boolean isRemovable() {
    return removable;
  }

  public Optional<InternalRequestStates> handle(BaragonRequest request, AgentManager agentManager, RequestManager requestManager) {
    return Optional.absent();
  }

  private static String buildResponseString(Map<String, Collection<AgentResponse>> agentResponses, AgentRequestType requestType) {
    if (agentResponses.containsKey(requestType.name())) {
      return JavaUtils.COMMA_JOINER.join(agentResponses.get(requestType.name()));
    } else {
      return "no responses";
    }
  }
}
