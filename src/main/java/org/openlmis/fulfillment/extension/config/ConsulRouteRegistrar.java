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

package org.openlmis.fulfillment.extension.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Registers the SELV extension endpoints with Consul on startup so the gateway forwards their
 * {@code /api/...} paths to this service. On {@code ApplicationReadyEvent} it writes the
 * {@code resources/api/...} Consul KV entries that the gateway's consul-template reads to build
 * its upstream routes. Each feature lists its endpoint paths in {@code CONSUL_KV_KEYS}.
 */
@Component
public class ConsulRouteRegistrar {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConsulRouteRegistrar.class);

  private static final String SERVICE_NAME = "fulfillment";
  private static final String[] CONSUL_KV_KEYS = {
      "resources/api/exchangeRates",
      "resources/api/exchangeRates/current",
      "resources/api/extension/shipments/withAdditionalInfo"
  };

  @Value("${CONSUL_HOST:consul}")
  private String consulHost;

  @Value("${CONSUL_PORT:8500}")
  private String consulPort;

  // package-private so tests can replace them
  RestTemplate restTemplate = new RestTemplate();
  int maxAttempts = 5;
  long retryBackoffMs = 5000L;

  /**
   * Publishes each extension endpoint's consul routing entry so nginx forwards its
   * {@code /api/...} path to this service. Each entry retries on failure; exhausted retries are
   * logged but never block service startup.
   */
  @EventListener(ApplicationReadyEvent.class)
  public void registerRoutes() {
    for (String consulKey : CONSUL_KV_KEYS) {
      registerRoute(consulKey);
    }
  }

  void registerRoute(String consulKey) {
    String url = "http://" + consulHost + ":" + consulPort + "/v1/kv/" + consulKey;
    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      try {
        restTemplate.exchange(url, HttpMethod.PUT,
            new HttpEntity<>(SERVICE_NAME), String.class);
        LOGGER.info("Registered consul route {} -> {} (attempt {})",
            consulKey, SERVICE_NAME, attempt);
        return;
      } catch (Exception ex) {
        LOGGER.warn("Attempt {}/{} to register consul route {} failed: {}",
            attempt, maxAttempts, consulKey, ex.getMessage());
        if (attempt < maxAttempts) {
          sleepQuietly(retryBackoffMs);
        }
      }
    }
    LOGGER.warn("Could not register consul route {} after {} attempts; calls will return 404 "
        + "until consul is updated", consulKey, maxAttempts);
  }

  private static void sleepQuietly(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }
}
