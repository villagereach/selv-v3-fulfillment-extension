/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2017 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *  
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 * See the GNU Affero General Public License for more details. You should have received a copy of
 * the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org. 
 */

package org.openlmis.fulfillment.repository;

import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class FacilityOrderSequenceRepository {

  private static final String UPSERT_SQL =
      "INSERT INTO fulfillment.facility_order_sequence "
          + "(supplyingfacilityid, lastsequencevalue) "
          + "VALUES (?1, ?2) "
          + "ON CONFLICT (supplyingfacilityid) "
          + "DO UPDATE SET lastsequencevalue = GREATEST("
          + "facility_order_sequence.lastsequencevalue + 1, "
          + "EXCLUDED.lastsequencevalue) "
          + "RETURNING lastsequencevalue";

  @PersistenceContext
  private EntityManager entityManager;

  /**
   * Atomically inserts or increments the sequence value for a facility.
   * On first call for a facility, inserts with the given initial value.
   * On subsequent calls, increments the existing value by 1.
   * Uses PostgreSQL's ON CONFLICT for row-level locking — no table lock needed.
   */
  @Transactional
  public int getNextSequenceValue(UUID facilityId, int initialValue) {
    Query query = entityManager.createNativeQuery(UPSERT_SQL);
    query.setParameter(1, facilityId);
    query.setParameter(2, initialValue);
    return ((Number) query.getSingleResult()).intValue();
  }
}
