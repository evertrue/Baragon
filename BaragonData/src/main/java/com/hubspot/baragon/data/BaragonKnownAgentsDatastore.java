package com.hubspot.baragon.data;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.baragon.exceptions.InvalidAgentMetadataStringException;
import com.hubspot.baragon.models.BaragonAgentMetadata;
import com.hubspot.baragon.models.BaragonKnownAgentMetadata;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

@Singleton
public class BaragonKnownAgentsDatastore extends AbstractDataStore {
  private static final Logger LOG = LoggerFactory.getLogger(BaragonKnownAgentsDatastore.class);

  public static final String KNOWN_AGENTS_GROUP_HOSTS_FORMAT = "/load-balancer/%s/known-agents";
  public static final String KNOWN_AGENTS_GROUP_HOST_FORMAT = KNOWN_AGENTS_GROUP_HOSTS_FORMAT + "/%s";

  @Inject
  public BaragonKnownAgentsDatastore(CuratorFramework curatorFramework, ObjectMapper objectMapper) {
    super(curatorFramework, objectMapper);
  }

  public Collection<BaragonKnownAgentMetadata> getKnownAgentsMetadata(String clusterName) {
    final Collection<String> nodes = getChildren(String.format(KNOWN_AGENTS_GROUP_HOSTS_FORMAT, clusterName));

    if (nodes.isEmpty()) {
      return Collections.emptyList();
    }

    final Collection<BaragonKnownAgentMetadata> metadata = Lists.newArrayListWithCapacity(nodes.size());

    for (String node : nodes) {
      metadata.addAll(readFromZk(String.format(KNOWN_AGENTS_GROUP_HOST_FORMAT, clusterName, node), BaragonKnownAgentMetadata.class).asSet());
    }

    return metadata;
  }

  public Optional<BaragonKnownAgentMetadata> getKnownAgentMetadata(String clusterName, String agentId) {
    return readFromZk(String.format(KNOWN_AGENTS_GROUP_HOST_FORMAT, clusterName, agentId), BaragonKnownAgentMetadata.class);
  }

  public void addKnownAgent(String clusterName, BaragonKnownAgentMetadata agentMetadata) {
    writeToZk(String.format(KNOWN_AGENTS_GROUP_HOST_FORMAT, clusterName, agentMetadata.getAgentId()), agentMetadata);
  }

  public void removeKnownAgent(String clusterName, String agentId) {
    deleteNode(String.format(KNOWN_AGENTS_GROUP_HOST_FORMAT, clusterName, agentId));
  }

  public void updateKnownAgentLastSeenAt(String clusterName, String agentId, long time) {
    Optional<BaragonKnownAgentMetadata> maybeAgent = getKnownAgentMetadata(clusterName, agentId);
    if (maybeAgent.isPresent()) {
      maybeAgent.get().setLastSeenAt(time);
      writeToZk(String.format(KNOWN_AGENTS_GROUP_HOST_FORMAT, clusterName, maybeAgent.get().getAgentId()), maybeAgent.get());
    } else {
      LOG.error("Could not fetch known agent metadata to update lastSeenAt time");
    }
  }

}
