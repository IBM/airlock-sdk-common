package com.ibm.airlock.common.cache;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class InMemoryCacheTest {


    @Test
    public void setDefaultExpirationPeriod() throws InterruptedException {
        InMemoryCache.setDefaultExpirationPeriod(10 * 1000);
        InMemoryCache inMemoryCache = new InMemoryCache();
        inMemoryCache.put("default", "value");
        inMemoryCache.put("20000", "value", 20000);
        inMemoryCache.put("1000", "value", 1000);
        Thread.sleep(5000);
        Assert.assertNotNull(inMemoryCache.get("default"));
        Assert.assertNotNull(inMemoryCache.get("20000"));
        Assert.assertNull(inMemoryCache.get("1000"));
        Thread.sleep(15000);
        Assert.assertNull(inMemoryCache.get("default"));
        Assert.assertNotNull(inMemoryCache.get("20000"));
    }

    @Test
    public void updateKey() throws InterruptedException {
        InMemoryCache.setDefaultExpirationPeriod(5 * 1000);
        InMemoryCache inMemoryCache = new InMemoryCache();
        inMemoryCache.put("10000", "value", 10000);
        Thread.sleep(7000);
        inMemoryCache.put("10000", "value", 10000);
        Thread.sleep(7000);
        Assert.assertEquals(1, inMemoryCache.size());
        Assert.assertNotNull(inMemoryCache.get("10000"));
    }

    @Test
    public void put() throws InterruptedException {
        InMemoryCache.setDefaultExpirationPeriod(10 * 1000);
        InMemoryCache inMemoryCache = new InMemoryCache();
        inMemoryCache.put("default", "value");
        inMemoryCache.put("20000", "value", 20000);
        inMemoryCache.put("20000", "value", 1000);
        Assert.assertEquals(2, inMemoryCache.size());
        Thread.sleep(5000);
        Assert.assertNull(inMemoryCache.get("20000"));
    }


    @Test
    public void removeAfterPutTheDSameKey() throws InterruptedException {
        InMemoryCache inMemoryCache = new InMemoryCache();
        inMemoryCache.put("20000", "value", 20000);
        Thread.sleep(5000);
        inMemoryCache.put("20000", "value", 20000);
        Assert.assertEquals(1, inMemoryCache.size());
        inMemoryCache.remove("20000");
        Assert.assertEquals(0, inMemoryCache.size());
    }


    @Test
    public void remove() throws InterruptedException {
        InMemoryCache inMemoryCache = new InMemoryCache();
        inMemoryCache.put("default", "value");
        inMemoryCache.put("20000", "value", 20000);
        inMemoryCache.put("1000", "value", 1000);
        Assert.assertNotNull(inMemoryCache.get("20000"));
        inMemoryCache.remove("20000");
        Assert.assertNull(inMemoryCache.get("20000"));
    }


    @Test
    public void containsKey() throws InterruptedException {
        InMemoryCache inMemoryCache = new InMemoryCache();
        inMemoryCache.put("default", "value");
        inMemoryCache.put("20000", "value", 20000);
        inMemoryCache.put("1000", "value", 1000);
        Assert.assertTrue(inMemoryCache.containsKey("1000"));
        Thread.sleep(6000);
        Assert.assertFalse(inMemoryCache.containsKey("1000"));
    }

    @Test
    public void clear() {
        InMemoryCache inMemoryCache = new InMemoryCache();
        inMemoryCache.put("default", "value");
        inMemoryCache.put("20000", "value", 20000);
        inMemoryCache.put("1000", "value", 1000);
        Assert.assertTrue(inMemoryCache.size() == 3);
        inMemoryCache.clear();
        Assert.assertTrue(inMemoryCache.size() == 0);
    }

    @Test
    public void keySet() {
        InMemoryCache inMemoryCache = new InMemoryCache();
        inMemoryCache.put("default", "value");
        inMemoryCache.put("20000", "value", 20000);
        inMemoryCache.put("1000", "value", 1000);
        Assert.assertTrue(inMemoryCache.keySet().size() == 3);
    }
}