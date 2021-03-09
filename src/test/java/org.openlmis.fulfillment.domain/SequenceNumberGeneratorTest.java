package org.openlmis.fulfillment.domain;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import org.openlmis.fulfillment.OrderDataBuilder;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Year;
import java.util.Optional;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.repository.FacilityRepository;


@RunWith(MockitoJUnitRunner.class)
public class SequenceNumberGeneratorTest {

  @InjectMocks
  private SequenceNumberGenerator sequenceNumberGenerator;

  @Mock
  private FacilityRepository facilityRepository;

  @Mock
  private OrderRepository orderRepository;

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

    Facility facility = new Facility();
    facility.setCode("TESTCODE");
    when(facilityRepository.findById(facility.getId())).thenReturn(Optional.of(facility));

    Order one = generateInstance(facility.getId());
    orderRepository.save(one);
    when(orderRepository.findLastOrderCodeOrCreateSequenceCode(facility.getId())).thenReturn(
        previous);

    String expectedCode = Year.now().getValue() + "/" + facility.getCode() + "/" + expected;


    String orderNumber = sequenceNumberGenerator.generate(one);

    assertEquals(expectedCode, orderNumber);
  }

  @Test
  public void shouldCreateSequenceOrderCodeWhenNoPreviousOrderExist() {

    String expected = "0001";

    Facility facility = new Facility();
    facility.setCode("TESTCODE");
    when(facilityRepository.findById(facility.getId())).thenReturn(Optional.of(facility));

    Order one = generateInstance(facility.getId());
    orderRepository.save(one);

    String expectedCode = Year.now().getValue() + "/" + facility.getCode() + "/" + expected;


    String orderNumber = sequenceNumberGenerator.generate(one);

    assertEquals(expectedCode, orderNumber);
  }

  @Test
  public void shouldCreateSecondSequenceOrderCode() {

    String previous = "0001";
    String expected = "0002";

    Facility facility = new Facility();
    facility.setCode("TESTCODE");
    when(facilityRepository.findById(facility.getId())).thenReturn(Optional.of(facility));

    Order one = generateInstance(facility.getId());
    orderRepository.save(one);
    when(orderRepository.findLastOrderCodeOrCreateSequenceCode(facility.getId())).thenReturn(
        previous);

    String expectedCode = Year.now().getValue() + "/" + facility.getCode() + "/" + expected;


    String orderNumber = sequenceNumberGenerator.generate(one);

    assertEquals(expectedCode, orderNumber);
  }

  @Test
  public void shouldCreateFirstSequenceOrderCodeWhenSendUnparsableString() {

    String previous = "XCASDW";
    String expected = "0001";

    Facility facility = new Facility();
    facility.setCode("TESTCODE");
    when(facilityRepository.findById(facility.getId())).thenReturn(Optional.of(facility));

    Order one = generateInstance(facility.getId());
    orderRepository.save(one);
    when(orderRepository.findLastOrderCodeOrCreateSequenceCode(facility.getId())).thenReturn(
        previous);

    String expectedCode = Year.now().getValue() + "/" + facility.getCode() + "/" + expected;


    String orderNumber = sequenceNumberGenerator.generate(one);

    assertEquals(expectedCode, orderNumber);
  }

}
