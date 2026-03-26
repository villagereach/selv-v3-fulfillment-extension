package org.openlmis.fulfillment.repository;

import org.openlmis.fulfillment.domain.FacilityOrderSequence;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.LockModeType;
import java.util.UUID;

public interface FacilityOrderSequenceRepository extends CrudRepository<FacilityOrderSequence, UUID> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT f FROM FacilityOrderSequence f WHERE f.facilityId = :facilityId")
  FacilityOrderSequence findByFacilityIdWithLock(@Param("facilityId") UUID facilityId);

  @Transactional
  @Modifying
  @Query(value = "LOCK TABLE fulfillment.facility_order_sequence IN ACCESS EXCLUSIVE MODE", nativeQuery = true)
  void lockTable();

}
