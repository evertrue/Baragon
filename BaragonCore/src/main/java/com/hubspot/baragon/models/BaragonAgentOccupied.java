package com.hubspot.baragon.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

@JsonIgnoreProperties( ignoreUnknown = true )
public class BaragonAgentOccupied {
  private final Optional<String> activeRequestId;
  private final long lockedAt;

  @JsonCreator
  public BaragonAgentOccupied(@JsonProperty("activeRequestId") Optional<String> activeRequestId,
                              @JsonProperty("lockedAt") long lockedAt) {
    this.activeRequestId = activeRequestId;
    this.lockedAt = lockedAt;
  }

  public Optional<String> getActiveRequestId() {
    return activeRequestId;
  }

  public long getLockedAt() {
    return lockedAt;
  }
}
