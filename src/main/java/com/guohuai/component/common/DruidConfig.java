package com.guohuai.component.common;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.support.http.StatViewServlet;
import com.alibaba.druid.support.http.WebStatFilter;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * druid 配置.
 * 
 *
 * 
 * 这样的方式不需要添加注解：@ServletComponentScan
 * 
 * @author Administrator
 * 
 *
 */
@Configuration
@Slf4j
public class DruidConfig {

    @Value("${spring.datasource.url}")
    private String dbUrl;
    
    @Value("${spring.datasource.username}")
    private String username;
    
    @Value("${spring.datasource.password}")
    private String password;
    
    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;
    
    @Value("${spring.datasource.initial-size:10}")
    private int initialSize;
    
    @Value("${spring.datasource.min-idle:10}")
    private int minIdle;
    
    @Value("${spring.datasource.max-active:100}")
    private int maxActive;
    
    @Value("${spring.datasource.max-wait:10000}")
    private int maxWait;
    
    @Value("${spring.datasource.time-between-eviction-runs-millis:10000}")
    private int timeBetweenEvictionRunsMillis;
    
    @Value("${spring.datasource.min-evictable-idle-time-millis:180000}")
    private int minEvictableIdleTimeMillis;
    
    @Value("${spring.datasource.validation-query:select 1}")
    private String validationQuery;
    
    @Value("${spring.datasource.test-while-idle:true}")
    private boolean testWhileIdle;
    
    @Value("${spring.datasource.test-on-borrow:true}")
    private boolean testOnBorrow;
    
    @Value("${spring.datasource.testOnReturn:false}")
    private boolean testOnReturn;
    
    @Value("${spring.datasource.poolPreparedStatements:true}")
    private boolean poolPreparedStatements;
    
    @Value("${spring.datasource.maxPoolPreparedStatementPerConnectionSize:20}")
    private int maxPoolPreparedStatementPerConnectionSize;
    
    @Value("${spring.datasource.filters:stat,wall}")
    private String filters;
    
    @Value("${spring.datasource.connectionProperties:druid.stat.slowSqlMillis=1000}")
    private String connectionProperties;
    
    @Bean     //声明其为Bean实例
    @Primary  //在同样的DataSource中，首先使用被标注的DataSource
    public DataSource dataSource(){
    	log.info("dataSource:Druid");
    	DruidDataSource datasource = new DruidDataSource();
    	
    	datasource.setUrl(this.dbUrl);
    	datasource.setUsername(username);
    	datasource.setPassword(password);
    	datasource.setDriverClassName(driverClassName);
    	
    	//configuration
    	datasource.setInitialSize(initialSize);
    	datasource.setMinIdle(minIdle);
    	datasource.setMaxActive(maxActive);
    	datasource.setMaxWait(maxWait);
    	datasource.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
    	datasource.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
    	datasource.setValidationQuery(validationQuery);
    	datasource.setTestWhileIdle(testWhileIdle);
    	datasource.setTestOnBorrow(testOnBorrow);
    	datasource.setTestOnReturn(testOnReturn);
    	datasource.setPoolPreparedStatements(poolPreparedStatements);
    	datasource.setMaxPoolPreparedStatementPerConnectionSize(maxPoolPreparedStatementPerConnectionSize);
    	try {
			datasource.setFilters(filters);
		} catch (SQLException e) {
//			log.error("druid configuration initialization filter", e);
		}
    	datasource.setConnectionProperties(connectionProperties);
    	
    	return datasource;
    }
	/**
	 * 
	 * 注册一个StatViewServlet
	 * 
	 * @return
	 */

	@Bean
	public ServletRegistrationBean DruidStatViewServle2() {
		ServletRegistrationBean servletRegistrationBean = new ServletRegistrationBean(
				new StatViewServlet(), "/druid/*");
		// 添加初始化参数：initParams
		// 白名单：
		servletRegistrationBean.addInitParameter("allow", "127.0.0.1");

		// IP黑名单 (存在共同时，deny优先于allow) : 如果满足deny的话提示:Sorry, you are not
		// permitted to view this page.
		servletRegistrationBean.addInitParameter("deny", "192.168.1.73");

		// 登录查看信息的账号密码.
		servletRegistrationBean.addInitParameter("loginUsername", "admin2");
		servletRegistrationBean.addInitParameter("loginPassword", "123456");

		// 是否能够重置数据.
		servletRegistrationBean.addInitParameter("resetEnable", "false");
		return servletRegistrationBean;
	}

	/**
	 * 
	 * 注册一个：filterRegistrationBean
	 * 
	 * @return
	 */

	@Bean
	public FilterRegistrationBean druidStatFilter2() {
		FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean(
				new WebStatFilter());
		// 添加过滤规则.
		filterRegistrationBean.addUrlPatterns("/*");

		// 添加不需要忽略的格式信息.
		filterRegistrationBean.addInitParameter("exclusions",
				"*.js,*.gif,*.jpg,*.png,*.css,*.ico,/druid/*");

		filterRegistrationBean.addInitParameter("profileEnabled", "true");

		return filterRegistrationBean;
	}
}
