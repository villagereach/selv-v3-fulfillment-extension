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

import java.util.Map;
import org.openlmis.fulfillment.web.ValidationException;
import org.springframework.stereotype.Component;

/**
 * Validates the SELV additional shipment fields carried in {@code ShipmentDto.extraData} before the
 * shipment is created. Every field is optional, so a value is checked only when it is present:
 * the counter fields must be non-negative whole numbers, the truck registration must match the
 * Mozambican plate format, and the free-text fields are length-bounded.
 */
@Component
public class AdditionalShipmentInfoValidator {

  static final String VOLUMES_COUNT = "volumesCount";
  static final String ICE_PACKS_COUNT = "icePacksCount";
  static final String PACKING_PERSON = "packingPerson";
  static final String TRUCK_REGISTRATION = "truckRegistration";
  static final String TRAILER_REGISTRATION = "trailerRegistration";
  static final String SECURITY_SEAL = "securitySeal";

  static final int MAX_TEXT_LENGTH = 255;
  // Three letters, three digits, then the two-letter province code. The HLD/SELVSUP-55 wrote the
  // suffix as "XX" (e.g. ABC123XX), but that is a placeholder for the two province letters, not a
  // literal "XX" — so any two trailing capitals are accepted (XX included).
  // Keep byte-identical with the UI (selv-v3-ui choose-date-modal.controller.js
  // truckRegistrationPattern).
  static final String TRUCK_REGISTRATION_PATTERN = "^[A-Z]{3}[0-9]{3}[A-Z]{2}$";
  // ASCII digits only - Integer.parseInt would also accept Unicode digits (e.g. Arabic-Indic) and
  // a leading sign. 1-9 digits keeps the value a non-negative integer within int range, so a
  // downstream report cast cannot overflow.
  static final String NON_NEGATIVE_INTEGER_PATTERN = "^[0-9]{1,9}$";

  // Literal messages (not message keys): an extension cannot contribute to the core
  // classpath:messages bundle, and core renders an unknown key verbatim, so a readable literal is
  // clearer here. Mirrors the SequenceNumberGenerator/ExchangeRate literal-message pattern.
  static final String ERROR_COUNT_INVALID = "must be a non-negative whole number";
  static final String ERROR_TEXT_TOO_LONG = "must be at most " + MAX_TEXT_LENGTH + " characters";
  static final String ERROR_TRUCK_FORMAT =
      "Truck registration must match the format AAA123XX "
          + "(three letters, three digits, two letters).";

  /**
   * Validates the additional shipment fields. Each field is optional; a present value is checked
   * against its constraint. A null map (no additional info supplied) passes.
   *
   * @param extraData the shipment's extraData map, may be null
   */
  public void validate(Map<String, String> extraData) {
    if (extraData == null) {
      return;
    }
    validateCount(extraData.get(VOLUMES_COUNT), VOLUMES_COUNT);
    validateCount(extraData.get(ICE_PACKS_COUNT), ICE_PACKS_COUNT);
    validateLength(extraData.get(PACKING_PERSON), PACKING_PERSON);
    validateTruckRegistration(extraData.get(TRUCK_REGISTRATION));
    validateLength(extraData.get(TRAILER_REGISTRATION), TRAILER_REGISTRATION);
    validateLength(extraData.get(SECURITY_SEAL), SECURITY_SEAL);
  }

  private void validateCount(String value, String field) {
    // Regex (not Integer.parseInt) so Unicode digits, signs, decimals and surrounding whitespace
    // are all rejected, and the stored value is guaranteed to equal the validated one.
    if (isProvided(value) && !value.matches(NON_NEGATIVE_INTEGER_PATTERN)) {
      throw new ValidationException(message(field, ERROR_COUNT_INVALID));
    }
  }

  private void validateLength(String value, String field) {
    if (isProvided(value) && value.length() > MAX_TEXT_LENGTH) {
      throw new ValidationException(message(field, ERROR_TEXT_TOO_LONG));
    }
  }

  private void validateTruckRegistration(String value) {
    if (isProvided(value) && !value.matches(TRUCK_REGISTRATION_PATTERN)) {
      throw new ValidationException(ERROR_TRUCK_FORMAT);
    }
  }

  private static String message(String field, String suffix) {
    return field + " " + suffix;
  }

  // A field counts as provided only when it is non-null and non-empty; an empty value is treated
  // the same as an absent one (the fields are optional).
  private static boolean isProvided(String value) {
    return value != null && !value.isEmpty();
  }
}
