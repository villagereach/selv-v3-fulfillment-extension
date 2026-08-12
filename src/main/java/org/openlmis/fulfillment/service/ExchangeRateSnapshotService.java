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

package org.openlmis.fulfillment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.openlmis.fulfillment.domain.ExchangeRate;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.repository.ExchangeRateRepository;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Writes the currently active USD-MZM exchange rate onto an order's {@code extraData}, so the
 * PoD and Order reports can show the rate and the MZM total. It is stored under the
 * {@code exchangeRate} key as a JSON object ({@code rate}, {@code exchangeRateId},
 * {@code capturedAt}). An existing snapshot is never overwritten, so a rate captured when the
 * order was created survives a later snapshot taken at shipment time.
 */
@Component
public class ExchangeRateSnapshotService {

  static final String EXCHANGE_RATE = "exchangeRate";

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Autowired
  private ExchangeRateRepository exchangeRateRepository;

  @Autowired
  private OrderRepository orderRepository;

  /**
   * Writes the active rate onto the order unless it already carries a snapshot or no rate has
   * been configured yet. The order is re-loaded before it is saved because callers may pass a
   * detached instance.
   *
   * @param order the order to enrich; a {@code null} order is ignored
   */
  public void snapshotCurrentRateIfAbsent(Order order) {
    if (order == null) {
      return;
    }

    ExchangeRate current = exchangeRateRepository.findFirstByOrderByValidFromDescIdDesc();
    if (current == null) {
      return;
    }

    Order managed = orderRepository.findById(order.getId()).orElse(order);
    Map<String, String> extraData = new HashMap<>(managed.getExtraData());
    if (extraData.containsKey(EXCHANGE_RATE)) {
      return;
    }

    extraData.put(EXCHANGE_RATE, serialize(current));
    managed.setExtraData(extraData);
    orderRepository.save(managed);
  }

  private String serialize(ExchangeRate rate) {
    Map<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("rate", rate.getRate());
    snapshot.put("exchangeRateId", rate.getId().toString());
    snapshot.put("capturedAt", ZonedDateTime.now(ZoneOffset.UTC).toString());
    try {
      return objectMapper.writeValueAsString(snapshot);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Unable to serialize exchange rate snapshot", ex);
    }
  }
}
