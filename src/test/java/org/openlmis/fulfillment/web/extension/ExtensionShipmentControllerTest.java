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

package org.openlmis.fulfillment.web.extension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openlmis.fulfillment.web.ValidationException;
import org.openlmis.fulfillment.web.shipment.ShipmentController;
import org.openlmis.fulfillment.web.shipment.ShipmentDto;

@RunWith(MockitoJUnitRunner.class)
public class ExtensionShipmentControllerTest {

  @Mock
  private ShipmentController shipmentController;

  @Mock
  private AdditionalShipmentInfoValidator additionalShipmentInfoValidator;

  @InjectMocks
  private ExtensionShipmentController controller;

  @Test
  public void shouldValidateBeforeDelegatingAndReturnCoreResult() {
    ShipmentDto request = shipmentWithExtraData();
    ShipmentDto created = new ShipmentDto();
    when(shipmentController.createShipment(request)).thenReturn(created);

    ShipmentDto result = controller.createWithAdditionalInfo(request);

    assertThat(result).isSameAs(created);
    InOrder inOrder = inOrder(additionalShipmentInfoValidator, shipmentController);
    inOrder.verify(additionalShipmentInfoValidator).validate(any());
    inOrder.verify(shipmentController).createShipment(request);
  }

  @Test
  public void shouldNotCreateShipmentWhenAdditionalInfoIsInvalid() {
    ShipmentDto request = shipmentWithExtraData();
    doThrow(new ValidationException("invalid"))
        .when(additionalShipmentInfoValidator).validate(any());

    Throwable thrown = catchThrowable(() -> controller.createWithAdditionalInfo(request));

    assertThat(thrown).isInstanceOf(ValidationException.class);
    verify(shipmentController, never()).createShipment(any());
  }

  @Test
  public void shouldDelegateWhenNoAdditionalInfoProvided() {
    ShipmentDto request = new ShipmentDto();
    ShipmentDto created = new ShipmentDto();
    when(shipmentController.createShipment(request)).thenReturn(created);

    ShipmentDto result = controller.createWithAdditionalInfo(request);

    assertThat(result).isSameAs(created);
    verify(shipmentController).createShipment(request);
  }

  private ShipmentDto shipmentWithExtraData() {
    ShipmentDto dto = new ShipmentDto();
    Map<String, String> extraData = new HashMap<>();
    extraData.put("volumesCount", "6");
    dto.setExtraData(extraData);
    return dto;
  }
}
