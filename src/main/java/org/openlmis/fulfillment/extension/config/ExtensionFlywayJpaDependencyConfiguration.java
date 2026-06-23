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

import org.springframework.boot.autoconfigure.data.jpa.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.context.annotation.Configuration;

/**
 * Ensures the JPA {@code entityManagerFactory} (and therefore Hibernate schema validation) is
 * created only AFTER the extension Flyway migrations have run, so entities backed by extension
 * migrations (e.g. {@code exchange_rates}) already exist when Hibernate validates the schema.
 *
 * <p>Core only orders {@code extensionFlywayMigrationInitializer} after the core
 * {@code flywayInitializer}; it does not order it before the entity manager factory, so without
 * this post-processor the factory may be built first and validation fails with a missing table.
 * This mirrors Spring Boot's own {@code FlywayInitializerJpaDependencyConfiguration}.
 */
@Configuration(proxyBeanMethods = false)
public class ExtensionFlywayJpaDependencyConfiguration extends EntityManagerFactoryDependsOnPostProcessor {

  /**
   * Declares the entity manager factory's dependency on the extension Flyway initializer bean.
   */
  public ExtensionFlywayJpaDependencyConfiguration() {
    super("extensionFlywayMigrationInitializer");
  }
}
