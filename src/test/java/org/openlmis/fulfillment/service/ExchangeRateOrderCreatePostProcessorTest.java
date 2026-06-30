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
import org.openlmis.fulfillment.repository.ExchangeRateRepository;
import org.openlmis.fulfillment.repository.OrderRepository;

@RunWith(MockitoJUnitRunner.class)
public class ExchangeRateOrderCreatePostProcessorTest {

  @Mock
  private DefaultOrderCreatePostProcessor defaultOrderCreatePostProcessor;

  @Mock
  private ExchangeRateRepository exchangeRateRepository;

  @Mock
  private OrderRepository orderRepository;

  @InjectMocks
  private ExchangeRateOrderCreatePostProcessor processor;

  private Order order;

  @Before
  public void setUp() {
    order = new OrderDataBuilder().withoutLineItems().build();
  }

  @Test
  public void shouldDelegateToDefaultProcessorBeforeSnapshotting() {
    when(exchangeRateRepository.findFirstByOrderByValidFromDescIdDesc())
        .thenReturn(rate("64.250000"));
    when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

    processor.process(order);

    InOrder inOrder = inOrder(defaultOrderCreatePostProcessor, orderRepository);
    inOrder.verify(defaultOrderCreatePostProcessor).process(order);
    inOrder.verify(orderRepository).findById(order.getId());
  }

  @Test
  public void shouldSnapshotCurrentRateIntoExtraData() {
    UUID rateId = UUID.randomUUID();
    ExchangeRate current = new ExchangeRate(new BigDecimal("64.250000"),
        ZonedDateTime.now(), UUID.randomUUID());
    current.setId(rateId);
    when(exchangeRateRepository.findFirstByOrderByValidFromDescIdDesc()).thenReturn(current);
    when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

    processor.process(order);

    String snapshot = order.getExtraData().get("exchangeRate");
    assertThat(snapshot).contains("\"rate\":64.250000");
    assertThat(snapshot).contains("\"exchangeRateId\":\"" + rateId + "\"");
    assertThat(snapshot).contains("\"capturedAt\":");
    verify(orderRepository).save(order);
  }

  @Test
  public void shouldNotSnapshotWhenNoCurrentRate() {
    when(exchangeRateRepository.findFirstByOrderByValidFromDescIdDesc()).thenReturn(null);

    processor.process(order);

    assertThat(order.getExtraData()).doesNotContainKey("exchangeRate");
    verify(defaultOrderCreatePostProcessor).process(order);
    verify(orderRepository, never()).findById(any());
  }

  @Test
  public void shouldPreserveExistingExtraData() {
    Map<String, String> existing = new HashMap<>();
    existing.put("foo", "bar");
    order.setExtraData(existing);
    when(exchangeRateRepository.findFirstByOrderByValidFromDescIdDesc())
        .thenReturn(rate("10.000000"));
    when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

    processor.process(order);

    assertThat(order.getExtraData()).containsEntry("foo", "bar");
    assertThat(order.getExtraData().get("exchangeRate")).contains("\"rate\":10.000000");
  }

  private ExchangeRate rate(String value) {
    ExchangeRate exchangeRate = new ExchangeRate(new BigDecimal(value),
        ZonedDateTime.now(), UUID.randomUUID());
    exchangeRate.setId(UUID.randomUUID());
    return exchangeRate;
  }
}
