package com.ibm.airlock.common.cache;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.lang.ref.SoftReference;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class InMemoryCache<K, T> implements Cacheable<K, T> {

    private static final ConcurrentHashMap<String, InMemoryCache> caches = new ConcurrentHashMap<>();
    private static long DEFAULT_EXPIRATION_PERIOD = Long.MAX_VALUE;
    private static boolean isEnabled = true;
    private final ConcurrentHashMap<K, SoftReference<T>> cache = new ConcurrentHashMap<>();
    private final DelayQueue<DelayedCacheEntry<K, T>> cleaningUpQueue = new DelayQueue<>();
    private long expirationPeriod = DEFAULT_EXPIRATION_PERIOD;

    static {
        Thread cleanerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted() && isEnabled) {
                    try {
                        for (InMemoryCache cache : caches.values()) {
                            if (!cache.cleaningUpQueue.isEmpty()) {
                                DelayedCacheEntry delayedCacheObject =
                                        (DelayedCacheEntry) cache.cleaningUpQueue.poll(10, TimeUnit.MICROSECONDS);
                                if (delayedCacheObject != null) {
                                    cache.cache.remove(delayedCacheObject.getKey(), delayedCacheObject.getReference());
                                }
                            }
                        }
                        Thread.sleep(2000);
                    } catch (Exception e) {
                        Thread.currentThread().interrupt();
                    }
                }

            }
        });
        cleanerThread.setDaemon(true);
        cleanerThread.start();
    }

    public static void setIsEnabled(boolean p_IsEnabled) {
        isEnabled = p_IsEnabled;
    }

    public static void setDefaultExpirationPeriod(long defaultExpirationPeriod) {
        DEFAULT_EXPIRATION_PERIOD = defaultExpirationPeriod;
    }


    public InMemoryCache() {
        caches.put(UUID.randomUUID().toString(), this);
    }


    public InMemoryCache(long expirationPeriod) {
        caches.put(UUID.randomUUID().toString(), this);
        this.expirationPeriod = expirationPeriod;
    }

    @Override
    public void put(K key, @Nullable T value) {
        put(key, value, expirationPeriod);
    }

    @Override
    public void put(K key, @Nullable T value, long periodInMillis) {
        if (value == null) {
            cache.remove(key);
        } else {
            long expiryTime = System.currentTimeMillis() + periodInMillis;
            SoftReference<T> reference = new SoftReference<>(value);
            cache.put(key, reference);
            cleaningUpQueue.put(new DelayedCacheEntry<>(key, reference, expiryTime));
        }
    }

    @Override
    public void remove(K key) {
        cache.remove(key);
    }

    @Override
    @CheckForNull
    public T get(K key) {
        return cache.get(key) == null ? null : cache.get(key).get();
    }

    @Override
    public boolean containsKey(K key) {
        SoftReference<T> value = this.cache.get(key);
        return value != null && value.get() != null;
    }

    @Override
    public void clear() {
        cache.clear();
    }

    @Override
    public long size() {
        return cache.size();
    }

    /**
     * Note: this class has a natural ordering that is inconsistent with equals.
     */
    private static class DelayedCacheEntry<K, T> implements Delayed {


        private final K key;
        private final SoftReference<T> reference;
        private final long expiryTime;

        private DelayedCacheEntry(K key, SoftReference<T> reference, long expiryTime) {
            this.key = key;
            this.reference = reference;
            this.expiryTime = expiryTime;
        }

        K getKey() {
            return key;
        }

        SoftReference<T> getReference() {
            return reference;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(expiryTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            return Long.compare(expiryTime, ((DelayedCacheEntry) o).expiryTime);
        }
    }

    @Override
    public Set<K> keySet() {
        try {
            return cache.keySet();
        } catch (NoSuchMethodError e) {
            //On android old versions the java do not support keySet interface...
            Set<K> keys = new HashSet<>();
            Enumeration<K> keysEnum = cache.keys();
            while (keysEnum.hasMoreElements()) {
                keys.add(keysEnum.nextElement());
            }
            return keys;
        }
    }

}
