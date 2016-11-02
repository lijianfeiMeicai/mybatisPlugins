package jeff.lee.mybaitis.plugins;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.log4j.Logger;

@Intercepts({
		@Signature(type = Executor.class, method = "query", args = { MappedStatement.class, Object.class, RowBounds.class,
				ResultHandler.class, CacheKey.class, BoundSql.class }),
		@Signature(type = Executor.class, method = "query", args = { MappedStatement.class, Object.class, RowBounds.class,
				ResultHandler.class }) })
public class MybatisQueryConsumeTimePlugin implements Interceptor {
	private static final Logger logger = Logger.getLogger(MybatisQueryConsumeTimePlugin.class);

	private Properties properties = new Properties();
	private static long defaultSlowQueryLimitMs = 1000;// 默认慢查询耗时(ms)

	private static Map<String, Long> qualifiedNameLimitMap = new HashMap<>();// 按限定名设置的慢查询时间

	@Override
	public Object intercept(Invocation invocation) throws Throwable {
		long currentTime = System.currentTimeMillis();
		SqlFootprintBean print = query(invocation.getArgs());
		Object proceed = invocation.proceed();
		long endTime = System.currentTimeMillis();
		long consumeTime = endTime - currentTime;
		print.setConsumeTime(consumeTime);
		if (consumeTime >= getSpecialLimit(print.getQualifiedName()))
			logger.warn(print.toString());
		return proceed;
	}

	@Override
	public Object plugin(Object target) {
		return Plugin.wrap(target, this);
	}

	@Override
	public void setProperties(Properties arg0) {
		if (arg0 != null)
			properties = arg0;
	}

	private long getSlowQueryLimitMs() {
		long slowQueryLimitMs = MybatisQueryConsumeTimePlugin.defaultSlowQueryLimitMs;
		if (properties != null) {
			String value = properties.getProperty("slowQueryLimitMs", "" + MybatisQueryConsumeTimePlugin.defaultSlowQueryLimitMs);
			try {
				slowQueryLimitMs = Long.parseLong(value.trim(), 10);
			} catch (NumberFormatException e) {
				logger.error("", e);
			}
		}

		return slowQueryLimitMs;
	}

	private Long getSpecialLimit(String qualifiedName) {
		if (qualifiedNameLimitMap.containsKey(qualifiedName)) {
			return qualifiedNameLimitMap.get(qualifiedName);
		} else {
			String value = properties.getProperty(qualifiedName);
			Long result = null;
			if (value == null) {
				result = getSlowQueryLimitMs();
			} else {
				try {
					result = Long.parseLong(value.trim(), 10);
				} catch (NumberFormatException e) {
					logger.error("", e);
				}
			}
			qualifiedNameLimitMap.put(qualifiedName, result);
			return result;
		}
	}

	private SqlFootprintBean query(Object[] args) {
		MappedStatement ms = (MappedStatement) args[0];
		String qualifiedName = ms.getId();
		Object obj = args[1];
		String params = obj == null ? "" : obj.toString();
		String sql = ms.getBoundSql(obj).getSql();
		SqlFootprintBean bean = new SqlFootprintBean();
		bean.setQualifiedName(qualifiedName);
		bean.setParams(params);
		bean.setSql(sql);
		return bean;
	}

	class SqlFootprintBean {
		private String qualifiedName;
		private String sql;
		private String params;
		private long consumeTime;

		public String getQualifiedName() {
			return qualifiedName;
		}

		public void setQualifiedName(String qualifiedName) {
			this.qualifiedName = qualifiedName;
		}

		public String getSql() {
			return sql;
		}

		public void setSql(String sql) {
			this.sql = sql;
		}

		public String getParams() {
			return params;
		}

		public void setParams(String params) {
			this.params = params;
		}

		public long getConsumeTime() {
			return consumeTime;
		}

		public void setConsumeTime(long consumeTime) {
			this.consumeTime = consumeTime;
		}

		@Override
		public String toString() {
			return "qualifiedName:" + this.qualifiedName + "\r\nsql:" + this.sql + "\r\nparams:" + this.getParams()
					+ "\r\n consume times(ms):" + this.consumeTime;
		}

	}
}

