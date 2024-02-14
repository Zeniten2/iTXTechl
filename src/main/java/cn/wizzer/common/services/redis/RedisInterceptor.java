package cn.wizzer.common.services.redis;

import org.nutz.aop.InterceptorChain;
import org.nutz.aop.MethodInterceptor;
import org.nutz.integration.jedis.JedisAgent;
import org.nutz.lang.Streams;
import redis.clients.jedis.Jedis;

/**
 * Created by Wizzer on 2016/7/31.
 */
public class RedisInterceptor implements MethodInterceptor {

    protected JedisAgent jedisAgent;

    protected static ThreadLocal<Jedis> TL = new ThreadLocal<Jedis>();

    public void filter(InterceptorChain chain) throws Throwable {
        if (TL.get() != null) {
            chain.doChain();
            return;
        }
        Jedis jedis = null;
        try {
            jedis = jedisAgent.jedis();
            TL.set(jedis);
            chain.doChain();
        } finally {
            Streams.safeClose(jedis);
            TL.remove();
        }
    }


    public static Jedis jedis() {
        return TL.get();
    }

    public void setJedisAgent(JedisAgent jedisAgent) {
        this.jedisAgent = jedisAgent;
    }
}
