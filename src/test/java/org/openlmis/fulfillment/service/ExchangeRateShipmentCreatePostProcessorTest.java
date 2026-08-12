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

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openlmis.fulfillment.OrderDataBuilder;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.Shipment;

@RunWith(MockitoJUnitRunner.class)
public class ExchangeRateShipmentCreatePostProcessorTest {

  @Mock
  private DefaultShipmentCreatePostProcessor defaultShipmentCreatePostProcessor;

  @Mock
  private ExchangeRateSnapshotService exchangeRateSnapshotService;

  @Mock
  private Shipment shipment;

  @InjectMocks
  private ExchangeRateShipmentCreatePostProcessor processor;

  @Test
  public void shouldRunDefaultProcessorThenSnapshotRateOntoOrder() {
    Order order = new OrderDataBuilder().withoutLineItems().build();
    when(shipment.getOrder()).thenReturn(order);

    processor.process(shipment);

    InOrder inOrder = inOrder(defaultShipmentCreatePostProcessor, exchangeRateSnapshotService);
    inOrder.verify(defaultShipmentCreatePostProcessor).process(shipment);
    inOrder.verify(exchangeRateSnapshotService).snapshotCurrentRateIfAbsent(order);
  }
}
