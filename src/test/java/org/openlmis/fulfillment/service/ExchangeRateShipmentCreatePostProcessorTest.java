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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openlmis.fulfillment.OrderDataBuilder;
import org.openlmis.fulfillment.domain.ExchangeRate;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.Shipment;
import org.openlmis.fulfillment.repository.ExchangeRateRepository;
import org.openlmis.fulfillment.repository.OrderRepository;

@RunWith(MockitoJUnitRunner.class)
public class ExchangeRateShipmentCreatePostProcessorTest {

  // Kept as a literal (not the production constant) to guard the exact key the report SQL reads.
  private static final String EXCHANGE_RATE_KEY = "exchangeRate";

  @Mock
  private DefaultShipmentCreatePostProcessor defaultShipmentCreatePostProcessor;

  @Mock
  private ExchangeRateRepository exchangeRateRepository;

  @Mock
  private OrderRepository orderRepository;

  @Mock
  private Shipment shipment;

  @InjectMocks
  private ExchangeRateShipmentCreatePostProcessor processor;

  private Order order;

  @Before
  public void setUp() {
    order = new OrderDataBuilder().withoutLineItems().build();
  }

  @Test
  public void shouldRunDefaultProcessorBeforeSnapshotting() {
    when(shipment.getOrder()).thenReturn(order);
    when(exchangeRateRepository.findFirstByOrderByValidFromDescIdDesc())
        .thenReturn(rate(UUID.randomUUID(), "64.250000"));
    when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

    processor.process(shipment);

    InOrder inOrder = inOrder(defaultShipmentCreatePostProcessor, orderRepository);
    inOrder.verify(defaultShipmentCreatePostProcessor).process(shipment);
    inOrder.verify(orderRepository).save(order);
  }

  @Test
  public void shouldSnapshotCurrentRateWhenOrderHasNoRate() {
    UUID rateId = UUID.randomUUID();
    when(shipment.getOrder()).thenReturn(order);
    when(exchangeRateRepository.findFirstByOrderByValidFromDescIdDesc())
        .thenReturn(rate(rateId, "64.250000"));
    when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

    processor.process(shipment);

    String snapshot = order.getExtraData().get(EXCHANGE_RATE_KEY);
    assertThat(snapshot).contains("\"rate\":64.250000");
    assertThat(snapshot).contains("\"exchangeRateId\":\"" + rateId + "\"");
    assertThat(snapshot).contains("\"capturedAt\":");
    verify(orderRepository).save(order);
  }

  @Test
  public void shouldKeepExistingSnapshotAndNotOverwrite() {
    Map<String, String> existing = new HashMap<>();
    existing.put(EXCHANGE_RATE_KEY, "{\"rate\":10.000000}");
    order.setExtraData(existing);
    when(shipment.getOrder()).thenReturn(order);
    when(exchangeRateRepository.findFirstByOrderByValidFromDescIdDesc())
        .thenReturn(rate(UUID.randomUUID(), "64.250000"));
    when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

    processor.process(shipment);

    assertThat(order.getExtraData().get(EXCHANGE_RATE_KEY)).isEqualTo("{\"rate\":10.000000}");
    verify(orderRepository, never()).save(any());
  }

  @Test
  public void shouldNotSnapshotWhenNoCurrentRate() {
    when(shipment.getOrder()).thenReturn(order);
    when(exchangeRateRepository.findFirstByOrderByValidFromDescIdDesc()).thenReturn(null);

    processor.process(shipment);

    assertThat(order.getExtraData()).doesNotContainKey(EXCHANGE_RATE_KEY);
    verify(orderRepository, never()).findById(any());
    verify(orderRepository, never()).save(any());
  }

  @Test
  public void shouldIgnoreShipmentWithoutOrder() {
    when(shipment.getOrder()).thenReturn(null);

    processor.process(shipment);

    verify(defaultShipmentCreatePostProcessor).process(shipment);
    verify(exchangeRateRepository, never()).findFirstByOrderByValidFromDescIdDesc();
    verify(orderRepository, never()).save(any());
  }

  private ExchangeRate rate(UUID id, String value) {
    ExchangeRate exchangeRate =
        new ExchangeRate(new BigDecimal(value), ZonedDateTime.now(), UUID.randomUUID());
    exchangeRate.setId(id);
    return exchangeRate;
  }
}
