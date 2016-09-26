/*
 *
 *  * Copyright 2016 EMBL - European Bioinformatics Institute
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package embl.ebi.variation.eva.vcfdump.regionutils;

import embl.ebi.variation.eva.vcfdump.VariantExporterTestDB;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opencb.biodata.models.feature.Region;
import org.opencb.datastore.core.QueryOptions;
import org.opencb.opencga.lib.auth.IllegalOpenCGACredentialsException;
import org.opencb.opencga.storage.core.variant.adaptors.VariantDBAdaptor;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by pagarcia on 31/05/2016.
 */
public class RegionFactoryTest {

    @BeforeClass
    public static void setUpClass() throws IOException, InterruptedException, URISyntaxException, IllegalAccessException, ClassNotFoundException, InstantiationException, IllegalOpenCGACredentialsException {
        VariantExporterTestDB.cleanDBs();
        VariantExporterTestDB.fillDB();
    }

    @AfterClass
    public static void tearDownClass() throws UnknownHostException {
        VariantExporterTestDB.cleanDBs();
    }

    @Test
    public void getRegionsForChromosome() {
        QueryOptions query = new QueryOptions(VariantDBAdaptor.REGION, "1:500-2499,2:100-300");
        RegionFactory regionFactory = new RegionFactory(1000, null, query);

        // chromosome that are in region list
        List<Region> chunks = regionFactory.getRegionsForChromosome("1");
        assertEquals(2, chunks.size());
        assertTrue(chunks.contains(new Region("1:500-1499")));
        assertTrue(chunks.contains(new Region("1:1500-2499")));
        chunks = regionFactory.getRegionsForChromosome("2");
        assertEquals(1, chunks.size());
        assertTrue(chunks.contains(new Region("2:100-300")));

        // chromosome that are not in region list
        chunks = regionFactory.getRegionsForChromosome("22");
        assertEquals(0, chunks.size());
    }

    @Test
    public void divideRegionInChunks() {
        RegionFactory regionFactory = new RegionFactory(1000, null, null);
        List<Region> regions = regionFactory.divideRegionInChunks("1", 500, 1499);
        assertTrue(regions.size() == 1);
        assertTrue(regions.contains(new Region("1", 500, 1499)));

        regions = regionFactory.divideRegionInChunks("1", 500, 1000);
        assertTrue(regions.size() == 1);
        assertTrue(regions.contains(new Region("1", 500, 1000)));

        regions = regionFactory.divideRegionInChunks("1", 2500, 2500);
        assertTrue(regions.size() == 1);
        assertTrue(regions.contains(new Region("1", 2500, 2500)));

        regions = regionFactory.divideRegionInChunks("1", 500, 2499);
        assertTrue(regions.size() == 2);
        assertTrue(regions.contains(new Region("1", 500, 1499)));
        assertTrue(regions.contains(new Region("1", 1500, 2499)));

        regions = regionFactory.divideRegionInChunks("1", 500, 2000);
        assertTrue(regions.size() == 2);
        assertTrue(regions.contains(new Region("1", 500, 1499)));
        assertTrue(regions.contains(new Region("1", 1500, 2000)));

        regions = regionFactory.divideRegionInChunks("1", 500, 2500);
        assertTrue(regions.size() == 3);
        assertTrue(regions.contains(new Region("1", 500, 1499)));
        assertTrue(regions.contains(new Region("1", 1500, 2499)));
        assertTrue(regions.contains(new Region("1", 2500, 2500)));

        regions = regionFactory.divideRegionInChunks("1", -1, 2500);
        assertTrue(regions.size() == 0);

        regions = regionFactory.divideRegionInChunks("1", 3000, 2500);
        assertTrue(regions.size() == 0);
    }

    @Test
    public void executeQueryForGettingMinAndMaxCoordinatesWhenTheyAreNotIncludedInTheRegionFilter() throws IOException, IllegalOpenCGACredentialsException {
        // choose a big window size to avoid a huge number of regions and a potential OutOfMemory exception if the test fails
        int windowSize = 100000000;

        VariantDBAdaptor variantDBAdaptor = VariantExporterTestDB.getVariantMongoDBAdaptor(VariantExporterTestDB.TEST_DB_NAME);
        QueryOptions query = new QueryOptions(VariantDBAdaptor.REGION, "1:500-2499,22,21:1000-2000");
        RegionFactory regionFactory = new RegionFactory(windowSize, variantDBAdaptor, query);

        // chromosome 1 has coordinates in the region filter, they should not change
        List<Region> regions = regionFactory.getRegionsForChromosome("1");
        assertTrue(regions.size() == 1);
        assertTrue(regions.contains(new Region("1", 500, 2499)));

        // chromosome 22 in the region filter has no coordinates, so the coordinates will be retrieved from the database
        regions = regionFactory.getRegionsForChromosome("22");
        assertTrue(regions.size() == 1);
        assertTrue(regions.contains(new Region("22", 16050075, 16110950)));

        // chromosome 21 has coordinates in the region filter, they should not change
        regions = regionFactory.getRegionsForChromosome("21");
        assertTrue(regions.size() == 1);
        assertTrue(regions.contains(new Region("21", 1000, 2000)));
    }
}