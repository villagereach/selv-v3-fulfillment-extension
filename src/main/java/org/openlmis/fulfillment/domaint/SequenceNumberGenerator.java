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

package org.openlmis.fulfillment.domaint;

import org.openlmis.fulfillment.extension.point.OrderNumberGenerator;
import org.openlmis.fulfillment.domain.Order;
import org.springframework.stereotype.Component;
import java.math.BigInteger;
import java.time.Year;

@Component(value = "SequenceNumberGenerator")
public class SequenceNumberGenerator implements OrderNumberGenerator {

  @Override
  public String generate(Order order) {

    order.getFacilityId();
    Year.now().getValue();
    String id = order.getExternalId().toString();
    String base36Id = new BigInteger(id.replace("-", ""), 16)
        .toString(36).toUpperCase();

    return base36Id.substring(0, 8);
  }
}
