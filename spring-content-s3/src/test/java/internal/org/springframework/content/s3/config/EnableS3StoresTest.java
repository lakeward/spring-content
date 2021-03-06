package internal.org.springframework.content.s3.config;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3ObjectId;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.content.commons.annotations.Content;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.s3.S3ObjectIdResolver;
import org.springframework.content.s3.config.EnableS3ContentRepositories;
import org.springframework.content.s3.config.EnableS3Stores;
import org.springframework.content.s3.config.S3ObjectIdResolvers;
import org.springframework.content.s3.config.S3StoreConfigurer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.ConverterRegistry;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.AfterEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(Ginkgo4jRunner.class)
public class EnableS3StoresTest {

	private AnnotationConfigApplicationContext context;

	// mocks
	static S3StoreConfigurer configurer;
	{
		Describe("EnableS3Stores", () -> {
			Context("given a context and a configuration with an S3 content repository bean",
					() -> {
						BeforeEach(() -> {
							context = new AnnotationConfigApplicationContext();
							context.register(TestConfig.class);
							context.refresh();
						});
						AfterEach(() -> {
							context.close();
						});
						It("should have a Content Repository bean", () -> {
							assertThat(context.getBean(TestEntityContentRepository.class),
									is(not(nullValue())));
						});
						It("should have an Placement Service", () -> {
							assertThat(context.getBean("s3StorePlacementService"),
									is(not(nullValue())));
						});
					});

			Context("given a context with a configurer", () -> {
				BeforeEach(() -> {
					configurer = mock(S3StoreConfigurer.class);

					context = new AnnotationConfigApplicationContext();
					context.register(ConverterConfig.class);
					context.refresh();
				});
				AfterEach(() -> {
					context.close();
				});
				It("should call that configurer to help setup the store", () -> {
					verify(configurer).configureS3StoreConverters(anyObject());
					verify(configurer).configureS3ObjectIdResolvers(anyObject());
				});
			});

			Context("given a context with an empty configuration", () -> {
				BeforeEach(() -> {
					context = new AnnotationConfigApplicationContext();
					context.register(EmptyConfig.class);
					context.refresh();
				});
				AfterEach(() -> {
					context.close();
				});
				It("should not contains any S3 repository beans", () -> {
					try {
						context.getBean(TestEntityContentRepository.class);
						fail("expected no such bean");
					}
					catch (NoSuchBeanDefinitionException e) {
						assertThat(true, is(true));
					}
				});
			});
		});

		Describe("EnableS3ContentRepositories", () -> {
			Context("given a context and a configuration with an S3 content repository bean",
					() -> {
						BeforeEach(() -> {
							context = new AnnotationConfigApplicationContext();
							context.register(EnableS3ContentRepositoriesConfig.class);
							context.refresh();
						});
						AfterEach(() -> {
							context.close();
						});
						It("should have a Content Repository bean", () -> {
							assertThat(context.getBean(TestEntityContentRepository.class),
									is(not(nullValue())));
						});
						It("should have an Placement Service", () -> {
							assertThat(context.getBean("s3StorePlacementService"),
									is(not(nullValue())));
						});
					});
		});
	}

	@Test
	public void noop() {
	}

	@Configuration
	@EnableS3Stores(basePackages = "contains.no.fs.repositores")
	@Import(InfrastructureConfig.class)
	public static class EmptyConfig {
	}

	@Configuration
	@EnableS3Stores
	@Import(InfrastructureConfig.class)
	public static class TestConfig {
	}

	@Configuration
	@EnableS3Stores
	@Import(InfrastructureConfig.class)
	public static class ConverterConfig {
		@Bean
		public S3StoreConfigurer configurer() {
			return configurer;
		}
	}

	@Configuration
	@EnableS3Stores
	@Import(InfrastructureConfig.class)
	public static class TestConverterConfig {
		@Bean
		public S3StoreConfigurer configurer() {
			return new S3StoreConfigurer() {

				@Override
				public void configureS3StoreConverters(ConverterRegistry registry) {
				}

				@Override
				public void configureS3ObjectIdResolvers(S3ObjectIdResolvers resolvers) {
					resolvers.add(new S3ObjectIdResolver<S3ObjectId>() {
						@Override
						public String getBucket(S3ObjectId idOrEntity,
								String defaultBucketName) {
							return idOrEntity.getBucket();
						}

						@Override
						public String getKey(S3ObjectId idOrEntity) {
							return idOrEntity.getKey();
						}
					});
				}
			};
		}
	}

	public interface TestEntityStore extends AssociativeStore<TestEntity, S3ObjectId> {
	}

	@Configuration
	@EnableS3ContentRepositories
	@Import(InfrastructureConfig.class)
	public static class EnableS3ContentRepositoriesConfig {
	}

	@Configuration
	public static class InfrastructureConfig {

		public Region region() {
			return Region.getRegion(Regions.US_WEST_1);
		}

		@Bean
		public AmazonS3 client() {
			AmazonS3Client client = new AmazonS3Client();
			client.setRegion(region());
			return client;
		}
	}

	@Content
	public class TestEntity {
		@ContentId
		private String contentId;
	}

	public interface TestEntityContentRepository
			extends ContentStore<TestEntity, String> {
	}
}
