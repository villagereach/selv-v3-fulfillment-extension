package org.openlmis.fulfillment.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "facility_order_sequence", schema = "fulfillment")
public class FacilityOrderSequence {

  @Id
  @Column(name = "supplyingfacilityid")
  private UUID supplyingFacilityId;

  @Column(name = "lastsequencevalue")
  private Integer lastSequenceValue;
}
