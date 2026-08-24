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
import static org.openlmis.fulfillment.web.extension.AdditionalShipmentInfoValidator.ICE_PACKS_COUNT;
import static org.openlmis.fulfillment.web.extension.AdditionalShipmentInfoValidator.PACKING_PERSON;
import static org.openlmis.fulfillment.web.extension.AdditionalShipmentInfoValidator.SECURITY_SEAL;
import static org.openlmis.fulfillment.web.extension.AdditionalShipmentInfoValidator.TRAILER_REGISTRATION;
import static org.openlmis.fulfillment.web.extension.AdditionalShipmentInfoValidator.TRUCK_REGISTRATION;
import static org.openlmis.fulfillment.web.extension.AdditionalShipmentInfoValidator.VOLUMES_COUNT;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openlmis.fulfillment.web.ValidationException;

@SuppressWarnings("PMD.TooManyMethods")
public class AdditionalShipmentInfoValidatorTest {

  private AdditionalShipmentInfoValidator validator;

  @Before
  public void setUp() {
    validator = new AdditionalShipmentInfoValidator();
  }

  @Test
  public void shouldPassWhenExtraDataIsNull() {
    assertThat(catchThrowable(() -> validator.validate(null))).isNull();
  }

  @Test
  public void shouldPassWhenExtraDataIsEmpty() {
    assertThat(catchThrowable(() -> validator.validate(new HashMap<>()))).isNull();
  }

  @Test
  public void shouldPassWhenAllFieldsAreValid() {
    Map<String, String> extraData = new HashMap<>();
    extraData.put(VOLUMES_COUNT, "6");
    extraData.put(ICE_PACKS_COUNT, "0");
    extraData.put(PACKING_PERSON, "J. Silva");
    extraData.put(TRUCK_REGISTRATION, "AAA123XX");
    extraData.put(TRAILER_REGISTRATION, "BBB456XX");
    extraData.put(SECURITY_SEAL, "98234");

    assertThat(catchThrowable(() -> validator.validate(extraData))).isNull();
  }

  @Test
  public void shouldAcceptZeroCounts() {
    assertThat(catchThrowable(() -> validator.validate(map(VOLUMES_COUNT, "0")))).isNull();
    assertThat(catchThrowable(() -> validator.validate(map(ICE_PACKS_COUNT, "0")))).isNull();
  }

  @Test
  public void shouldRejectNegativeVolumesCount() {
    assertThat(catchThrowable(() -> validator.validate(map(VOLUMES_COUNT, "-1"))))
        .isInstanceOf(ValidationException.class);
  }

  @Test
  public void shouldRejectNonIntegerCount() {
    assertThat(catchThrowable(() -> validator.validate(map(VOLUMES_COUNT, "abc"))))
        .isInstanceOf(ValidationException.class);
    assertThat(catchThrowable(() -> validator.validate(map(VOLUMES_COUNT, "1.5"))))
        .isInstanceOf(ValidationException.class);
  }

  @Test
  public void shouldRejectUnicodeDigitCount() {
    // Integer.parseInt would have accepted these; the ASCII-only regex must reject them.
    // Built from code points (not escaped literals) to satisfy AvoidEscapedUnicodeCharacters.
    String arabicIndicSix = new String(Character.toChars(0x0666));
    String fullwidthSix = new String(Character.toChars(0xFF16));
    assertThat(catchThrowable(() -> validator.validate(map(VOLUMES_COUNT, arabicIndicSix))))
        .isInstanceOf(ValidationException.class);
    assertThat(catchThrowable(() -> validator.validate(map(VOLUMES_COUNT, fullwidthSix))))
        .isInstanceOf(ValidationException.class);
  }

  @Test
  public void shouldRejectWhitespacePaddedCount() {
    assertThat(catchThrowable(() -> validator.validate(map(VOLUMES_COUNT, " 6 "))))
        .isInstanceOf(ValidationException.class);
  }

  @Test
  public void shouldRejectSignedCount() {
    assertThat(catchThrowable(() -> validator.validate(map(VOLUMES_COUNT, "+6"))))
        .isInstanceOf(ValidationException.class);
  }

  @Test
  public void shouldRejectCountWithMoreThanNineDigits() {
    assertThat(catchThrowable(() -> validator.validate(map(VOLUMES_COUNT, "1000000000"))))
        .isInstanceOf(ValidationException.class);
  }

  @Test
  public void shouldAcceptNineDigitCount() {
    assertThat(catchThrowable(() -> validator.validate(map(VOLUMES_COUNT, "999999999")))).isNull();
  }

  @Test
  public void shouldTreatEmptyValueAsAbsent() {
    Map<String, String> extraData = new HashMap<>();
    extraData.put(VOLUMES_COUNT, "");
    extraData.put(TRUCK_REGISTRATION, "");
    extraData.put(PACKING_PERSON, "");

    assertThat(catchThrowable(() -> validator.validate(extraData))).isNull();
  }

  @Test
  public void shouldRejectNegativeIcePacksCount() {
    assertThat(catchThrowable(() -> validator.validate(map(ICE_PACKS_COUNT, "-3"))))
        .isInstanceOf(ValidationException.class);
  }

  @Test
  public void shouldAcceptValidTruckRegistration() {
    // The "XX" suffix is a placeholder for the two-letter province code, not a literal — any two
    // trailing capitals are valid (XX is just one of them).
    assertThat(catchThrowable(() -> validator.validate(map(TRUCK_REGISTRATION, "AAA123XX"))))
        .isNull();
    assertThat(catchThrowable(() -> validator.validate(map(TRUCK_REGISTRATION, "ABC123MP"))))
        .isNull();
  }

  @Test
  public void shouldAcceptNoVehiclePlaceholderTruckRegistration() {
    // Last-mile without a vehicle: the XXXXXXXX placeholder is accepted instead of a plate.
    assertThat(catchThrowable(() -> validator.validate(map(TRUCK_REGISTRATION, "XXXXXXXX"))))
        .isNull();
  }

  @Test
  public void shouldRejectMalformedTruckRegistration() {
    // No dashes or other separators are allowed; the groups run together (AAA123XX).
    String[] malformed = {"abc123xx", "AB1234XX", "ABC12XX", "AAA123A", "AAA1234XX", "AAA123",
        "AAA123xx", "AAA123XX\n", " AAA123XX", "ABC-123-MP", "AAA1-23XX", "A-AA123XX"};
    for (String value : malformed) {
      assertThat(catchThrowable(() -> validator.validate(map(TRUCK_REGISTRATION, value))))
          .as("expected '%s' to be rejected", value)
          .isInstanceOf(ValidationException.class);
    }
  }

  @Test
  public void shouldRejectTextExceedingMaxLength() {
    String tooLong = text(256);
    assertThat(catchThrowable(() -> validator.validate(map(PACKING_PERSON, tooLong))))
        .isInstanceOf(ValidationException.class);
    assertThat(catchThrowable(() -> validator.validate(map(TRAILER_REGISTRATION, tooLong))))
        .isInstanceOf(ValidationException.class);
    assertThat(catchThrowable(() -> validator.validate(map(SECURITY_SEAL, tooLong))))
        .isInstanceOf(ValidationException.class);
  }

  @Test
  public void shouldAcceptTextAtMaxLength() {
    assertThat(catchThrowable(() -> validator.validate(map(PACKING_PERSON, text(255))))).isNull();
  }

  @Test
  public void shouldReportFieldNameAndReasonInMessages() {
    assertThat(catchThrowable(() -> validator.validate(map(VOLUMES_COUNT, "-1"))))
        .hasMessageContaining("volumesCount must be a non-negative whole number");
    assertThat(catchThrowable(() -> validator.validate(map(PACKING_PERSON, text(256)))))
        .hasMessageContaining("packingPerson must be at most 255 characters");
    assertThat(catchThrowable(() -> validator.validate(map(TRUCK_REGISTRATION, "abc"))))
        .hasMessageContaining("Truck registration must match the format AAA123XX");
  }

  private static Map<String, String> map(String key, String value) {
    return Collections.singletonMap(key, value);
  }

  private static String text(int length) {
    return String.join("", Collections.nCopies(length, "a"));
  }
}
