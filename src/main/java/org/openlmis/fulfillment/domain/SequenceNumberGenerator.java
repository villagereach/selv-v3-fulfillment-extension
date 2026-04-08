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

import org.openlmis.fulfillment.repository.FacilityOrderSequenceRepository;
import org.openlmis.fulfillment.repository.OrderSelvRepository;
import org.openlmis.fulfillment.extension.point.OrderNumberGenerator;
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
  private OrderSelvRepository orderSelvRepository;

  @Autowired
  private FacilityOrderSequenceRepository facilityOrderSequenceRepository;

  @Autowired
  private FacilityReferenceDataService facilityReferenceDataService;


  @Override
  public String generate(Order order) {

    FacilityDto supplyingFacility = findSupplyingFacility(order.getSupplyingFacilityId());

    int sequenceValue = getNextSequenceValue(supplyingFacility.getId());

    String newCode = String.format("%04d", sequenceValue);

    return Year.now().getValue() + "/" + supplyingFacility.getCode() + "/" + newCode;
  }

  private int getNextSequenceValue(UUID facilityId) {
    facilityOrderSequenceRepository.lockTable();
    FacilityOrderSequence sequence = facilityOrderSequenceRepository.findByFacilityIdWithLock(facilityId);

    if (sequence == null) {
      String lastOrderCode = orderSelvRepository.findLastOrderCodeOrCreateSequenceCode(facilityId);
      int initialValue = (lastOrderCode != null) ? parseOrderCode(lastOrderCode) : 0;
      FacilityOrderSequence newSequence = new FacilityOrderSequence(facilityId, initialValue);
      facilityOrderSequenceRepository.save(newSequence);
      sequence = facilityOrderSequenceRepository.findByFacilityIdWithLock(facilityId);
    }

    int newValue = sequence.getLastSequenceValue() + 1;
    sequence.setLastSequenceValue(newValue);
    facilityOrderSequenceRepository.save(sequence);
    return newValue;
  }

  private int parseOrderCode(String orderCode) {
    if (orderCode == null) {
      return 0;
    }
    try {
      return Integer.parseInt(orderCode);
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  private FacilityDto findSupplyingFacility(UUID facilityId) {

    FacilityDto facility = facilityReferenceDataService.findOne(facilityId);

    if (facility == null) {
      throw new NotFoundException(new Message(
          "Facility not found", facilityId).toString());
    }
    return facility;
  }
}
