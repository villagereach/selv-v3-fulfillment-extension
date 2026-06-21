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

package org.openlmis.fulfillment.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.openlmis.fulfillment.repository.ExchangeRateRepository;
import org.openlmis.fulfillment.service.ResultDto;
import org.openlmis.fulfillment.service.referencedata.RightDto;
import org.openlmis.fulfillment.service.referencedata.UserDto;
import org.openlmis.fulfillment.service.referencedata.UserReferenceDataService;
import org.openlmis.fulfillment.util.AuthenticationException;
import org.openlmis.fulfillment.util.AuthenticationHelper;
import org.openlmis.fulfillment.web.util.ExchangeRateDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * SELV exchange-rate (USD-MZM) endpoints. Reads open to any authenticated user; POST requires
 * the {@code EXCHANGE_RATE_MANAGE} right.
 */
@Controller
public class ExchangeRateController extends BaseController {

  static final String EXCHANGE_RATE_MANAGE = "EXCHANGE_RATE_MANAGE";
  // Literal message (not a key): an extension cannot contribute to the core `classpath:messages`
  // bundle (single basename, no merge), and core renders unknown keys verbatim anyway.
  static final String ERROR_RATE_INVALID = "Exchange rate must be a positive number";

  @Autowired
  private ExchangeRateRepository exchangeRateRepository;

  @Autowired
  private AuthenticationHelper authenticationHelper;

  @Autowired
  private UserReferenceDataService userReferenceDataService;

  /**
   * Returns the currently active rate (the one with the greatest valid_from), or 204 if none.
   */
  @GetMapping("/exchangeRates/current")
  @ResponseBody
  public ResponseEntity<ExchangeRateDto> getCurrent() {
    ExchangeRateDto current = exchangeRateRepository.findCurrent();
    if (current == null) {
      return ResponseEntity.noContent().build();
    }
    return ResponseEntity.ok(resolveAuthor(current));
  }

  /**
   * Returns the full rate history, newest first.
   */
  @GetMapping("/exchangeRates")
  @ResponseBody
  public List<ExchangeRateDto> getHistory() {
    List<ExchangeRateDto> history = exchangeRateRepository.findHistory();
    Set<UUID> authorIds = history.stream()
        .map(ExchangeRateDto::getCreatedById)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    if (!authorIds.isEmpty()) {
      Map<UUID, String> names = new HashMap<>();
      userReferenceDataService.findByIds(authorIds)
          .forEach(user -> names.put(user.getId(), user.printName()));
      history.forEach(dto -> dto.setCreatedByName(names.get(dto.getCreatedById())));
    }
    return history;
  }

  /**
   * Inserts a new rate, which immediately becomes the current one. Requires EXCHANGE_RATE_MANAGE.
   */
  @PostMapping("/exchangeRates")
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public ExchangeRateDto create(@RequestBody ExchangeRateDto exchangeRateDto) {
    checkManageRight();
    if (exchangeRateDto.getRate() == null || exchangeRateDto.getRate().signum() <= 0) {
      throw new ValidationException(ERROR_RATE_INVALID);
    }
    UUID createdById = authenticationHelper.getCurrentUser().getId();
    return resolveAuthor(exchangeRateRepository.insert(exchangeRateDto.getRate(), createdById));
  }

  @SuppressWarnings("PMD.PreserveStackTrace") // intentional translation to a 403, original is noise
  private void checkManageRight() {
    UserDto user = authenticationHelper.getCurrentUser();
    if (user == null) {
      throw new MissingPermissionException(EXCHANGE_RATE_MANAGE);
    }
    RightDto right;
    try {
      right = authenticationHelper.getRight(EXCHANGE_RATE_MANAGE);
    } catch (AuthenticationException ex) {
      // Right not provisioned in referencedata — treat as missing permission (403), not a 500.
      throw new MissingPermissionException(EXCHANGE_RATE_MANAGE);
    }
    ResultDto<Boolean> result = userReferenceDataService
        .hasRight(user.getId(), right.getId(), null, null, null);
    if (result == null || !Boolean.TRUE.equals(result.getResult())) {
      throw new MissingPermissionException(EXCHANGE_RATE_MANAGE);
    }
  }

  private ExchangeRateDto resolveAuthor(ExchangeRateDto dto) {
    if (dto.getCreatedById() != null) {
      UserDto user = userReferenceDataService.findOne(dto.getCreatedById());
      if (user != null) {
        dto.setCreatedByName(user.printName());
      }
    }
    return dto;
  }
}
