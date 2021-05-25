package com.ibm.airlock.common.cache;

import com.ibm.airlock.common.util.LocaleProvider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.Locale;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * @author Denis Voloshin on 2019-06-10.
 */
public class UnsupportedCountryCodeTest {

    @Spy
    CacheManager cacheManager;
    @Mock
    LocaleProvider localeProvider;

    @Mock
    PersistenceHandler persistenceHandler;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(localeProvider.getLocale()).thenReturn(new Locale("c", "GH"));
        when(persistenceHandler.read(any(String.class), any(String.class))).thenReturn(" [\"en\", \"ar\", \"ca\", \"cs\", \"da\", \"de\", \"el\", " +
                "\"en_AU\", \"en_GB\", \"es\", \"fa\", \"fi\", \"fr\", \"fr_CA\", \"hi\", \"hr\", \"hu\", \"in\"," +
                " \"it\", \"iw\", \"ja\", \"ko\", \"ms\", \"nb\", \"nl\", \"no\", \"pl\", \"pt\", \"pt_BR\", \"ro\", \"ru\", " +
                "\"sk\", \"sv\", \"th\", \"tr\", \"uk\", \"vi\", \"zh_CN\", \"zh_TW\"]");
        cacheManager.setLocaleProvider(localeProvider);
        cacheManager.setPersistenceHandler(persistenceHandler);
    }

    @Test
    public void shouldReturnTheFirstZHLangauge() {
        Assert.assertEquals("CN", cacheManager.getSupportedCountryByLanguage("zh"));
    }
}
