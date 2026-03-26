package org.openlmis.fulfillment.repository;

import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.repository.custom.OrderRepositoryCustom;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface OrderSelvRepository extends PagingAndSortingRepository<Order, UUID> {

  @Query(value =
          "SELECT CASE " +
                  "WHEN (SUBSTRING(o.ordercode, 1, 5)) = 'ORDER' THEN '0000' " +
                  "ELSE SUBSTRING(o.ordercode, LENGTH(o.ordercode)-3, LENGTH(o.ordercode)) " +
                  "END " +
                  "FROM fulfillment.orders o " +
                  "WHERE o.supplyingfacilityid = :id " +
                  "ORDER BY o.createddate DESC " +
                  "LIMIT 1 " +
                  "FOR UPDATE",
          nativeQuery = true)
  String findLastOrderCodeOrCreateSequenceCode(@Param("id") UUID supplyingFacilityId);

}
