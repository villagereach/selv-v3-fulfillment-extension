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

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.openlmis.fulfillment.web.util.ExchangeRateDto;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Native-SQL access to {@code exchange_rates} (no JPA entity, cf. FacilityOrderSequenceRepository).
 * Current rate = row with the greatest {@code valid_from}.
 */
@Repository
public class ExchangeRateRepository {

  // CAST uuid columns to varchar: native queries have no Hibernate dialect mapping for JDBC type
  // 1111 (uuid); parsed back via UUID.fromString in map().
  private static final String SELECT =
      "SELECT CAST(id AS varchar) AS id, rate, valid_from, "
          + "CAST(created_by AS varchar) AS created_by FROM fulfillment.exchange_rates";
  private static final String ORDER = " ORDER BY valid_from DESC, id DESC";

  @PersistenceContext
  private EntityManager entityManager;

  /**
   * Returns the currently active rate (greatest valid_from), or {@code null} if none exists.
   */
  public ExchangeRateDto findCurrent() {
    List<?> rows = entityManager.createNativeQuery(SELECT + ORDER).setMaxResults(1).getResultList();
    return rows.isEmpty() ? null : map((Object[]) rows.get(0));
  }

  /**
   * Returns the full rate history, newest first.
   */
  public List<ExchangeRateDto> findHistory() {
    List<?> rows = entityManager.createNativeQuery(SELECT + ORDER).getResultList();
    List<ExchangeRateDto> result = new ArrayList<>(rows.size());
    for (Object row : rows) {
      result.add(map((Object[]) row));
    }
    return result;
  }

  /**
   * Inserts a new immutable rate with server-assigned {@code valid_from = now()}; it becomes the
   * current rate. Returns the inserted row.
   */
  @Transactional
  public ExchangeRateDto insert(BigDecimal rate, UUID createdById) {
    // INSERT ... RETURNING fetches the row in one round-trip (cf. FacilityOrderSequenceRepository).
    Query query = entityManager.createNativeQuery(
        "INSERT INTO fulfillment.exchange_rates (id, rate, valid_from, created_by) "
            + "VALUES (?1, ?2, now(), ?3) "
            + "RETURNING CAST(id AS varchar) AS id, rate, valid_from, "
            + "CAST(created_by AS varchar) AS created_by");
    query.setParameter(1, UUID.randomUUID());
    query.setParameter(2, rate);
    query.setParameter(3, createdById);
    return map((Object[]) query.getSingleResult());
  }

  private ExchangeRateDto map(Object[] row) {
    UUID id = UUID.fromString((String) row[0]);
    BigDecimal rate = (BigDecimal) row[1];
    ZonedDateTime validFrom = toZonedDateTime(row[2]);
    UUID createdBy = row[3] == null ? null : UUID.fromString((String) row[3]);
    return new ExchangeRateDto(id, rate, validFrom, createdBy, null);
  }

  private ZonedDateTime toZonedDateTime(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Timestamp) {
      return ((Timestamp) value).toInstant().atZone(ZoneOffset.UTC);
    }
    if (value instanceof OffsetDateTime) {
      return ((OffsetDateTime) value).toZonedDateTime();
    }
    if (value instanceof Instant) {
      return ((Instant) value).atZone(ZoneOffset.UTC);
    }
    throw new IllegalStateException("Unsupported valid_from type: " + value.getClass());
  }
}
