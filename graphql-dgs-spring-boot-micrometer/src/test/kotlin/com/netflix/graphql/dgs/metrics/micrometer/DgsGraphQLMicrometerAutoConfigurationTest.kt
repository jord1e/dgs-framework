/*
 * Copyright 2021 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.graphql.dgs.metrics.micrometer

import com.netflix.graphql.dgs.metrics.micrometer.dataloader.DgsDataLoaderInstrumentationProvider
import com.netflix.graphql.dgs.metrics.micrometer.tagging.DgsGraphQLMetricsTagsProvider
import com.netflix.graphql.dgs.metrics.micrometer.tagging.SimpleGqlOutcomeTagCustomizer
import io.mockk.mockkClass
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.context.annotation.UserConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

internal class DgsGraphQLMicrometerAutoConfigurationTest {

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(
                CompositeMeterRegistryAutoConfiguration::class.java,
                MetricsAutoConfiguration::class.java,
                DgsGraphQLMicrometerAutoConfiguration::class.java
            )
        )

    @Test
    fun `Default settings`() {
        contextRunner.run { ctx ->

            assertThat(ctx)
                .hasSingleBean(DgsGraphQLMetricsInstrumentation::class.java)
                .hasSingleBean(DgsDataLoaderInstrumentationProvider::class.java)
                .hasSingleBean(SimpleGqlOutcomeTagCustomizer::class.java)

            assertThat(ctx)
                .hasSingleBean(DgsMeterRegistrySupplier::class.java)
                .getBean(DgsMeterRegistrySupplier::class.java)
                .extracting { assertThat(it.get()).isNotNull }

            assertThat(ctx)
                .hasSingleBean(DgsGraphQLMetricsTagsProvider::class.java)
                .getBean(DgsGraphQLMetricsTagsProvider::class.java)
                .extracting {
                    assertThat(it).isExactlyInstanceOf(DgsGraphQLCollatedMetricsTagsProvider::class.java)
                }
        }
    }

    @Test
    fun `Beans can be disabled`() {
        contextRunner
            .withPropertyValues("management.metrics.dgs-graphql.enabled=false").run { ctx ->
                assertThat(ctx).doesNotHaveBean(DgsGraphQLMicrometerAutoConfiguration::class.java)
            }

        contextRunner
            .withPropertyValues("management.metrics.dgs-graphql.instrumentation.enabled=false").run { ctx ->
                assertThat(ctx)
                    .doesNotHaveBean(DgsGraphQLMetricsInstrumentation::class.java)

                assertThat(ctx)
                    .hasSingleBean(DgsDataLoaderInstrumentationProvider::class.java)
                    .hasSingleBean(SimpleGqlOutcomeTagCustomizer::class.java)
            }

        contextRunner
            .withPropertyValues("management.metrics.dgs-graphql.data-loader-instrumentation.enabled=false").run { ctx ->
                assertThat(ctx)
                    .doesNotHaveBean(DgsDataLoaderInstrumentationProvider::class.java)

                assertThat(ctx)
                    .hasSingleBean(DgsGraphQLMetricsInstrumentation::class.java)
                    .hasSingleBean(SimpleGqlOutcomeTagCustomizer::class.java)
            }
    }

    @Test
    fun `Beans can be replaced`() {
        contextRunner
            .withConfiguration(UserConfigurations.of(LocalConfiguration::class.java)).run { ctx ->
                assertThat(ctx).hasSingleBean(DgsMeterRegistrySupplier::class.java)
                    .getBean(DgsMeterRegistrySupplier::class.java).extracting {
                        assertThat(it).isNotExactlyInstanceOf(
                            DgsGraphQLMicrometerAutoConfiguration.DefaultMeterRegistrySupplier::class.java
                        )
                    }
            }
    }

    @Configuration
    open class LocalConfiguration {
        @Bean
        open fun mockMeterRegistrySupplier(): DgsMeterRegistrySupplier {
            return mockkClass(DgsMeterRegistrySupplier::class)
        }
    }
}
