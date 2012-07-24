import redis.clients.jedis.*;
import java.util.Date;


public class Test {

    public static void main(String argv[]) {
	Long sleepMillis = 50L;
	try {
	    sleepMillis = Long.parseLong(argv[0]);
	}
	catch (Exception e) {}

	JedisPoolConfig conf = new JedisPoolConfig();
	conf.setMaxWait(1000);
	conf.setTestOnBorrow(true);
	conf.setMaxActive(10);
	
	JedisPool pool = new JedisPool(conf, "10.1.10.11", 6379, 1000, null);
	Jedis jedis = null;
	int i=0;
	int fails = 0;
	long downTime = 0;
	while (true) {
	    try {
		Thread.currentThread().sleep(sleepMillis);
		jedis = pool.getResource();
		jedis.set("foo", "bar");
		String value = jedis.get("foo");
		System.out.print(".");
		if (i++ % 100 == 0) {
		    System.out.println("\n" + new Date());
		}
		if (fails != 0) {
		    long duration = System.currentTimeMillis() - downTime;
		    System.out.println("\n" + new Date() + ", SERVER UP, was down for " + duration + "ms, failed commands: " + fails);
		    fails = 0;
		}
	    }
	    catch(Exception e) {
		if (fails == 0) {
		    downTime = System.currentTimeMillis();
		    System.out.println("\n" + new Date() + ", SERVER DOWN");
		}
		fails++;
		//System.out.println("\n" + new Date() + ", " + e.getMessage());
		System.out.print("x");
	    }
	    finally {
		if (jedis != null) {
		    pool.returnResource(jedis);
		}
	    }

	}
    }
}