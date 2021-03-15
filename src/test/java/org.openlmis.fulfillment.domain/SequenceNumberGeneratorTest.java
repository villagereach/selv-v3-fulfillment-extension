package org.openlmis.fulfillment.domain;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import org.openlmis.fulfillment.OrderDataBuilder;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Year;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.openlmis.fulfillment.repository.OrderSelvRepository;
import org.openlmis.fulfillment.service.referencedata.FacilityDto;
import org.openlmis.fulfillment.service.referencedata.FacilityReferenceDataService;


@RunWith(MockitoJUnitRunner.class)
public class SequenceNumberGeneratorTest {

  @InjectMocks
  private SequenceNumberGenerator sequenceNumberGenerator;

  @Mock
  private FacilityReferenceDataService facilityReferenceDataService;

  @Mock
  private OrderSelvRepository orderSelvRepository;

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

    String previous = "0002";
    String expected = "0003";

    FacilityDto facility = new FacilityDto();
    facility.setCode("TESTCODE");
    when(facilityReferenceDataService.findOne(facility.getId())).thenReturn(facility);

    Order one = generateInstance(facility.getId());
    orderSelvRepository.save(one);
    when(orderSelvRepository.findLastOrderCodeOrCreateSequenceCode(facility.getId())).thenReturn(
        previous);

    String expectedCode = Year.now().getValue() + "/" + facility.getCode() + "/" + expected;


    String orderNumber = sequenceNumberGenerator.generate(one);

    assertEquals(expectedCode, orderNumber);
  }

  @Test
  public void shouldCreateSequenceOrderCodeWhenNoPreviousOrderExist() {

    String expected = "0001";

    FacilityDto facility = new FacilityDto();
    facility.setCode("TESTCODE");
    when(facilityReferenceDataService.findOne(facility.getId())).thenReturn(facility);

    Order one = generateInstance(facility.getId());
    orderSelvRepository.save(one);

    String expectedCode = Year.now().getValue() + "/" + facility.getCode() + "/" + expected;


    String orderNumber = sequenceNumberGenerator.generate(one);

    assertEquals(expectedCode, orderNumber);
  }

  @Test
  public void shouldCreateSecondSequenceOrderCode() {

    String previous = "0001";
    String expected = "0002";

    FacilityDto facility = new FacilityDto();
    facility.setCode("TESTCODE");
    when(facilityReferenceDataService.findOne(facility.getId())).thenReturn(facility);

    Order one = generateInstance(facility.getId());
    orderSelvRepository.save(one);
    when(orderSelvRepository.findLastOrderCodeOrCreateSequenceCode(facility.getId())).thenReturn(
        previous);

    String expectedCode = Year.now().getValue() + "/" + facility.getCode() + "/" + expected;


    String orderNumber = sequenceNumberGenerator.generate(one);

    assertEquals(expectedCode, orderNumber);
  }

  @Test
  public void shouldCreateFirstSequenceOrderCodeWhenSendUnparsableString() {

    String previous = "XCASDW";
    String expected = "0001";

    FacilityDto facility = new FacilityDto();
    facility.setCode("TESTCODE");
    when(facilityReferenceDataService.findOne(facility.getId())).thenReturn(facility);

    Order one = generateInstance(facility.getId());
    orderSelvRepository.save(one);
    when(orderSelvRepository.findLastOrderCodeOrCreateSequenceCode(facility.getId())).thenReturn(
        previous);

    String expectedCode = Year.now().getValue() + "/" + facility.getCode() + "/" + expected;


    String orderNumber = sequenceNumberGenerator.generate(one);

    assertEquals(expectedCode, orderNumber);
  }

}
