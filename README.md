# Overview

Testing setup and docs for using Redis Sentinel (http://redis.io/topics/sentinel) with Jedis java client proxying through an F5 Big-IP load balancer.

# Setup

Ensure you have the very latest Redis master compiled and installed.

Clone this repo, then grab some dependencies:

```
cd redis-sentinel-tests
wget https://github.com/downloads/xetorthio/jedis/jedis-2.1.0.jar
wget http://www.fightrice.com/mirrors/apache//commons/pool/binaries/commons-pool-1.6-bin.tar.gz
tar -zxf commons-pool-1.6-bin.tar.gz commons-pool-1.6/commons-pool-1.6.jar
mv commons-pool-1.6/commons-pool-1.6.jar ./
rm commons-pool-1.6-bin.tar.gz
rmdir commons-pool-1.6
```

Open a bunch of shells and fire up redis instances:

```
redis-server redis-master.conf
redis-server redis-slave1.conf

redis-sentinel sent1.conf
redis-sentinel sent2.conf
redis-sentinel sent3.conf
```

You should see the slave connect to the master and sync, and the sentinels connect to the master, discover one another, and find the slave.

Note the load-balancer config is outside the scope of this doc but basically you just need a virtual server that has the master and slave instances as pool members and monitors them for "up" status by checking "info" output for "role:master".

# Run some tests

Now we can run our test client:

```
export CLASSPATH=$CLASSPATH:commons-pool-1.6.jar:jedis-2.1.0.jar
javac Test.java 
java Test myvirtualip 100
```

The second argument is the delay in ms between redis write commands. That will tick happily along and now you can start creating mayhem.

1. Kill the master (kill -9 or ctrl-c)
1. Watch sentinel recognize it and promote the slave to master
1. Watch your load balancer detect the failed master and the newly "up" slave
1. Watch the test code spit out x's for a while during the transition, indicating failed redis commands
1. Watch the test code recover and output a line about how long it was blocked and total failed commands

Now, edit the redis-master.conf, set it to slaveof the new master (slave1, port 6380) and restart the master instance.

1. Watch as the sentinels detect the new slave
1. Kill the new master (port 6380) 
1. Watch sentinels elect the original master again
1. Watch client reconnect
1. Declare victory!

# TODO/Issues

* Killing both instances will result in a state that sentinels will never recover from
* What's a safe setting for down-after-milliseconds? will setting this too low cause false positives when there are slow operations (e.g. big sort)?
* (fixed) starting a downed master back up w/o reconfig'ing as slave results in sentinel confusion
* Since the LB is only checking "role:master" on each instance, a poorly config'd instance can result in 2 masters being "up"
