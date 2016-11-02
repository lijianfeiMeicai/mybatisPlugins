如何使用：
    在mybatis config文件中添加：
        <plugins>
        <plugin interceptor="com.sprucetec.dmallgws.common.interceptors.MybatisQueryConsumeTimePlugin">
            <property name="slowQueryLimitMs" value="1000"/>
            <property name="com.sprucetec.dmallgws.periodism.dao.GoodsPcpBatchDao.getListByStoreId" value="5"/>
        </plugin>   
    </plugins>

    说明：
　name=slowQueryLimitMs：查询超过这个设置的select语句会打印，单位：毫秒．默认1000．输入值无法解析时按默认 值处理．
    name=查询dao的全限定名：该设置会覆盖slowQueryLimitMs配置，输入的值无法解析时按slowQueryLimitMs值处理．
  
    打印样式：
      2016-06-24 21:16:14 WARN  jeff.lee.mybaitis.plugins.MybatisQueryConsumeTimePlugin intercept:42 - qualifiedName:com.example.Dao.get
sql:select * from table where id=? and is_deleted=0
         
            and status=?
         
         
            and start_time>?
         
        order by id desc
params:{id=1466774174, status=1, start_time=1466774174}
 consume times(ms):8