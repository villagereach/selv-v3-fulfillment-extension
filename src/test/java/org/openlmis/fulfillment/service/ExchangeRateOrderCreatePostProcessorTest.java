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
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.repository.ExchangeRateRepository;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.openlmis.fulfillment.web.util.ExchangeRateDto;

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
    when(exchangeRateRepository.findCurrent()).thenReturn(rate("64.250000"));
    when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

    processor.process(order);

    InOrder inOrder = inOrder(defaultOrderCreatePostProcessor, orderRepository);
    inOrder.verify(defaultOrderCreatePostProcessor).process(order);
    inOrder.verify(orderRepository).findById(order.getId());
  }

  @Test
  public void shouldSnapshotCurrentRateIntoExtraData() {
    UUID rateId = UUID.randomUUID();
    when(exchangeRateRepository.findCurrent())
        .thenReturn(new ExchangeRateDto(rateId, new BigDecimal("64.250000"),
            ZonedDateTime.now(), UUID.randomUUID(), null));
    when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

    processor.process(order);

    Map<String, String> extraData = order.getExtraData();
    assertThat(extraData).containsEntry("exchangeRateValue", "64.250000");
    assertThat(extraData).containsEntry("exchangeRateId", rateId.toString());
    assertThat(extraData.get("exchangeRateCapturedAt")).isNotBlank();
  }

  @Test
  public void shouldNotSnapshotWhenNoCurrentRate() {
    when(exchangeRateRepository.findCurrent()).thenReturn(null);

    processor.process(order);

    assertThat(order.getExtraData()).doesNotContainKey("exchangeRateValue");
    verify(defaultOrderCreatePostProcessor).process(order);
    verify(orderRepository, never()).findById(any());
  }

  @Test
  public void shouldPreserveExistingExtraData() {
    Map<String, String> existing = new HashMap<>();
    existing.put("foo", "bar");
    order.setExtraData(existing);
    when(exchangeRateRepository.findCurrent()).thenReturn(rate("10.000000"));
    when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

    processor.process(order);

    assertThat(order.getExtraData()).containsEntry("foo", "bar");
    assertThat(order.getExtraData()).containsEntry("exchangeRateValue", "10.000000");
  }

  private ExchangeRateDto rate(String value) {
    return new ExchangeRateDto(UUID.randomUUID(), new BigDecimal(value),
        ZonedDateTime.now(), UUID.randomUUID(), null);
  }
}
