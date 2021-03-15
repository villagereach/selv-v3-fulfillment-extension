package org.openlmis.fulfillment.repository;

import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.repository.custom.OrderRepositoryCustom;
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
