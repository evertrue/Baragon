package com.hubspot.baragon.exceptions;

import com.google.common.base.Optional;
import com.hubspot.baragon.models.BaragonAgentOccupied;

public class BaragonAgentOccupiedException extends Exception {
  private final BaragonAgentOccupied occupiedInfo;

  public BaragonAgentOccupiedException(Optional<String> activeRequestId, long lockedAt) {
    this.occupiedInfo = new BaragonAgentOccupied(activeRequestId, lockedAt);
  }

  public BaragonAgentOccupied getEntity() {
    return occupiedInfo;
  }
}
