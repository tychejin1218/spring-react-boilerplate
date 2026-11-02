package com.example.api.config;

import com.example.api.common.constants.Constants;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import javax.sql.DataSource;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.Alias;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.boot.jpa.autoconfigure.JpaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.util.ObjectUtils;

/**
 * 메인 데이터 소스 설정
 *
 * <p>Writer/Reader 분리 라우팅, JPA, MyBatis, QueryDSL 통합 설정
 * {@code @Transactional(readOnly = true)} 여부에 따라 자동으로 데이터 소스 라우팅
 *
 * <p>JPA Auditing 은 {@link com.example.api.ApiApplication} 의 {@code @EnableJpaAuditing} 에서만 활성화
 */
@Slf4j
@AllArgsConstructor
@EnableTransactionManagement
@MapperScan(
    annotationClass = Mapper.class,
    basePackages = MainDataSourceConfig.MYBATIS_MAPPER_PACKAGES,
    sqlSessionFactoryRef = MainDataSourceConfig.SESSION_FACTORY_BEAN
)
@EnableJpaRepositories(
    basePackages = MainDataSourceConfig.JPA_REPOSITORY_PACKAGES,
    entityManagerFactoryRef = MainDataSourceConfig.ENTITY_MANAGER_FACTORY,
    transactionManagerRef = MainDataSourceConfig.TX_MANAGER
)
@Configuration
public class MainDataSourceConfig {

  public static final String DATASOURCE_PREFIX = "main";
  private static final String DATASOURCE_PROPERTY_PREFIX = "main.datasource";

  private static final String DATASOURCE_BEAN_NAME = "DataSource";

  public static final String WRITER_DATASOURCE_BEAN = DATASOURCE_PREFIX + "WriterDataSource";
  public static final String READER_DATASOURCE_BEAN = DATASOURCE_PREFIX + "ReaderDataSource";
  public static final String ROUTING_DATASOURCE = DATASOURCE_PREFIX + DATASOURCE_BEAN_NAME;

  public static final String SESSION_FACTORY_BEAN = DATASOURCE_PREFIX + "SessionFactory";
  public static final String ENTITY_MANAGER_FACTORY = DATASOURCE_PREFIX + "EntityManagerFactory";
  public static final String TX_MANAGER = DATASOURCE_PREFIX + "TransactionManager";

  public static final String MYBATIS_MAPPER_PACKAGES = Constants.BASE_PACKAGE + ".**.mapper";
  private static final String[] JPA_ENTITY_PACKAGES = {Constants.BASE_PACKAGE + ".**.entity"};
  public static final String JPA_REPOSITORY_PACKAGES = Constants.BASE_PACKAGE + ".**.repository";

  /**
   * 데이터 소스 속성 설정
   *
   * @param url Writer JDBC URL
   * @return 설정된 {@link DataSourceProperties} 객체
   */
  @Primary
  @ConfigurationProperties(prefix = DATASOURCE_PROPERTY_PREFIX)
  @Bean
  public DataSourceProperties dataSourceProperties(
      @Value("${" + DATASOURCE_PROPERTY_PREFIX + ".writer-jdbc-url}") String url) {
    DataSourceProperties dataSourceProperties = new DataSourceProperties();
    dataSourceProperties.setUrl(url);
    return dataSourceProperties;
  }

  /**
   * Writer 데이터 소스 설정
   *
   * @param dataSourceProperties 데이터 소스 속성
   * @return HikariCP 기반의 Writer {@link DataSource}
   */
  @Primary
  @ConfigurationProperties(prefix = DATASOURCE_PROPERTY_PREFIX + ".hikari")
  @Bean(name = WRITER_DATASOURCE_BEAN)
  public DataSource writerDataSource(DataSourceProperties dataSourceProperties) {
    return dataSourceProperties
        .initializeDataSourceBuilder()
        .type(HikariDataSource.class)
        .build();
  }

  /**
   * Reader 데이터 소스 설정
   *
   * <p>Writer 데이터 소스의 HikariCP 설정을 상속받아 URL만 변경
   *
   * @param url        Reader JDBC URL
   * @param dataSource Writer 데이터 소스
   * @return Reader {@link DataSource}
   */
  @Bean(READER_DATASOURCE_BEAN)
  public DataSource readDataSource(
      @Value("${" + DATASOURCE_PROPERTY_PREFIX + ".reader-jdbc-url}") String url,
      @Qualifier(DATASOURCE_PREFIX + "WriterDataSource") DataSource dataSource) {
    return DataSourceBuilder.derivedFrom(dataSource).url(url).build();
  }

  /**
   * Read/Write 라우팅 데이터 소스 설정
   *
   * <p>{@code @Transactional(readOnly = true)} 여부에 따라 자동 라우팅
   *
   * @param writerDataSource Writer 데이터 소스
   * @param readDataSource   Reader 데이터 소스
   * @return 라우팅 {@link DataSource}
   */
  @Primary
  @Bean(name = ROUTING_DATASOURCE)
  public DataSource dataSource(
      @Qualifier(WRITER_DATASOURCE_BEAN) DataSource writerDataSource,
      @Qualifier(READER_DATASOURCE_BEAN) DataSource readDataSource) {
    LazyConnectionDataSourceProxy proxy = new LazyConnectionDataSourceProxy(writerDataSource);
    proxy.setReadOnlyDataSource(readDataSource);
    return proxy;
  }

  /**
   * MyBatis SqlSessionFactory 설정
   *
   * @param dataSource 라우팅 데이터 소스
   * @return 설정된 {@link SqlSessionFactory} 객체
   * @throws Exception SqlSessionFactory 생성 실패 시
   */
  @Bean(name = SESSION_FACTORY_BEAN)
  public SqlSessionFactory sessionFactory(
      @Qualifier(ROUTING_DATASOURCE) DataSource dataSource) throws Exception {

    SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
    factoryBean.setDataSource(dataSource);
    factoryBean.setTypeAliases(getTypeAliases(Constants.BASE_PACKAGE));

    try {
      Resource[] mapperLocations =
          new PathMatchingResourcePatternResolver().getResources("classpath:mapper/**/*.xml");

      if (!ObjectUtils.isEmpty(mapperLocations)) {
        factoryBean.setMapperLocations(mapperLocations);
      }
    } catch (IOException e) {
      log.warn("mapper load fail: {}", e.getMessage());
    }

    return factoryBean.getObject();
  }

  /**
   * {@link Alias} 어노테이션이 붙은 클래스를 스캔하여 MyBatis 타입 별칭 등록
   *
   * @param basePackage 스캔 대상 기본 패키지
   * @return 타입 별칭 클래스 배열
   * @throws ClassNotFoundException 클래스 로드 실패 시
   */
  private Class<?>[] getTypeAliases(String basePackage) throws ClassNotFoundException {
    ClassPathScanningCandidateComponentProvider provider =
        new ClassPathScanningCandidateComponentProvider(false);
    provider.addIncludeFilter(new AnnotationTypeFilter(Alias.class));

    Set<Class<?>> typeAliases = new HashSet<>();
    for (var bean : provider.findCandidateComponents(basePackage)) {
      Class<?> clazz = Class.forName(bean.getBeanClassName());
      typeAliases.add(clazz);
    }
    return typeAliases.toArray(new Class[0]);
  }

  /**
   * MyBatis SqlSessionTemplate 설정
   *
   * @param factory SqlSessionFactory
   * @return {@link SqlSessionTemplate} 객체
   */
  @Bean(name = DATASOURCE_PREFIX + "SqlSessionTemplate")
  public SqlSessionTemplate sqlSessionTemplate(
      @Qualifier(SESSION_FACTORY_BEAN) SqlSessionFactory factory) {
    return new SqlSessionTemplate(factory);
  }


  /**
   * JPA 속성 설정
   *
   * @return 설정된 {@link JpaProperties} 객체
   */
  @Primary
  @Bean(DATASOURCE_PREFIX + "JpaProperties")
  @ConfigurationProperties(prefix = DATASOURCE_PREFIX + ".jpa")
  public JpaProperties jpaProperties() {
    return new JpaProperties();
  }

  /**
   * JPA 엔티티 매니저 팩토리 설정
   *
   * @param dataSource    데이터 소스
   * @param jpaProperties JPA 관련 속성
   * @return 설정된 {@link LocalContainerEntityManagerFactoryBean}
   */
  @Primary
  @Bean(name = ENTITY_MANAGER_FACTORY)
  public LocalContainerEntityManagerFactoryBean entityManagerFactory(
      @Qualifier(DATASOURCE_PREFIX + DATASOURCE_BEAN_NAME) DataSource dataSource,
      @Qualifier(DATASOURCE_PREFIX + "JpaProperties") JpaProperties jpaProperties) {
    return this.entityManagerFactoryBuilder(jpaProperties)
        .dataSource(dataSource)
        .packages(JPA_ENTITY_PACKAGES)
        .properties(jpaProperties.getProperties())
        .persistenceUnit(DATASOURCE_PREFIX + "EntityManager")
        .build();
  }

  /**
   * 엔티티 매니저 팩토리 빌더 설정
   *
   * @param jpaProperties JPA 관련 속성 객체
   * @return 설정된 {@link EntityManagerFactoryBuilder}
   */
  private EntityManagerFactoryBuilder entityManagerFactoryBuilder(JpaProperties jpaProperties) {
    HibernateJpaVendorAdapter jpaVendorAdapter = new HibernateJpaVendorAdapter();
    jpaVendorAdapter.setGenerateDdl(jpaProperties.isGenerateDdl());
    jpaVendorAdapter.setShowSql(jpaProperties.isShowSql());
    jpaVendorAdapter.setDatabasePlatform(jpaProperties.getDatabasePlatform());
    return new EntityManagerFactoryBuilder(
        jpaVendorAdapter,
        dataSource -> jpaProperties.getProperties(),
        null
    );
  }

  /**
   * 트랜잭션 매니저 설정
   *
   * @param entityManagerFactory JPA 엔티티 매니저 팩토리
   * @return JPA 기반의 {@link PlatformTransactionManager}
   */
  @Primary
  @Bean(name = TX_MANAGER)
  public PlatformTransactionManager transactionManager(
      @Qualifier(ENTITY_MANAGER_FACTORY)
      LocalContainerEntityManagerFactoryBean entityManagerFactory) {
    JpaTransactionManager jpaTransactionManager = new JpaTransactionManager();
    jpaTransactionManager.setEntityManagerFactory(entityManagerFactory.getObject());
    return jpaTransactionManager;
  }

  /**
   * JPA 및 QueryDSL 설정을 위한 내부 클래스
   */
  @Configuration
  class QuerydslConfig {

    @PersistenceContext(unitName = DATASOURCE_PREFIX + "EntityManager")
    private EntityManager mainEntityManager;

    /**
     * JPAQueryFactory 빈 설정
     *
     * @return {@link JPAQueryFactory} 객체
     */
    @Bean
    public JPAQueryFactory mainJpaQueryFactory() {
      return new JPAQueryFactory(mainEntityManager);
    }
  }
}
