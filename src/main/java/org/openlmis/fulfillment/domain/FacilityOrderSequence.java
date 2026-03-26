package org.openlmis.fulfillment.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "facility_order_sequence", schema = "fulfillment")
public class FacilityOrderSequence {

  @Id
  @Column(name = "supplyingfacilityid")
  private UUID supplyingFacilityId;

  @Column(name = "lastsequencevalue")
  private Integer lastSequenceValue;

  public FacilityOrderSequence() {
  }

  public FacilityOrderSequence(UUID supplyingFacilityId, Integer lastSequenceValue) {
    this.supplyingFacilityId = supplyingFacilityId;
    this.lastSequenceValue = lastSequenceValue;
  }

  public UUID getSupplyingFacilityId() {
    return supplyingFacilityId;
  }

  public void setSupplyingFacilityId(UUID supplyingFacilityId) {
    this.supplyingFacilityId = supplyingFacilityId;
  }

  public Integer getLastSequenceValue() {
    return lastSequenceValue;
  }

  public void setLastSequenceValue(Integer lastSequenceValue) {
    this.lastSequenceValue = lastSequenceValue;
  }
}
