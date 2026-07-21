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

import org.openlmis.fulfillment.web.BaseController;
import org.openlmis.fulfillment.web.shipment.ShipmentController;
import org.openlmis.fulfillment.web.shipment.ShipmentDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * SELV extension endpoint that captures the additional shipment fields. It pre-validates the fields
 * carried in {@code ShipmentDto.extraData} and then delegates to the core shipment-create flow in a
 * single {@code @Transactional} method, so a shipment is never created without its additional info
 * having been validated first.
 */
@Controller
public class ExtensionShipmentController extends BaseController {

  // Delegates to the core ShipmentController bean (rather than a service) on purpose: core exposes
  // the full create-shipment orchestration only through this controller method — there is no
  // reusable ShipmentCreateService — and the HLD mandates no core changes, exact reuse, and one
  // transaction. Calling the handler programmatically is valid: the Spring proxy still applies the
  // class-level @Transactional, while @RequestMapping/@RequestBody are inert on a direct call.
  @Autowired
  private ShipmentController shipmentController;

  @Autowired
  private AdditionalShipmentInfoValidator additionalShipmentInfoValidator;

  /**
   * Validates the additional shipment fields and creates the shipment via the core controller.
   *
   * @param shipmentDto the shipment to create, carrying the additional fields in its extraData
   * @return the created shipment
   */
  @PostMapping("/extension/shipments/withAdditionalInfo")
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  @Transactional
  public ShipmentDto createWithAdditionalInfo(@RequestBody ShipmentDto shipmentDto) {
    additionalShipmentInfoValidator.validate(shipmentDto.getExtraData());
    return shipmentController.createShipment(shipmentDto);
  }
}
