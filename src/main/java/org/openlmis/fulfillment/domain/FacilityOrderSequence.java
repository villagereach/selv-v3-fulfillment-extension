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
  @Column(name = "facilityid")
  private UUID facilityId;

  @Column(name = "lastsequencevalue")
  private Integer lastSequenceValue;

  public FacilityOrderSequence() {
  }

  public FacilityOrderSequence(UUID facilityId, Integer lastSequenceValue) {
    this.facilityId = facilityId;
    this.lastSequenceValue = lastSequenceValue;
  }

  public UUID getFacilityId() {
    return facilityId;
  }

  public void setFacilityId(UUID facilityId) {
    this.facilityId = facilityId;
  }

  public Integer getLastSequenceValue() {
    return lastSequenceValue;
  }

  public void setLastSequenceValue(Integer lastSequenceValue) {
    this.lastSequenceValue = lastSequenceValue;
  }
}
