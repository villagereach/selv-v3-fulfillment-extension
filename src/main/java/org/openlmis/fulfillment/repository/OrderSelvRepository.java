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

import org.openlmis.fulfillment.domain.Order;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface OrderSelvRepository extends PagingAndSortingRepository<Order, UUID> {

  @Query(value =
          "SELECT\n"
                  + "CASE\n"
                  + "WHEN (SUBSTRING(o.ordercode, 1, 5)) = 'ORDER' THEN '0000'\n"
                  + "ELSE SUBSTRING(o.ordercode, LENGTH(o.ordercode)-3, LENGTH(o.ordercode))\n"
                  + "END\n"
                  + "FROM\n"
                  + "fulfillment.orders o\n"
                  + "WHERE o.supplyingfacilityid = :id\n"
                  + "ORDER BY\n"
                  + "o.createddate DESC\n"
                  + "LIMIT 1\n",
          nativeQuery = true
  )
  String findLastOrderCodeOrCreateSequenceCode(@Param("id") UUID supplyingFacilityId);

}
