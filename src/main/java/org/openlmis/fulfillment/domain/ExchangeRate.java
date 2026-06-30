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

package org.openlmis.fulfillment.domain;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

/**
 * SELV USD-MZM exchange rate. Immutable rows; the row with the greatest {@code validFrom} is the
 * currently active rate. Maps the {@code fulfillment.exchange_rates} table.
 */
@Entity
@Table(name = "exchange_rates", schema = "fulfillment")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ExchangeRate extends BaseEntity {

  @Column(nullable = false, precision = 12, scale = 6)
  private BigDecimal rate;

  @Column(name = "valid_from", nullable = false, columnDefinition = "timestamp with time zone")
  private ZonedDateTime validFrom;

  @Type(type = UUID_TYPE)
  @Column(name = "created_by")
  private UUID createdById;
}
