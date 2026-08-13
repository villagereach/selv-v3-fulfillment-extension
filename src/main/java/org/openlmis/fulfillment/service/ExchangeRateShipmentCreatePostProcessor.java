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
import org.openlmis.fulfillment.domain.Shipment;
import org.openlmis.fulfillment.extension.point.ShipmentCreatePostProcessor;
import org.openlmis.fulfillment.repository.ExchangeRateRepository;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Runs after a shipment is created: performs the core default behaviour (stock event) and then
 * snapshots the current USD-MZM exchange rate onto the shipment's order for the PoD and Order
 * reports. Shipment creation is the single point where the rate is captured, so it applies to
 * every order regardless of how it was created. The value is stored under the {@code exchangeRate}
 * key as a JSON object ({@code rate}, {@code exchangeRateId}, {@code capturedAt}); an order that
 * already carries a snapshot is left untouched so repeated processing stays idempotent.
 */
@Component("ExchangeRateShipmentCreatePostProcessor")
public class ExchangeRateShipmentCreatePostProcessor implements ShipmentCreatePostProcessor {

  static final String EXCHANGE_RATE = "exchangeRate";

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Autowired
  private DefaultShipmentCreatePostProcessor defaultShipmentCreatePostProcessor;

  @Autowired
  private ExchangeRateRepository exchangeRateRepository;

  @Autowired
  private OrderRepository orderRepository;

  @Override
  public void process(Shipment shipment) {
    defaultShipmentCreatePostProcessor.process(shipment);

    Order order = shipment.getOrder();
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
