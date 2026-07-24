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

import static org.openlmis.fulfillment.service.ExchangeRateOrderCreatePostProcessor.EXCHANGE_RATE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.openlmis.fulfillment.domain.ExchangeRate;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.Shipment;
import org.openlmis.fulfillment.extension.point.ShipmentCreatePostProcessor;
import org.openlmis.fulfillment.repository.ExchangeRateRepository;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * After a shipment is created: delegates to the core default processor (stock event), then snapshots
 * the current USD-MZM rate onto the shipment's {@code Order.extraData} under the
 * {@code exchangeRate} key for later usage in the reports (only if exchangeRate is absent).
 *
 * Requisition-less orders never pass through {@link ExchangeRateOrderCreatePostProcessor}: the
 * {@code OrderCreatePostProcessor} extension point is invoked only when converting a requisition to
 * an order, not on the requisition-less (local-fulfillment) creation path.
 */
@Component("ExchangeRateShipmentCreatePostProcessor")
public class ExchangeRateShipmentCreatePostProcessor implements ShipmentCreatePostProcessor {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Autowired
  private DefaultShipmentCreatePostProcessor defaultShipmentCreatePostProcessor;

  @Autowired
  private ExchangeRateRepository exchangeRateRepository;

  @Autowired
  private OrderRepository orderRepository;

  @Override
  public void process(Shipment shipment) {
    // Core default behaviour first (stock event submission).
    defaultShipmentCreatePostProcessor.process(shipment);

    Order order = shipment.getOrder();
    if (order == null) {
      return;
    }

    ExchangeRate current = exchangeRateRepository.findFirstByOrderByValidFromDescIdDesc();
    if (current == null) {
      return; // no rate yet — snapshot stays empty (nullable)
    }

    Order managed = orderRepository.findById(order.getId()).orElse(order);
    Map<String, String> extraData = new HashMap<>(managed.getExtraData());
    if (extraData.containsKey(EXCHANGE_RATE)) {
      return; // keep the creation-time snapshot set by the order-create processor
    }

    Map<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("rate", current.getRate());
    snapshot.put("exchangeRateId", current.getId().toString());
    snapshot.put("capturedAt", ZonedDateTime.now(ZoneOffset.UTC).toString());

    extraData.put(EXCHANGE_RATE, writeSnapshot(snapshot));
    managed.setExtraData(extraData);
    orderRepository.save(managed);
  }

  private String writeSnapshot(Map<String, Object> snapshot) {
    try {
      return objectMapper.writeValueAsString(snapshot);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Unable to serialize exchange rate snapshot", ex);
    }
  }
}
