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

package org.openlmis.fulfillment.domain;

import org.openlmis.fulfillment.extension.point.OrderNumberGenerator;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.openlmis.fulfillment.service.referencedata.FacilityDto;
import org.openlmis.fulfillment.service.referencedata.FacilityReferenceDataService;
import org.openlmis.fulfillment.util.Message;
import org.openlmis.fulfillment.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.time.Year;
import java.util.UUID;


@Component(value = "SequenceNumberGenerator")
public class SequenceNumberGenerator implements OrderNumberGenerator {

  @Autowired
  private OrderRepository orderRepository;

  @Autowired
  private FacilityReferenceDataService facilityReferenceDataService;


  @Override
  public String generate(Order order) {

    FacilityDto supplyingFacility = findSupplyingFacility(order.getSupplyingFacilityId());

    String previousNumberOrderCode =
        orderRepository.findLastOrderCodeOrCreateSequenceCode(supplyingFacility.getId());

    String newCode = generateOrderCode(previousNumberOrderCode);

    return Year.now().getValue() + "/" + supplyingFacility.getCode() + "/" + newCode;
  }

  private FacilityDto  findSupplyingFacility(UUID facilityId) {

    FacilityDto facility = facilityReferenceDataService.findOne(facilityId);

    if (facility == null) {
      throw new NotFoundException(new Message(
          "Facility not found", facilityId).toString());
    }
    return facility;
  }

  private String generateOrderCode(String previousNumberOrderCode) {

    if (previousNumberOrderCode == null) {
      previousNumberOrderCode = "0000";
    }

    String newCode;
    Integer parsed;

    try {
      parsed = Integer.parseInt(previousNumberOrderCode);
    } catch (NumberFormatException e) {
      parsed = 0;
    }
    parsed++;
    if (parsed <= 9) {
      newCode = "000" + parsed;
    } else if (parsed <= 99) {
      newCode = "00" + parsed;
    } else if (parsed <= 999) {
      newCode = "0" + parsed;
    } else {
      newCode = parsed.toString();
    }
    return newCode;
  }
}
