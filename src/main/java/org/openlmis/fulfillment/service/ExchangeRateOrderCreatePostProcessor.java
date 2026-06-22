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

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import org.openlmis.fulfillment.domain.ExchangeRate;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.extension.point.OrderCreatePostProcessor;
import org.openlmis.fulfillment.repository.ExchangeRateRepository;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * After an order is created: delegates to the core default processor (FTP, e-mail), then snapshots
 * the current USD-MZM rate onto {@code Order.extraData} as flat keys ({@code exchangeRateValue},
 * {@code exchangeRateId}, {@code exchangeRateCapturedAt}) for the PoD report.
 */
@Component("ExchangeRateOrderCreatePostProcessor")
public class ExchangeRateOrderCreatePostProcessor implements OrderCreatePostProcessor {

  static final String EXCHANGE_RATE_VALUE = "exchangeRateValue";
  static final String EXCHANGE_RATE_ID = "exchangeRateId";
  static final String EXCHANGE_RATE_CAPTURED_AT = "exchangeRateCapturedAt";

  @Autowired
  private DefaultOrderCreatePostProcessor defaultOrderCreatePostProcessor;

  @Autowired
  private ExchangeRateRepository exchangeRateRepository;

  @Autowired
  private OrderRepository orderRepository;

  @Override
  public void process(Order order) {
    // Core default behaviour first (FTP, e-mail).
    defaultOrderCreatePostProcessor.process(order);

    ExchangeRate current = exchangeRateRepository.findFirstByOrderByValidFromDescIdDesc();
    if (current == null) {
      return; // no rate yet — snapshot stays empty (nullable)
    }

    // The passed order is detached (createOrder flushed+cleared); re-load the managed row, update
    // extraData and explicitly re-persist (the .orElse fallback would otherwise be a no-op).
    Order managed = orderRepository.findById(order.getId()).orElse(order);
    Map<String, String> extraData = new HashMap<>(managed.getExtraData());
    extraData.put(EXCHANGE_RATE_VALUE, current.getRate().toPlainString());
    extraData.put(EXCHANGE_RATE_ID, current.getId().toString());
    extraData.put(EXCHANGE_RATE_CAPTURED_AT, ZonedDateTime.now(ZoneOffset.UTC).toString());
    managed.setExtraData(extraData);
    orderRepository.save(managed);
  }
}
