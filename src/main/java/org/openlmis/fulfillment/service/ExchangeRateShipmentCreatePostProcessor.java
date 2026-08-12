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

import org.openlmis.fulfillment.domain.Shipment;
import org.openlmis.fulfillment.extension.point.ShipmentCreatePostProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Runs after a shipment is created: performs the core default behaviour (stock event) and then
 * snapshots the current exchange rate onto the shipment's order. This covers requisition-less
 * orders, which do not pass through the order-create extension point and would otherwise never
 * have a rate captured.
 */
@Component("ExchangeRateShipmentCreatePostProcessor")
public class ExchangeRateShipmentCreatePostProcessor implements ShipmentCreatePostProcessor {

  @Autowired
  private DefaultShipmentCreatePostProcessor defaultShipmentCreatePostProcessor;

  @Autowired
  private ExchangeRateSnapshotService exchangeRateSnapshotService;

  @Override
  public void process(Shipment shipment) {
    defaultShipmentCreatePostProcessor.process(shipment);
    exchangeRateSnapshotService.snapshotCurrentRateIfAbsent(shipment.getOrder());
  }
}
