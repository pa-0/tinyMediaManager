package org.tinymediamanager.scraper.thesportsdb;

import org.junit.Test;
import org.tinymediamanager.core.BasicITest;
import org.tinymediamanager.core.tvshow.TvShowSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.MediaSearchResult;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.assertj.core.api.Assertions.assertThat;

public class ITSportsDbMetadataProviderTest extends BasicITest {

    @Test
    public void providerInfoTest() {
        try {
            SportsDbMetadataProvider mp = new SportsDbTvShowMetadataProvider();
            MediaProviderInfo providerInfo = mp.getProviderInfo();

            assertNotNull(providerInfo.getDescription());
            assertNotNull(providerInfo.getId());
            assertNotNull(providerInfo.getName());

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testEventSearch() throws Exception {
        SportsDbTvShowMetadataProvider mp = new SportsDbTvShowMetadataProvider();
        TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
        options.setSearchQuery("Arsenal vs Chelsea");
        List<MediaSearchResult> resultList = new ArrayList<>(mp.search(options));
        assertNotNull(resultList);
        assertThat(resultList.get(0).getTitle()).isEqualTo("Arsenal vs Chelsea");
    }

}
