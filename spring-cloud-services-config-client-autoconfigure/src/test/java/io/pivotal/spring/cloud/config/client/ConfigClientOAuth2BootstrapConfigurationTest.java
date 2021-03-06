/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.pivotal.spring.cloud.config.client;

import java.lang.reflect.Field;

import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.cloud.config.client.ConfigClientAutoConfiguration;
import org.springframework.cloud.config.client.ConfigServiceBootstrapConfiguration;
import org.springframework.cloud.config.client.ConfigServicePropertySourceLocator;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dylan Roberts
 */
public class ConfigClientOAuth2BootstrapConfigurationTest {

	private static final String CLIENT_ID = "clientId";

	private static final String CLIENT_SECRET = "clientSecret";

	private static final String TOKEN_URI = "tokenUri";

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withAllowBeanDefinitionOverriding(true)
			.withConfiguration(AutoConfigurations.of(ConfigClientAutoConfiguration.class,
					ConfigServiceBootstrapConfiguration.class, ConfigClientOAuth2BootstrapConfiguration.class));

	@Test
	public void configServicePropertySourceLocatorHasOAuth2AuthorizedClientHttpRequestInterceptor() throws Exception {
		this.contextRunner.withPropertyValues("spring.cloud.config.client.oauth2.client-id=" + CLIENT_ID,
				"spring.cloud.config.client.oauth2.client-secret=" + CLIENT_SECRET,
				"spring.cloud.config.client.oauth2.access-token-uri=" + TOKEN_URI).run(context -> {
					assertThat(context).hasSingleBean(ConfigServicePropertySourceLocator.class);
					ConfigServicePropertySourceLocator locator = context
							.getBean(ConfigServicePropertySourceLocator.class);
					RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(locator, "restTemplate");
					assertThat(restTemplate).isNotNull();
					assertThat(restTemplate.getInterceptors()).hasSize(1);
					assertThat(restTemplate.getInterceptors().get(0))
							.isInstanceOf(OAuth2AuthorizedClientHttpRequestInterceptor.class);
					OAuth2AuthorizedClientHttpRequestInterceptor interceptor = (OAuth2AuthorizedClientHttpRequestInterceptor) restTemplate
							.getInterceptors().get(0);
					ClientRegistration clientRegistration = interceptor.clientRegistration;
					assertThat(clientRegistration.getClientId()).isEqualTo(CLIENT_ID);
					assertThat(clientRegistration.getClientSecret()).isEqualTo(CLIENT_SECRET);
					assertThat(clientRegistration.getProviderDetails().getTokenUri()).isEqualTo(TOKEN_URI);
					assertThat(clientRegistration.getAuthorizationGrantType())
							.isEqualTo(AuthorizationGrantType.CLIENT_CREDENTIALS);
				});
	}

	@Test
	public void configServicePropertySourceLocatorIsUnchanged() throws Exception {
		this.contextRunner.run(context -> {
			assertThat(context).hasSingleBean(ConfigServicePropertySourceLocator.class);
			ConfigServicePropertySourceLocator locator = context.getBean(ConfigServicePropertySourceLocator.class);
			Field restTemplateField = ReflectionUtils.findField(ConfigServicePropertySourceLocator.class,
					"restTemplate");
			restTemplateField.setAccessible(true);
			assertThat(ReflectionUtils.getField(restTemplateField, locator)).isNull();
		});
	}

}