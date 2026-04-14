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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import org.openlmis.fulfillment.OrderDataBuilder;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Year;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.openlmis.fulfillment.repository.FacilityOrderSequenceRepository;
import org.openlmis.fulfillment.repository.OrderSelvRepository;
import org.openlmis.fulfillment.service.referencedata.FacilityDto;
import org.openlmis.fulfillment.service.referencedata.FacilityReferenceDataService;


@RunWith(MockitoJUnitRunner.class)
public class SequenceNumberGeneratorTest {

  private static final String FACILITY_CODE = "TESTCODE";

  @InjectMocks
  private SequenceNumberGenerator sequenceNumberGenerator;

  @Mock
  private FacilityReferenceDataService facilityReferenceDataService;

  @Mock
  private OrderSelvRepository orderSelvRepository;

  @Mock
  private FacilityOrderSequenceRepository facilityOrderSequenceRepository;

  protected Order generateInstance() {
    return generateInstance(OrderStatus.FULFILLING);
  }

  private Order generateInstance(OrderStatus status) {
    return generateInstance(status, UUID.randomUUID(), UUID.randomUUID());
  }

  private Order generateInstance(UUID supplyingFacilityId) {
    return generateInstance(OrderStatus.FULFILLING, supplyingFacilityId, UUID.randomUUID());
  }

  private Order generateInstance(OrderStatus status, UUID supplyingFacilityId,
                                 UUID requestingFacilityId) {
    return new OrderDataBuilder()
        .withoutId()
        .withoutLineItems()
        .withStatus(status)
        .withSupplyingFacilityId(supplyingFacilityId)
        .withRequestingFacilityId(requestingFacilityId)
        .build();
  }

  @Test
  public void shouldCreateSequenceOrderCode() {
    FacilityDto facility = new FacilityDto();
    facility.setCode(FACILITY_CODE);
    when(facilityReferenceDataService.findOne(facility.getId())).thenReturn(facility);

    Order one = generateInstance(facility.getId());
    orderSelvRepository.save(one);
    String previous = "0002";
    when(orderSelvRepository.findLastOrderCodeOrCreateSequenceCode(facility.getId())).thenReturn(
        previous);
    when(facilityOrderSequenceRepository.getNextSequenceValue(eq(facility.getId()), anyInt()))
        .thenReturn(3);

    String expected = "0003";
    String expectedCode = Year.now().getValue() + "/" + facility.getCode() + "/" + expected;


    String orderNumber = sequenceNumberGenerator.generate(one);

    assertEquals(expectedCode, orderNumber);
  }

  @Test
  public void shouldCreateSequenceOrderCodeWhenNoPreviousOrderExist() {
    FacilityDto facility = new FacilityDto();
    facility.setCode(FACILITY_CODE);
    when(facilityReferenceDataService.findOne(facility.getId())).thenReturn(facility);

    Order one = generateInstance(facility.getId());
    orderSelvRepository.save(one);
    when(facilityOrderSequenceRepository.getNextSequenceValue(eq(facility.getId()), anyInt()))
        .thenReturn(1);

    String expected = "0001";
    String expectedCode = Year.now().getValue() + "/" + facility.getCode() + "/" + expected;


    String orderNumber = sequenceNumberGenerator.generate(one);

    assertEquals(expectedCode, orderNumber);
  }

  @Test
  public void shouldCreateSecondSequenceOrderCode() {
    FacilityDto facility = new FacilityDto();
    facility.setCode(FACILITY_CODE);
    when(facilityReferenceDataService.findOne(facility.getId())).thenReturn(facility);

    Order one = generateInstance(facility.getId());
    orderSelvRepository.save(one);
    String previous = "0001";
    when(orderSelvRepository.findLastOrderCodeOrCreateSequenceCode(facility.getId())).thenReturn(
        previous);
    when(facilityOrderSequenceRepository.getNextSequenceValue(eq(facility.getId()), anyInt()))
        .thenReturn(2);

    String expected = "0002";
    String expectedCode = Year.now().getValue() + "/" + facility.getCode() + "/" + expected;


    String orderNumber = sequenceNumberGenerator.generate(one);

    assertEquals(expectedCode, orderNumber);
  }

  @Test
  public void shouldCreateFirstSequenceOrderCodeWhenSendUnparsableString() {
    FacilityDto facility = new FacilityDto();
    facility.setCode(FACILITY_CODE);
    when(facilityReferenceDataService.findOne(facility.getId())).thenReturn(facility);

    Order one = generateInstance(facility.getId());
    orderSelvRepository.save(one);
    String previous = "XCASDW";
    when(orderSelvRepository.findLastOrderCodeOrCreateSequenceCode(facility.getId())).thenReturn(
        previous);
    when(facilityOrderSequenceRepository.getNextSequenceValue(eq(facility.getId()), anyInt()))
        .thenReturn(1);

    String expected = "0001";
    String expectedCode = Year.now().getValue() + "/" + facility.getCode() + "/" + expected;


    String orderNumber = sequenceNumberGenerator.generate(one);

    assertEquals(expectedCode, orderNumber);
  }

}
