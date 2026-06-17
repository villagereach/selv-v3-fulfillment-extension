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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openlmis.fulfillment.repository.ExchangeRateRepository;
import org.openlmis.fulfillment.service.ResultDto;
import org.openlmis.fulfillment.service.referencedata.RightDto;
import org.openlmis.fulfillment.service.referencedata.UserDto;
import org.openlmis.fulfillment.service.referencedata.UserReferenceDataService;
import org.openlmis.fulfillment.util.AuthenticationHelper;
import org.openlmis.fulfillment.web.util.ExchangeRateDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"unchecked", "PMD.UnusedPrivateField"})
public class ExchangeRateControllerTest {

  @Mock
  private ExchangeRateRepository exchangeRateRepository;

  @Mock
  private AuthenticationHelper authenticationHelper;

  @Mock
  private UserReferenceDataService userReferenceDataService;

  @InjectMocks
  private ExchangeRateController controller;

  private static final String RATE = "64.25";

  private final UUID userId = UUID.randomUUID();
  private final UUID rightId = UUID.randomUUID();

  @Before
  public void setUp() {
    UserDto user = new UserDto();
    user.setId(userId);
    user.setFirstName("Ada");
    user.setLastName("Macamo");
    when(authenticationHelper.getCurrentUser()).thenReturn(user);

    RightDto right = new RightDto();
    right.setId(rightId);
    when(authenticationHelper.getRight("EXCHANGE_RATE_MANAGE")).thenReturn(right);

    when(userReferenceDataService.findOne(userId)).thenReturn(user);
  }

  private void grantManageRight(boolean granted) {
    ResultDto<Boolean> result = new ResultDto<>(granted);
    when(userReferenceDataService.hasRight(eq(userId), eq(rightId), any(), any(), any()))
        .thenReturn(result);
  }

  @Test
  public void postShouldRejectWhenUserHasNoManageRight() {
    grantManageRight(false);
    ExchangeRateDto body = new ExchangeRateDto();
    body.setRate(new BigDecimal(RATE));

    Throwable thrown = catchThrowable(() -> controller.create(body));

    assertThat(thrown).isInstanceOf(MissingPermissionException.class);
    verify(exchangeRateRepository, never()).insert(any(), any());
  }

  @Test
  public void postShouldRejectNonPositiveRate() {
    grantManageRight(true);
    ExchangeRateDto body = new ExchangeRateDto();
    body.setRate(BigDecimal.ZERO);

    Throwable thrown = catchThrowable(() -> controller.create(body));

    assertThat(thrown).isInstanceOf(ValidationException.class);
    verify(exchangeRateRepository, never()).insert(any(), any());
  }

  @Test
  public void postShouldInsertWithCurrentUserAsCreator() {
    grantManageRight(true);
    ExchangeRateDto body = new ExchangeRateDto();
    body.setRate(new BigDecimal(RATE));
    when(exchangeRateRepository.insert(new BigDecimal(RATE), userId))
        .thenReturn(rateDto(new BigDecimal(RATE), userId));

    ExchangeRateDto result = controller.create(body);

    verify(exchangeRateRepository).insert(new BigDecimal(RATE), userId);
    assertThat(result.getRate()).isEqualByComparingTo(RATE);
  }

  @Test
  public void getCurrentShouldReturnRateWithResolvedAuthorName() {
    when(exchangeRateRepository.findCurrent()).thenReturn(rateDto(new BigDecimal(RATE), userId));

    ResponseEntity<ExchangeRateDto> response = controller.getCurrent();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getCreatedByName()).isEqualTo("Ada Macamo");
  }

  @Test
  public void getCurrentShouldReturnNoContentWhenNoRate() {
    when(exchangeRateRepository.findCurrent()).thenReturn(null);

    ResponseEntity<ExchangeRateDto> response = controller.getCurrent();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
  }

  @Test
  public void getHistoryShouldReturnAllRatesWithResolvedNames() {
    when(exchangeRateRepository.findHistory())
        .thenReturn(Arrays.asList(rateDto(new BigDecimal(RATE), userId),
            rateDto(new BigDecimal("63.50"), userId)));
    UserDto author = new UserDto();
    author.setId(userId);
    author.setFirstName("Ada");
    author.setLastName("Macamo");
    when(userReferenceDataService.findByIds(any())).thenReturn(Collections.singletonList(author));

    java.util.List<ExchangeRateDto> history = controller.getHistory();

    assertThat(history).hasSize(2);
    assertThat(history.get(0).getCreatedByName()).isEqualTo("Ada Macamo");
  }

  private ExchangeRateDto rateDto(BigDecimal rate, UUID createdById) {
    return new ExchangeRateDto(UUID.randomUUID(), rate, ZonedDateTime.now(), createdById, null);
  }
}
